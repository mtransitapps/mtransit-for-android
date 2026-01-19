@file:JvmName("RTSAgencyRoutesFragment") // ANALYTICS // do not change to avoid breaking tracking
package org.mtransit.android.ui.type.rds

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.data.RouteManager
import org.mtransit.android.databinding.FragmentRdsAgencyRoutesBinding
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.empty.EmptyLayoutUtils.updateEmptyLayout
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.rds.route.RDSRouteFragment
import org.mtransit.android.ui.setUpFabEdgeToEdge
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.SpacesItemDecoration
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.LinkUtils
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class RDSAgencyRoutesFragment : MTFragmentX(R.layout.fragment_rds_agency_routes) {

    companion object {
        private val LOG_TAG = RDSAgencyRoutesFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(agency : IAgencyUIProperties) =
            newInstance(agency.authority, agency.colorInt)

        @JvmStatic
        fun newInstance(
            agencyAuthority: String,
            optColorInt: Int? = null,
        ) = RDSAgencyRoutesFragment().apply {
            arguments = bundleOf(
                RDSAgencyRoutesViewModel.EXTRA_AGENCY_AUTHORITY to agencyAuthority,
                RDSAgencyRoutesViewModel.EXTRA_COLOR_INT to optColorInt,
            )
        }
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<RDSAgencyRoutesViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var binding: FragmentRdsAgencyRoutesBinding? = null

    private val listItemDecoration: ItemDecoration by lazy { DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL) }

    private val gridItemDecoration: ItemDecoration by lazy { SpacesItemDecoration(requireContext(), R.dimen.grid_view_spacing) }

    private var listGridAdapter: RDSAgencyRoutesAdapter? = null

    private fun makeListGridAdapter() = RDSAgencyRoutesAdapter(serviceUpdateLoader, this::openRouteScreen).apply {
        setAgency(attachedViewModel?.agency?.value)
        setShowingListInsteadOfGrid(attachedViewModel?.showingListInsteadOfGrid?.value)
        submitList(attachedViewModel?.routesM?.value)
    }

    private fun openRouteScreen(view: View, route: RouteManager) {
        if (FeatureFlags.F_NAVIGATION) {
            var extras: FragmentNavigator.Extras? = null
            if (FeatureFlags.F_TRANSITION) {
                extras = FragmentNavigatorExtras(view to view.transitionName)
            }
            findNavController().navigate(
                R.id.nav_to_rds_route_screen,
                RDSRouteFragment.newInstanceArgs(route.route),
                null,
                extras
            )
        } else {
            (activity as? MainActivity)?.addFragmentToStack(
                RDSRouteFragment.newInstance(route.route),
                this,
                view,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRdsAgencyRoutesBinding.bind(view).apply {
            listGrid.adapter = listGridAdapter ?: makeListGridAdapter().also { listGridAdapter = it } // must null in destroyView() to avoid memory leak
            fabListGrid.apply {
                setOnClickListener {
                    viewModel.saveShowingListInsteadOfGrid(viewModel.showingListInsteadOfGrid.value == false) // switching
                }
                setUpFabEdgeToEdge(
                    originalMarginEndDimenRes = R.dimen.fab_mini_margin_end,
                    originalMarginBottomDimenRes = R.dimen.fab_mini_margin_bottom,
                )
            }
            fabFares.apply {
                setUpFabEdgeToEdge(
                    originalMarginEndDimenRes = R.dimen.fab_mini_margin_end_above_fab,
                    originalMarginBottomDimenRes = R.dimen.fab_mini_margin_bottom,
                )
            }
        }
        viewModel.colorIntDistinct.observe(viewLifecycleOwner) { colorIntDistinct ->
            colorIntDistinct?.let {
                binding?.apply {
                    fabFares.apply {
                        rippleColor = colorIntDistinct
                        backgroundTintList = ColorStateList.valueOf(colorIntDistinct)
                    }
                    fabListGrid.apply {
                        rippleColor = colorIntDistinct
                        backgroundTintList = ColorStateList.valueOf(colorIntDistinct)
                    }
                }
            }
        }
        viewModel.authorityShort.observe(viewLifecycleOwner) {
            theLogTag = it?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
        }
        viewModel.agency.observe(viewLifecycleOwner) { agency ->
            agency?.let {
                listGridAdapter?.setAgency(agency)
                switchView()
            }
            binding?.fabFares?.apply {
                agency?.faresWebForLang?.let { url ->
                    isVisible = true
                    setOnClickListener {
                        activity?.let { activity ->
                            LinkUtils.open(view, activity, url, getString(R.string.fares), true)
                        }
                    }
                } ?: run {
                    isVisible = false
                    setOnClickListener(null)
                }
            }
        }
        viewModel.showingListInsteadOfGrid.observe(viewLifecycleOwner) { showingListInsteadOfGrid ->
            showingListInsteadOfGrid?.let { listInsteadOfGrid ->
                binding?.apply {
                    fabListGrid.apply {
                        @Suppress("LiftReturnOrAssignment")
                        if (listInsteadOfGrid) { // LIST
                            setImageResource(R.drawable.switch_action_apps_dark_16dp)
                            contentDescription = getString(R.string.menu_action_grid)
                        } else { // GRID
                            setImageResource(R.drawable.switch_action_view_headline_dark_16dp)
                            contentDescription = getString(R.string.menu_action_list)
                        }
                    }
                    listGrid.apply {
                        val scrollPosition = (layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition() ?: -1
                        if (listInsteadOfGrid) { // LIST
                            if (layoutManager == null || layoutManager is GridLayoutManager) {
                                removeItemDecoration(gridItemDecoration)
                                addItemDecoration(listItemDecoration)
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                        } else { // GRID
                            if (layoutManager == null || layoutManager !is GridLayoutManager) {
                                removeItemDecoration(listItemDecoration)
                                addItemDecoration(gridItemDecoration)
                                layoutManager = GridLayoutManager(requireContext(), calculateNoOfColumns(requireContext(), 64f))
                            }
                        }
                        if (scrollPosition > 0) {
                            scrollToPosition(scrollPosition)
                        }
                    }
                    listGridAdapter?.setShowingListInsteadOfGrid(showingListInsteadOfGrid)
                    switchView()
                }
            }
        }
        viewModel.routesM.observe(viewLifecycleOwner) { routes ->
            listGridAdapter?.setList(routes)
            switchView()
            binding?.emptyLayout?.updateEmptyLayout(routes.isEmpty(), viewModel.agency.value?.pkg, activity)
        }
        viewModel.serviceUpdateLoadedEvent.observe(viewLifecycleOwner, EventObserver { _ ->
            listGridAdapter?.onServiceUpdatesLoaded()
        })
    }

    private fun calculateNoOfColumns(context: Context, @Suppress("SameParameterValue") columnWidthDp: Float): Int {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val screenWidthDp: Float = dm.widthPixels / dm.density
        return (screenWidthDp / columnWidthDp + 0.5f).toInt()
    }

    private fun switchView() = binding?.apply {
        when {
            listGridAdapter?.isReady() != true -> {
                emptyLayout.isVisible = false
                listGrid.isVisible = false
                loadingLayout.isVisible = true
            }

            listGridAdapter?.itemCount == 0 -> {
                loadingLayout.isVisible = false
                listGrid.isVisible = false
                emptyLayout.isVisible = true
            }

            else -> {
                emptyLayout.isVisible = false
                loadingLayout.isVisible = false
                listGrid.isVisible = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        switchView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.listGrid?.adapter = null
        listGridAdapter = null
        binding = null
    }

    override fun <T : View?> findViewById(id: Int) = this.view?.findViewById<T>(id)
}