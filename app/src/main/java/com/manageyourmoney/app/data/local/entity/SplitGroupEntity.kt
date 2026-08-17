package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors one `split:<id>` group record (minus its people/spends/settlements, each
 *  normalized into their own table below), plus its presence in `splits-index`. */
@Entity(tableName = "split_groups")
data class SplitGroupEntity(
    @PrimaryKey val id: String,
    val description: String,
    val createdAt: String, // "yyyy-MM-dd"
)

/** Normalized form of `group.people: string[]`. The constant "YOU" (== SPLIT_YOU in the
 *  web app) is always present and represents the app's own user in the split math. */
@Entity(
    tableName = "split_people",
    primaryKeys = ["groupId", "name"],
    foreignKeys = [ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId")]
)
data class SplitPersonEntity(
    val groupId: String,
    val name: String,
    /** Preserves the original array order so the UI lists members the way they were added. */
    val sortOrder: Int,
)

const val SPLIT_YOU = "YOU"
