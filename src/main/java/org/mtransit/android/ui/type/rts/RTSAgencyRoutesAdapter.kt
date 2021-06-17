package org.mtransit.android.ui.type.rts

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.dp
import org.mtransit.android.commons.getDimensionInt
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.databinding.LayoutRtsRouteItemBinding

class RTSAgencyRoutesAdapter(private val onClick: (Route, IAgencyProperties) -> Unit) :
    ListAdapter<Route, RTSAgencyRoutesAdapter.RouteViewHolder>(RoutesDiffCallback),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG = RTSAgencyRoutesAdapter::class.java.simpleName
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private var _agency: IAgencyUIProperties? = null

    private var _showingListInsteadOfGrid: Boolean? = null

    private var _listSet: Boolean? = null

    fun setAgency(agency: IAgencyUIProperties?) {
        if (_agency == agency) {
            MTLog.d(this, "setAgency() > SKIP (same: $agency)")
            return
        }
        theLogTag = agency?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
        _agency = agency
        notifyDataSetChanged()
    }

    fun setShowingListInsteadOfGrid(showingListInsteadOfGrid: Boolean?) {
        if (_showingListInsteadOfGrid == showingListInsteadOfGrid) {
            MTLog.d(this, "setShowingListInsteadOfGrid() > SKIP (same: $showingListInsteadOfGrid)")
            return
        }
        _showingListInsteadOfGrid = showingListInsteadOfGrid
        notifyDataSetChanged()
    }

    fun setList(list: List<Route>?) {
        submitList(list)
        if (_listSet == (list != null)) {
            MTLog.d(this, "setListSet() > SKIP (same: $_listSet)")
            return
        }
        _listSet = list != null
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        if (_agency == null || _showingListInsteadOfGrid == null || _listSet == null) {
            return -1
        }
        return super.getItemCount()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        return RouteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(
            getItem(position),
            _agency,
            _showingListInsteadOfGrid,
            onClick
        )
    }

    class RouteViewHolder private constructor(
        private val binding: LayoutRtsRouteItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): RouteViewHolder {
                val binding = LayoutRtsRouteItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return RouteViewHolder(binding)
            }
        }

        fun bind(
            route: Route?,
            agency: IAgencyUIProperties?,
            showingListInsteadOfGrid: Boolean?,
            onClick: (Route, IAgencyProperties) -> Unit
        ) {
            if (route == null || agency == null || showingListInsteadOfGrid == null) {
                MTLog.d(LOG_TAG, "onBindViewHolder() > SKIP (missing data)")
                binding.route.isVisible = false
                return
            }
            // SHORT NAME & LOGO
            if (route.shortName.isBlank()) { // NO RSN
                binding.routeShortName.visibility = View.INVISIBLE // keep size
                if (binding.routeTypeImg.hasPaths()
                    && agency.authority == binding.routeTypeImg.tag
                ) {
                    binding.routeTypeImg.isVisible = true
                } else {
                    agency.logo?.let {
                        binding.routeTypeImg.setJSON(it)
                        binding.routeTypeImg.tag = agency.authority
                        binding.routeTypeImg.isVisible = true
                    } ?: run {
                        binding.routeTypeImg.isVisible = false
                    }
                }
            } else {
                binding.routeTypeImg.isVisible = false
                binding.routeShortName.text = route.shortName
                binding.routeShortName.isVisible = true
            }
            // LONG NAME (grid only)
            if (showingListInsteadOfGrid != false) { // LIST
                binding.route.setPaddingRelative(8.dp, 0, 8.dp, 0)
                binding.rsnOrLogo.layoutParams = LinearLayout.LayoutParams(
                    binding.root.context.resources.getDimensionInt(R.dimen.poi_extra_width),
                    // 64.dp, // TO DO poi_extra_width
                    // ResourceUtils.convertDPtoPX(binding.root.context, 64).toInt(),
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                binding.routeLongName.text = route.longName
                binding.routeLongName.isVisible = route.longName.isNotBlank()
            } else { // GRID
                binding.route.setPaddingRelative(0, 0, 0, 0)
                binding.rsnOrLogo.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                binding.routeLongName.text = null
                binding.routeLongName.isVisible = false
            }
            // BG COLOR
            binding.route.setBackgroundColor((if (route.hasColor()) route.colorInt else null) ?: agency.colorInt ?: Color.BLACK)
            binding.route.isVisible = true
            binding.root.apply {
                setOnClickListener { onClick(route, agency) }
            }
        }
    }
}