package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the web app's `custom-spend-tags` storage key. Combined at read time with the
 * hardcoded DEFAULT_TAGS list (see [com.manageyourmoney.app.domain.DefaultTags]) the same
 * way `allSpendTags()` merges them, de-duped case-insensitively.
 */
@Entity(tableName = "custom_tags")
data class CustomTagEntity(
    @PrimaryKey val name: String,
)
