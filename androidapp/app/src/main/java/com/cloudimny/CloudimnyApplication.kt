package com.cloudimny

import android.app.Application
import com.cloudimny.server.UploadNotifications
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class CloudimnyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        UploadNotifications.createChannel(this)
    }
}
