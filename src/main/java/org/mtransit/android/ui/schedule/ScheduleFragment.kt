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
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible

@AndroidEntryPoint
class ScheduleFragment : ABFragment(R.layout.fragment_schedule) {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentScheduleBinding.bind(view).apply {
            viewPager.offscreenPageLimit = 2
            viewPager.registerOnPageChangeCallback(onPageChangeCallback)
            viewPager.adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
            viewPager.setCurrentItem(lastPageSelected, false)
        }
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
        binding?.apply {
            if (!wasReady && pagerAdapter?.isReady() == true) {
                viewPager.setCurrentItem(lastPageSelected, false)
            }
            loadingLayout.isVisible = pagerAdapter?.isReady() == false
            viewPager.isVisible = pagerAdapter?.isReady() == true
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
        binding?.viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewPager?.adapter = null // cannot re-use Adapter w/ ViewPager
        pagerAdapter = null // cannot re-use Adapter w/ ViewPager
        binding = null
    }
}