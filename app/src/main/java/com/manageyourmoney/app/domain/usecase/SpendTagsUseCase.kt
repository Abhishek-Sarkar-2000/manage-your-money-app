package com.manageyourmoney.app.domain.usecase

import com.manageyourmoney.app.data.local.dao.CustomTagDao
import com.manageyourmoney.app.data.local.entity.CustomTagEntity
import javax.inject.Inject

/** Direct port of `DEFAULT_TAGS` (index.html:629). */
object DefaultTags {
    val values = listOf(
        "Groceries", "Dining", "Fuel", "Subscription", "Rent", "Utility", "Recharge", "Transport", "Gift"
    )
}

/**
 * Direct port of `allSpendTags()` (index.html:656-666): default tags first, then the
 * user's custom ones, de-duplicated case-insensitively while preserving first-seen casing.
 */
class GetAllSpendTagsUseCase @Inject constructor(
    private val customTagDao: CustomTagDao,
) {
    suspend operator fun invoke(): List<String> {
        val custom = customTagDao.getAllTags().map { it.name }
        val seen = HashSet<String>()
        val out = mutableListOf<String>()
        for (t in DefaultTags.values + custom) {
            val key = t.trim().lowercase()
            if (key.isEmpty() || !seen.add(key)) continue
            out.add(t)
        }
        return out
    }
}

/**
 * Direct port of the custom-tag branch of `resolveTagFromForm()` (index.html:668-682):
 * persists a brand-new custom tag the first time it's used (case-insensitive de-dupe
 * against the combined default+custom list), otherwise a no-op.
 */
class AddCustomTagIfNewUseCase @Inject constructor(
    private val customTagDao: CustomTagDao,
    private val getAllSpendTags: GetAllSpendTagsUseCase,
) {
    suspend operator fun invoke(candidate: String): String {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty()) return ""
        val exists = getAllSpendTags().any { it.equals(trimmed, ignoreCase = true) }
        if (!exists) {
            customTagDao.insertTag(CustomTagEntity(trimmed))
        }
        return trimmed
    }
}
