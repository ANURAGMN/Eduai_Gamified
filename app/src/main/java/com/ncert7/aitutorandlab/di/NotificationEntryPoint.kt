package com.ncert7.aitutorandlab.di

import com.ncert7.aitutorandlab.notification.NotificationOrchestrator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun notificationOrchestrator(): NotificationOrchestrator
}
