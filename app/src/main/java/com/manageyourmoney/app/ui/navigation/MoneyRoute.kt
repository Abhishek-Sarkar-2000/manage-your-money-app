package com.manageyourmoney.app.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions (Navigation Compose 2.8's `@Serializable` route objects)
 * mirroring the web app's `State.view` single-page-app router (index.html:633
 * `view: 'home'`, plus the month-detail / split-money view switches throughout
 * `render()`). Each object/class below is a distinct back-stack entry, so predictive
 * back naturally unwinds Home -> MonthDetail -> (form panel handled in-screen) the same
 * layered way the original `history.pushState`-free SPA faked with `State.view`.
 */
sealed interface MoneyRoute {

    @Serializable
    data object Home : MoneyRoute

    @Serializable
    data class MonthDetail(val monthKey: String) : MoneyRoute

    @Serializable
    data object SplitMoney : MoneyRoute

    @Serializable
    data class SplitGroupDetail(val groupId: String) : MoneyRoute

    @Serializable
    data object ManageCards : MoneyRoute

    @Serializable
    data object ManageEmis : MoneyRoute
}
