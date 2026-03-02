package com.example.poshon.ui.report

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.entity.OrderEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReportAdapter(
    private var items: List<ReportListItem>,
    private val onItemClick: (OrderEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHeaderDate)
        val tvTotal: TextView = view.findViewById(R.id.tvHeaderTotal)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvTimeId: TextView = view.findViewById(R.id.tvTimeId)
        val tvMethod: TextView = view.findViewById(R.id.tvPaymentMethod)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ReportListItem.DateHeader -> TYPE_HEADER
            is ReportListItem.TransactionItem -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_card, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0

        when (val item = items[position]) {
            is ReportListItem.DateHeader -> {
                val headerHolder = holder as HeaderViewHolder
                try {
                    val inputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US) // Bisa ganti Locale("id", "ID") untuk B.Indo
                    val dateObj = inputFormat.parse(item.date)
                    headerHolder.tvDate.text = if (dateObj != null) outputFormat.format(dateObj) else item.date
                } catch (e: Exception) {
                    headerHolder.tvDate.text = item.date
                }
                headerHolder.tvTotal.text = formatRp.format(item.totalDaily)
            }
            is ReportListItem.TransactionItem -> {
                val itemHolder = holder as ItemViewHolder
                val order = item.order

                itemHolder.tvAmount.text = formatRp.format(order.totalAmount)

                // 🌟 BAGIAN INI YANG DIUBAH 🌟
                // Dulu: order.time + " - #TRX" + order.id
                // Sekarang: order.time + " - " + order.customerName
                itemHolder.tvTimeId.text = "${order.time} - ${order.customerName}"

                itemHolder.tvMethod.text = order.paymentMethod

                if (order.paymentMethod == "QRIS") {
                    itemHolder.tvMethod.setBackgroundResource(R.drawable.bg_badge_blue)
                } else {
                    itemHolder.tvMethod.setBackgroundResource(R.drawable.bg_badge_blue)
                }

                itemHolder.itemView.setOnClickListener {
                    onItemClick(order)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<ReportListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}