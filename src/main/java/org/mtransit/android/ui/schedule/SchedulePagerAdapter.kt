package org.mtransit.android.ui.schedule

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.schedule.day.ScheduleDayFragment
import org.mtransit.android.util.UITimeUtils
import java.util.Calendar

class SchedulePagerAdapter(
    f: Fragment,
    todayStartsAtInMs: Long = UITimeUtils.getBeginningOfTodayInMs()
) : FragmentStateAdapter(f), MTLog.Loggable {

    companion object {
        private val LOG_TAG = SchedulePagerAdapter::class.java.simpleName

        private const val BEFORE_TODAY = 100

        private const val COUNT = BEFORE_TODAY + 365 // should be enough

        internal val STARTING_POSITION = if (BEFORE_TODAY > 0) BEFORE_TODAY else COUNT / 2
    }

    override fun getLogTag(): String = LOG_TAG

    private val todayStartsAtCal = UITimeUtils.getNewCalendarInstance(todayStartsAtInMs)

    private fun getPageDayCal(position: Int): Calendar {
        val pageDay = todayStartsAtCal.clone() as Calendar
        pageDay.add(Calendar.DATE, position - STARTING_POSITION)
        return pageDay
    }

    private var _uuid: String? = null
    private var _authority: String? = null

    fun setUUID(uuid: String?) {
        if (_uuid == uuid) {
            return
        }
        _uuid = uuid
        notifyDataSetChanged()
    }

    fun setAuthority(authority: String?) {
        if (_authority == authority) {
            return
        }
        _authority = authority
        notifyDataSetChanged()
    }

    fun isReady() = _uuid != null && _authority != null

    override fun getItemCount() = COUNT

    override fun createFragment(position: Int): Fragment = ScheduleDayFragment.newInstance(
        this._uuid ?: throw RuntimeException("Trying to create schedule day fragment w/o UUID!"),
        this._authority ?: throw RuntimeException("Trying to create schedule day fragment w/o authority!"),
        getPageDayCal(position).timeInMillis,
    )
}