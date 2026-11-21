package com.example.dailycheck.core.di

import android.content.Context
import androidx.room.Room
import com.example.dailycheck.data.local.AppDatabase
import com.example.dailycheck.data.local.dao.CheckInRecordDao
import com.example.dailycheck.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideCheckInRecordDao(database: AppDatabase): CheckInRecordDao = database.checkInRecordDao()
}
