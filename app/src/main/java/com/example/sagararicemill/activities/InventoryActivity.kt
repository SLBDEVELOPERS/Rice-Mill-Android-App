package com.example.sagararicemill.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.adapters.RiceBagAdapter
import com.example.sagararicemill.models.RiceBag
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class InventoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddRiceBag: FloatingActionButton
    private lateinit var riceBagAdapter: RiceBagAdapter
    private val riceBagList = mutableListOf<RiceBag>()

    private val db = FirebaseFirestore.getInstance()
    private val riceBagsRef = db.collection("rice_bags")

    // Threshold for low stock alerts
    private val lowStockThreshold = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerViewInventory)
        fabAddRiceBag = findViewById(R.id.fabAddRiceBag)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        riceBagAdapter = RiceBagAdapter(
            this,
            riceBagList,
            onItemClick = { riceBag -> navigateToAddEditRiceBag(riceBag) },
            onItemLongClick = { riceBag -> showDeleteConfirmation(riceBag) }
        )
        recyclerView.adapter = riceBagAdapter

        // Fetch Rice Bags from Firestore with real-time updates
        riceBagsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            riceBagList.clear()
            for (document in snapshots!!) {
                val riceBag = document.toObject(RiceBag::class.java)
                riceBag.id = document.id
                riceBagList.add(riceBag)
            }
            riceBagAdapter.updateList(riceBagList)

            // Check for low stock and alert
            checkLowStock()
        }

        // FAB Click Listener to Add Rice Bag
        fabAddRiceBag.setOnClickListener {
            navigateToAddEditRiceBag(null)
        }

        // Implement Swipe to Delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We are not moving items up/down
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val riceBag = riceBagList[position]

                AlertDialog.Builder(this@InventoryActivity)
                    .setTitle("Delete Rice Bag")
                    .setMessage("Are you sure you want to delete '${riceBag.name}'?")
                    .setPositiveButton("Yes") { _, _ ->
                        deleteRiceBag(riceBag, position)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        riceBagAdapter.notifyItemChanged(position) // Restore the swiped item
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

  /*  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.inventory_top_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        searchView?.queryHint = "Search Rice Bags..."
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search on submit if needed
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                riceBagAdapter.filter.filter(newText)
                return true
            }
        })

        return true
    } */

  /*  override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_name -> {
                sortRiceBagsByName()
                true
            }
            R.id.action_sort_price -> {
                sortRiceBagsByPrice()
                true
            }
            R.id.action_sort_stock -> {
                sortRiceBagsByStock()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    } */

    private fun sortRiceBagsByName() {
        val sortedList = riceBagList.sortedBy { it.name.toLowerCase(Locale.getDefault()) }
        riceBagAdapter.updateList(sortedList)
        Toast.makeText(this, "Sorted by Name", Toast.LENGTH_SHORT).show()
    }

    private fun sortRiceBagsByPrice() {
        val sortedList = riceBagList.sortedBy { it.price }
        riceBagAdapter.updateList(sortedList)
        Toast.makeText(this, "Sorted by Price", Toast.LENGTH_SHORT).show()
    }

    private fun sortRiceBagsByStock() {
        val sortedList = riceBagList.sortedByDescending { it.stock }
        riceBagAdapter.updateList(sortedList)
        Toast.makeText(this, "Sorted by Stock", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAddEditRiceBag(riceBag: RiceBag?) {
        val intent = Intent(this, AddEditRiceBagActivity::class.java)
        if (riceBag != null) {
            intent.putExtra("riceBagId", riceBag.id)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(riceBag: RiceBag) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rice Bag")
            .setMessage("Are you sure you want to delete '${riceBag.name}'?")
            .setPositiveButton("Yes") { _, _ ->
                deleteRiceBag(riceBag, riceBagList.indexOf(riceBag))
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteRiceBag(riceBag: RiceBag, position: Int) {
        riceBagsRef.document(riceBag.id).delete()
            .addOnSuccessListener {
                riceBagList.removeAt(position)
                riceBagAdapter.updateList(riceBagList)
                Toast.makeText(this, "Rice Bag deleted successfully", Toast.LENGTH_SHORT).show()
                // Show Snackbar with Undo option
                Snackbar.make(findViewById(R.id.coordinatorLayoutInventory), "Rice Bag deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        // Re-add the rice bag
                        riceBagsRef.document(riceBag.id).set(riceBag)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Deletion undone", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error restoring rice bag: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting rice bag: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkLowStock() {
        val lowStockBags = riceBagList.filter { it.stock <= lowStockThreshold }
        if (lowStockBags.isNotEmpty()) {
            val message = "Low stock for ${lowStockBags.size} rice bag(s)"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            // Optionally, highlight low stock items in the RecyclerView
        }
    }
}
