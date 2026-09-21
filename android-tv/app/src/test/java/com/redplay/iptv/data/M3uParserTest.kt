package com.redplay.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test fun parsesEpgGroupsHeadersAndRelativeUrls() {
        val input = """
            #EXTM3U x-tvg-url="epg.xml"
            #EXTINF:-1 tvg-id="western-channel" tvg-name="Western Channel" group-title="Movies",Western Channel
            #EXTVLCOPT:http-user-agent=RedPlay Test
            live/western.m3u8|Referer=http%3A%2F%2Fexample.com%2F
        """.trimIndent()
        val playlist = M3uParser.parse(input, "http://127.0.0.1/western/playlist.m3u")
        assertEquals("http://127.0.0.1/western/epg.xml", playlist.epgUrl)
        assertEquals(1, playlist.channels.size)
        val channel = playlist.channels.first()
        assertEquals("Western Channel", channel.name)
        assertEquals("Movies", channel.group)
        assertEquals("http://127.0.0.1/western/live/western.m3u8", channel.url)
        assertEquals("RedPlay Test", channel.headers["User-Agent"])
        assertEquals("http://example.com/", channel.headers["Referer"])
    }

    @Test fun preservesBuiltinProviderUrls() {
        val providers = builtInProviders()
        assertTrue(providers.any { it.url == "http://92.5.59.81/western/playlist.m3u" })
        assertTrue(providers.any { it.url == "http://92.5.59.81/starwars.m3u" })
    }
}
