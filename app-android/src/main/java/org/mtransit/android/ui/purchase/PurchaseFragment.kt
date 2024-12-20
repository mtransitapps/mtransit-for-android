package org.mtransit.android.ui.purchase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.databinding.FragmentPurchaseBinding
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.view.common.toEvent

@AndroidEntryPoint
class PurchaseFragment : MTFragmentX(R.layout.fragment_purchase) {

    companion object {
        private val LOG_TAG = PurchaseFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(): PurchaseFragment = PurchaseFragment()
    }

    override fun getLogTag(): String = LOG_TAG

    private val viewModel: PurchaseViewModel by activityViewModels()

    private var binding: FragmentPurchaseBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPurchaseBinding.bind(view).apply {
            closeButton.setOnClickListener {
                viewModel.closeEvent.postValue(true.toEvent())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


    override fun <T : View?> findViewById(id: Int) = this.view?.findViewById<T>(id)

}