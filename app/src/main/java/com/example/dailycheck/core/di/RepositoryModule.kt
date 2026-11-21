package com.example.dailycheck.core.di

import com.example.dailycheck.data.repository.StatsRepository
import com.example.dailycheck.data.repository.StatsRepositoryImpl
import com.example.dailycheck.data.repository.TaskRepository
import com.example.dailycheck.data.repository.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository
}
