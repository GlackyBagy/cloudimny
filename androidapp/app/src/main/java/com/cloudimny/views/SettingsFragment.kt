package com.cloudimny.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cloudimny.AppPreferences
import com.cloudimny.R
import com.cloudimny.covers.CoverDiskCache
import com.cloudimny.server.ServerExport
import com.cloudimny.server.buildServerExport
import com.cloudimny.server.security.ServerCertificateStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.nav_settings))

        val addressInput: EditText = view.findViewById(R.id.server_address_input)
        val applyButton: Button = view.findViewById(R.id.server_address_apply)
        val exportButton: Button = view.findViewById(R.id.export_server_button)

        val currentHost = ServerCertificateStore.host(requireContext()).orEmpty()
        addressInput.setText(currentHost)

        addressInput.doAfterTextChanged { text ->
            val entered = text?.toString().orEmpty().trim()
            applyButton.isEnabled = entered.isNotEmpty() &&
                    entered != currentHost &&
                    ServerCertificateStore.isValidHost(entered)
        }

        applyButton.setOnClickListener {
            val entered = addressInput.text.toString().trim()
            if (!ServerCertificateStore.isValidHost(entered)) {
                toast(getString(R.string.settings_address_invalid))
                return@setOnClickListener
            }
            confirmAddressChange(ServerCertificateStore.normalizeHost(entered))
        }

        setUpCacheSlider(
            slider = view.findViewById(R.id.track_cache_slider),
            valueLabel = view.findViewById(R.id.track_cache_value),
            minMb = AppPreferences.MIN_TRACK_CACHE_MB,
            maxMb = AppPreferences.MAX_TRACK_CACHE_MB,
            stepMb = AppPreferences.TRACK_CACHE_STEP_MB,
            storedMb = AppPreferences.getTrackCacheMb(requireContext()),
            onChanged = { AppPreferences.setTrackCacheMb(requireContext(), it) }
        )

        val coverSlider: Slider = view.findViewById(R.id.cover_cache_slider)
        setUpCacheSlider(
            slider = coverSlider,
            valueLabel = view.findViewById(R.id.cover_cache_value),
            minMb = AppPreferences.MIN_COVER_CACHE_MB,
            maxMb = AppPreferences.MAX_COVER_CACHE_MB,
            stepMb = AppPreferences.COVER_CACHE_STEP_MB,
            storedMb = AppPreferences.getCoverCacheMb(requireContext()),
            onChanged = { AppPreferences.setCoverCacheMb(requireContext(), it) }
        )
        trimCoversOnRelease(coverSlider)

        exportButton.setOnClickListener { exportServer() }
    }

    private fun setUpCacheSlider(
        slider: Slider,
        valueLabel: TextView,
        minMb: Int,
        maxMb: Int,
        stepMb: Int,
        storedMb: Int,
        onChanged: (Int) -> Unit
    ) {
        // диапазон задаётся кодом, а не в разметке: границы живут в AppPreferences,
        // и XML не смог бы на них сослаться, а две копии чисел разъехались бы
        slider.valueFrom = minMb.toFloat()
        slider.valueTo = maxMb.toFloat()
        slider.stepSize = stepMb.toFloat()

        slider.value = storedMb.toFloat()
        valueLabel.text = getString(R.string.settings_cache_size_value, storedMb)

        slider.addOnChangeListener { _, value, fromUser ->
            val megabytes = value.toInt()
            valueLabel.text = getString(R.string.settings_cache_size_value, megabytes)
            if (fromUser) onChanged(megabytes)
        }
    }

    /**
     * Runs on release rather than on every step of the drag: the trim walks the whole cache
     * directory, and doing that once per notch would be pointless I/O.
     */
    private fun trimCoversOnRelease(slider: Slider) {
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit

            override fun onStopTrackingTouch(slider: Slider) {
                viewLifecycleOwner.lifecycleScope.launch {
                    CoverDiskCache.trimToLimit(requireContext())
                }
            }
        })
    }

    private fun confirmAddressChange(host: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_address_confirm_title)
            .setMessage(getString(R.string.settings_address_confirm_message, host))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.settings_address_confirm_action) { _, _ ->
                ServerCertificateStore.saveHost(requireContext(), host)
                toast(getString(R.string.settings_address_changed))
            }
            .show()
    }

    private fun exportServer() {
        val context = requireContext()
        val host = ServerCertificateStore.host(context)
        val fingerprint = ServerCertificateStore.fingerprint(context)
        val authSecret = ServerCertificateStore.authSecret(context)

        if (host == null || fingerprint == null || authSecret == null) {
            toast(getString(R.string.settings_export_unavailable))
            return
        }

        val payload = buildServerExport(ServerExport(host, fingerprint, authSecret))

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, EXPORT_FILE_NAME)
            putExtra(Intent.EXTRA_TEXT, payload)
        }

        startActivity(
            Intent.createChooser(share, getString(R.string.settings_export_button))
        )
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val EXPORT_FILE_NAME = "cloudimny-server.json"
    }
}
