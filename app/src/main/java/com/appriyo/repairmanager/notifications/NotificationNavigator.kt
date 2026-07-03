package com.appriyo.repairmanager.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationNavigator {
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute = _pendingRoute.asStateFlow()

    fun navigateTo(route: String) {
        _pendingRoute.value = route
    }

    fun consume() {
        _pendingRoute.value = null
    }
}