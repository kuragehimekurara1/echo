package dev.brahmkshatriya.echo.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoApplication
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.ui.common.ClientLoadingAdapter
import dev.brahmkshatriya.echo.ui.common.ClientNotSupportedAdapter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    val trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
    val lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
    val extensionFlow: MutableStateFlow<MusicExtension?>,
    val settings: SharedPreferences,
    val database: EchoDatabase,
    val userFlow: MutableSharedFlow<UserEntity?>
) : CatchingViewModel(throwableFlow) {

    val currentExtension
        get() = extensionFlow.value

    private val userDao = database.userDao()
    fun setExtension(extension: MusicExtension?) {
        EchoApplication.setExtension(
            viewModelScope,
            settings,
            extensionFlow,
            userDao,
            userFlow,
            throwableFlow,
            extension
        )
    }

    companion object {
        fun Context.noClient() = SnackBar.Message(
            getString(R.string.error_no_client)
        )

        fun Context.searchNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.search), client)
        )

        fun Context.trackNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.track), client)
        )

        fun Context.radioNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.radio), client)
        )

        fun Context.loginNotSupported(client: String) = SnackBar.Message(
            getString(R.string.is_not_supported, getString(R.string.login), client)
        )

        inline fun <reified T> RecyclerView.applyAdapter(
            extension: MusicExtension?,
            name: Int,
            adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
            block: ((T?) -> Unit) = {}
        ) {
            block(extension?.client as? T)
            setAdapter(
                if (extension == null)
                    ClientLoadingAdapter()
                else if (extension.client !is T)
                    ClientNotSupportedAdapter(name, extension.metadata.name)
                else adapter
            )
        }

    }
}