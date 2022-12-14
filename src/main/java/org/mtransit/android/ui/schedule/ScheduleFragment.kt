@file:JvmName("ScheduleFragment") // ANALYTICS
package org.mtransit.android.ui.schedule

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.data.POI
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentScheduleBinding
import org.mtransit.android.databinding.FragmentScheduleInfiniteBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.FeatureFlags

@AndroidEntryPoint
class ScheduleFragment : ABFragment(if (FeatureFlags.F_SCHEDULE_INFINITE) R.layout.fragment_schedule_infinite else R.layout.fragment_schedule) {

    companion object {
        private val LOG_TAG = ScheduleFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Schedule"

        @JvmStatic
        fun newInstance(poim: POIManager, dataSourcesRepository: DataSourcesRepository) = newInstance(poim.poi, poim.getColor(dataSourcesRepository))

        @JvmStatic
        fun newInstance(poi: POI, optColorInt: Int? = null) = newInstance(poi.uuid, poi.authority, optColorInt?.let { ColorUtils.toRGBColor(it) })

        @JvmStatic
        fun newInstance(
            uuid: String,
            authority: String,
            optColor: String? = null
        ): ScheduleFragment {
            return ScheduleFragment().apply {
                arguments = newInstanceArgs(uuid, authority, optColor)
            }
        }

        @JvmStatic
        fun newInstanceArgs(poim: POIManager, dataSourcesRepository: DataSourcesRepository) = newInstanceArgs(poim.poi, poim.getColor(dataSourcesRepository))

        @JvmStatic
        fun newInstanceArgs(poi: POI, optColorInt: Int? = null) = newInstanceArgs(poi.uuid, poi.authority, optColorInt?.let { ColorUtils.toRGBColor(it) })

        @JvmStatic
        fun newInstanceArgs(
            uuid: String,
            authority: String,
            optColor: String? = null
        ) = bundleOf(
            ScheduleViewModel.EXTRA_POI_UUID to uuid,
            ScheduleViewModel.EXTRA_AUTHORITY to authority,
            ScheduleViewModel.EXTRA_COLOR to (optColor ?: ScheduleViewModel.EXTRA_COLOR_DEFAULT)
        )
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<ScheduleViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    private var binding: FragmentScheduleBinding? = null

    private var lastPageSelected: Int = SchedulePagerAdapter.STARTING_POSITION

    private var pagerAdapter: SchedulePagerAdapter? = null

    private fun makePagerAdapter() = SchedulePagerAdapter(this).apply {
        setUUID(attachedViewModel?.uuid?.value)
        setAuthority(attachedViewModel?.authority?.value)
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            attachedViewModel?.onPageSelected(position)
            lastPageSelected = position
        }
    }

    /**
     * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - START
     */
    private var bindingI: FragmentScheduleInfiniteBinding? = null

    private val listAdapter: ScheduleAdapter by lazy {
        ScheduleAdapter()
    }

    private var timeChangedReceiverEnabled = false

    private val timeChangedReceiver = UITimeUtils.TimeChangedReceiver { listAdapter.onTimeChanged() }

