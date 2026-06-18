package org.mtransit.android.ui.type.rds

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
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
import org.mtransit.android.commons.data.distinctByOriginalId
import org.mtransit.android.commons.data.isSeverityWarningInfo
import org.mtransit.android.commons.dpToPx
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.data.POIListFooterManager
import org.mtransit.android.data.RouteManager
import org.mtransit.android.databinding.LayoutRdsRouteItemBinding
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.ui.common.UIColorUtils
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.setImageResourceAndVisibility
import org.mtransit.android.ui.view.common.setPadding
import org.mtransit.android.ui.view.common.textAndVisibility
import org.mtransit.android.ui.view.listfooter.FooterViewHolder
import org.mtransit.android.ui.view.setJSONAndVisibility
import org.mtransit.android.util.UIRouteUtils
import java.util.concurrent.atomic.AtomicBoolean

class RDSAgencyRoutesAdapter(
    private val serviceUpdateLoader: ServiceUpdateLoader,
    private val footerManager: POIListFooterManager,
    private val onClick: (View, RouteManager) -> Unit,
) : ListAdapter<RouteManager, RecyclerView.ViewHolder>(RoutesDiffCallback),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RDSAgencyRoutesAdapter::class.java.simpleName

        private const val TYPE_ROUTE = 0
        private const val TYPE_FOOTER = 1
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private var _agency: IAgencyUIProperties? = null

    private var _showingListInsteadOfGrid: Boolean? = null

    private var _listSet = AtomicBoolean(false)
    private var listSet: Boolean
        get() = _listSet.get()
        set(value) = _listSet.set(value)

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
        val newListSet = list != null
        if (listSet == newListSet) {
            MTLog.d(this, "setListSet() > SKIP (same: ${listSet})")
            return
        }
        listSet = newListSet
        notifyDataSetChanged()
    }

    val routeCount: Int get() = super.getItemCount()

    override fun getItemCount(): Int = routeCount + 1 // footer

    override fun getItemViewType(position: Int): Int =
        when (position) {
            getItemCount() - 1 -> TYPE_FOOTER // last = footer
            else -> TYPE_ROUTE
        }

    @SuppressLint("NotifyDataSetChanged")
    fun onServiceUpdatesLoaded() {
        notifyDataSetChanged()
    }

    fun isReady() = _agency != null && _showingListInsteadOfGrid != null && listSet

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_ROUTE -> RouteViewHolder.from(parent, serviceUpdateLoader)
            TYPE_FOOTER -> FooterViewHolder.from(parent)
            else -> throw RuntimeException("Unexpected view type $viewType!")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RouteViewHolder -> holder.bind(
                getItem(position),
                _agency,
                _showingListInsteadOfGrid,
                onClick
            )

            is FooterViewHolder -> holder.bind(footerManager)
            else -> throw RuntimeException("Unexpected view type!")
        }
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

            private const val DEBUG_LAYOUT = false
            // private const val DEBUG_LAYOUT = true // DEBUG
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
            routeShortName.textAndVisibility = route.shortName.takeIf { it.isNotBlank() }?.let { UIRouteUtils.decorateRouteShortName(context, it) }
            routeTypeImg.setJSONAndVisibility(agency)
            if (routeShortName.isVisible && routeTypeImg.isVisible) {
                routeSpaceStart.isVisible = showingListInsteadOfGrid
                routeTypeImg.updateLayoutParams<LinearLayout.LayoutParams> {
                    weight = 2f
                }
                routeShortName.updateLayoutParams<LinearLayout.LayoutParams> {
                    weight = 2f
                }
                routeShortName.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            } else {
                routeSpaceStart.isVisible = false
                routeTypeImg.updateLayoutParams<LinearLayout.LayoutParams> {
                    weight = 4f
                }
                routeShortName.updateLayoutParams<LinearLayout.LayoutParams> {
                    weight = 4f
                }
                routeShortName.gravity = Gravity.CENTER
            }
            serviceUpdateLayout.routeServiceUpdateImg.apply {
                val serviceUpdates = routeM.getServiceUpdates(
                    serviceUpdateLoader = serviceUpdateLoader,
                    ignoredUUIDsOrUnknown = emptyList() // TODO agency-level UI?
                ).distinctByOriginalId()
                val (isWarning, isInfo) = serviceUpdates.isSeverityWarningInfo()
                if (isWarning) {
                    setImageResourceAndVisibility(R.drawable.ic_warning_on_surface_16dp)
                } else if (isInfo) {
                    setImageResourceAndVisibility(R.drawable.ic_info_outline_on_surface_16dp)
                } else {
                    setImageResourceAndVisibility(null)
                }
            }
            // LONG NAME (grid only)
            if (showingListInsteadOfGrid) { // LIST
                routeLayout.setPadding(horizontal = 8.dpToPx, relative = true)
                serviceUpdateLayout.routeServiceUpdateImg.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 8.dpToPx
                }
                rsnImgServiceUpdate.updateLayoutParams {
                    width = context.resources.getDimensionPixelSize(R.dimen.poi_extra_width)
                    // 64.dp, // TO DO poi_extra_width
                    // ResourceUtils.convertDPtoPX(context, 64).toInt(),
                }
                routeLongName.textAndVisibility = route.longName.takeIf { it.isNotBlank() }
            } else { // GRID
                routeLayout.setPadding(horizontal = 4.dpToPx, relative = true)
                serviceUpdateLayout.routeServiceUpdateImg.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 4.dpToPx
                }
                rsnImgServiceUpdate.updateLayoutParams {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
                routeLongName.textAndVisibility = null
            }
            // BG COLOR
            routeLayout.setBackgroundColor(
                UIColorUtils.adaptBackgroundColorToLightText(
                    context,
                    (if (route.hasColor()) route.colorInt else null) ?: agency.colorInt ?: UIColorUtils.DEFAULT_BACKGROUND_COLOR
                )
            )
            routeLayout.apply {
                setOnClickListener { view ->
                    onClick(view, routeM)
                }
                isVisible = true
            }
            if (DEBUG_LAYOUT) {
                serviceUpdateLayout.routeServiceUpdateImg.apply {
                    setImageResourceAndVisibility(R.drawable.ic_warning_on_surface_16dp)
                    setBackgroundColor(Color.RED)
                }
                routeLayout.setBackgroundColor(Color.YELLOW)
                rsnImgServiceUpdate.setBackgroundColor(Color.BLUE)
                routeTypeImg.setBackgroundColor(Color.CYAN)
                routeShortName.setBackgroundColor(Color.MAGENTA)
                rsnImg.setBackgroundColor(Color.GREEN)
            }
        }
    }
}
