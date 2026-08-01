package com.cloudimny.views

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.cloudimny.AppPreferences
import com.cloudimny.R
import com.cloudimny.views.home.HomeFragment
import com.cloudimny.views.setup.SetupCredentialsFragment

class MainActivity : AppCompatActivity() {
    fun setHeaderTitle(title: CharSequence) {
        findViewById<TextView>(R.id.header_title).apply {
            text = title
            visibility = View.VISIBLE
        }
    }

    fun openPlayer() {
        findViewById<View>(R.id.player_container).visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.player_container, PlayerFragment())
            .addToBackStack("player")
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                findViewById<View>(R.id.player_container).visibility = View.GONE
            }
        }

        val header = findViewById<TextView>(R.id.header_title)
        ViewCompat.setOnApplyWindowInsetsListener(header) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBars.top)
            insets
        }

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
