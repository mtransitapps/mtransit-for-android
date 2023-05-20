package org.mtransit.android.ui.nearby

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.ui.nearby.type.NearbyAgencyTypeFragment

class NearbyPagerAdapter(f: Fragment) : FragmentStateAdapter(f), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NearbyPagerAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private var types: MutableList<DataSourceType>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setTypes(newTypes: List<DataSourceType>?): Boolean { // TODO DiffUtil
        var changed = false
        if (!this.types.isNullOrEmpty()) {
            this.types?.clear()
            this.types = null // loading
            changed = true
        }
        newTypes?.let {
            this.types = mutableListOf<DataSourceType>().apply {
                changed = addAll(it)
            }
        }
        if (changed) {
            notifyDataSetChanged()
        }
        return changed
    }

    fun isReady() = types != null

    override fun getItemCount() = types?.size ?: 0

    override fun createFragment(position: Int): Fragment {
        val dst = types?.getOrNull(position) ?: throw RuntimeException("Trying to create fragment at $position!")
        return NearbyAgencyTypeFragment.newInstance(dst)
    }
}