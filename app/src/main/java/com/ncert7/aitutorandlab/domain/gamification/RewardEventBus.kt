package com.ncert7.aitutorandlab.domain.gamification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<RewardUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RewardUiEvent> = _events.asSharedFlow()

    fun tryEmit(event: RewardUiEvent) {
        _events.tryEmit(event)
    }
}
