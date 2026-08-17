package com.manageyourmoney.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.manageyourmoney.app.data.local.converter.Converters
import com.manageyourmoney.app.data.local.dao.CreditCardDao
import com.manageyourmoney.app.data.local.dao.CustomTagDao
import com.manageyourmoney.app.data.local.dao.EmiSeriesDao
import com.manageyourmoney.app.data.local.dao.MonthDao
import com.manageyourmoney.app.data.local.dao.SplitGroupDao
import com.manageyourmoney.app.data.local.dao.TransactionDao
import com.manageyourmoney.app.data.local.entity.CreditCardEntity
import com.manageyourmoney.app.data.local.entity.CustomTagEntity
import com.manageyourmoney.app.data.local.entity.EmiDeletionEntity
import com.manageyourmoney.app.data.local.entity.EmiSeriesEntity
import com.manageyourmoney.app.data.local.entity.LentShareEntity
import com.manageyourmoney.app.data.local.entity.MonthEntity
import com.manageyourmoney.app.data.local.entity.SplitGroupEntity
import com.manageyourmoney.app.data.local.entity.SplitPersonEntity
import com.manageyourmoney.app.data.local.entity.SplitSettlementEntity
import com.manageyourmoney.app.data.local.entity.SplitSpendEntity
import com.manageyourmoney.app.data.local.entity.SplitSpendShareEntity
import com.manageyourmoney.app.data.local.entity.TransactionEntity

/**
 * Local replacement for the web app's Flask + SQLite `Store.get/set` key-value layer.
 * Every entity here corresponds 1:1 to a storage key or a normalized fragment of one —
 * see each entity's KDoc for the exact web-app field it replaces.
 */
@Database(
    entities = [
        MonthEntity::class,
        EmiDeletionEntity::class,
        TransactionEntity::class,
        LentShareEntity::class,
        CreditCardEntity::class,
        EmiSeriesEntity::class,
        SplitGroupEntity::class,
        SplitPersonEntity::class,
        SplitSpendEntity::class,
        SplitSpendShareEntity::class,
        SplitSettlementEntity::class,
        CustomTagEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monthDao(): MonthDao
    abstract fun transactionDao(): TransactionDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun emiSeriesDao(): EmiSeriesDao
    abstract fun splitGroupDao(): SplitGroupDao
    abstract fun customTagDao(): CustomTagDao

    companion object {
        const val DATABASE_NAME = "manage_your_money.db"
    }
}
