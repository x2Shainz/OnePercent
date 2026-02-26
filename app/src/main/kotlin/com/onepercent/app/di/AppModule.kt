package com.onepercent.app.di

import android.content.Context
import com.onepercent.app.data.db.AppDatabase
import com.onepercent.app.data.repository.EntryRepository
import com.onepercent.app.data.repository.EntryRepositoryImpl
import com.onepercent.app.data.repository.SectionRepository
import com.onepercent.app.data.repository.SectionRepositoryImpl
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.data.repository.TaskRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and all three repository singletons.
 *
 * Installed in [SingletonComponent] so each binding lives for the entire app lifetime,
 * matching the previous behaviour of the lazy properties in `OnePercentApp`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideTaskRepository(db: AppDatabase): TaskRepository =
        TaskRepositoryImpl(db.taskDao())

    @Provides
    @Singleton
    fun provideEntryRepository(db: AppDatabase): EntryRepository =
        EntryRepositoryImpl(db.entryDao())

    @Provides
    @Singleton
    fun provideSectionRepository(db: AppDatabase): SectionRepository =
        SectionRepositoryImpl(db.sectionDao(), db.entryDao())
}
