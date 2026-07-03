package com.appriyo.repairmanager.domain.delivery

import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.model.RepairStatus
import com.appriyo.repairmanager.presentation.utils.DeliveryDateUtils
import java.time.LocalDate

object DeliveryFilterUtils {

    private fun isActive(repair: Repair): Boolean =
        repair.status != RepairStatus.DELIVERED && repair.status != RepairStatus.CANCELLED

    fun matches(repair: Repair, filter: DeliveryFilter, today: LocalDate = LocalDate.now()): Boolean =
        when (filter) {
            DeliveryFilter.TODAY ->
                isActive(repair) && DeliveryDateUtils.isToday(repair.expectedDeliveryDate, today)
            DeliveryFilter.TOMORROW ->
                isActive(repair) && DeliveryDateUtils.isTomorrow(repair.expectedDeliveryDate, today)
            DeliveryFilter.OVERDUE ->
                isActive(repair) && DeliveryDateUtils.isOverdue(repair.expectedDeliveryDate, today)
            DeliveryFilter.DELIVERED ->
                repair.status == RepairStatus.DELIVERED
            DeliveryFilter.ALL ->
                repair.expectedDeliveryDate.isNotBlank()
        }

    fun filter(repairs: List<Repair>, filter: DeliveryFilter, today: LocalDate = LocalDate.now()): List<Repair> =
        repairs.filter { matches(it, filter, today) }

    fun summarize(repairs: List<Repair>, today: LocalDate = LocalDate.now()): DeliverySummary = DeliverySummary(
        todayCount = filter(repairs, DeliveryFilter.TODAY, today).size,
        tomorrowCount = filter(repairs, DeliveryFilter.TOMORROW, today).size,
        overdueCount = filter(repairs, DeliveryFilter.OVERDUE, today).size,
        deliveredCount = filter(repairs, DeliveryFilter.DELIVERED, today).size,
        allCount = filter(repairs, DeliveryFilter.ALL, today).size
    )
}