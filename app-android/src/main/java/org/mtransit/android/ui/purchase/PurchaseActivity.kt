package org.mtransit.android.ui.purchase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.applyWindowInsetsEdgeToEdge
import org.mtransit.android.ui.enableEdgeToEdgeMT
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.end
import org.mtransit.android.ui.view.common.endMargin
import org.mtransit.android.ui.view.common.start
import org.mtransit.android.ui.view.common.startMargin
import kotlin.getValue

@AndroidEntryPoint
class PurchaseActivity : MTActivity(R.layout.activity_purchase) {

    companion object {
        private val LOG_TAG: String = PurchaseActivity::class.java.simpleName

        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, PurchaseActivity::class.java)
        }
    }

    override fun getLogTag() = LOG_TAG

    private val viewModel by viewModels<PurchaseViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeMT()
        window.decorView // fix random crash (gesture nav back then re-open app)
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.support_fragment).applyWindowInsetsEdgeToEdge(WindowInsetsCompat.Type.systemBars(), consumed = true) { insets ->
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                startMargin = insets.start
                endMargin = insets.end
            }
        }
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