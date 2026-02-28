package com.onepercent.app.di

import android.content.Context
import androidx.room.Room
import com.onepercent.app.data.db.AppDatabase
import com.onepercent.app.data.repository.EntryRepository
import com.onepercent.app.data.repository.EntryRepositoryImpl
import com.onepercent.app.data.repository.SectionRepository
import com.onepercent.app.data.repository.SectionRepositoryImpl
import com.onepercent.app.data.repository.TaskRepository
import com.onepercent.app.data.repository.TaskRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideInMemoryDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideTaskRepository(db: AppDatabase): TaskRepository =
        TaskRepositoryImpl(db.taskDao())

    @Provides
    @Singleton
    fun provideEntryRepository(db: AppDatabase): EntryRepository =
        EntryRepositoryImpl(db, db.entryDao())

    @Provides
    @Singleton
    fun provideSectionRepository(db: AppDatabase): SectionRepository =
        SectionRepositoryImpl(db, db.sectionDao(), db.entryDao())
}
