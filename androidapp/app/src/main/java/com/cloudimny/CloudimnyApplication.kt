package com.cloudimny

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class CloudimnyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}
