package org.mtransit.android.ui.type.rds

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.dp
import org.mtransit.android.commons.getDimensionInt
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.data.RouteManager
import org.mtransit.android.databinding.LayoutRdsRouteItemBinding
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.ui.common.UIColorUtils
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.setPadding
import org.mtransit.android.util.UIRouteUtils

class RDSAgencyRoutesAdapter(
    private val serviceUpdateLoader: ServiceUpdateLoader,
    private val onClick: (View, RouteManager) -> Unit,
) : ListAdapter<RouteManager, RDSAgencyRoutesAdapter.RouteViewHolder>(RoutesDiffCallback),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG = RDSAgencyRoutesAdapter::class.java.simpleName
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private var _agency: IAgencyUIProperties? = null

    private var _showingListInsteadOfGrid: Boolean? = null

    private var _listSet: Boolean? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setAgency(agency: IAgencyUIProperties?) {
        if (_agency == agency) {
            MTLog.d(this, "setAgency() > SKIP (same: $agency)")
            return
        }
        theLogTag = agency?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
        _agency = agency
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setShowingListInsteadOfGrid(showingListInsteadOfGrid: Boolean?) {
        if (_showingListInsteadOfGrid == showingListInsteadOfGrid) {
            MTLog.d(this, "setShowingListInsteadOfGrid() > SKIP (same: $showingListInsteadOfGrid)")
            return
        }
        _showingListInsteadOfGrid = showingListInsteadOfGrid
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: List<RouteManager>?) {
        submitList(list)
        if (_listSet == (list != null)) {
            MTLog.d(this, "setListSet() > SKIP (same: $_listSet)")
            return
        }
        _listSet = list != null
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun onServiceUpdatesLoaded() {
        notifyDataSetChanged()
    }

    fun isReady() = _agency != null && _showingListInsteadOfGrid != null && _listSet != null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        return RouteViewHolder.from(parent, serviceUpdateLoader)
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
        private val binding: LayoutRdsRouteItemBinding,
        private val serviceUpdateLoader: ServiceUpdateLoader,
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, serviceUpdateLoader: ServiceUpdateLoader): RouteViewHolder {
                val binding = LayoutRdsRouteItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return RouteViewHolder(binding, serviceUpdateLoader)
            }
        }

        fun bind(
            routeM: RouteManager?,
            agency: IAgencyUIProperties?,
            showingListInsteadOfGrid: Boolean?,
            onClick: (View, RouteManager) -> Unit
        ) = binding.apply {
            if (routeM?.route == null || agency == null || showingListInsteadOfGrid == null) {
                MTLog.d(LOG_TAG, "onBindViewHolder() > SKIP (missing data)")
                routeLayout.isVisible = false
                return@apply
            }
            val route = routeM.route
            MTTransitions.setTransitionName(routeLayout, "r_" + agency.authority + "_" + route.id)
            // SHORT NAME & LOGO
            if (route.shortName.isBlank()) { // NO RSN
                routeShortName.visibility = View.INVISIBLE // keep size
                if (routeTypeImg.hasPaths()
                    && agency.authority == routeTypeImg.tag
                ) {
                    routeTypeImg.isVisible = true
                } else {
                    agency.logo?.let {
                        routeTypeImg.setJSON(it)
                        routeTypeImg.tag = agency.authority
                        routeTypeImg.isVisible = true
                    } ?: run {
                        routeTypeImg.isVisible = false
                    }
                }
            } else {
                routeTypeImg.isVisible = false
                routeShortName.text = UIRouteUtils.decorateRouteShortName(context, route.shortName)
                routeShortName.isVisible = true
            }

            serviceUpdateLayout.routeServiceUpdateImg.apply {
                val (isWarning, isInfo) = routeM.getServiceUpdates(
                    serviceUpdateLoader,
                    emptyList() // TODO agency-level UI?
                ).let {
                    ServiceUpdate.isSeverityWarning(it) to ServiceUpdate.isSeverityInfo(it)
                }
                if (isWarning) {
                    setImageResource(R.drawable.ic_warning_on_surface_16dp)
                    isVisible = true
                } else if (isInfo) {
                    setImageResource(R.drawable.ic_info_outline_on_surface_16dp)
                    isVisible = true
                } else {
                    setImageDrawable(null)
                    isVisible = false
                }
            }
            // LONG NAME (grid only)
            if (showingListInsteadOfGrid != false) { // LIST
                routeLayout.setPadding(horizontal = 8.dp, relative = true)
                serviceUpdateLayout.routeServiceUpdateImg.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 8.dp
                }
                rsnOrLogo.layoutParams = LinearLayout.LayoutParams(
                    context.resources.getDimensionInt(R.dimen.poi_extra_width),
                    // 64.dp, // TO DO poi_extra_width
                    // ResourceUtils.convertDPtoPX(context, 64).toInt(),
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                routeLongName.text = route.longName
                routeLongName.isVisible = route.longName.isNotBlank()
            } else { // GRID
                routeLayout.setPadding(horizontal = 4.dp, relative = true)
                serviceUpdateLayout.routeServiceUpdateImg.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 4.dp
                }
                rsnOrLogo.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                routeLongName.text = null
                routeLongName.isVisible = false
            }
            // BG COLOR
            routeLayout.setBackgroundColor(
                UIColorUtils.adaptBackgroundColorToLightText(
                    context,
                    (if (route.hasColor()) route.colorInt else null) ?: agency.colorInt ?: UIColorUtils.DEFAULT_BACKGROUND_COLOR
                )
            )
            routeLayout.isVisible = true
            routeLayout.apply {
                setOnClickListener { view ->
                    onClick(view, routeM)
                }
            }
        }
    }
}