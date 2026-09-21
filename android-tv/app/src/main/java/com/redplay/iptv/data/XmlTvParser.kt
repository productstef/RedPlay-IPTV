package com.redplay.iptv.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object XmlTvParser {
    fun parse(bytes: ByteArray, minMillis: Long = 0L, maxMillis: Long = Long.MAX_VALUE): EpgData {
        val displayNames = linkedMapOf<String, MutableList<String>>()
        val programmes = linkedMapOf<String, MutableList<Programme>>()
        val nameToIds = linkedMapOf<String, MutableList<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), null)

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "channel" -> {
                        val id = parser.getAttributeValue(null, "id").orEmpty().trim()
                        if (id.isNotBlank()) {
                            val names = mutableListOf<String>()
                            val depth = parser.depth
                            while (true) {
                                event = parser.next()
                                if (event == XmlPullParser.START_TAG && parser.name == "display-name") {
                                    val name = parser.nextText().trim()
                                    if (name.isNotBlank()) names += name
                                }
                                if (event == XmlPullParser.END_TAG && parser.depth == depth && parser.name == "channel") break
                            }
                            displayNames[id] = names
                            names.forEach { name ->
                                val normalized = normalizeName(name)
                                if (normalized.isNotBlank()) {
                                    val ids = nameToIds.getOrPut(normalized) { mutableListOf() }
                                    if (!ids.contains(id)) ids += id
                                }
                            }
                        }
                    }
                    "programme" -> {
                        val channel = parser.getAttributeValue(null, "channel").orEmpty().trim()
                        val start = parseXmlTvTime(parser.getAttributeValue(null, "start").orEmpty()) ?: 0L
                        val stop = parseXmlTvTime(parser.getAttributeValue(null, "stop").orEmpty()) ?: (start + 2 * 60 * 60 * 1000)
                        var title = ""
                        var subTitle = ""
                        var desc = ""
                        var category = ""
                        val depth = parser.depth
                        while (true) {
                            event = parser.next()
                            if (event == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "title" -> title = parser.nextText().trim()
                                    "sub-title" -> subTitle = parser.nextText().trim()
                                    "desc" -> desc = parser.nextText().trim()
                                    "category" -> if (category.isBlank()) category = parser.nextText().trim() else parser.nextText()
                                }
                            }
                            if (event == XmlPullParser.END_TAG && parser.depth == depth && parser.name == "programme") break
                        }
                        if (start > 0L && stop >= minMillis && start <= maxMillis && channel.isNotBlank()) {
                            programmes.getOrPut(channel) { mutableListOf() } += Programme(
                                channelId = channel,
                                startMillis = start,
                                stopMillis = stop,
                                title = title.ifBlank { "Untitled programme" },
                                subTitle = subTitle,
                                description = desc,
                                category = category,
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        programmes.values.forEach { it.sortBy(Programme::startMillis) }
        return EpgData(
            displayNames = displayNames,
            programmes = programmes,
            nameToIds = nameToIds,
        )
    }

    fun matchChannel(channel: Channel, epg: EpgData): String {
        if (channel.tvgId.isNotBlank() && (epg.programmes.containsKey(channel.tvgId) || epg.displayNames.containsKey(channel.tvgId))) {
            return channel.tvgId
        }
        for (candidate in listOf(channel.tvgName, channel.name)) {
            val ids = epg.nameToIds[normalizeName(candidate)].orEmpty()
            if (ids.size == 1) return ids.first()
        }
        return ""
    }

    fun nowNext(channel: Channel, epg: EpgData, nowMillis: Long = System.currentTimeMillis()): Pair<Programme?, Programme?> {
        val id = matchChannel(channel, epg)
        if (id.isBlank()) return null to null
        val list = epg.programmes[id].orEmpty()
        list.forEachIndexed { index, p ->
            if (nowMillis >= p.startMillis && nowMillis < p.stopMillis) {
                return p to list.getOrNull(index + 1)
            }
            if (p.startMillis > nowMillis) return null to p
        }
        return null to null
    }

    private fun parseXmlTvTime(rawValue: String): Long? {
        val raw = rawValue.trim()
        if (raw.length < 12) return null
        val pieces = raw.split(Regex("\\s+"), limit = 2)
        val digits = pieces[0]
        val zoneText = pieces.getOrNull(1)?.trim().orEmpty()
        val pattern = when (digits.length) {
            14 -> "yyyyMMddHHmmss"
            12 -> "yyyyMMddHHmm"
            else -> return null
        }
        val tz = when {
            zoneText.equals("Z", ignoreCase = true) -> TimeZone.getTimeZone("UTC")
            zoneText.matches(Regex("[+-]\\d{4}")) -> TimeZone.getTimeZone("GMT${zoneText.substring(0, 3)}:${zoneText.substring(3)}")
            zoneText.matches(Regex("[+-]\\d{2}:\\d{2}")) -> TimeZone.getTimeZone("GMT$zoneText")
            else -> TimeZone.getDefault()
        }
        return runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = tz
            }.parse(digits)?.time
        }.getOrNull()
    }
}
