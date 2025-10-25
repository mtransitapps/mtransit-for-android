package org.mtransit.android.ui.serviceupdates

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.getDimensionInt
import org.mtransit.android.data.UIServiceUpdates
import org.mtransit.android.databinding.FragmentDialogServiceUpdatesBinding
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.serviceupdate.ServiceUpdatesHolder
import org.mtransit.android.ui.common.UISourceLabelUtils.setSourceLabelTextView
import org.mtransit.android.ui.fragment.MTBottomSheetDialogFragmentX
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.LinkUtils
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class ServiceUpdatesDialog : MTBottomSheetDialogFragmentX() {

    companion object {
        private val LOG_TAG: String = ServiceUpdatesDialog::class.java.simpleName

        @JvmStatic
        fun newInstance(
            authority: String,
            routeId: Long,
            directionId: Long? = null,
        ): ServiceUpdatesDialog {
            return ServiceUpdatesDialog().apply {
                arguments = newInstanceArgs(authority, routeId, directionId)
            }
        }

        @JvmStatic
        fun newInstanceArgs(
            authority: String,
            routeId: Long,
            directionId: Long? = null,
        ) = bundleOf(
            ServiceUpdatesViewModel.EXTRA_AUTHORITY to authority,
            ServiceUpdatesViewModel.EXTRA_ROUTE_ID to routeId,
            ServiceUpdatesViewModel.EXTRA_DIRECTION_ID to directionId,
        )
    }

    override fun getLogTag() = LOG_TAG

    private val viewModel by viewModels<ServiceUpdatesViewModel>()
    private val attachedViewModel get() = if (isAttached()) viewModel else null

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var binding: FragmentDialogServiceUpdatesBinding? = null

    private var behavior: BottomSheetBehavior<*>? = null

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
        return inflater.inflate(R.layout.fragment_dialog_service_updates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDialogServiceUpdatesBinding.bind(view).apply {
            dialog?.setOnShowListener {
                root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.color_background))
            }
        }
        viewModel.holder.observe(viewLifecycleOwner) { holder ->
            updateServiceUpdatesView(holder)
        }
        viewModel.serviceUpdateLoadedEvent.observe(viewLifecycleOwner) {
            updateServiceUpdatesView()
        }
    }

    private fun updateServiceUpdatesView(
        holder: ServiceUpdatesHolder? = attachedViewModel?.holder?.value,
    ) = binding?.apply {
        val serviceUpdates: List<ServiceUpdate>? = holder?.getServiceUpdates(
            serviceUpdateLoader,
            emptyList() // TODO?
        )
        serviceUpdates ?: run {
            emptyLayout.isVisible = false
            poiServiceUpdate.isVisible = false
            loadingLayout.isVisible = true
            return@apply // unknown
        }
        loadingLayout.isVisible = false
        poiServiceUpdate.apply {
            val serviceUpdatesHTMLText = UIServiceUpdates.makeServiceUpdatesHTMLText(context, serviceUpdates)
            val hasServiceUpdatesToShow = serviceUpdatesHTMLText.isNotEmpty()
            val hasWarning = UIServiceUpdates.hasWarnings(serviceUpdates)
            serviceUpdateText.setText(LinkUtils.linkifyHtml(HtmlUtils.fromHtml(serviceUpdatesHTMLText), false), TextView.BufferType.SPANNABLE)
            serviceUpdateText.movementMethod = LinkUtils.LinkMovementMethodInterceptor.getInstance { view, url ->
                LinkUtils.open(view, requireActivity(), url, getString(commonsR.string.web_browser), true)
            }
            serviceUpdateText.setBackgroundResource(
                if (hasWarning) R.drawable.service_update_warning else R.drawable.service_update_info
            )
            setSourceLabelTextView(sourceLabel, serviceUpdates)
            root.isVisible = hasServiceUpdatesToShow
            emptyLayout.isVisible = !hasServiceUpdatesToShow
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        behavior = null
    }
}
