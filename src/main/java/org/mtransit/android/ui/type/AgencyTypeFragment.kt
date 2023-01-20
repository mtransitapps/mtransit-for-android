@file:JvmName("AgencyTypeFragment") // ANALYTICS
package org.mtransit.android.ui.type

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
import org.mtransit.android.ui.ActionBarController.SimpleActionBarColorizer
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.nearby.NearbyFragment
import org.mtransit.android.ui.view.common.MTTabLayoutMediator
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.commons.FeatureFlags
import kotlin.math.abs

@AndroidEntryPoint
class AgencyTypeFragment : ABFragment(R.layout.fragment_agency_type), MTActivityWithLocation.DeviceLocationListener, MenuProvider {

    companion object {
        private val LOG_TAG = AgencyTypeFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Browse"

        @JvmStatic
        fun newInstance(dst: DataSourceType) = newInstance(dst.id)


        @JvmStatic
        fun newInstance(dstId: Int): AgencyTypeFragment {
            return AgencyTypeFragment().apply {
                arguments = newInstanceArgs(dstId)
            }
        }

        @JvmStatic
        fun newInstanceArgs(dst: DataSourceType) = newInstanceArgs(dst.id)

        @JvmStatic
        fun newInstanceArgs(dstId: Int) = bundleOf(
            AgencyTypeViewModel.EXTRA_TYPE_ID to dstId
        )
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = attachedViewModel?.type?.value?.let { type -> "$TRACKING_SCREEN_NAME/${type.id}" } ?: TRACKING_SCREEN_NAME

    private val viewModel by viewModels<AgencyTypeViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    private var binding: FragmentAgencyTypeBinding? = null

    private var abBgColor: Int? = null

    private var lastPageSelected = -1
    private var selectedPosition = -1
    private var selectionOffset = 0f
    private var pageScrollStateIdle = true

    private var updateABColorJob: Job? = null

    private val defaultColor: Int by lazy { ThemeUtils.resolveColorAttribute(requireContext(), R.attr.colorPrimary) }

    private val abColorizer: SimpleActionBarColorizer by lazy { SimpleActionBarColorizer() }

    private var pagerAdapter: AgencyTypePagerAdapter? = null

    private fun makePagerAdapter() = AgencyTypePagerAdapter(this).apply {
        setAgencies(attachedViewModel?.typeAgencies?.value)
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            attachedViewModel?.onPageSelected(position)
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MTTransitions.setContainerTransformTransition(this)
        // if (FeatureFlags.F_TRANSITION) {
        // exitTransition = MTTransitions.newHoldTransition() // not working with AdapterView // FIXME #RecyclerView
        // }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MTTransitions.postponeEnterTransition(this)
        (requireActivity() as MenuHost).addMenuProvider(
            this, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
        binding = FragmentAgencyTypeBinding.bind(view).apply {
            viewPager.offscreenPageLimit = 2
            viewPager.registerOnPageChangeCallback(onPageChangeCallback)
            viewPager.adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
            MTTabLayoutMediator(tabs, viewPager, autoRefresh = true, smoothScroll = true) { tab, position ->
                tab.text = viewModel.typeAgencies.value?.get(position)?.shortName
            }.attach()
            if (FeatureFlags.F_NAVIGATION) {
                (activity as? org.mtransit.android.ui.main.MainActivity?)?.supportActionBar?.elevation?.let {
                    tabs.elevation = it
                }
            }
            showSelectedTab()
        }
        viewModel.typeAgencies.observe(viewLifecycleOwner) { agencies ->
            if (pagerAdapter?.setAgencies(agencies) == true) {
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
        }
        viewModel.type.observe(viewLifecycleOwner) { type ->
            binding?.tabs?.isVisible = type != DataSourceType.TYPE_MODULE
            abController?.setABTitle(this, getABTitle(context), false)
            abController?.setABReady(this, isABReady, true)
        }
        viewModel.selectedTypeAgencyPosition.observe(viewLifecycleOwner) { newLastPageSelected ->
            newLastPageSelected?.let {
                if (this.lastPageSelected < 0) {
                    this.lastPageSelected = it
                    showSelectedTab()
                    onPageChangeCallback.onPageSelected(this.lastPageSelected) // tell the current page it's selected
                }
            }
        }
    }

    private fun switchView() {
        binding?.apply {
            when {
                lastPageSelected < 0 || pagerAdapter?.isReady() != true -> { // LOADING
                    emptyLayout.isVisible = false
                    viewPager.isVisible = false
                    tabs.isVisible = false
                    loadingLayout.isVisible = true
                }
                pagerAdapter?.itemCount == 0 -> { // EMPTY
                    loadingLayout.isVisible = false
                    viewPager.isVisible = false
                    tabs.isVisible = false
                    emptyLayout.isVisible = true
                }
                else -> { // LOADED
                    loadingLayout.isVisible = false
                    emptyLayout.isVisible = false
                    tabs.isVisible = viewModel.type.value != DataSourceType.TYPE_MODULE
                    viewPager.isVisible = true
                }
            }
        }
    }

    private fun showSelectedTab() {
        if (this.pagerAdapter?.isReady() != true) {
            MTLog.d(this, "showSelectedTab() > SKIP (no adapter items)")
            return
        }
        if (this.lastPageSelected < 0) {
            MTLog.d(this, "showSelectedTab() > SKIP (no last page selected)")
            return
        }
        val smoothScroll = this.selectedPosition >= 0
        val itemToSelect = this.lastPageSelected
        binding?.viewPager?.doOnAttach {
            binding?.viewPager?.setCurrentItem(itemToSelect, smoothScroll)
        }
        this.selectedPosition = this.lastPageSelected // set selected position before update tabs color
        updateABColorNow()
        switchView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_agency_type, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.menu_nearby) {
            viewModel.type.value?.let { type ->
                (activity as? MainActivity)?.addFragmentToStack(
                    NearbyFragment.newNearbyInstance(type),
                    this
                )
            }
            return true // handled
        }
        return false // not handled
    }

    override fun onResume() {
        super.onResume()
        switchView()
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
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

    override fun isABReady() = attachedViewModel?.type?.value != null

    override fun getABTitle(context: Context?): CharSequence? {
        return attachedViewModel?.type?.value?.let { context?.getString(it.allStringResId) }
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
            val count = this.pagerAdapter?.itemCount ?: 0
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
        binding?.viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewPager?.adapter = null // cannot re-use Adapter w/ ViewPager
        pagerAdapter = null // cannot re-use Adapter w/ ViewPager
        binding = null
    }
}