package org.mtransit.android.ui.rds.route

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.ui.rds.route.direction.RDSDirectionStopsFragment

class RDSRouteDirectionPagerAdapter(f: Fragment) : FragmentStateAdapter(f), MTLog.Loggable {

    companion object {
        private val LOG_TAG = RDSRouteDirectionPagerAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private var authority: String? = null

    private var routeDirections: MutableList<Direction>? = null

    private var selectedStopId: Int? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setAuthority(newAuthority: String?) {
        if (this.authority == newAuthority) {
            return // SKIP same
        }
        this.authority = newAuthority
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setRouteDirections(newRouteDirections: List<Direction>?): Boolean { // TODO DiffUtil
        var changed = false
        if (!this.routeDirections.isNullOrEmpty()) {
            this.routeDirections?.clear()
            this.routeDirections = null // loading
            changed = true
        }
        newRouteDirections?.let {
            this.routeDirections = mutableListOf<Direction>().apply {
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

    fun isReady() = authority != null && routeDirections != null

    override fun getItemCount() = routeDirections?.size ?: 0

    override fun createFragment(position: Int): Fragment {
        val authority: String = this.authority ?: throw RuntimeException("Trying to create fragment w/ authority at $position!")
        val direction = routeDirections?.getOrNull(position) ?: throw RuntimeException("Trying to create fragment at $position!")
        return RDSDirectionStopsFragment.Companion.newInstance(
            authority,
            direction.routeId,
            direction.id,
            this.selectedStopId,
        )
    }
}