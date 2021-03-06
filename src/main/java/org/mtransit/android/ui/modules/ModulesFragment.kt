@file:JvmName("ModulesFragment") // ANALYTICS
package org.mtransit.android.ui.modules

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.databinding.FragmentModulesBinding

@AndroidEntryPoint
class ModulesFragment : Fragment(R.layout.fragment_modules), MTLog.Loggable {

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentModulesBinding.bind(view).apply {
            modulesList.adapter = listAdapter
        }
        viewModel.agencies.observe(viewLifecycleOwner, { newAgencies ->
            listAdapter.submitList(newAgencies)
            binding?.modulesLinearLayout?.isVisible = !newAgencies.isNullOrEmpty()
            binding?.noModulesLayout?.isVisible = newAgencies.isNullOrEmpty()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_modules, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_sort -> {
                viewModel.flipSort()
                true
            }
            R.id.menu_refresh_available_version -> {
                viewModel.refreshAvailableVersions()
                true
            }
            R.id.menu_force_refresh_available_version -> {
                viewModel.forceRefreshAvailableVersions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}