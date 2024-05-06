package dev.brahmkshatriya.echo.ui.login

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toCurrentUser
import dev.brahmkshatriya.echo.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val extensionList: MutableStateFlow<List<MusicExtension>?>,
    private val context: Application,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    database: EchoDatabase,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    private val userDao = database.userDao()
    val loadingOver = MutableSharedFlow<Unit>()

    fun onWebViewStop(
        id: String,
        webViewClient: LoginClient.WebView,
        url: String,
        cookie: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = tryWith {
                webViewClient.onLoginWebviewStop(url, cookie)
            }?.map { it.toEntity(id) }
            loadingOver.emit(Unit)

            users ?: return@launch
            users.ifEmpty {
                messageFlow.emit(SnackBar.Message(context.getString(R.string.no_user_found)))
                return@launch
            }
            userDao.setUsers(users)
            userDao.setCurrentUser(users.first().toCurrentUser())
        }
    }
}