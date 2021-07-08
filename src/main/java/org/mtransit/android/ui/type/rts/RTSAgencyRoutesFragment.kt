@file:JvmName("RTSAgencyRoutesFragment") // ANALYTICS
package org.mtransit.android.ui.type.rts

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.StateSet
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
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
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.rts.route.RTSRouteFragment
import org.mtransit.android.ui.view.common.SpacesItemDecoration
import org.mtransit.android.ui.view.common.attached
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

    private var binding: FragmentRtsAgencyRoutesBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null

    private var listGridToggleMenuItem: MenuItem? = null
    private var listGridSwitchMenuItem: SwitchCompat? = null

    private val listGridToggleSelector: StateListDrawable by lazy {
        StateListDrawable().apply {
            (ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_list, requireContext().theme) as? LayerDrawable)?.apply {
                attached { viewModel }?.colorInt?.value?.let { (findDrawableByLayerId(R.id.switch_list_oval_shape) as? GradientDrawable)?.setColor(it) }
                addState(intArrayOf(android.R.attr.state_checked), this)
            }
            (ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_grid, requireContext().theme) as? LayerDrawable)?.apply {
                attached { viewModel }?.colorInt?.value?.let { (findDrawableByLayerId(R.id.switch_grid_oval_shape) as? GradientDrawable)?.setColor(it) }
                addState(StateSet.WILD_CARD, this)
            }
        }
    }

    private val listItemDecoration: ItemDecoration by lazy { DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL) }

    private val gridItemDecoration: ItemDecoration by lazy { SpacesItemDecoration(requireContext(), R.dimen.grid_view_spacing) }

    private val adapter: RTSAgencyRoutesAdapter by lazy {
        RTSAgencyRoutesAdapter(this::openRouteScreen).apply {
            setAgency(attached { viewModel }?.agency?.value)
            setShowingListInsteadOfGrid(attached { viewModel }?.showingListInsteadOfGrid?.value)
            submitList(attached { viewModel }?.routes?.value)
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRtsAgencyRoutesBinding.bind(view).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            routesListGrid.adapter = adapter
        }
        viewModel.colorInt.observe(viewLifecycleOwner, { colorInt ->
            colorInt?.let {
                activity?.invalidateOptionsMenu() // initialize action bar list/grid switch icon
            }
        })
        viewModel.authorityShort.observe(viewLifecycleOwner, {
            theLogTag = it?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
        })
        viewModel.agency.observe(viewLifecycleOwner, { agency ->
            agency?.let {
                adapter.setAgency(agency)
                switchView()
            }
        })
        viewModel.showingListInsteadOfGrid.observe(viewLifecycleOwner, { showingListInsteadOfGrid ->
            showingListInsteadOfGrid?.let { listInsteadOfGrid ->
                binding?.routesListGrid?.apply {
                    val scrollPosition = (layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition() ?: 0
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
                adapter.setShowingListInsteadOfGrid(showingListInsteadOfGrid)
                switchView()
            }
        })
        viewModel.routes.observe(viewLifecycleOwner, { routes ->
            routes?.let {
                adapter.setList(routes)
                switchView()
            }
        })
    }

    private fun calculateNoOfColumns(context: Context, @Suppress("SameParameterValue") columnWidthDp: Float): Int {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val screenWidthDp: Float = dm.widthPixels / dm.density
        return (screenWidthDp / columnWidthDp + 0.5f).toInt()
    }

    private fun switchView() {
        binding?.apply {
            when {
                !adapter.isReady() -> {
                    emptyBinding?.root?.isVisible = false
                    routesListGrid.isVisible = false
                    loading.root.isVisible = true
                }
                adapter.itemCount == 0 -> {
                    loading.root.isVisible = false
                    routesListGrid.isVisible = false
                    (emptyBinding?.root ?: emptyStub.inflate()).isVisible = true
                }
                else -> {
                    emptyBinding?.root?.isVisible = false
                    loading.root.isVisible = false
                    routesListGrid.isVisible = true
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (isResumed) {
            if (menu.findItem(R.id.menu_toggle_list_grid) == null) {
                inflater.inflate(R.menu.menu_rts_agency_routes, menu)
            }
            listGridToggleMenuItem = menu.findItem(R.id.menu_toggle_list_grid)
            listGridSwitchMenuItem = listGridToggleMenuItem?.actionView?.findViewById(R.id.action_bar_switch_list_grid)
            listGridSwitchMenuItem?.thumbDrawable = listGridToggleSelector
            updateListGridToggleMenuItem()
        } else {
            listGridSwitchMenuItem?.setOnCheckedChangeListener(null)
            listGridSwitchMenuItem?.visibility = View.GONE
            listGridSwitchMenuItem = null
            listGridToggleMenuItem?.isVisible = false
            listGridToggleMenuItem = null
        }
    }

    private fun updateListGridToggleMenuItem() {
        if (!isResumed) {
            return
        }
        val listInsteadOfGrid = viewModel.showingListInsteadOfGrid.value
        listGridSwitchMenuItem?.isChecked = listInsteadOfGrid != false
        listGridSwitchMenuItem?.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            onCheckedChanged(buttonView, isChecked)
        }
        listGridSwitchMenuItem?.isVisible = listInsteadOfGrid != null
        listGridToggleMenuItem?.isVisible = listInsteadOfGrid != null
    }

    private fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (!isResumed) {
            return
        }
        if (buttonView.id == R.id.action_bar_switch_list_grid) {
            viewModel.saveShowingListInsteadOfGrid(isChecked)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isResumed) {
            if (item.itemId == R.id.menu_toggle_list_grid) {
                viewModel.saveShowingListInsteadOfGrid(viewModel.showingListInsteadOfGrid.value == false) // switching
                return true // handled
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        switchView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emptyBinding = null
        binding = null
    }
}