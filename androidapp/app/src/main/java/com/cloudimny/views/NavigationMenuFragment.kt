package com.cloudimny.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cloudimny.R
import com.cloudimny.views.home.HomeFragment
import com.cloudimny.views.playlist.CreatePlaylistFragment
import com.cloudimny.views.upload.UploadTrackFragment

class NavigationMenuFragment : Fragment(R.layout.fragment_navigation_menu) {

    private lateinit var homeButton: ImageButton
    private lateinit var homeLabel: TextView
    private lateinit var searchButton: ImageButton
    private lateinit var searchLabel: TextView
    private lateinit var addButton: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeButton = view.findViewById(R.id.home_button)
        homeLabel = view.findViewById(R.id.home_label)
        searchButton = view.findViewById(R.id.search_button)
        searchLabel = view.findViewById(R.id.search_label)
        addButton = view.findViewById(R.id.add_button)

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

        addButton.setOnClickListener {
            showAddMenu(addButton)
        }
    }

    private fun showAddMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_add, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_upload_track -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main, UploadTrackFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.action_create_playlist -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main, CreatePlaylistFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }

        popup.show()
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