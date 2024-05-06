package dev.brahmkshatriya.echo.ui.common

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.utils.tryWith
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class FeedViewModel<T>(
    throwableFlow: MutableSharedFlow<Throwable>,
    open val extensionFlow: MutableStateFlow<MusicExtension?>,
) : CatchingViewModel(throwableFlow) {
    abstract suspend fun getTabs(client: T): List<Tab>
    abstract fun getFeed(client: T): Flow<PagingData<MediaItemsContainer>>

    var recyclerPosition = 0

    val loading = MutableSharedFlow<Boolean>()
    val feed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    val genres = MutableStateFlow<List<Tab>>(emptyList())
    var tab: Tab? = null

    override fun onInitialize() {
        viewModelScope.launch {
            extensionFlow.collect { refresh(true) }
        }
    }


    private suspend fun loadGenres(client: T) {
        loading.emit(true)
        val list = tryWith { getTabs(client) } ?: emptyList()
        loading.emit(false)
        if (!list.any { it.id == tab?.id }) tab = list.firstOrNull()
        genres.value = list
    }


    private suspend fun loadFeed(client: T) = tryWith {
        getFeed(client).collectTo(feed)
    }

    @Suppress("UNCHECKED_CAST")
    fun refresh(reset: Boolean = false) {
        feed.value = null
        val client = tryWith(false) { extensionFlow.value?.client as T } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (reset) loadGenres(client)
            loadFeed(client)
        }
    }
}