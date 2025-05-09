@file:JvmName("ModulesActivity") // ANALYTICS
package org.mtransit.android.ui.modules

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.enableEdgeToEdgeMT
import org.mtransit.android.ui.setUpStatusBarBgEdgeToEdge

@AndroidEntryPoint
class ModulesActivity : MTActivity(R.layout.activity_modules) {

    companion object {

        private val LOG_TAG = ModulesActivity::class.java.simpleName

        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, ModulesActivity::class.java)
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeMT()
        window.decorView // fix random crash (gesture nav back then re-open app)
        setUpStatusBarBgEdgeToEdge(transparent = false)
        super.onCreate(savedInstanceState)
    }

    override val currentFragment: Fragment?
        get() = supportFragmentManager.primaryNavigationFragment

    override fun onSupportNavigateUp(): Boolean {
        return when {
            supportFragmentManager.popBackStackImmediate() -> true
            super.onSupportNavigateUp() -> true
            else -> {
                finish() // close screen
                true
            }
        }
    }
}