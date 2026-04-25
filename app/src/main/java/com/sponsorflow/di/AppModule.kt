package com.sponsorflow.di

import javax.inject.Singleton

import android.content.Context
import com.sponsorflow.core.ONNXSemanticEngine
import com.sponsorflow.security.SecurityVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideSecurityVault(@ApplicationContext context: Context): SecurityVault {
        return SecurityVault(context)
    }

    @Singleton
    @Provides
    fun provideONNXSemanticEngine(@ApplicationContext context: Context): ONNXSemanticEngine {
        return ONNXSemanticEngine(context)
    }
}
