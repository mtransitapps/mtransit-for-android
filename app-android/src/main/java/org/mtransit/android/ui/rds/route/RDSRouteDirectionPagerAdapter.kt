package org.mtransit.android.ui.rds.route

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.maps.model.CameraPosition
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.ui.rds.route.direction.RDSDirectionStopsFragment

class RDSRouteDirectionPagerAdapter(f: Fragment) : FragmentStateAdapter(f), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RDSRouteDirectionPagerAdapter::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private var routeDirections: MutableList<Direction>? = null

    var selectedStopId: Int? = null
    var selectedDirectionId: Long? = null

    var selectedCameraPosition: CameraPosition? = null

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

    fun isReady() = routeDirections != null

    override fun getItemCount() = routeDirections?.size ?: 0

    override fun createFragment(position: Int): Fragment {
        val direction = routeDirections?.getOrNull(position) ?: throw RuntimeException("Trying to create fragment at $position!")
        return RDSDirectionStopsFragment.newInstance(
            direction,
            this.selectedStopId?.takeIf { this.selectedDirectionId == direction.id },
            this.selectedCameraPosition?.takeIf { this.selectedDirectionId == direction.id },
        )
    }
}