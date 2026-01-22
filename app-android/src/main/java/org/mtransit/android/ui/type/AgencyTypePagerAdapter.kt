package org.mtransit.android.ui.type

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.maps.model.CameraPosition
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.ui.type.poi.AgencyPOIsFragment
import org.mtransit.android.ui.type.rds.RDSAgencyRoutesFragment

class AgencyTypePagerAdapter(f: Fragment) : FragmentStateAdapter(f), MTLog.Loggable {

    companion object {
        private val LOG_TAG = AgencyTypePagerAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private var agencies: MutableList<IAgencyUIProperties>? = null

    var selectedCameraPosition: CameraPosition? = null
    var selectedAgencyAuthority: String? = null
    var selectedUUID: String? = null

    @SuppressLint("NotifyDataSetChanged")
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

    fun isReady() = agencies != null

    override fun getItemCount() = agencies?.size ?: 0

    override fun createFragment(position: Int): Fragment {
        val agency = agencies?.getOrNull(position) ?: throw RuntimeException("Trying to create fragment at $position!")
        return when {
            agency.isRDS -> RDSAgencyRoutesFragment.newInstance(agency)
            else -> AgencyPOIsFragment.newInstance(
                agency,
                selectedCameraPosition?.takeIf { agency.authority == selectedAgencyAuthority },
                selectedUUID?.takeIf { agency.authority == selectedAgencyAuthority }
            )
        }
    }
}
