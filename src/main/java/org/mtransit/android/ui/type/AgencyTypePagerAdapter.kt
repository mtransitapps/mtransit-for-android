package org.mtransit.android.ui.type

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.ui.type.poi.AgencyPOIsFragment
import org.mtransit.android.ui.type.rts.RTSAgencyRoutesFragment

class AgencyTypePagerAdapter(f: Fragment) : FragmentStateAdapter(f), MTLog.Loggable {

    companion object {
        private val LOG_TAG = AgencyTypePagerAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private var agencies: MutableList<IAgencyUIProperties>? = null

    fun setAgencies(newAgencies: List<IAgencyUIProperties>?): Boolean { // TODO DiffUtil
        var changed = false
        if (!this.agencies.isNullOrEmpty()) {
            this.agencies?.clear()
            this.agencies = null // loading
            changed = true
        }
        newAgencies?.let {
            this.agencies = mutableListOf<IAgencyUIProperties>().apply {
                changed = addAll(it)
            }
        }
        if (changed) {
            notifyDataSetChanged()
        }
        return changed
    }

    override fun getItemCount() = agencies?.size ?: -1

    override fun createFragment(position: Int): Fragment {
        val agency = agencies?.getOrNull(position) ?: throw RuntimeException("Trying to create fragment at $position!")
        if (agency.isRTS) {
            return RTSAgencyRoutesFragment.newInstance(
                agency.authority,
                agency.colorInt
            )
        }
        return AgencyPOIsFragment.newInstance(
            agency.authority,
            agency.colorInt
        )
    }
}