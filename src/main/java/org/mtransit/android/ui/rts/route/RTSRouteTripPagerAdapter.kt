package org.mtransit.android.ui.rts.route

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.ui.rts.route.trip.RTSTripStopsFragment

class RTSRouteTripPagerAdapter(f: Fragment) : FragmentStateAdapter(f), MTLog.Loggable {

    companion object {
        private val LOG_TAG = RTSRouteTripPagerAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private var authority: String? = null

    private var routeTrips: MutableList<Trip>? = null

    private var selectedStopId: Int? = null

    fun setAuthority(newAuthority: String?) {
        if (this.authority == newAuthority) {
            return // SKIP same
        }
        this.authority = newAuthority
        notifyDataSetChanged()
    }

    fun setRouteTrips(newRouteTrips: List<Trip>?): Boolean { // TODO DiffUtil
        var changed = false
        if (!this.routeTrips.isNullOrEmpty()) {
            this.routeTrips?.clear()
            this.routeTrips = null // loading
            changed = true
        }
        newRouteTrips?.let {
            this.routeTrips = mutableListOf<Trip>().apply {
                changed = addAll(it)
            }
        }
        if (changed) {
            notifyDataSetChanged()
        }
        return changed
    }

    fun setSelectedStopId(newSelectedStopId: Int?) {
        if (this.selectedStopId == newSelectedStopId) {
            return // SKIP same
        }
        this.selectedStopId = newSelectedStopId // NICE TO HAVE -> not triggering notifyDataSetChanged()
    }

    fun isReady() = authority != null && routeTrips != null

    override fun getItemCount() = routeTrips?.size ?: 0

    override fun createFragment(position: Int): Fragment {
        val authority: String = this.authority ?: throw RuntimeException("Trying to create fragment w/ authority at $position!")
        val trip = routeTrips?.getOrNull(position) ?: throw RuntimeException("Trying to create fragment at $position!")
        return RTSTripStopsFragment.newInstance(
            authority,
            trip.routeId,
            trip.id,
            this.selectedStopId,
        )
    }
}