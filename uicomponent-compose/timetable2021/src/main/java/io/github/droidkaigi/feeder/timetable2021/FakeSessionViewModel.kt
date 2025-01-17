package io.github.droidkaigi.feeder.timetable2021

import app.cash.exhaustive.Exhaustive
import io.github.droidkaigi.feeder.AppError
import io.github.droidkaigi.feeder.Filters
import io.github.droidkaigi.feeder.TimetableContents
import io.github.droidkaigi.feeder.fakeTimetableContents
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun fakeTimetableViewModel(errorFetchData: Boolean = false): FakeSessionViewModel {
    return FakeSessionViewModel(errorFetchData)
}

class FakeSessionViewModel(val errorFetchData: Boolean) : SessionViewModel {

    private val effectChannel = Channel<SessionViewModel.Effect>(Channel.UNLIMITED)
    override val effect: Flow<SessionViewModel.Effect> = effectChannel.receiveAsFlow()

    private val coroutineScope = CoroutineScope(
        object : CoroutineDispatcher() {
            // for preview
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                block.run()
            }
        }
    )
    private val mutableSessionContents = MutableStateFlow(
        fakeTimetableContents()
    )
    private val errorSessionContents = flow<TimetableContents> {
        throw AppError.ApiException.ServerException(null)
    }
        .catch { error ->
            effectChannel.send(SessionViewModel.Effect.ErrorMessage(error as AppError))
        }
        .stateIn(coroutineScope, SharingStarted.Lazily, fakeTimetableContents())

    private val mTimetableContents: StateFlow<TimetableContents> = if (errorFetchData) {
        errorSessionContents
    } else {
        mutableSessionContents
    }

    private val filters: MutableStateFlow<Filters> = MutableStateFlow(Filters())

    override val state: StateFlow<SessionViewModel.State> =
        combine(mTimetableContents, filters) { feedContents, filters ->
            SessionViewModel.State(
                timetableContents = feedContents,
            )
        }
            .stateIn(coroutineScope, SharingStarted.Eagerly, SessionViewModel.State())

    override fun event(event: SessionViewModel.Event) {
        coroutineScope.launch {
            @Exhaustive
            when (event) {
                is SessionViewModel.Event.ChangeFavoriteFilter -> {
                    filters.value = event.filters
                }
                is SessionViewModel.Event.ToggleFavorite -> {
                    val value = mTimetableContents.value
//                    val newFavorites = if (!value.favorites.contains(event.feedItem.id)) {
//                        value.favorites + event.feedItem.id
//                    } else {
//                        value.favorites - event.feedItem.id
//                    }
//                    mutableSessionContents.value = value.copy(
//                        favorites = newFavorites
//                    )
                }
                is SessionViewModel.Event.ReloadContent -> {
                    // Sorry, Currently not implemented
                }
            }
        }
    }
}
