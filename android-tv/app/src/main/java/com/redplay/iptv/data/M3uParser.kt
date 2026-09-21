package com.redplay.iptv.data

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset

object M3uParser {
    private val attrRegex = Regex("""([A-Za-z0-9_-]+)=(?:\"([^\"]*)\"|'([^']*)'|([^\s]+))""")
    private val allowedHeaders = mapOf(
        "user-agent" to "User-Agent",
        "referer" to "Referer",
        "referrer" to "Referer",
        "origin" to "Origin",
        "cookie" to "Cookie",
        "authorization" to "Authorization",
        "accept-language" to "Accept-Language",
    )

    fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte()) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte()) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16BE)
        }
        val utf8 = bytes.toString(Charsets.UTF_8)
        if (!utf8.contains('\uFFFD')) return utf8.removePrefix("\uFEFF")
        return bytes.toString(Charset.forName("windows-1251"))
    }

    fun parse(text: String, baseUrl: String): Playlist {
        val lines = text.removePrefix("\uFEFF").lineSequence()
        val channels = mutableListOf<Channel>()
        var epgUrl = ""
        var pending: Channel? = null
        var extGroup = ""

        for (rawLine in lines) {
            val line = rawLine.trim().trimEnd('\r')
            if (line.isBlank()) continue
            val upper = line.uppercase()
            when {
                upper.startsWith("#EXTM3U") -> {
                    val attrs = parseAttrs(line)
                    val raw = attrs["url-tvg"].orEmpty().ifBlank { attrs["x-tvg-url"].orEmpty() }
                    if (raw.isNotBlank()) epgUrl = resolveUrl(baseUrl, firstEpgUrl(raw))
                }
                upper.startsWith("#EXTINF:") -> {
                    val comma = extinfComma(line)
                    val meta = if (comma >= 0) line.substring(0, comma) else line
                    val display = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    val a = parseAttrs(meta)
                    val name = display.ifBlank { a["tvg-name"].orEmpty() }
                        .ifBlank { a["tvg-id"].orEmpty() }
                        .ifBlank { "Unnamed channel" }
                    val group = a["group-title"].orEmpty().ifBlank { extGroup }.ifBlank { "Other" }
                    val id = a["tvg-id"].orEmpty().ifBlank { a["tvg-name"].orEmpty() }.ifBlank { name }
                    pending = Channel(
                        id = id,
                        name = name,
                        tvgId = a["tvg-id"].orEmpty(),
                        tvgName = a["tvg-name"].orEmpty(),
                        logo = a["tvg-logo"].orEmpty(),
                        group = group,
                        language = a["tvg-language"].orEmpty(),
                        url = "",
                    )
                }
                upper.startsWith("#EXTGRP:") -> {
                    extGroup = line.substringAfter(':').trim()
                    pending = pending?.let { if (it.group == "Other" && extGroup.isNotBlank()) it.copy(group = extGroup) else it }
                }
                pending != null && upper.startsWith("#EXTVLCOPT:") -> {
                    val opt = line.substringAfter(':')
                    val index = opt.indexOf('=')
                    if (index > 0) {
                        val key = opt.substring(0, index).trim().lowercase()
                        val value = opt.substring(index + 1).trim()
                        val header = when (key) {
                            "http-user-agent" -> mapOf("User-Agent" to value)
                            "http-referrer", "http-referer" -> mapOf("Referer" to value)
                            "http-origin" -> mapOf("Origin" to value)
                            "http-cookie" -> mapOf("Cookie" to value)
                            else -> emptyMap()
                        }
                        pending = pending?.copy(headers = pending!!.headers + safeHeaders(header))
                    }
                }
                pending != null && upper.startsWith("#KODIPROP:INPUTSTREAM.ADAPTIVE.STREAM_HEADERS=") -> {
                    val headers = parseHeaderPairs(line.substringAfter('='))
                    pending = pending?.copy(headers = pending!!.headers + headers)
                }
                line.startsWith("#") -> Unit
                pending != null -> {
                    val (streamUrl, urlHeaders) = splitStreamUrlHeaders(line)
                    channels += pending!!.copy(
                        url = resolveUrl(baseUrl, streamUrl),
                        headers = pending!!.headers + urlHeaders,
                    )
                    pending = null
                }
            }
        }
        return Playlist(channels = channels, epgUrl = epgUrl)
    }

    fun safeHeaders(input: Map<String, String>): Map<String, String> = buildMap {
        for ((key, value) in input) {
            val canonical = allowedHeaders[key.trim().lowercase()] ?: continue
            val clean = value.trim()
            if (clean.isNotEmpty() && clean.length <= 8192) put(canonical, clean)
        }
    }

    fun resolveUrl(base: String, raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return ""
        return runCatching {
            val uri = URI(clean)
            if (uri.isAbsolute || base.isBlank()) uri.toString() else URI(base).resolve(uri).toString()
        }.getOrDefault(clean)
    }

    private fun parseAttrs(input: String): Map<String, String> = buildMap {
        attrRegex.findAll(input).forEach { match ->
            val groups = match.groupValues
            val value = groups[2].ifBlank { groups[3] }.ifBlank { groups[4] }
            put(groups[1].lowercase(), value)
        }
    }

    private fun extinfComma(line: String): Int {
        var quote: Char? = null
        for (i in line.indices) {
            val c = line[i]
            if (quote != null) {
                if (c == quote) quote = null
            } else if (c == '\'' || c == '"') {
                quote = c
            } else if (c == ',') {
                return i
            }
        }
        return -1
    }

    private fun firstEpgUrl(raw: String): String = raw.split(';', ',').firstOrNull { it.isNotBlank() }?.trim().orEmpty()

    private fun splitStreamUrlHeaders(raw: String): Pair<String, Map<String, String>> {
        val parts = raw.trim().split('|', limit = 2)
        return parts[0] to if (parts.size == 2) parseHeaderPairs(parts[1]) else emptyMap()
    }

    private fun parseHeaderPairs(raw: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        raw.replace('|', '&').split('&').forEach { pair ->
            val i = pair.indexOf('=')
            if (i > 0) {
                val key = URLDecoder.decode(pair.substring(0, i), "UTF-8")
                val value = URLDecoder.decode(pair.substring(i + 1), "UTF-8")
                out[key] = value
            }
        }
        return safeHeaders(out)
    }
}

fun normalizeName(value: String): String {
    val b = StringBuilder()
    var space = false
    value.trim().lowercase().forEach { c ->
        val ok = c.isLetterOrDigit() || c == 'ъ' || c == 'ь' || c == 'ѝ'
        if (ok) {
            b.append(c)
            space = false
        } else if (!space) {
            b.append(' ')
            space = true
        }
    }
    return b.toString().trim().split(Regex("\\s+")).joinToString(" ")
}

fun channelKey(channel: Channel): String = if (channel.tvgId.isNotBlank()) "id:${channel.tvgId}" else "url:${channel.url}"
