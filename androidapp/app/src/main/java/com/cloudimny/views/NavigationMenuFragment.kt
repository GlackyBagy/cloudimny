package com.cloudimny.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cloudimny.R
import com.cloudimny.mirror.MirrorController
import com.cloudimny.mirror.MirrorService
import com.cloudimny.mirror.SourceSession
import com.cloudimny.views.home.HomeFragment
import com.cloudimny.views.playlist.CreatePlaylistFragment
import com.cloudimny.views.upload.UploadTrackFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class NavigationMenuFragment : Fragment(R.layout.fragment_navigation_menu) {

    private lateinit var homeButton: ImageButton
    private lateinit var homeLabel: TextView
    private lateinit var searchButton: ImageButton
    private lateinit var searchLabel: TextView
    private lateinit var mirrorButton: ImageButton
    private lateinit var mirrorLabel: TextView
    private lateinit var addButton: ImageButton

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startMirroring() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeButton = view.findViewById(R.id.home_button)
        homeLabel = view.findViewById(R.id.home_label)
        searchButton = view.findViewById(R.id.search_button)
        searchLabel = view.findViewById(R.id.search_label)
        mirrorButton = view.findViewById(R.id.mirror_button)
        mirrorLabel = view.findViewById(R.id.mirror_label)
        addButton = view.findViewById(R.id.add_button)

        mirrorButton.setOnClickListener { openMirror() }
        mirrorButton.setOnLongClickListener {
            stopMirror()
            true
        }

        // подсветка следует за состоянием, а не за нажатием: сервис поднимается и гаснет
        // асинхронно, а остановить зеркало можно ещё и кнопкой в уведомлении
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MirrorController.state.collect { applyMirrorTint() }
            }
        }

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

    /**
     * A tap always ends at the player, whether or not mirroring was already on. Making this a
     * toggle instead cost the only way back: with the player closed and no substitution playing,
     * nothing on screen led to it, and the button that looked like the way in switched it off.
     * Stopping moved to the long press.
     */
    private fun openMirror() {
        if (MirrorController.isActive) {
            (requireActivity() as MainActivity).openPlayer()
            return
        }

        // доступ к уведомлениям выдаётся только руками в системных настройках,
        // поэтому спрашиваем его до всего остального и выходим, если его нет
        if (!SourceSession.hasAccess(requireContext())) {
            requestNotificationAccess()
            return
        }

        requestBackgroundExemption()

        // без разрешения на уведомления сервис работает, но его уведомление скрыто —
        // вместе с кнопкой остановки, которая в нём единственная заметная
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMirroring()
        }
    }

    /**
     * Mirroring runs from a service rather than straight off [MirrorController]: between
     * substitutions our own player is paused, and without a foreground service the system is free
     * to reclaim the process — mirroring would stop with nothing to show for it.
     */
    private fun startMirroring() {
        MirrorService.start(requireContext())
        toast(R.string.mirror_started)
        (requireActivity() as MainActivity).openPlayer()
    }

    private fun stopMirror() {
        if (!MirrorController.isActive) return

        MirrorService.stop(requireContext())
        toast(R.string.mirror_stopped)
    }

    private fun requestNotificationAccess() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mirror_access_title)
            .setMessage(R.string.mirror_access_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.mirror_access_grant) { _, _ ->
                startActivity(SourceSession.accessSettingsIntent())
            }
            .show()
    }

    private fun requestBackgroundExemption() {
        val power = requireContext().getSystemService<PowerManager>() ?: return
        if (power.isIgnoringBatteryOptimizations(requireContext().packageName)) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mirror_battery_title)
            .setMessage(R.string.mirror_battery_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                )
            }
            .show()
    }

    private fun applyMirrorTint() {
        val color = ContextCompat.getColor(
            requireContext(),
            if (MirrorController.isActive) R.color.primary else R.color.text_secondary
        )
        mirrorButton.imageTintList = ColorStateList.valueOf(color)
        mirrorLabel.setTextColor(color)
    }

    private fun toast(messageResId: Int) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show()
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