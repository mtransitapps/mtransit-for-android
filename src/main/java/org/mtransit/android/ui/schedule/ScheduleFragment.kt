@file:JvmName("ScheduleFragment") // ANALYTICS
package org.mtransit.android.ui.schedule

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import org.mtransit.android.R
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentScheduleBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.common.EventObserver

class ScheduleFragment : ABFragment(R.layout.fragment_schedule) {

    companion object {
        private val LOG_TAG = ScheduleFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Schedule"

        @JvmStatic
        fun newInstance(poim: POIManager): ScheduleFragment {
            return newInstance(
                poim.poi.uuid,
                poim.poi.authority,
                poim.color,
            )
        }

        @JvmStatic
        fun newInstance(uuid: String, authority: String, colorInt: Int?): ScheduleFragment {
            return ScheduleFragment().apply {
                arguments = bundleOf(
                    ScheduleViewModel.EXTRA_POI_UUID to uuid,
                    ScheduleViewModel.EXTRA_AUTHORITY to authority,
                    ScheduleViewModel.EXTRA_COLOR_INT to colorInt,
                )
            }
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<ScheduleViewModel>()

    private var binding: FragmentScheduleBinding? = null

    private var lastPageSelected: Int = SchedulePagerAdapter.STARTING_POSITION

    private val adapter: SchedulePagerAdapter by lazy {
        SchedulePagerAdapter(this).apply {
            setUUID(viewModel.uuid.value)
            setAuthority(viewModel.authority.value)
        }
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            lastPageSelected = position
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentScheduleBinding.bind(view).apply {
            viewpager.offscreenPageLimit = 2
            viewpager.registerOnPageChangeCallback(onPageChangeCallback)
            viewpager.adapter = adapter
            viewpager.setCurrentItem(lastPageSelected, false)
        }
        viewModel.authority.observe(viewLifecycleOwner, { authority ->
            val wasReady = adapter.isReady()
            adapter.setAuthority(authority)
            updateViews(wasReady)
        })
        viewModel.uuid.observe(viewLifecycleOwner, { uuid ->
            val wasReady = adapter.isReady()
            adapter.setUUID(uuid)
            updateViews(wasReady)
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
            if (!wasReady && adapter.isReady()) {
                viewpager.setCurrentItem(lastPageSelected, false)
            }
            loading.root.isVisible = !adapter.isReady()
            viewpager.isVisible = adapter.isReady()
        }
    }

    override fun isABReady() = viewModel.rts.value != null

    override fun getABTitle(context: Context?) = context?.getString(R.string.full_schedule) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = viewModel.rts.value?.let { rts ->
        POIManager.getOneLineDescription(viewModel.agency.value, rts)
    } ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) = viewModel.colorInt.value ?: super.getABBgColor(context)

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewpager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding = null
    }
}