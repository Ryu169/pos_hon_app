package com.example.poshon.ui.pos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.entity.TransactionEntity

class TransactionAdapter(
    private var items: List<TransactionEntity>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProduct: TextView = view.findViewById(R.id.tvProduct)
        val tvDetail: TextView = view.findViewById(R.id.tvDetail)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvProduct.text = item.productName
        holder.tvDetail.text = "${item.quantity} x ${item.price}"
        holder.tvTotal.text = "Rp ${item.total}"
    }

    fun updateData(newItems: List<TransactionEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
