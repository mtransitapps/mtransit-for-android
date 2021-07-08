@file:JvmName("PickPOIDialogFragment") // ANALYTICS
package org.mtransit.android.ui.pick

import android.app.Dialog
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.collection.ArrayMap
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentDialogPickPoiBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.fragment.MTDialogFragmentX
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.attached
import javax.inject.Inject

@AndroidEntryPoint
class PickPOIDialogFragment : MTDialogFragmentX(), MTActivityWithLocation.UserLocationListener, IActivity {

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

    @Inject
    lateinit var sensorManager: MTSensorManager

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var poiRepository: POIRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var binding: FragmentDialogPickPoiBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null

    private val adapter: POIArrayAdapter by lazy {
        POIArrayAdapter(
            this,
            this.sensorManager,
            this.dataSourcesRepository,
            this.poiRepository,
            this.favoriteManager,
            this.statusLoader,
            this.serviceUpdateLoader
        ).apply {
            setOnClickHandledListener { dismiss() }
            logTag = logTag
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.adapter.setActivity(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return FragmentDialogPickPoiBinding.inflate(inflater).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            adapter.setManualScrollView(scrollview)
            adapter.setManualLayout(list)
            binding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.poiList.observe(viewLifecycleOwner, { poiList ->
            adapter.setPois(poiList)
            adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            adapter.initManual()
            binding?.apply {
                when {
                    !adapter.isInitialized -> {
                        emptyBinding?.root?.isVisible = false
                        list.isVisible = false
                        loading.root.isVisible = true
                    }
                    adapter.poisCount == 0 -> {
                        loading.root.isVisible = false
                        list.isVisible = false
                        (emptyBinding?.root ?: emptyStub.inflate()).isVisible = true
                    }
                    else -> {
                        loading.root.isVisible = false
                        emptyBinding?.root?.isVisible = false
                        list.isVisible = true
                    }
                }
            }
        })
        viewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation ->
            adapter.setLocation(deviceLocation)
        })
        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
            if (removed == true) {
                dismiss()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        this.adapter.onResume(this, viewModel.deviceLocation.value)
        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
    }

    override fun onUserLocationChanged(newLocation: Location?) {
        attached { viewModel }?.onDeviceLocationChanged(newLocation)
    }

    override fun onPause() {
        super.onPause()
        this.adapter.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emptyBinding = null
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.adapter.onDestroy()
    }

    override fun getLifecycleOwner() = this

    override fun finish() {
        activity?.finish()
    }

    override fun <T : View?> findViewById(id: Int): T? = view?.findViewById<T>(id)
}