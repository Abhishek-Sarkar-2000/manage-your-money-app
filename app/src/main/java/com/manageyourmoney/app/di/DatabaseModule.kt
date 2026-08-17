package com.manageyourmoney.app.di

import android.content.Context
import androidx.room.Room
import com.manageyourmoney.app.data.local.dao.CreditCardDao
import com.manageyourmoney.app.data.local.dao.CustomTagDao
import com.manageyourmoney.app.data.local.dao.EmiSeriesDao
import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.SplitGroupDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.db.AppDatabase
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
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Fresh installs only ever see version 1, so no migrations are needed yet.
            // Once this ships, replace this with real Migration objects rather than
            // falling back to destructive migration. (Room 2.6.1 only has the no-arg
            // overload here — the dropAllTables(Boolean) overload was added in a later
            // Room release than what this project pins.)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMonthDao(db: AppDatabase): MonthDao = db.monthDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCreditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()

    @Provides
    fun provideEmiSeriesDao(db: AppDatabase): EmiSeriesDao = db.emiSeriesDao()

    @Provides
    fun provideSplitGroupDao(db: AppDatabase): SplitGroupDao = db.splitGroupDao()

    @Provides
    fun provideCustomTagDao(db: AppDatabase): CustomTagDao = db.customTagDao()
}
