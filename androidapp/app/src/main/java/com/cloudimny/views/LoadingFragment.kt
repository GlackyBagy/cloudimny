package com.cloudimny.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import com.cloudimny.R

private const val ARG_PRIMARY_TEXT = "primary_text"
private const val ARG_SECONDARY_TEXT = "secondary_text"

class LoadingFragment : Fragment(R.layout.fragment_loading) {
    private var primaryText: String? = null
    private var secondaryText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            primaryText = it.getString(ARG_PRIMARY_TEXT)
            secondaryText = it.getString(ARG_SECONDARY_TEXT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.primary_text).text = primaryText ?: ""
        view.findViewById<TextView>(R.id.secondary_text).text = secondaryText ?: ""
    }

    companion object {
        @JvmStatic
        fun newInstance(primaryText: String?, secondaryText: String?) =
            LoadingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRIMARY_TEXT, primaryText)
                    putString(ARG_SECONDARY_TEXT, secondaryText)
                }
            }
    }
}