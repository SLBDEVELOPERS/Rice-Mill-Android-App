//package com.example.sagararicemill.fragment
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.GridView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.cardview.widget.CardView
//import androidx.recyclerview.widget.GridLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.sagararicemill.R
//import com.example.sagararicemill.activities.*
//import com.example.sagararicemill.adapters.HomeAdapter
//import com.example.sagararicemill.models.MenuItem
//
//class HomeFragment : Fragment() {
//
//    private lateinit var recyclerViewHome: RecyclerView
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.activity_home, container, false)
//        recyclerViewHome = view.findViewById(R.id.recyclerViewHome)
//        return view
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val menuItems = listOf(
//            MenuItem(
//                title = "Manage Shops",
//                iconRes = R.drawable.ic_store,
//                action = { startActivity(Intent(requireContext(), ShopActivity::class.java)) }
//            ),
//            MenuItem(
//                title = "Stock Management",
//                iconRes = R.drawable.ic_list_alt,
//                action = { startActivity(Intent(requireContext(), InventoryActivity::class.java)) }
//            ),
//            MenuItem(
//                title = "Distribute Rice",
//                iconRes = R.drawable.ic_local_shipping,
//                action = { startActivity(Intent(requireContext(), IssueRiceActivity::class.java)) }
//            ),
//            MenuItem(
//                title = "Billing",
//                iconRes = R.drawable.ic_receipt,
//                action = { startActivity(Intent(requireContext(), BillListActivity::class.java)) }
//            ),
//            MenuItem(
//                title = "Reports",
//                iconRes = R.drawable.ic_assessment,
//                action = { startActivity(Intent(requireContext(), ReportsActivity::class.java)) }
//            ),
//            MenuItem(
//                title = "Fleet Management",
//                iconRes = R.drawable.ic_truck,
//                action = { startActivity(Intent(requireContext(), LorryActivity::class.java)) }
//            )
//            // Add more features as needed
//        )
//
//        val adapter = HomeAdapter(menuItems)
//        recyclerViewHome.adapter = adapter
//
//        // Use a GridLayoutManager to display items in a grid
//        recyclerViewHome.layoutManager = GridLayoutManager(requireContext(), 2) // 2 columns
//    }
//}
//

package com.example.sagararicemill.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.sagararicemill.R
import com.example.sagararicemill.activities.*

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the card views
        val manageShopsCard = view.findViewById<CardView>(R.id.manageShopsCard)
        val stockManagementCard = view.findViewById<CardView>(R.id.stockManagementCard)
        val distributeRiceCard = view.findViewById<CardView>(R.id.distributeRiceCard)
        val billingCard = view.findViewById<CardView>(R.id.billingCard)
        val reportsCard = view.findViewById<CardView>(R.id.reportsCard)
        val fleetManagementCard = view.findViewById<CardView>(R.id.fleetManagementCard)

        // Set click listeners
        manageShopsCard.setOnClickListener {
            startActivity(Intent(requireContext(), ShopActivity::class.java))
        }

        stockManagementCard.setOnClickListener {
            startActivity(Intent(requireContext(), InventoryActivity::class.java))
        }

        distributeRiceCard.setOnClickListener {
            startActivity(Intent(requireContext(), IssueRiceActivity::class.java))
        }

        billingCard.setOnClickListener {
            startActivity(Intent(requireContext(), BillListActivity::class.java))
        }

        reportsCard.setOnClickListener {
            startActivity(Intent(requireContext(), ReportsActivity::class.java))
        }

        fleetManagementCard.setOnClickListener {
            startActivity(Intent(requireContext(), LorryActivity::class.java))
        }
    }

    // Click handler for "Manage Shops"
    fun onManageShopsClick(view: View) {
        startActivity(Intent(requireContext(), ShopActivity::class.java))
    }

    // Click handler for "Stock Management"
    fun onStockManagementClick(view: View) {
        startActivity(Intent(requireContext(), InventoryActivity::class.java))
    }

    // Click handler for "Distribute Rice"
    fun onDistributeRiceClick(view: View) {
        startActivity(Intent(requireContext(), IssueRiceActivity::class.java))
    }

    // Click handler for "Billing"
    fun onBillingClick(view: View) {
        startActivity(Intent(requireContext(), BillListActivity::class.java))
    }

    // Click handler for "Reports"
    fun onReportsClick(view: View) {
        startActivity(Intent(requireContext(), ReportsActivity::class.java))
    }

    // Click handler for "Fleet Management"
    fun onFleetManagementClick(view: View) {
        startActivity(Intent(requireContext(), LorryActivity::class.java))
    }
}
