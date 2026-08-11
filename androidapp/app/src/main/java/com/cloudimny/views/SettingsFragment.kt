package com.cloudimny.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.cloudimny.R
import com.cloudimny.server.security.ServerCertificateStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

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

        exportButton.setOnClickListener { exportServer() }
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

        val payload = JSONObject()
            .put("host", host)
            .put("fingerprint", fingerprint)
            .put("authSecret", authSecret)
            .toString(2)

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
