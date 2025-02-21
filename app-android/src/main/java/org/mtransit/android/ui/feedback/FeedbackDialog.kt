package org.mtransit.android.ui.feedback

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.getDimensionInt
import org.mtransit.android.databinding.FragmentDialogFeedbackBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.fragment.MTBottomSheetDialogFragmentX
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.LinkUtils
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class FeedbackDialog : MTBottomSheetDialogFragmentX() {

    companion object {
        private val LOG_TAG = FeedbackDialog::class.java.simpleName

        @JvmStatic
        fun newInstance(
        ): FeedbackDialog {
            return FeedbackDialog()
        }
    }

    override fun getLogTag(): String = LOG_TAG

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    private val viewModel by viewModels<FeedbackViewModel>()

    private var binding: FragmentDialogFeedbackBinding? = null

    private var behavior: BottomSheetBehavior<*>? = null

    private val headerAdapter by lazy {
        HeaderFeedbackAdapter(onClick = {
            activity?.let { activityNN ->
                LinkUtils.sendEmail(activityNN, dataSourcesRepository)
                behavior?.state = BottomSheetBehavior.STATE_HIDDEN
                dismissAllowingStateLoss()
            }
        })
    }

    private val agenciesAdapter by lazy {
        AgenciesFeedbackAdapter(onClick = { view, url ->
            activity?.let {
                LinkUtils.open(view, it, url, getString(commonsR.string.web_browser), false) // force external web browser
                behavior?.state = BottomSheetBehavior.STATE_HIDDEN
                dismissAllowingStateLoss()
            }
        })
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
        return inflater.inflate(R.layout.fragment_dialog_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDialogFeedbackBinding.bind(view).apply {
            list.adapter = ConcatAdapter(headerAdapter, agenciesAdapter)
            dialog?.setOnShowListener {
                root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.color_background))
            }
        }
        viewModel.agencies.observe(this) { newAgencies ->
            agenciesAdapter.submitList(newAgencies)
            binding?.apply {
                when (newAgencies) {
                    null -> {
                        emptyLayout.isVisible = false
                        list.isVisible = false
                        loadingLayout.isVisible = true
                    }

                    else -> {
                        loadingLayout.isVisible = false
                        emptyLayout.isVisible = false
                        list.isVisible = true
                    }
                }
            }
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