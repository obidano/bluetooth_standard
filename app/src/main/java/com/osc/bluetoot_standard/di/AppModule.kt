package com.osc.bluetoot_standard.di

import android.content.Context
import com.osc.bluetoot_standard.services.MyBluetoothService

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideBluetooth(@ApplicationContext context: Context): MyBluetoothService {
        return MyBluetoothService(context)
    }



}