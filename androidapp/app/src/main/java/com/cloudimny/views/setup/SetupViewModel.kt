package com.cloudimny.views.setup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudimny.R
import com.cloudimny.models.SshConnectionCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.userauth.UserAuthException
import java.io.IOException

class SetupViewModel : ViewModel() {
    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected

    private val _errorMessageResId = MutableLiveData<Int?>(null)
    val errorMessageResId: LiveData<Int?> = _errorMessageResId

    fun connectAndConfigure(credentials: SshConnectionCredentials) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SSHClient().use { client ->
                    connect(credentials, client)

                    client.startSession().use { session ->
//                        session.exec()  // todo
                    }
                }
                _connected.postValue(true)
            } catch (e: UserAuthException) {
                postError(R.string.invalid_credentials_message)
            } catch (e: IOException) {
                postError(R.string.cannot_connect_message)
            }
        }
    }

    private fun postError(messageResId: Int) {
        _connected.postValue(false)
        _errorMessageResId.postValue(messageResId)
    }

    private fun connect(credentials: SshConnectionCredentials, client: SSHClient) {
        val (host, port) = credentials.address.split(":")
        client.connect(host, port.toInt())
        client.authPassword(credentials.username, credentials.password)
    }
}
