@file:JvmName("ScheduleFragment") // ANALYTICS
package org.mtransit.android.ui.schedule

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.dp
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentScheduleInfiniteBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.StickyHeaderItemDecorator
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.ui.view.common.scrollToPositionWithOffset
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.FeatureFlags

@AndroidEntryPoint
class ScheduleFragment : ABFragment(R.layout.fragment_schedule_infinite), MenuProvider {

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

    private var bindingI: FragmentScheduleInfiniteBinding? = null

    private val listAdapter: ScheduleAdapter by lazy {
        ScheduleAdapter()
    }

    private var timeChangedReceiverEnabled = false

    private val timeChangedReceiver = UITimeUtils.TimeChangedReceiver { listAdapter.onTimeChanged() }

    private val onScrollListener = object : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            (recyclerView.layoutManager as LinearLayoutManager).let { linearLayoutManager ->
                val loadingPosition: Int = recyclerView.adapter?.itemCount?.minus(1) ?: -1
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == loadingPosition) {
                    recyclerView.post {
                        attachedViewModel?.increaseEndTime()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MenuHost).addMenuProvider(
            this, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
        bindingI = FragmentScheduleInfiniteBinding.bind(view).apply {
            list.adapter = listAdapter
            list.addOnScrollListener(onScrollListener)
            list.addItemDecoration(
                StickyHeaderItemDecorator(
                    listAdapter,
                    list,
                )
            )
        }
        viewModel.startEndAt.observe(viewLifecycleOwner) { startEndAt ->
            startEndAt?.let { (startInMs, endInMs) ->
                val scrollPosition = (bindingI?.list?.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition() ?: -1
                listAdapter.setStartEnd(startInMs, endInMs)
                bindingI?.list?.apply {
                    if (scrollPosition > 0) {
                        scrollToPosition(scrollPosition)
                    }
                }
            }
        }
        viewModel.scrolledToNow.observe(viewLifecycleOwner) {
            // NOTHING
        }
        viewModel.timestamps.observe(viewLifecycleOwner) { timestamps ->
            MTLog.d(this, "onChange() > ${timestamps?.size} - timestamps")
            val scrollPosition = (bindingI?.list?.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition() ?: -1
            listAdapter.setTimes(timestamps)
            bindingI?.apply {
                if (timestamps != null) {
                    if (viewModel.scrolledToNow.value == false) {
                        listAdapter.getScrollToNowPosition()?.let {
                            MTLog.d(this, "onChange() > getScrollToNowPosition() = $it")
                            list.scrollToPositionWithOffset(it, 48.dp)
                        }
                        viewModel.setScrolledToNow(true)
                    } else if (scrollPosition > 0) {
                        list.scrollToPosition(scrollPosition)
                    }
                }
                loadingLayout.isVisible = !listAdapter.isReady()
                list.isVisible = listAdapter.isReady()
            }
        }
        viewModel.rts.observe(viewLifecycleOwner) { rts ->
            listAdapter.setRTS(rts)
        }
        if (FeatureFlags.F_ACCESSIBILITY_CONSUMER) {
            viewModel.showAccessibility.observe(viewLifecycleOwner) { showAccessibility ->
                listAdapter.showingAccessibility = showAccessibility
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_schedule, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        MTLog.v(this, "onMenuItemSelected($menuItem)")
        return when (menuItem.itemId) {
            R.id.menu_today -> {
                bindingI?.apply {
                    listAdapter.getScrollToNowPosition()?.let {
                        MTLog.d(this, "onMenuItemSelected() > getScrollToNowPosition() = $it")
                        this.list.scrollToPositionWithOffset(it, 48.dp)
                    }
                    viewModel.setScrolledToNow(true)
                }
                true // handled
            }
            else -> false // not handled
        }
    }

    override fun onDetach() {
        super.onDetach()
        disableTimeChangedReceiver()
    }

    override fun onResume() {
        super.onResume()
        viewModel.initStartEndTimeIfNotSet()
        enableTimeChangedReceiver()
    }

    override fun onPause() {
        super.onPause()
        disableTimeChangedReceiver()
    }

    private fun enableTimeChangedReceiver() {
        if (!timeChangedReceiverEnabled) {
            activity?.registerReceiver(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER)
            timeChangedReceiverEnabled = true
            listAdapter.onTimeChanged() // force update to current time before next change
        }
    }

    private fun disableTimeChangedReceiver() {
        if (timeChangedReceiverEnabled) {
            activity?.unregisterReceiver(timeChangedReceiver)
            timeChangedReceiverEnabled = false
            listAdapter.onTimeChanged(-1L) // mark time as not updating anymore
        }
    }

    override fun isABReady() = attachedViewModel?.rts?.value != null

    override fun getABTitle(context: Context?) = context?.getString(R.string.full_schedule) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = attachedViewModel?.rts?.value?.let { rts ->
        POIManager.getNewOneLineDescription(rts, attachedViewModel?.agency?.value)
    } ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) = attachedViewModel?.colorInt?.value ?: super.getABBgColor(context)

    override fun onDestroyView() {
        super.onDestroyView()
        bindingI = null
    }
}