package com.appriyo.repairmanager.domain.delivery

enum class DeliveryFilter(val key: String, val displayTitle: String) {
    TODAY("today", "Today's Deliveries"),
    TOMORROW("tomorrow", "Tomorrow's Deliveries"),
    OVERDUE("overdue", "Overdue Deliveries"),
    DELIVERED("delivered", "Delivered"),
    ALL("all", "All Deliveries");

    companion object {
        fun fromKey(key: String?): DeliveryFilter =
            entries.find { it.key == key } ?: ALL
    }
}