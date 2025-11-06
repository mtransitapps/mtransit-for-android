package org.mtransit.android.ui.fares

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.getDimensionInt
import org.mtransit.android.databinding.FragmentDialogFaresBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.common.adater.AgenciesLinkAdapter
import org.mtransit.android.ui.common.adater.AgenciesLinkType
import org.mtransit.android.ui.fragment.MTBottomSheetDialogFragmentX
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.LinkUtils
import javax.inject.Inject

@AndroidEntryPoint
class FaresDialog : MTBottomSheetDialogFragmentX() {

    companion object {
        private val LOG_TAG = FaresDialog::class.java.simpleName

        @JvmStatic
        fun newInstance(
        ): FaresDialog {
            return FaresDialog()
        }
    }

    override fun getLogTag(): String = LOG_TAG

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    private val viewModel by viewModels<FaresViewModel>()

    private var binding: FragmentDialogFaresBinding? = null

    private var behavior: BottomSheetBehavior<*>? = null

    private val agenciesAdapter by lazy {
        AgenciesLinkAdapter(AgenciesLinkType.FARES, onClick = { view, url ->
            activity?.let {
                behavior?.state = BottomSheetBehavior.STATE_HIDDEN
                dismissAllowingStateLoss()
                LinkUtils.open(view, it, url, getString(R.string.fares), true)
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
        return inflater.inflate(R.layout.fragment_dialog_fares, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDialogFaresBinding.bind(view).apply {
            list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            list.adapter = agenciesAdapter
            dialog?.setOnShowListener {
                root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.color_background))
            }
        }
        viewModel.agencies.observe(viewLifecycleOwner) { newAgencies ->
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