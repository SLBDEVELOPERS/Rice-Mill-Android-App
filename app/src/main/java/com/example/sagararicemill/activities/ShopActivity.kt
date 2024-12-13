package com.example.sagararicemill.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.ShopAdapter
import com.example.sagararicemill.models.Shop
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ShopActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddShop: FloatingActionButton
    private lateinit var shopAdapter: ShopAdapter
    private val shopList = mutableListOf<Shop>()

    private val db = FirebaseFirestore.getInstance()
    private val shopsRef = db.collection("shops")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerViewShops)
        fabAddShop = findViewById(R.id.fabAddShop)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        shopAdapter = ShopAdapter(
            this,
            shopList,
            onItemClick = { shop -> navigateToAddEditShop(shop) },
            onItemLongClick = { shop -> showDeleteConfirmation(shop) }
        )
        recyclerView.adapter = shopAdapter

        // Fetch Shops from Firestore
        fetchShops()

        // FAB Click Listener to Add Shop
        fabAddShop.setOnClickListener {
            navigateToAddEditShop(null)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchShops()
    }

    private fun fetchShops() {
        shopsRef.orderBy("name").get()
            .addOnSuccessListener { documents ->
                shopList.clear()
                for (document in documents) {
                    val shop = document.toObject(Shop::class.java)
                    shop.id = document.id
                    shopList.add(shop)
                }
                shopAdapter.updateList(shopList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching shops: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToAddEditShop(shop: Shop?) {
        val intent = Intent(this, AddEditShopActivity::class.java)
        if (shop != null) {
            intent.putExtra("shopId", shop.id)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(shop: Shop) {
        AlertDialog.Builder(this)
            .setTitle("Delete Shop")
            .setMessage("Are you sure you want to delete '${shop.name}'?")
            .setPositiveButton("Yes") { _, _ ->
                deleteShop(shop)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteShop(shop: Shop) {
        shopsRef.document(shop.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Shop deleted successfully", Toast.LENGTH_SHORT).show()
                fetchShops()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting shop: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Inflate Menu for Search and Sorting
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.shop_top_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        searchView?.queryHint = "Search Shops..."
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search on submit if needed
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                shopAdapter.filter.filter(newText)
                return true
            }
        })

        return true
    }

    // Handle Menu Item Selections
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_name -> {
                sortShopsByName()
                true
            }
            R.id.action_sort_due -> {
                sortShopsByDue()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sortShopsByName() {
        val sortedList = shopList.sortedBy { it.name.toLowerCase(Locale.getDefault()) }
        shopAdapter.updateList(sortedList)
    }

    private fun sortShopsByDue() {
        val sortedList = shopList.sortedByDescending { it.outstandingDue }
        shopAdapter.updateList(sortedList)
    }
}
