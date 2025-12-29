package com.example.poshon.ui.product

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.entity.ProductEntity

class ProductAdapter(
    private var products: List<ProductEntity>,
    private val onDeleteClick: (ProductEntity) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.tvName.text = product.name
        holder.tvPrice.text = "Rp ${product.price}"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(product)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateData(newProducts: List<ProductEntity>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
