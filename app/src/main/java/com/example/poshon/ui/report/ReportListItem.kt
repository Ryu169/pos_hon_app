package com.example.poshon.ui.report

import com.example.poshon.data.entity.OrderEntity

// Model pintar untuk membedakan Header vs Item
sealed class ReportListItem {
    data class DateHeader(
        val date: String,
        val totalDaily: Int
    ) : ReportListItem()

    data class TransactionItem(
        val order: OrderEntity
    ) : ReportListItem()
}