    /**
     * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - END
     */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FeatureFlags.F_SCHEDULE_INFINITE) {
            bindingI = FragmentScheduleInfiniteBinding.bind(view).apply {
                list.adapter = listAdapter
            }
        } else {
            binding = FragmentScheduleBinding.bind(view).apply {
                viewPager.offscreenPageLimit = 2
                viewPager.registerOnPageChangeCallback(onPageChangeCallback)
                viewPager.adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
                viewPager.setCurrentItem(lastPageSelected, false)
            }
        }
        if (FeatureFlags.F_SCHEDULE_INFINITE) {
            viewModel.startEndAt.observe(viewLifecycleOwner) { startEndAt ->
                startEndAt?.let { (startInMs, endInMs) ->
                    listAdapter.setStartEnd(startInMs, endInMs)
                }
            }
            viewModel.timestamps.observe(viewLifecycleOwner) { timestamps ->
                listAdapter.setTimes(timestamps)
                bindingI?.apply {
                    if (timestamps != null && viewModel.scrolledToNow.value != true) {
                        listAdapter.getScrollToNowPosition()?.let {
                            list.scrollToPosition(it)
                        }
                        viewModel.setScrolledToNow(true)
                    }
                    loadingLayout.isVisible = !listAdapter.isReady()
                    list.isVisible = listAdapter.isReady()
                }
            }
            viewModel.rts.observe(viewLifecycleOwner) { rts ->
                listAdapter.setRTS(rts)
            }
        } else {
            viewModel.authority.observe(viewLifecycleOwner) { authority ->
                pagerAdapter?.apply {
                    val wasReady = isReady()
                    setAuthority(authority)
                    updateViews(wasReady)
                }
            }
            viewModel.uuid.observe(viewLifecycleOwner) { uuid ->
                pagerAdapter?.apply {
                    val wasReady = isReady()
                    setUUID(uuid)
                    updateViews(wasReady)
                }
            }
        }
        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
            if (removed) {
                (activity as MainActivity?)?.popFragmentFromStack(this) // close this fragment
            }
        })
        viewModel.colorInt.observe(viewLifecycleOwner) {
            abController?.setABBgColor(this, getABBgColor(context), false)
        }
        viewModel.agency.observe(viewLifecycleOwner) {
            abController?.setABSubtitle(this, getABSubtitle(context), false)
        }
        viewModel.rts.observe(viewLifecycleOwner) {
            abController?.setABBgColor(this, getABBgColor(context), false)
            abController?.setABSubtitle(this, getABSubtitle(context), false)
            abController?.setABReady(this, isABReady, true)
        }
    }

    private fun updateViews(wasReady: Boolean) {
        if (FeatureFlags.F_SCHEDULE_INFINITE) {
            return
        }
        binding?.apply {
            if (!wasReady && pagerAdapter?.isReady() == true) {
                viewPager.setCurrentItem(lastPageSelected, false)
            }
            loadingLayout.isVisible = pagerAdapter?.isReady() == false
            viewPager.isVisible = pagerAdapter?.isReady() == true
        }
    }

    /**
     * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - START
     */

    override fun onDetach() {
        super.onDetach()
        if (!FeatureFlags.F_SCHEDULE_INFINITE) {
            return
        }
        disableTimeChangedReceiver()
    }

    override fun onResume() {
        super.onResume()
        if (!FeatureFlags.F_SCHEDULE_INFINITE) {
            return
        }
        viewModel.initStartEndTimeIfNotSet()
        enableTimeChangedReceiver()
    }

    override fun onPause() {
        super.onPause()
        if (!FeatureFlags.F_SCHEDULE_INFINITE) {
            return
        }
        disableTimeChangedReceiver()
    }

    private fun enableTimeChangedReceiver() {
        if (!FeatureFlags.F_SCHEDULE_INFINITE) {
            return
        }
        if (!timeChangedReceiverEnabled) {
            activity?.registerReceiver(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER)
            timeChangedReceiverEnabled = true
            listAdapter.onTimeChanged() // force update to current time before next change
        }
    }

    private fun disableTimeChangedReceiver() {
        if (!FeatureFlags.F_SCHEDULE_INFINITE) {
            return
        }
        if (timeChangedReceiverEnabled) {
            activity?.unregisterReceiver(timeChangedReceiver)
            timeChangedReceiverEnabled = false
            listAdapter.onTimeChanged(-1L) // mark time as not updating anymore
        }
    }

    /**
     * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - END
     */

    override fun isABReady() = attachedViewModel?.rts?.value != null

    override fun getABTitle(context: Context?) = context?.getString(R.string.full_schedule) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = attachedViewModel?.rts?.value?.let { rts ->
        POIManager.getNewOneLineDescription(rts, attachedViewModel?.agency?.value)
    } ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) = attachedViewModel?.colorInt?.value ?: super.getABBgColor(context)

    override fun onDestroyView() {
        super.onDestroyView()
        if (FeatureFlags.F_SCHEDULE_INFINITE) {
            binding?.viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
            binding?.viewPager?.adapter = null // cannot re-use Adapter w/ ViewPager
            pagerAdapter = null // cannot re-use Adapter w/ ViewPager
            binding = null
        } else {
            bindingI = null
        }
    }
}