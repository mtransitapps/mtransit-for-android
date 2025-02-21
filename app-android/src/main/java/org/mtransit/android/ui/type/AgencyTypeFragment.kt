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
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
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
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.databinding.FragmentAgencyTypeBinding
import org.mtransit.android.ui.ActionBarController.SimpleActionBarColorizer
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.common.UIColorUtils
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledUI
import org.mtransit.android.ui.main.NextMainActivity
import org.mtransit.android.ui.nearby.NearbyFragment
import org.mtransit.android.ui.applyStatusBarsHeightEdgeToEdge
import org.mtransit.android.ui.view.common.MTTabLayoutMediator
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.commons.FeatureFlags
import kotlin.math.abs
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class AgencyTypeFragment : ABFragment(R.layout.fragment_agency_type),
    MTActivityWithLocation.DeviceLocationListener,
    ModuleDisabledAwareFragment,
    MenuProvider {

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

    override val viewModel by viewModels<AgencyTypeViewModel>()
    override val attachedViewModel
        get() = if (isAttached()) viewModel else null

    override fun getContextView(): View? = this.binding?.contextView ?: this.view

    private var binding: FragmentAgencyTypeBinding? = null

    @ColorInt
    private var abBgColorInt: Int? = null

    private var lastPageSelected = -1
    private var selectedPosition = -1
    private var selectionOffset = 0f
    private var pageScrollStateIdle = true

    private var updateABColorJob: Job? = null

    private val defaultColor: Int by lazy { getDefaultABBgColor(requireContext()) }

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
                (activity as? NextMainActivity?)?.supportActionBar?.elevation?.let {
                    tabs.elevation = it
                }
            }
            showSelectedTab()
            fragmentStatusBarBg.applyStatusBarsHeightEdgeToEdge(
                initialHeightPx = context.resources.getDimensionPixelSize(R.dimen.action_bar_size_static)
            )
        }
        viewModel.typeAgencies.observe(viewLifecycleOwner) { agencies ->
            if (pagerAdapter?.setAgencies(agencies) == true) {
                showSelectedTab()
                abBgColorInt = null // reset
                abColorizer.setBgColors(
                    *(agencies
                        ?.filter { it.type != DataSourceType.TYPE_MODULE }
                        ?.takeIf { it.isNotEmpty() }
                        ?.map { UIColorUtils.adaptBackgroundColorToLightText(context, it.colorInt ?: defaultColor) }
                        ?.toIntArray()
                        ?: arrayOf(defaultColor).toIntArray())
                )
                updateABColorNow()
            } else {
                switchView()
            }
            agencies?.let {
                MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this)
            }
        }
        viewModel.title.observe(viewLifecycleOwner) {
            abController?.setABTitle(this, getABTitle(context), false)
            abController?.setABReady(this, isABReady, true)
        }
        viewModel.tabsVisible.observe(viewLifecycleOwner) { tabsVisible ->
            binding?.apply {
                tabs.isVisible = tabsVisible
            }
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
        ModuleDisabledUI.onViewCreated(this)
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
                    tabs.isVisible = viewModel.tabsVisible.value == true
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
        binding?.apply {
            viewPager.doOnAttach {
                viewPager.setCurrentItem(itemToSelect, smoothScroll)
            }
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
        updateABColorJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) { // CPU
            if (abBgColorInt != null && delayInMs > 0L) {
                delay(delayInMs) // debounce
            }
            abBgColorInt = getNewABBgColorInt()?.also { abBgColorInt ->
                withContext(Dispatchers.Main) { // UI
                    binding?.tabs?.setBackgroundColor(abBgColorInt)
                    abController?.apply {
                        setABBgColor(this@AgencyTypeFragment, abBgColorInt, false)
                        updateABBgColor()
                    }
                }
            }
            updateABColorJob?.cancel()
        }
    }

    override fun isABReady() = attachedViewModel?.title?.value != null

    override fun getABTitle(context: Context?): CharSequence? {
        return attachedViewModel?.title?.value
            ?: context?.getString(commonsR.string.ellipsis)
            ?: super.getABTitle(context)
    }

    @ColorInt
    override fun getABBgColor(context: Context?): Int? {
        return abBgColorInt
            ?: super.getABBgColor(context)
    }

    @ColorInt
    @WorkerThread
    private fun getNewABBgColorInt(): Int? {
        val selectedPosition = this.selectedPosition.takeIf { it >= 0 } ?: return null
        val colorInt = this.abColorizer.getBgColor(selectedPosition) ?: return null
        val count = this.pagerAdapter?.itemCount ?: 0
        if (this.selectionOffset > 0f && selectedPosition < (count - 1)) {
            this.abColorizer.getBgColor(selectedPosition + 1)?.let { nextColorInt ->
                if (colorInt != nextColorInt) {
                    return ColorUtils.blendColors(nextColorInt, colorInt, selectionOffset)
                }
            }
        }
        return colorInt
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