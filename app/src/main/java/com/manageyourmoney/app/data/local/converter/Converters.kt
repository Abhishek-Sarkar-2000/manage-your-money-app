package com.manageyourmoney.app.data.local.converter

import androidx.room.TypeConverter
import com.manageyourmoney.app.data.local.entity.PaymentMode
import com.manageyourmoney.app.data.local.entity.StartingBalanceMode
import com.manageyourmoney.app.data.local.entity.TransactionType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room can only persist primitive columns, so:
 *  1) any field that was a free-form JSON blob in the web app's SQLite `storage` table
 *     (e.g. an entry's `lent[]` array, a split group's `people[]` list) is normalized
 *     into its own child table wherever it participates in a query or a foreign key
 *     (see [com.manageyourmoney.app.data.local.entity.LentShareEntity],
 *     [com.manageyourmoney.app.data.local.entity.SplitPersonEntity]) — the couple of
 *     fields left over that are genuinely opaque, order-preserving string lists are
 *     still just JSON-encoded here since nothing ever needs to query *into* them;
 *  2) every Kotlin enum used as an entity column ([TransactionType], [PaymentMode],
 *     [StartingBalanceMode]) needs an explicit to/from-String pair, since Room has no
 *     built-in enum support and silently fails at KSP/compile time without one.
 */
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        json.encodeToString(value ?: emptyList())

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else json.decodeFromString<List<String>>(value)

    // ---- Enums ----

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode?): String? = value?.name

    @TypeConverter
    fun toPaymentMode(value: String?): PaymentMode? = value?.let { PaymentMode.valueOf(it) }

    @TypeConverter
    fun fromStartingBalanceMode(value: StartingBalanceMode): String = value.name

    @TypeConverter
    fun toStartingBalanceMode(value: String): StartingBalanceMode = StartingBalanceMode.valueOf(value)
}
