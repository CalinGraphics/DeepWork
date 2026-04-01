package com.deepwork.data.remote.di

import com.deepwork.data.remote.client.DeepWorkWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Provides
    @Singleton
    fun provideWebSocketClient(): DeepWorkWebSocketClient {
        return DeepWorkWebSocketClient()
    }
}
