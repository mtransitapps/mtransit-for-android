@file:JvmName("PickPOIDialogFragment") // ANALYTICS
package org.mtransit.android.ui.pick

import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.getDimensionInt
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentDialogPickPoiBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.fragment.MTBottomSheetDialogFragmentX
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.IFragment
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import javax.inject.Inject

@AndroidEntryPoint
class PickPOIDialogFragment : MTBottomSheetDialogFragmentX(), DeviceLocationListener, IFragment {

    companion object {
        private val LOG_TAG = PickPOIDialogFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            uuidsAndAuthorities: ArrayMap<String, String>
        ): PickPOIDialogFragment {
            return newInstance(
                ArrayList(uuidsAndAuthorities.keys),
                ArrayList(uuidsAndAuthorities.values),
            )
        }

        @JvmStatic
        fun newInstance(
            uuids: ArrayList<String>,
            authorities: ArrayList<String>
        ): PickPOIDialogFragment {
            return PickPOIDialogFragment().apply {
                arguments = bundleOf(
                    PickPOIViewModel.EXTRA_POI_UUIDS to uuids,
                    PickPOIViewModel.EXTRA_POI_AUTHORITIES to authorities,
                )
            }
        }
    }

    override fun getLogTag(): String = LOG_TAG

    private val viewModel by viewModels<PickPOIViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    @Inject
    lateinit var sensorManager: MTSensorManager

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var defaultPrefRepository: DefaultPreferenceRepository

    @Inject
    lateinit var localPreferenceRepository: LocalPreferenceRepository

    @Inject
    lateinit var poiRepository: POIRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var binding: FragmentDialogPickPoiBinding? = null

    private var behavior: BottomSheetBehavior<*>? = null

    private val onClickHandledListener: POIArrayAdapter.OnClickHandledListener by lazy {
        POIArrayAdapter.OnClickHandledListener {
            behavior?.state = BottomSheetBehavior.STATE_HIDDEN
            dismissAllowingStateLoss()
        }
    }

    private val adapter: POIArrayAdapter by lazy {
        POIArrayAdapter(
            this,
            this.sensorManager,
            this.dataSourcesRepository,
            this.defaultPrefRepository,
            this.localPreferenceRepository,
            this.poiRepository,
            this.favoriteManager,
            this.statusLoader,
            this.serviceUpdateLoader
        ).apply {
            logTag = this@PickPOIDialogFragment.logTag
            setOnClickHandledListener(onClickHandledListener)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.adapter.setActivity(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            behavior = (this as? BottomSheetDialog)?.behavior
                ?.apply {
                    resources.getDimensionInt(R.dimen.bottom_sheet_min_height).takeIf { it > 0 }?.let {
                        peekHeight = it
                    }
                }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_dialog_pick_poi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDialogPickPoiBinding.bind(view).apply {
            adapter.setManualScrollView(scrollView)
            adapter.setManualLayout(list)
            dialog?.setOnShowListener {
                root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.color_background))
            }
        }
        viewModel.poiList.observe(viewLifecycleOwner) { poiList ->
            adapter.setPois(poiList)
            adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            adapter.initManual()
            binding?.apply {
                when {
                    !adapter.isInitialized -> {
                        emptyLayout.isVisible = false
                        list.isVisible = false
                        loadingLayout.isVisible = true
                    }

                    adapter.poisCount == 0 -> {
                        loadingLayout.isVisible = false
                        list.isVisible = false
                        emptyLayout.isVisible = true
                    }

                    else -> {
                        loadingLayout.isVisible = false
                        emptyLayout.isVisible = false
                        list.isVisible = true
                    }
                }
            }
        }
        viewModel.deviceLocation.observe(viewLifecycleOwner) { deviceLocation ->
            adapter.setLocation(deviceLocation)
        }
        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
            if (removed == true) {
                dismiss()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        this.adapter.onResume(this, viewModel.deviceLocation.value)
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    override fun onPause() {
        super.onPause()
        this.adapter.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.adapter.onDestroy()
        behavior = null
    }

    override fun <T : View?> findViewById(id: Int) = this.view?.findViewById<T>(id)
}