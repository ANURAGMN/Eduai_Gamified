package com.anurag.eduai.uikit.events

import kotlinx.coroutines.flow.SharedFlow

interface GamificationEventBus {
    val events: SharedFlow<GamificationEvent>
    suspend fun emit(event: GamificationEvent)
}

class InMemoryGamificationEventBus : GamificationEventBus {
    private val _events =
        kotlinx.coroutines.flow.MutableSharedFlow<GamificationEvent>(
            extraBufferCapacity = 32,
        )

    override val events: SharedFlow<GamificationEvent> = _events

    override suspend fun emit(event: GamificationEvent) {
        _events.emit(event)
    }
}
