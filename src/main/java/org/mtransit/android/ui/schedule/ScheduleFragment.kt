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
import org.mtransit.android.ui.view.common.attached

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

    private var binding: FragmentScheduleBinding? = null

    private var lastPageSelected: Int = SchedulePagerAdapter.STARTING_POSITION

    private var adapter: SchedulePagerAdapter? = null

    private fun makeAdapter() = SchedulePagerAdapter(this).apply {
        setUUID(attached { viewModel }?.uuid?.value)
        setAuthority(attached { viewModel }?.authority?.value)
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            attached { viewModel }?.onPageSelected(position)
            lastPageSelected = position
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentScheduleBinding.bind(view).apply {
            viewpager.offscreenPageLimit = 2
            viewpager.registerOnPageChangeCallback(onPageChangeCallback)
            viewpager.adapter = adapter ?: makeAdapter().also { adapter = it } // cannot re-use Adapter w/ ViewPager
            viewpager.setCurrentItem(lastPageSelected, false)
        }
        viewModel.authority.observe(viewLifecycleOwner, { authority ->
            adapter?.apply {
                val wasReady = isReady()
                setAuthority(authority)
                updateViews(wasReady)
            }
        })
        viewModel.uuid.observe(viewLifecycleOwner, { uuid ->
            adapter?.apply {
                val wasReady = isReady()
                setUUID(uuid)
                updateViews(wasReady)
            }
        })
        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
            if (removed) {
                (activity as MainActivity?)?.popFragmentFromStack(this) // close this fragment
            }
        })
        viewModel.colorInt.observe(viewLifecycleOwner, {
            abController?.setABBgColor(this, getABBgColor(context), false)
        })
        viewModel.agency.observe(viewLifecycleOwner, {
            abController?.setABSubtitle(this, getABSubtitle(context), false)
        })
        viewModel.rts.observe(viewLifecycleOwner, {
            abController?.setABBgColor(this, getABBgColor(context), false)
            abController?.setABSubtitle(this, getABSubtitle(context), false)
            abController?.setABReady(this, isABReady, true)
        })
    }

    private fun updateViews(wasReady: Boolean) {
        binding?.apply {
            if (!wasReady && adapter?.isReady() == true) {
                viewpager.setCurrentItem(lastPageSelected, false)
            }
            loading.root.isVisible = adapter?.isReady() == false
            viewpager.isVisible = adapter?.isReady() == true
        }
    }

    override fun isABReady() = attached { viewModel }?.rts?.value != null

    override fun getABTitle(context: Context?) = context?.getString(R.string.full_schedule) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = attached { viewModel }?.rts?.value?.let { rts ->
        POIManager.getNewOneLineDescription(rts, attached { viewModel }?.agency?.value)
    } ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) = attached { viewModel }?.colorInt?.value ?: super.getABBgColor(context)

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewpager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewpager?.adapter = null // cannot re-use Adapter w/ ViewPager
        adapter = null // cannot re-use Adapter w/ ViewPager
        binding = null
    }
}