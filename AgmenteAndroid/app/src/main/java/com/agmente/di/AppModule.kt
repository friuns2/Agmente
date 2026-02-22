package com.agmente.di

import android.content.Context
import com.agmente.data.SessionStorage
import com.agmente.data.db.AgmenteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AgmenteDatabase =
        AgmenteDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideSessionStorage(database: AgmenteDatabase): SessionStorage =
        SessionStorage(database)
}
