package com.forem.android.application

import com.forem.android.BuildConfig
import com.forem.android.app.model.ForemCollection
import com.forem.android.data.local.CurrentForemLocalDataSource
import com.forem.android.data.local.ForemCollectionLocalDataSource
import com.forem.android.data.remote.discover.DiscoverForemDeserializer
import com.forem.android.data.remote.discover.DiscoverForemRemoteDataSource
import com.forem.android.data.remote.discover.DiscoverForemService
import com.forem.android.data.repository.DiscoverForemRepository
import com.forem.android.data.repository.MyForemRepository
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/** Provides core infrastructure for all other dependencies in the app. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Provides singleton instance of Retrofit with GsonConverterFactory. */
    @Singleton
    @Provides
    fun provideRetrofit(gsonConverterFactory: GsonConverterFactory): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.FOREM_DISCOVER_URL)
        .addConverterFactory(gsonConverterFactory)
        .build()

    /** Provides new GsonConverterFactory instance. */
    @Provides
    fun provideGsonConverterFactory(): GsonConverterFactory {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(ForemCollection::class.java, DiscoverForemDeserializer())
        return GsonConverterFactory.create(gsonBuilder.create())
    }

    /** Provides new instance of [DiscoverForemService]. */
    @Provides
    fun provideDiscoverForemService(retrofit: Retrofit): DiscoverForemService =
        retrofit.create(DiscoverForemService::class.java)

    /** Provides singleton instance of [DiscoverForemRemoteDataSource]. */
    @Singleton
    @Provides
    fun provideDiscoverForemRemoteDataSource(discoverForemService: DiscoverForemService) =
        DiscoverForemRemoteDataSource(discoverForemService)

    /** Provides singleton instance of [DiscoverForemRepository]. */
    @Singleton
    @Provides
    fun provideDiscoverForemRepository(
        localDataSource: ForemCollectionLocalDataSource,
        remoteDataSource: DiscoverForemRemoteDataSource
    ) = DiscoverForemRepository(localDataSource, remoteDataSource)

    /** Provides singleton instance of [MyForemRepository]. */
    @Singleton
    @Provides
    fun provideMyForemRepository(
        localDataSource: ForemCollectionLocalDataSource,
        currentForemLocalDataSource: CurrentForemLocalDataSource
    ) = MyForemRepository(localDataSource, currentForemLocalDataSource)
}
