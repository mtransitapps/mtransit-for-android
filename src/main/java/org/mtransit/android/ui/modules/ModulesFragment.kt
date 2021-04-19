package org.mtransit.android.ui.modules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.databinding.FragmentModulesBinding

class ModulesFragment : Fragment(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ModulesFragment::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val viewModel by viewModels<ModulesViewModel>()

    private val listAdapter: ModulesAdapter by lazy { ModulesAdapter() }

    private var binding: FragmentModulesBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.agencies.observe(this, { newAgencies ->
            listAdapter.submitList(newAgencies)
            if (newAgencies.isNullOrEmpty()) {
                binding?.modulesLinearLayout?.visibility = View.GONE
                binding?.noModulesLayout?.visibility = View.VISIBLE
            } else {
                binding?.noModulesLayout?.visibility = View.GONE
                binding?.modulesLinearLayout?.visibility = View.VISIBLE
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentModulesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_modules, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_sort) {
            viewModel.flipSort()
            return true
        } else if (item.itemId == R.id.menu_refresh_available_version) {
            viewModel.refreshAvailableVersions()
            return true
        } else if (item.itemId == R.id.menu_force_refresh_available_version) {
            viewModel.forceRefreshAvailableVersions()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListAdapter()
    }

    private fun setupListAdapter() {
        binding?.modulesList?.let {
            it.adapter = listAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}