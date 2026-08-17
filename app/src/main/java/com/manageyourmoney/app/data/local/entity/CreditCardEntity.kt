package com.manageyourmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the web app's `creditcards` storage key (an array of {id, name, billingDay}).
 */
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val billingDay: Int,
)
