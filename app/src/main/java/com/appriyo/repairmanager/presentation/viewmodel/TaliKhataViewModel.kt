package com.appriyo.repairmanager.presentation.viewmodel

/**
 * Chip filter shown above the TaliKhata list.
 */
enum class TaliKhataFilter(val label: String) {
    ALL("All"),
    YOU_OWE("You Owe"),
    THEY_OWE_YOU("Owes You")
}

/**
 * Sort options exposed in the TaliKhata list toolbar.
 */
enum class TaliKhataSortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_BALANCE("Highest Balance"),
    LOWEST_BALANCE("Lowest Balance"),
    ALPHABETICAL("Alphabetical")
}

/**
 * Aggregate figures shown in the summary dashboard at the top of the
 * TaliKhata screen. Computed over ALL entries (ignores search/filter/sort).
 */
data class TaliKhataSummary(
    val totalEntries: Int = 0,
    val totalYouOwe: Double = 0.0,
    val totalTheyOweYou: Double = 0.0
) {
    /** Positive = net you are owed. Negative = net you owe. */
    val netBalance: Double
        get() = totalTheyOweYou - totalYouOwe
}