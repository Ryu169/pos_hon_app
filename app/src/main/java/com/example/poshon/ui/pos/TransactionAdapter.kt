package com.example.poshon.ui.pos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.entity.TransactionEntity
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<TransactionEntity>,
    private val onPlusClick: (TransactionEntity) -> Unit,   // Callback Plus
    private val onMinusClick: (TransactionEntity) -> Unit,  // Callback Minus
    private val onDeleteClick: (TransactionEntity) -> Unit  // Callback Delete
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvTransactionName)
        val tvQty: TextView = view.findViewById(R.id.tvTransactionQty)
        val tvPrice: TextView = view.findViewById(R.id.tvTransactionPrice) // Harga Satuan
        val tvTotal: TextView = view.findViewById(R.id.tvTransactionTotal)

        val btnPlus: ImageButton = view.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = view.findViewById(R.id.btnMinus)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTransaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0

        holder.tvName.text = item.productName
        holder.tvQty.text = item.quantity.toString() // Hanya angka
        holder.tvPrice.text = "@ ${formatRp.format(item.price)}"
        holder.tvTotal.text = formatRp.format(item.total)

        // AKSI TOMBOL
        holder.btnPlus.setOnClickListener { onPlusClick(item) }
        holder.btnMinus.setOnClickListener { onMinusClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<TransactionEntity>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}