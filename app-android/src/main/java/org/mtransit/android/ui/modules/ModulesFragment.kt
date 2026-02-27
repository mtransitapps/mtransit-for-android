@file:JvmName("ModulesFragment") // ANALYTICS
package org.mtransit.android.ui.modules

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.databinding.FragmentModulesBinding
import org.mtransit.android.ui.view.common.isVisible

@AndroidEntryPoint
class ModulesFragment : Fragment(R.layout.fragment_modules), MTLog.Loggable, MenuProvider {

    companion object {
        private val LOG_TAG = ModulesFragment::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val viewModel by viewModels<ModulesViewModel>()

    private val listAdapter: ModulesAdapter by lazy { ModulesAdapter() }

    private var binding: FragmentModulesBinding? = null

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MenuHost).addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        binding = FragmentModulesBinding.bind(view).apply {
            (activity as? AppCompatActivity)?.setSupportActionBar(abToolbar)
            (activity as? AppCompatActivity)?.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
            }
            emptyLayout.emptyText.text = "NO MODULES"
            list.adapter = listAdapter
        }
        viewModel.agencies.observe(viewLifecycleOwner) { newAgencies ->
            listAdapter.submitList(newAgencies)
            binding?.apply {
                list.isVisible = !newAgencies.isNullOrEmpty()
                emptyLayout.isVisible = newAgencies.isNullOrEmpty()
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_modules, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_sort -> {
                viewModel.flipSort()
                true // handled
            }

            R.id.menu_refresh_available_version -> {
                viewModel.refreshAvailableVersions()
                true // handled
            }

            R.id.menu_force_refresh_available_version -> {
                viewModel.forceRefreshAvailableVersions()
                true // handled
            }

            else -> false // not handled
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}