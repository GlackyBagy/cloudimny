package com.cloudimny.views.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cloudimny.R
import com.cloudimny.models.SshConnectionCredentials
import com.cloudimny.security.ServerCertificateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.UserAuthException
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val setupScriptUrl: String =
    "https://raw.githubusercontent.com/GlackyBagy/cloudimny/setup-script/setup-script.bash"
private const val remoteCertificatePath: String = "/etc/ssl/cloudimny/server.crt"
private const val setupTimeoutMinutes: Long = 10

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected

    private val _completed = MutableLiveData(false)
    val completed: LiveData<Boolean> = _completed

    private val _errorMessageResId = MutableLiveData<Int?>(null)
    val errorMessageResId: LiveData<Int?> = _errorMessageResId

    fun connectAndConfigure(credentials: SshConnectionCredentials) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SSHClient().use { client ->
                    connect(credentials, client)
                    _connected.postValue(true)

                    val command = "echo ${shellQuote(credentials.password)} | sudo -S -v && " +
                            "export PROVIDED_ADDRESS=${shellQuote(credentials.host)} && " +
                            "curl -fsSL -O $setupScriptUrl && " +
                            "bash setup-script.bash > /tmp/cloudimny-setup.log 2>&1 && " +
                            "sudo openssl x509 -in $remoteCertificatePath -noout -fingerprint -sha256"

                    val (exitStatus, output) = client.startSession().use { session ->
                        val remoteCommand = session.exec(command)
                        val output = remoteCommand.inputStream.bufferedReader().readText()
                        remoteCommand.join(setupTimeoutMinutes, TimeUnit.MINUTES)
                        Pair(remoteCommand.exitStatus ?: -1, output)
                    }

                    if (exitStatus != 0) {
                        postError(R.string.setup_failed_message)
                        return@launch
                    }

                    val fingerprint = output.substringAfter("=", missingDelimiterValue = "").trim()
                    if (fingerprint.isEmpty()) {
                        postError(R.string.certificate_error_message)
                        return@launch
                    }

                    ServerCertificateStore.save(getApplication(), fingerprint, credentials.host)
                }
                _completed.postValue(true)
            } catch (_: UserAuthException) {
                postError(R.string.invalid_credentials_message)
            } catch (_: IOException) {
                postError(R.string.cannot_connect_message)
            }
        }
    }

    private fun postError(messageResId: Int) {
        _connected.postValue(false)
        _errorMessageResId.postValue(messageResId)
    }

    private fun connect(credentials: SshConnectionCredentials, client: SSHClient) {
        client.addHostKeyVerifier(PromiscuousVerifier()) // Trust-on-first-use
        client.connect(credentials.host, credentials.port)
        client.authPassword(credentials.username, credentials.password)
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
