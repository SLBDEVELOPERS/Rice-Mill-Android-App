package com.example.sagararicemill.models


data class MenuItem(
    val title: String,
    val iconRes: Int,          // Drawable resource ID for the menu item icon
    val action: () -> Unit     // A lambda that executes when this menu item is clicked
)
