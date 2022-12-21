@file:JvmName("RTSAgencyRoutesFragment") // ANALYTICS
package org.mtransit.android.ui.type.rts

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
import org.mtransit.android.commons.data.Route
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.databinding.FragmentRtsAgencyRoutesBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.rts.route.RTSRouteFragment
import org.mtransit.android.ui.view.common.SpacesItemDecoration
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.commons.FeatureFlags

@AndroidEntryPoint
class RTSAgencyRoutesFragment : MTFragmentX(R.layout.fragment_rts_agency_routes) {

    companion object {
        private val LOG_TAG = RTSAgencyRoutesFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            agencyAuthority: String,
            optColorInt: Int? = null
        ): RTSAgencyRoutesFragment {
            return RTSAgencyRoutesFragment().apply {
                arguments = bundleOf(
                    RTSAgencyRoutesViewModel.EXTRA_AGENCY_AUTHORITY to agencyAuthority,
                    RTSAgencyRoutesViewModel.EXTRA_COLOR_INT to optColorInt,
                )
            }
        }
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<RTSAgencyRoutesViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    private var binding: FragmentRtsAgencyRoutesBinding? = null

    private val listItemDecoration: ItemDecoration by lazy { DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL) }

    private val gridItemDecoration: ItemDecoration by lazy { SpacesItemDecoration(requireContext(), R.dimen.grid_view_spacing) }

    private var listGridAdapter: RTSAgencyRoutesAdapter? = null

    private fun makeListGridAdapter() = RTSAgencyRoutesAdapter(this::openRouteScreen).apply {
        setAgency(attachedViewModel?.agency?.value)
        setShowingListInsteadOfGrid(attachedViewModel?.showingListInsteadOfGrid?.value)
        submitList(attachedViewModel?.routes?.value)
    }

    private fun openRouteScreen(view: View, route: Route, agency: IAgencyProperties) {
        if (FeatureFlags.F_NAVIGATION) {
            var extras: FragmentNavigator.Extras? = null
            if (FeatureFlags.F_TRANSITION) {
                extras = FragmentNavigatorExtras(view to view.transitionName)
            }
            findNavController().navigate(
                R.id.nav_to_rts_route_screen,
                RTSRouteFragment.newInstanceArgs(
                    agency.authority,
                    route.id,
                ),
                null,
                extras
            )
        } else {
            (activity as? MainActivity)?.addFragmentToStack(
                RTSRouteFragment.newInstance(
                    agency.authority,
                    route.id,
                ),
                this,
                view,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRtsAgencyRoutesBinding.bind(view).apply {
            listGrid.adapter = listGridAdapter ?: makeListGridAdapter().also { listGridAdapter = it } // must null in destroyView() to avoid memory leak
            fabListGrid.setOnClickListener {
                viewModel.saveShowingListInsteadOfGrid(viewModel.showingListInsteadOfGrid.value == false) // switching
            }
        }
        viewModel.colorIntDistinct.observe(viewLifecycleOwner) { colorIntDistinct ->
            colorIntDistinct?.let {
                binding?.fabListGrid?.apply {
                    rippleColor = colorIntDistinct
                    backgroundTintList = ColorStateList.valueOf(colorIntDistinct)
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
        viewModel.routes.observe(viewLifecycleOwner) { routes ->
            routes?.let {
                listGridAdapter?.setList(routes)
                switchView()
            }
        }
    }

    private fun calculateNoOfColumns(context: Context, @Suppress("SameParameterValue") columnWidthDp: Float): Int {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val screenWidthDp: Float = dm.widthPixels / dm.density
        return (screenWidthDp / columnWidthDp + 0.5f).toInt()
    }

    private fun switchView() {
        binding?.apply {
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
}