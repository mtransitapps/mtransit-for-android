@file:JvmName("ModulesActivity") // ANALYTICS
package org.mtransit.android.ui.modules

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.mtransit.android.R

class ModulesActivity : AppCompatActivity(R.layout.activity_modules) {

    companion object {
        @JvmStatic
        fun newInstance(context: Context): Intent {
            return Intent(context, ModulesActivity::class.java)
        }
    }
}