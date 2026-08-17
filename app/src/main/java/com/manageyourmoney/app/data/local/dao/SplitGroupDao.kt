package com.manageyourmoney.app.data.local.dao

import androidx.room.*
import com.manageyourmoney.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class SplitSpendWithShares(
    @Embedded val spend: SplitSpendEntity,
    @Relation(parentColumn = "id", entityColumn = "spendId")
    val shares: List<SplitSpendShareEntity>,
)

/** Full aggregate for one split group — mirrors the shape of a `split:<id>` JSON
 *  record after `loadSplit()` normalizes its defaults (people/spends/settlements
 *  always present as arrays). */
data class SplitGroupWithDetails(
    @Embedded val group: SplitGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val people: List<SplitPersonEntity>,
    @Relation(parentColumn = "id", entityColumn = "groupId", entity = SplitSpendEntity::class)
    val spends: List<SplitSpendWithShares>,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val settlements: List<SplitSettlementEntity>,
)

@Dao
interface SplitGroupDao {

    /** Mirrors `loadAllSplitGroups()` — sorted by createdAt desc, id desc as a tiebreaker. */
    @Transaction
    @Query("SELECT * FROM split_groups ORDER BY createdAt DESC, id DESC")
    fun observeAllGroups(): Flow<List<SplitGroupWithDetails>>

    @Transaction
    @Query("SELECT * FROM split_groups ORDER BY createdAt DESC, id DESC")
    suspend fun getAllGroups(): List<SplitGroupWithDetails>

    @Transaction
    @Query("SELECT * FROM split_groups WHERE id = :id")
    suspend fun getGroup(id: String): SplitGroupWithDetails?

    @Transaction
    @Query("SELECT * FROM split_groups WHERE id = :id")
    fun observeGroup(id: String): Flow<SplitGroupWithDetails?>

    @Upsert
    suspend fun upsertGroup(group: SplitGroupEntity)

    @Query("DELETE FROM split_groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    // ---- People ----
    @Upsert
    suspend fun upsertPeople(people: List<SplitPersonEntity>)

    @Query("DELETE FROM split_people WHERE groupId = :groupId")
    suspend fun clearPeople(groupId: String)

    // ---- Spends ----
    @Upsert
    suspend fun upsertSpend(spend: SplitSpendEntity)

    @Query("DELETE FROM split_spends WHERE id = :id")
    suspend fun deleteSpend(id: String)

    @Upsert
    suspend fun upsertShares(shares: List<SplitSpendShareEntity>)

    @Query("DELETE FROM split_spend_shares WHERE spendId = :spendId")
    suspend fun clearShares(spendId: String)

    // ---- Settlements ----
    @Upsert
    suspend fun upsertSettlement(settlement: SplitSettlementEntity)

    @Query("DELETE FROM split_settlements WHERE id = :id")
    suspend fun deleteSettlement(id: String)

    @Query("SELECT * FROM split_settlements WHERE id = :id")
    suspend fun getSettlement(id: String): SplitSettlementEntity?
}
