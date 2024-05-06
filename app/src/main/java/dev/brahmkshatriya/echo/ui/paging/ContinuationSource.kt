package dev.brahmkshatriya.echo.ui.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingState
import dev.brahmkshatriya.echo.common.helpers.Page

class ContinuationSource<C : Any, P : Any>(
    private val load: suspend (token: P?) -> Page<C, P?>,
) : ErrorPagingSource<P, C>() {

    override val config = PagingConfig(pageSize = 10, enablePlaceholders = false)
    override suspend fun loadData(params: LoadParams<P>): LoadResult.Page<P, C> {
        val token = params.key
        val page = load(token)
        return LoadResult.Page(
            data = page.data,
            prevKey = null,
            nextKey = page.continuation
        )
    }

    override fun getRefreshKey(state: PagingState<P, C>): P? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}