package com.redplay.iptv.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RedPlayUiState(
    val providers: List<Provider> = emptyList(),
    val currentProvider: Provider? = null,
    val playlist: Playlist = Playlist(),
    val epg: EpgData = EpgData(),
    val selectedChannel: Channel? = null,
    val search: String = "",
    val group: String = "All categories",
    val favoritesOnly: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val loading: Boolean = false,
    val status: String = "Ready",
    val error: String = "",
    val playRequest: PlaybackRequest? = null,
    val library: ChannelLibrary? = null,
    val libraryLoading: Boolean = false,
    val libraryError: String = "",
    val screen: Screen = Screen.Live,
    val guideOpen: Boolean = false,
    val providerDialogOpen: Boolean = false,
    val fullscreenPlayer: Boolean = false,
    val subtitleSize: Int = 40,
)

enum class Screen { Live, Library }

class RedPlayViewModel(app: Application) : AndroidViewModel(app) {
    private val store = RedPlayStore(app)
    private val _state = MutableStateFlow(
        RedPlayUiState(
            providers = store.providers(),
            favorites = store.favorites(),
            subtitleSize = store.subtitleSize(),
        )
    )
    val state: StateFlow<RedPlayUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        val initial = _state.value.providers.firstOrNull { it.id == store.lastProvider() } ?: _state.value.providers.firstOrNull()
        initial?.let { loadProvider(it) }
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _state.value = _state.value.copy() // refresh Now/Next time-dependent UI
            }
        }
    }

    fun filteredChannels(): List<Channel> {
        val s = _state.value
        val query = s.search.trim().lowercase()
        return s.playlist.channels.filter { channel ->
            (!s.favoritesOnly || s.favorites.contains(channelKey(channel))) &&
                (s.group == "All categories" || channel.group == s.group) &&
                (query.isBlank() || listOf(channel.name, channel.tvgName, channel.group).any { it.lowercase().contains(query) })
        }
    }

    fun groups(): List<String> = listOf("All categories") + _state.value.playlist.channels.map { it.group.ifBlank { "Other" } }.distinct().sorted()

    fun nowNext(channel: Channel? = _state.value.selectedChannel): Pair<Programme?, Programme?> {
        if (channel == null) return null to null
        return XmlTvParser.nowNext(channel, _state.value.epg)
    }

    fun programmes(channel: Channel? = _state.value.selectedChannel): List<Programme> {
        channel ?: return emptyList()
        val id = XmlTvParser.matchChannel(channel, _state.value.epg)
        return _state.value.epg.programmes[id].orEmpty()
    }

    fun loadProvider(provider: Provider) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(currentProvider = provider, loading = true, error = "", status = "Loading ${provider.name}…", screen = Screen.Live)
            store.setLastProvider(provider.id)
            try {
                val result = withContext(Dispatchers.IO) {
                    val bytes = RedPlayNetwork.fetchBytes(provider.url, provider.headers, 64 * 1024 * 1024)
                    val playlist = M3uParser.parse(M3uParser.decodeText(bytes), provider.url)
                    if (playlist.channels.isEmpty()) error("The playlist contains no channels")
                    val epgUrl = provider.epgUrl.ifBlank { playlist.epgUrl }
                    val epg = if (epgUrl.isNotBlank()) {
                        runCatching {
                            val epgBytes = RedPlayNetwork.fetchBytes(epgUrl, provider.headers, 96 * 1024 * 1024)
                            XmlTvParser.parse(
                                epgBytes,
                                System.currentTimeMillis() - 12L * 60 * 60 * 1000,
                                System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000,
                            )
                        }.getOrDefault(EpgData())
                    } else EpgData()
                    playlist to epg
                }
                val selected = result.first.channels.firstOrNull()
                _state.value = _state.value.copy(
                    playlist = result.first,
                    epg = result.second,
                    selectedChannel = selected,
                    loading = false,
                    status = "${result.first.channels.size} channels • ${provider.name}",
                )
                selected?.let { playChannel(it) }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message ?: "Provider load failed", status = "Provider error")
            }
        }
    }

    fun refresh() = _state.value.currentProvider?.let(::loadProvider)

    fun selectChannel(channel: Channel) {
        _state.value = _state.value.copy(selectedChannel = channel, screen = Screen.Live, guideOpen = false)
        playChannel(channel)
    }

    private fun playChannel(channel: Channel) {
        val provider = _state.value.currentProvider ?: return
        _state.value = _state.value.copy(
            playRequest = PlaybackRequest(
                title = channel.name,
                url = channel.url,
                headers = provider.headers + channel.headers,
                isLive = true,
            ),
            status = "Playing ${channel.name}",
            error = "",
        )
    }

    fun toggleFavorite(channel: Channel? = _state.value.selectedChannel) {
        channel ?: return
        val key = channelKey(channel)
        val values = _state.value.favorites.toMutableSet()
        if (!values.add(key)) values.remove(key)
        store.setFavorites(values)
        _state.value = _state.value.copy(favorites = values)
    }

    fun setSearch(value: String) { _state.value = _state.value.copy(search = value) }
    fun setGroup(value: String) { _state.value = _state.value.copy(group = value) }
    fun toggleFavoritesOnly() { _state.value = _state.value.copy(favoritesOnly = !_state.value.favoritesOnly) }
    fun toggleGuide() { _state.value = _state.value.copy(guideOpen = !_state.value.guideOpen) }
    fun openProviderDialog() { _state.value = _state.value.copy(providerDialogOpen = true) }
    fun closeProviderDialog() { _state.value = _state.value.copy(providerDialogOpen = false) }
    fun toggleFullscreen() { _state.value = _state.value.copy(fullscreenPlayer = !_state.value.fullscreenPlayer) }
    fun exitFullscreen() { _state.value = _state.value.copy(fullscreenPlayer = false) }

    fun addProvider(name: String, url: String, epgUrl: String) {
        if (name.isBlank() || url.isBlank()) return
        val provider = store.saveProvider(name, url, epgUrl)
        val providers = store.providers()
        _state.value = _state.value.copy(providers = providers, providerDialogOpen = false)
        loadProvider(provider)
    }

    fun deleteCurrentProvider() {
        val current = _state.value.currentProvider ?: return
        if (current.builtIn) {
            _state.value = _state.value.copy(status = "Built-in providers cannot be deleted")
            return
        }
        store.removeProvider(current.id)
        val providers = store.providers()
        _state.value = _state.value.copy(providers = providers)
        providers.firstOrNull()?.let(::loadProvider)
    }

    fun openLibrary() {
        val provider = _state.value.currentProvider ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(screen = Screen.Library, libraryLoading = true, libraryError = "")
            try {
                val library = withContext(Dispatchers.Default) { WesternLibrary.build(provider, _state.value.playlist, _state.value.epg) }
                _state.value = _state.value.copy(library = library, libraryLoading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(libraryLoading = false, libraryError = t.message ?: "Library unavailable")
            }
        }
    }

    fun closeLibrary() { _state.value = _state.value.copy(screen = Screen.Live) }

    fun playLibraryMovie(movie: LibraryMovie) {
        val provider = _state.value.currentProvider ?: return
        val headers = provider.headers + (_state.value.library?.channel?.headers ?: emptyMap())
        viewModelScope.launch {
            _state.value = _state.value.copy(libraryLoading = true, libraryError = "")
            try {
                val resolved = withContext(Dispatchers.IO) { WesternLibrary.resolveMovie(movie, headers) }
                val subtitle = withContext(Dispatchers.IO) { WesternLibrary.prepareSubtitle(getApplication(), movie, headers) }
                _state.value = _state.value.copy(
                    libraryLoading = false,
                    playRequest = PlaybackRequest(
                        title = movie.displayTitle,
                        url = resolved,
                        headers = headers,
                        localSubtitlePath = subtitle,
                        isLive = false,
                    ),
                    screen = Screen.Live,
                    status = "Playing ${movie.displayTitle} from Library",
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(libraryLoading = false, libraryError = t.message ?: "Movie playback failed")
            }
        }
    }

    fun setSubtitleSize(value: Int) {
        val v = value.coerceIn(24, 60)
        store.setSubtitleSize(v)
        _state.value = _state.value.copy(subtitleSize = v)
    }
}
