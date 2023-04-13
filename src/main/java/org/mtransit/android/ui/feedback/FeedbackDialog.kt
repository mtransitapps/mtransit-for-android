package org.mtransit.android.ui.feedback

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.databinding.FragmentDialogFeedbackBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.fragment.MTDialogFragmentX
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.LinkUtils
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class FeedbackDialog : MTDialogFragmentX() {

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

    private val headerAdapter by lazy {
        HeaderFeedbackAdapter {
            activity?.let {
                LinkUtils.sendEmail(it, dataSourcesRepository)
                dismiss()
            }
        }
    }

    private val agenciesAdapter by lazy {
        AgenciesFeedbackAdapter { view, url ->
            activity?.let {
                LinkUtils.open(view, it, url, getString(commonsR.string.web_browser), false) // force external web browser
                dismiss()
            }
        }
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
        return FragmentDialogFeedbackBinding.inflate(inflater).apply {
            list.adapter = ConcatAdapter(headerAdapter, agenciesAdapter)
            binding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
}