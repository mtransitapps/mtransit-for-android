package org.mtransit.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.mtransit.android.commons.di.NetworkComponents
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun providesOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        return NetworkComponents.getOkHttpClient(context)
    }
}