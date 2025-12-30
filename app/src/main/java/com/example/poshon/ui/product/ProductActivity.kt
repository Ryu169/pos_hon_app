package com.example.poshon.ui.product

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductActivity : AppCompatActivity() {

    private lateinit var db: PosDatabase
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        db = PosDatabase.getInstance(this)

        val database = PosDatabase.getInstance(this)
        val productDao = database.productDao()
        val etName = findViewById<EditText>(R.id.etProductName)
        val etPrice = findViewById<EditText>(R.id.etProductPrice)
        val btnSave = findViewById<Button>(R.id.btnSaveProduct)
        val recyclerView = findViewById<RecyclerView>(R.id.rvProduct)

        adapter = ProductAdapter(emptyList()) { product ->
            deleteProduct(product)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val price = etPrice.text.toString().toIntOrNull() ?: 0

            if (name.isNotEmpty()) {
                saveProduct(name, price)
                etName.text.clear()
                etPrice.text.clear()
            }
        }

        loadProducts()
    }

    private fun loadProducts() {
        CoroutineScope(Dispatchers.IO).launch {
            val products = db.productDao().getAllProducts()
            withContext(Dispatchers.Main) {
                adapter.updateData(products)
            }
        }
    }

    private fun saveProduct(name: String, price: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            db.productDao().insertProduct(
                ProductEntity(name = name, price = price)
            )
            loadProducts()
        }
    }

    private fun deleteProduct(product: ProductEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            db.productDao().deleteProduct(product)
            loadProducts()
        }
    }
}
