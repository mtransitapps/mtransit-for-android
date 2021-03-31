package org.mtransit.android.ui.modules

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog

class ModulesFragment : Fragment(R.layout.fragment_modules), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ModulesFragment::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val viewModel by viewModels<ModulesViewModel>()

    private val listAdapter: ModulesAdapter by lazy { ModulesAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.allAgencies.observe(this, { newAgencies ->
            listAdapter.submitList(newAgencies)
            if (newAgencies.isEmpty()) {
                view?.findViewById<View>(R.id.modules_linear_layout)?.visibility = View.GONE
                view?.findViewById<View>(R.id.no_modules_layout)?.visibility = View.VISIBLE
            } else {
                view?.findViewById<View>(R.id.no_modules_layout)?.visibility = View.GONE
                view?.findViewById<View>(R.id.modules_linear_layout)?.visibility = View.VISIBLE
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListAdapter(view)
    }

    private fun setupListAdapter(view: View) {
        view.findViewById<RecyclerView>(R.id.modules_list)?.let {
            it.adapter = listAdapter
        }
    }
}