package com.cloudimny.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cloudimny.R

class NavigationMenuFragment : Fragment(R.layout.fragment_navigation_menu) {

    private lateinit var homeButton: ImageButton
    private lateinit var homeLabel: TextView
    private lateinit var searchButton: ImageButton
    private lateinit var searchLabel: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeButton = view.findViewById(R.id.home_button)
        homeLabel = view.findViewById(R.id.home_label)
        searchButton = view.findViewById(R.id.search_button)
        searchLabel = view.findViewById(R.id.search_label)

        homeButton.setOnClickListener {
            selectTab(homeButton, homeLabel)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main, HomeFragment())
                .commit()
        }

        searchButton.setOnClickListener {
            selectTab(searchButton, searchLabel)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main, SearchFragment())
                .commit()
        }
    }

    private fun selectTab(selectedButton: ImageButton, selectedLabel: TextView) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        for ((button, label) in listOf(homeButton to homeLabel, searchButton to searchLabel)) {
            val color = if (button === selectedButton) activeColor else inactiveColor
            button.imageTintList = ColorStateList.valueOf(color)
            label.setTextColor(color)
        }
    }
}