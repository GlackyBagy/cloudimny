package com.cloudimny.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cloudimny.R
import com.cloudimny.views.setup.SetupCredentialsFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main, SetupCredentialsFragment())
                .commit()
        }
    }
}