package com.redplay.iptv.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

object RedPlayNetwork {
    private const val USER_AGENT = "RedPlay-IPTV-TV/0.1"

    fun fetchBytes(url: String, headers: Map<String, String> = emptyMap(), maxBytes: Int = 32 * 1024 * 1024): ByteArray {
        val connection = open(url, "GET", headers)
        connection.connect()
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode} for $url")
            val length = connection.contentLengthLong
            if (length > maxBytes) error("Response is too large (${length} bytes)")
            return connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                while (true) {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    if (out.size() + n > maxBytes) error("Response exceeded ${maxBytes} bytes")
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun assetExists(url: String, headers: Map<String, String>): Boolean {
        fun attempt(method: String, useRange: Boolean): Boolean = runCatching {
            val c = open(url, method, headers)
            if (useRange) c.setRequestProperty("Range", "bytes=0-0")
            c.connectTimeout = 4500
            c.readTimeout = 4500
            c.connect()
            val ok = c.responseCode in 200..299 || c.responseCode == 206
            c.disconnect()
            ok
        }.getOrDefault(false)
        return attempt("HEAD", false) || attempt("GET", true)
    }

    private fun open(url: String, method: String, headers: Map<String, String>): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "*/*")
            setRequestProperty("User-Agent", USER_AGENT)
            M3uParser.safeHeaders(headers).forEach { (key, value) -> setRequestProperty(key, value) }
        }
    }
}

object WesternLibrary {
    private val extensions = listOf(".mkv", ".mp4", ".avi", ".m4v", ".mov")
    private val trailingYear = Regex("""^\s*(.*?)\s*\((\d{4})\)\s*$""")

    fun build(provider: Provider, playlist: Playlist, epg: EpgData): ChannelLibrary {
        val channel = playlist.channels.firstOrNull { c ->
            listOf(c.id, c.name, c.tvgId, c.tvgName, c.group).joinToString(" ").contains("western", ignoreCase = true)
        } ?: error("Western Channel was not found in the current provider")
        val epgId = XmlTvParser.matchChannel(channel, epg).ifBlank { error("Western Channel TV guide is not loaded yet") }
        val programmes = epg.programmes[epgId].orEmpty()
        if (programmes.isEmpty()) error("Western Channel TV guide contains no movies")

        val seen = hashSetOf<String>()
        val movies = programmes.mapNotNull { p ->
            val rawTitle = p.title.trim()
            if (rawTitle.isBlank() || !seen.add(rawTitle.lowercase())) return@mapNotNull null
            val match = trailingYear.matchEntire(rawTitle)
            val displayTitle = match?.groupValues?.get(1)?.trim().orEmpty().ifBlank { rawTitle }
            val year = match?.groupValues?.get(2).orEmpty()
            LibraryMovie(
                title = rawTitle,
                displayTitle = displayTitle,
                year = year,
                description = p.description.ifBlank { "Available on demand from the Western Channel library." },
                candidateUrls = extensions.map { directAssetUrl(provider.url, rawTitle + it) },
                subtitleUrl = directAssetUrl(provider.url, "$rawTitle.srt"),
                durationMillis = (p.stopMillis - p.startMillis).coerceAtLeast(0L),
            )
        }.sortedWith(compareBy<LibraryMovie> { it.displayTitle.lowercase() }.thenBy { it.year })
        if (movies.isEmpty()) error("Western Channel TV guide contains no usable movie titles")
        return ChannelLibrary("Western Channel Library", channel, movies)
    }

    fun resolveMovie(movie: LibraryMovie, headers: Map<String, String>): String {
        return movie.candidateUrls.firstOrNull { RedPlayNetwork.assetExists(it, headers) }
            ?: error("Movie file was not found on the server (tried MKV, MP4, AVI, M4V and MOV)")
    }

    fun prepareSubtitle(context: Context, movie: LibraryMovie, headers: Map<String, String>): String? {
        if (!RedPlayNetwork.assetExists(movie.subtitleUrl, headers)) return null
        return runCatching {
            val bytes = RedPlayNetwork.fetchBytes(movie.subtitleUrl, headers, 8 * 1024 * 1024)
            val normalized = normalizeSubtitle(bytes)
            val dir = File(context.cacheDir, "redplay-subs").apply { mkdirs() }
            dir.listFiles()?.filter { it.isFile && System.currentTimeMillis() - it.lastModified() > 24L * 60 * 60 * 1000 }?.forEach { it.delete() }
            val safe = movie.title.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(80)
            File(dir, "$safe-${movie.title.hashCode()}.srt").apply { writeText(normalized, Charsets.UTF_8) }.absolutePath
        }.getOrNull()
    }

    private fun directAssetUrl(providerUrl: String, fileName: String): String {
        val provider = URI(providerUrl)
        require(provider.scheme == "http" || provider.scheme == "https") { "Western library requires an HTTP/HTTPS provider URL" }
        val encoded = fileName.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        val authority = provider.rawAuthority ?: error("Provider URL has no host")
        return "${provider.scheme}://$authority/videos/western/$encoded"
    }

    private fun normalizeSubtitle(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16BE)
        }
        val utf8 = bytes.toString(Charsets.UTF_8)
        if (!utf8.contains('\uFFFD')) return utf8
        return bytes.toString(Charset.forName("windows-1251"))
    }
}
