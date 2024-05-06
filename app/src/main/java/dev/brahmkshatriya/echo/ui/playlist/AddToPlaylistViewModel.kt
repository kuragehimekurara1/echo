package dev.brahmkshatriya.echo.ui.playlist

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.helpers.PagedData.Companion.all
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getClient
import dev.brahmkshatriya.echo.ui.playlist.EditPlaylistViewModel.Companion.addToPlaylists
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    private val app: Application,
) : CatchingViewModel(throwableFlow) {
    val playlists = MutableStateFlow<List<Playlist>?>(null)
    val selectedPlaylists = mutableListOf<Playlist>()
    lateinit var clientId: String
    lateinit var item: EchoMediaItem
    val tracks = mutableListOf<Track>()
    override fun onInitialize() {
        val client = extensionListFlow.getClient(clientId)?.client ?: return
        if (client !is LibraryClient) return
        viewModelScope.launch(Dispatchers.IO) {
            tryWith {
                when (val mediaItem = item) {
                    is EchoMediaItem.Lists.AlbumItem ->
                        tracks.addAll(client.loadTracks(mediaItem.album).all())
                    is EchoMediaItem.Lists.PlaylistItem ->
                        tracks.addAll(client.loadTracks(mediaItem.playlist).all())
                    is EchoMediaItem.TrackItem -> tracks.add(mediaItem.track)
                    else -> throw IllegalStateException()
                }
            }
            playlists.value = tryWith { client.listEditablePlaylists() }
            if (playlists.value == null) dismiss.emit(Unit)
        }
    }

    fun togglePlaylist(playlist: Playlist) = with(selectedPlaylists) {
        if (contains(playlist)) remove(playlist) else add(playlist)
    }

    var saving = false
    val dismiss = MutableSharedFlow<Unit>()
    fun addToPlaylists() = viewModelScope.launch {
        saving = true
        addToPlaylists(
            extensionListFlow, messageFlow, app, clientId, selectedPlaylists, tracks
        )
        saving = false
        dismiss.emit(Unit)
    }
}
