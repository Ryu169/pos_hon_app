package com.example.poshon.ui.pos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.poshon.R
import com.example.poshon.data.entity.ProductEntity
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ProductGridAdapter(
    private var products: List<ProductEntity>,
    private val onProductClick: (ProductEntity) -> Unit
) : RecyclerView.Adapter<ProductGridAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvProductPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0

        holder.tvName.text = product.name
        holder.tvPrice.text = formatRp.format(product.price)

        // Tampilkan Gambar dengan Coil
        if (product.imageUri != null) {
            holder.ivImage.load(product.imageUri) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Saat Kartu di-Klik
        holder.itemView.setOnClickListener {
            onProductClick(product)
        }
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<ProductEntity>) {
        products = newProducts
        notifyDataSetChanged()
    }
}