package org.mtransit.android.ui.purchase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.enableEdgeToEdgeMT
import org.mtransit.android.ui.setUpEdgeToEdgeTop
import org.mtransit.android.ui.view.common.EventObserver
import kotlin.getValue

@AndroidEntryPoint
class PurchaseActivity : MTActivity(R.layout.activity_purchase) {

    companion object {
        private val LOG_TAG = PurchaseActivity::class.java.simpleName

        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, PurchaseActivity::class.java)
        }
    }

    override fun getLogTag(): String = LOG_TAG

    private val viewModel by viewModels<PurchaseViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeMT()
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.support_fragment).setUpEdgeToEdgeTop() // after super.onCreate()
        viewModel.closeEvent.observe(this, EventObserver { close ->
            if (close) {
                finish()
            }
        })
    }

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

    override val currentFragment: Fragment?
        get() = supportFragmentManager.primaryNavigationFragment
}