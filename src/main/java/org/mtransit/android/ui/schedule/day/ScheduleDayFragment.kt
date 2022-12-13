@file:JvmName("ScheduleDayFragment") // ANALYTICS
package org.mtransit.android.ui.schedule.day

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.databinding.FragmentScheduleDayBinding
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.UITimeUtils
import java.util.Locale

@AndroidEntryPoint
class ScheduleDayFragment : MTFragmentX(R.layout.fragment_schedule_day), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ScheduleDayFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(uuid: String, authority: String, dayStartsAtInMs: Long): ScheduleDayFragment {
            return ScheduleDayFragment().apply {
                arguments = bundleOf(
                    ScheduleDayViewModel.EXTRA_POI_UUID to uuid,
                    ScheduleDayViewModel.EXTRA_AUTHORITY to authority,
                    ScheduleDayViewModel.EXTRA_DAY_START_AT_IN_MS to dayStartsAtInMs,
                )
            }
        }
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<ScheduleDayViewModel>()

    private var binding: FragmentScheduleDayBinding? = null

    private val adapter: ScheduleDayAdapter by lazy {
        ScheduleDayAdapter()
    }

    private var timeChangedReceiverEnabled = false

    private val timeChangedReceiver = UITimeUtils.TimeChangedReceiver { adapter.onTimeChanged() }

    private val dayDateFormat by lazy { ThreadSafeDateFormatter("EEEE, MMM d, yyyy", Locale.getDefault()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentScheduleDayBinding.bind(view).apply {
            list.adapter = adapter
        }
        viewModel.yearMonthDay.observe(viewLifecycleOwner) { yearMonthDay ->
            theLogTag = yearMonthDay?.let { "$LOG_TAG-$it" } ?: LOG_TAG
            adapter.setYearMonthDay(yearMonthDay)
        }
        viewModel.dayStartsAtInMs.observe(viewLifecycleOwner) { dayStartsAtInMs ->
            dayStartsAtInMs?.let {
                binding?.dayDate?.text = getDayDateString(it)
                adapter.setDayStartsAt(it)
            }
        }
        viewModel.timestamps.observe(viewLifecycleOwner) { timestamps ->
            adapter.setTimes(timestamps)
            binding?.apply {
                if (timestamps != null && viewModel.scrolledToNow.value != true) {
                    adapter.getScrollToNowPosition()?.let {
                        list.scrollToPosition(it)
                    }
                    viewModel.setScrolledToNow(true)
                }
                loadingLayout.isVisible = !adapter.isReady()
                list.isVisible = adapter.isReady()
            }
        }
        viewModel.rts.observe(viewLifecycleOwner) { rts ->
            adapter.setRTS(rts)
        }
    }

    private fun getDayDateString(dayStartsAtInMs: Long): CharSequence {
        return UITimeUtils.getNearRelativeDay(
            requireContext(),
            dayStartsAtInMs,
            dayDateFormat.formatThreadSafe(UITimeUtils.getNewCalendar(dayStartsAtInMs).time),
        )
    }

    override fun onDetach() {
        super.onDetach()
        disableTimeChangedReceiver()
    }

    override fun onResume() {
        super.onResume()
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
            adapter.onTimeChanged() // force update to current time before next change
        }
    }

    private fun disableTimeChangedReceiver() {
        if (timeChangedReceiverEnabled) {
            activity?.unregisterReceiver(timeChangedReceiver)
            timeChangedReceiverEnabled = false
            adapter.onTimeChanged(-1L) // mark time as not updating anymore
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}