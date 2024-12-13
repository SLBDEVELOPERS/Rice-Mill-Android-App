package com.example.sagararicemill.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sagararicemill.R
import com.example.sagararicemill.activities.*
import com.example.sagararicemill.adapters.HomeAdapter
import com.example.sagararicemill.models.MenuItem

class HomeFragment : Fragment() {

    private lateinit var recyclerViewHome: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_home, container, false)
        recyclerViewHome = view.findViewById(R.id.recyclerViewHome)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuItems = listOf(
            MenuItem(
                title = "Shop Management",
                iconRes = R.drawable.ic_store,
                action = { startActivity(Intent(requireContext(), ShopActivity::class.java)) }
            ),
            MenuItem(
                title = "Inventory Management",
                iconRes = R.drawable.ic_list_alt,
                action = { startActivity(Intent(requireContext(), InventoryActivity::class.java)) }
            ),
            MenuItem(
                title = "Issue Rice Bags",
                iconRes = R.drawable.ic_local_shipping,
                action = { startActivity(Intent(requireContext(), IssueRiceActivity::class.java)) }
            ),
            MenuItem(
                title = "Bills",
                iconRes = R.drawable.ic_receipt,
                action = { startActivity(Intent(requireContext(), BillListActivity::class.java)) }
            ),
            MenuItem(
                title = "Reports",
                iconRes = R.drawable.ic_assessment,
                action = { startActivity(Intent(requireContext(), ReportsActivity::class.java)) }
            ),
            MenuItem(
                title = "Lorry Management",
                iconRes = R.drawable.ic_truck,
                action = { startActivity(Intent(requireContext(), LorryActivity::class.java)) }
            )
            // Add more features as needed
        )

        val adapter = HomeAdapter(menuItems)
        recyclerViewHome.adapter = adapter

        // Use a GridLayoutManager to display items in a grid
        recyclerViewHome.layoutManager = GridLayoutManager(requireContext(), 2) // 2 columns
    }
}

