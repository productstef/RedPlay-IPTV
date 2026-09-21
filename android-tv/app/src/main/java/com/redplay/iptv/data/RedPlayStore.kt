package com.redplay.iptv.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RedPlayStore(context: Context) {
    private val prefs = context.getSharedPreferences("redplay-tv-state", Context.MODE_PRIVATE)

    fun providers(): List<Provider> {
        val saved = runCatching {
            val array = JSONArray(prefs.getString("providers", "[]") ?: "[]")
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                Provider(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    url = o.optString("url"),
                    epgUrl = o.optString("epgUrl"),
                    headers = jsonToMap(o.optJSONObject("headers")),
                    builtIn = o.optBoolean("builtIn", false),
                )
            }
        }.getOrDefault(emptyList())
        val merged = saved.toMutableList()
        builtInProviders().forEach { builtIn ->
            if (merged.none { it.url.equals(builtIn.url, ignoreCase = true) }) merged += builtIn
        }
        if (merged.isEmpty()) merged += builtInProviders()
        persistProviders(merged)
        return merged
    }

    fun saveProvider(name: String, url: String, epgUrl: String, headers: Map<String, String> = emptyMap()): Provider {
        val provider = Provider(UUID.randomUUID().toString(), name.trim(), url.trim(), epgUrl.trim(), M3uParser.safeHeaders(headers))
        persistProviders(providers() + provider)
        return provider
    }

    fun removeProvider(id: String) {
        persistProviders(providers().filterNot { it.id == id && !it.builtIn })
    }

    fun favorites(): Set<String> = prefs.getStringSet("favorites", emptySet()).orEmpty()
    fun setFavorites(values: Set<String>) = prefs.edit().putStringSet("favorites", values).apply()
    fun lastProvider(): String = prefs.getString("lastProvider", "builtin-western") ?: "builtin-western"
    fun setLastProvider(id: String) = prefs.edit().putString("lastProvider", id).apply()
    fun volume(): Int = prefs.getInt("volume", 80).coerceIn(0, 100)
    fun setVolume(value: Int) = prefs.edit().putInt("volume", value.coerceIn(0, 100)).apply()
    fun subtitleSize(): Int = prefs.getInt("subtitleSize", 40).coerceIn(24, 60)
    fun setSubtitleSize(value: Int) = prefs.edit().putInt("subtitleSize", value.coerceIn(24, 60)).apply()

    private fun persistProviders(items: List<Provider>) {
        val array = JSONArray()
        items.distinctBy { it.url.lowercase() }.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("url", p.url)
                put("epgUrl", p.epgUrl)
                put("builtIn", p.builtIn)
                put("headers", JSONObject(p.headers))
            })
        }
        prefs.edit().putString("providers", array.toString()).apply()
    }

    private fun jsonToMap(o: JSONObject?): Map<String, String> {
        if (o == null) return emptyMap()
        return buildMap {
            o.keys().forEach { key -> put(key, o.optString(key)) }
        }
    }
}
