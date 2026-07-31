package com.cloudimny.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cloudimny.AppPreferences
import com.cloudimny.R
import com.cloudimny.views.home.HomeFragment
import com.cloudimny.views.setup.SetupCredentialsFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val authorized = AppPreferences.getAuthorized(this)
            val startFragment: Fragment =
                if (authorized) HomeFragment() else SetupCredentialsFragment()

            val transaction = supportFragmentManager.beginTransaction()
                .replace(R.id.main, startFragment)

            if (authorized) {
                transaction
                    .replace(R.id.mini_player_container, MiniPlayerFragment())
                    .replace(R.id.navigation_menu_container, NavigationMenuFragment())
            }

            transaction.commit()
        }
    }
}
