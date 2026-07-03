package com.appriyo.repairmanager.domain.delivery

data class DeliverySummary(
    val todayCount: Int = 0,
    val tomorrowCount: Int = 0,
    val overdueCount: Int = 0,
    val deliveredCount: Int = 0,
    val allCount: Int = 0
)