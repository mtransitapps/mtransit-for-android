@file:JvmName("AgencyTypeFragment") // ANALYTICS
package org.mtransit.android.ui.type

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.databinding.FragmentAgencyTypeBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.ui.ActionBarController.SimpleActionBarColorizer
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.nearby.NearbyFragment
import org.mtransit.android.ui.view.common.MTTabLayoutMediator
import org.mtransit.android.ui.view.common.MTTransitions
import kotlin.math.abs

@AndroidEntryPoint
class AgencyTypeFragment : ABFragment(R.layout.fragment_agency_type), MTActivityWithLocation.UserLocationListener {

    companion object {
        private val LOG_TAG = AgencyTypeFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Browse"

        @JvmStatic
        fun newInstance(dst: DataSourceType): AgencyTypeFragment {
            return AgencyTypeFragment().apply {
                arguments = bundleOf(
                    AgencyTypeViewModel.EXTRA_TYPE_ID to dst.id
                )
            }
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = addedViewModel?.type?.value?.let { type -> "$TRACKING_SCREEN_NAME/${type.id}" } ?: TRACKING_SCREEN_NAME

    private val viewModel by viewModels<AgencyTypeViewModel>()
    private val addedViewModel: AgencyTypeViewModel?
        get() = if (isAdded) viewModel else null

    private var binding: FragmentAgencyTypeBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null

    private var abBgColor: Int? = null

    private var lastPageSelected = -1
    private var selectedPosition = -1
    private var selectionOffset = 0f
    private var pageScrollStateIdle = true

    private var updateABColorJob: Job? = null

    private val defaultColor: Int by lazy { ThemeUtils.resolveColorAttribute(requireContext(), R.attr.colorPrimary) }

    private val abColorizer: SimpleActionBarColorizer by lazy { SimpleActionBarColorizer() }

    private var adapter: AgencyTypePagerAdapter? = null

    private fun makeAdapter() = AgencyTypePagerAdapter(this).apply {
        setAgencies(addedViewModel?.typeAgencies?.value)
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            addedViewModel?.onPageSelected(position)
            lastPageSelected = position
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            if (abs(lastPageSelected - position) > 1) {
                return // TODO really?
            }
            selectedPosition = position
            selectionOffset = positionOffset
            updateABColor()
        }

        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            val newPageScrollStateIdle = state == ViewPager2.SCROLL_STATE_IDLE
            if (newPageScrollStateIdle == pageScrollStateIdle) {
                return // SKIP (same)
            }
            pageScrollStateIdle = newPageScrollStateIdle
            activity?.invalidateOptionsMenu() // initialize action bar list/grid switch icon
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        MTTransitions.setContainerTransformTransition(this)
        // if (FeatureFlags.F_TRANSITION) {
        // exitTransition = MTTransitions.newHoldTransition() // not working with AdapterView // FIXME #RecyclerView
        // }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MTTransitions.postponeEnterTransition(this)
        binding = FragmentAgencyTypeBinding.bind(view).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            viewpager.offscreenPageLimit = 2
            viewpager.registerOnPageChangeCallback(onPageChangeCallback)
            viewpager.adapter = adapter ?: makeAdapter().also { adapter = it } // cannot re-use Adapter w/ ViewPager
            MTTabLayoutMediator(tabs, viewpager, autoRefresh = true, smoothScroll = true) { tab, position ->
                tab.text = viewModel.typeAgencies.value?.get(position)?.shortName
            }.attach()
            showSelectedTab()
        }
        viewModel.typeAgencies.observe(viewLifecycleOwner, { agencies ->
            if (adapter?.setAgencies(agencies) == true) {
                showSelectedTab()
                abBgColor = null // reset
                abColorizer.setBgColors(*(agencies?.map { it.colorInt ?: defaultColor }?.toIntArray() ?: arrayOf(defaultColor).toIntArray()))
                abController?.setABBgColor(this, getABBgColor(context), true)
            } else {
                switchView()
            }
            agencies?.let {
                MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this)
            }
        })
        viewModel.type.observe(viewLifecycleOwner, { type ->
            binding?.tabs?.isVisible = type != DataSourceType.TYPE_MODULE
            abController?.setABTitle(this, getABTitle(context), false)
            abController?.setABReady(this, isABReady, true)
        })
        viewModel.selectedTypeAgencyPosition.observe(viewLifecycleOwner, { newLastPageSelected ->
            newLastPageSelected?.let {
                if (this.lastPageSelected < 0) {
                    this.lastPageSelected = it
                    showSelectedTab()
                    onPageChangeCallback.onPageSelected(this.lastPageSelected) // tell the current page it's selected
                }
            }
        })
    }

    private fun switchView() {
        binding?.apply {
            when {
                lastPageSelected < 0 || adapter?.isReady() != true -> { // LOADING
                    emptyBinding?.root?.isVisible = false
                    viewpager.isVisible = false
                    tabs.isVisible = false
                    loading.root.isVisible = true
                }
                adapter?.itemCount == 0 -> { // EMPTY
                    loading.root.isVisible = false
                    viewpager.isVisible = false
                    tabs.isVisible = false
                    (emptyBinding?.root ?: emptyStub.inflate()).isVisible = true
                }
                else -> { // LOADED
                    loading.root.isVisible = false
                    emptyBinding?.root?.isVisible = false
                    tabs.isVisible = viewModel.type.value != DataSourceType.TYPE_MODULE
                    viewpager.isVisible = true
                }
            }
        }
    }

    private fun showSelectedTab() {
        if (this.adapter?.isReady() != true) {
            MTLog.d(this, "showSelectedTab() > SKIP (no adapter items)")
            return
        }
        if (this.lastPageSelected < 0) {
            MTLog.d(this, "showSelectedTab() > SKIP (no last page selected)")
            return
        }
        val smoothScroll = this.selectedPosition >= 0
        val itemToSelect = this.lastPageSelected
        binding?.viewpager?.doOnAttach {
            binding?.viewpager?.setCurrentItem(itemToSelect, smoothScroll)
        }
        this.selectedPosition = this.lastPageSelected // set selected position before update tabs color
        updateABColorNow()
        switchView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_agency_type, menu)
        childFragmentManager.fragments.forEach { fragment ->
            fragment.setHasOptionsMenu(this.pageScrollStateIdle)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_nearby) {
            viewModel.type.value?.let { type ->
                (activity as? MainActivity)?.addFragmentToStack(
                    NearbyFragment.newNearbyInstance(type),
                    this
                )
            }
            return true // handled
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        switchView()
        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
    }

    override fun onUserLocationChanged(newLocation: Location?) {
        addedViewModel?.onDeviceLocationChanged(newLocation)
    }

    private fun updateABColorNow() {
        updateABColor(delayInMs = 0L)
    }

    fun updateABColor(delayInMs: Long = 50L) {
        if (updateABColorJob?.isActive == true) {
            return // SKIP (already planned)
        }
        updateABColorJob = viewLifecycleOwner.lifecycleScope.launch {
            if (abBgColor != null && delayInMs > 0L) {
                delay(delayInMs) // debounce
            }
            withContext(Dispatchers.Default) { // CPU
                abBgColor = getNewABBgColor()?.also { abBgColor ->
                    withContext(Dispatchers.Main) { // UI
                        binding?.tabs?.setBackgroundColor(abBgColor)
                        abController?.apply {
                            setABBgColor(this@AgencyTypeFragment, abBgColor, false)
                            updateABBgColor()
                        }
                    }
                }
            }
            updateABColorJob?.cancel()
        }
    }

    override fun isABReady() = addedViewModel?.type?.value != null

    override fun getABTitle(context: Context?): CharSequence? {
        return addedViewModel?.type?.value?.let { context?.getString(it.allStringResId) }
            ?: context?.getString(R.string.ellipsis)
            ?: super.getABTitle(context)
    }

    override fun getABBgColor(context: Context?): Int? {
        return abBgColor
            ?: getNewABBgColor()?.also { newABBGColor -> abBgColor = newABBGColor }
            ?: super.getABBgColor(context)
    }

    private fun getNewABBgColor(): Int? {
        if (this.selectedPosition >= 0) {
            val colorInt = abColorizer.getBgColor(this.selectedPosition) ?: return null
            val count = this.adapter?.itemCount ?: 0
            if (this.selectionOffset > 0f && this.selectedPosition < (count - 1)) {
                val nextColorInt = abColorizer.getBgColor(this.selectedPosition + 1) ?: return null
                if (colorInt != nextColorInt) {
                    return ColorUtils.blendColors(nextColorInt, colorInt, selectionOffset)
                }
            }
            return colorInt
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateABColorJob?.cancel()
        binding?.viewpager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewpager?.adapter = null // cannot re-use Adapter w/ ViewPager
        adapter = null // cannot re-use Adapter w/ ViewPager
        emptyBinding = null
        binding = null
    }
}