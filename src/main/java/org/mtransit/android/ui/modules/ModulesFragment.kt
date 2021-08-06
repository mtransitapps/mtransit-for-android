@file:JvmName("ModulesFragment") // ANALYTICS
package org.mtransit.android.ui.modules

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.databinding.FragmentModulesBinding
import org.mtransit.android.ui.view.common.isVisible

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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentModulesBinding.bind(view).apply {
            (activity as? AppCompatActivity)?.setSupportActionBar(abToolbar)
            (activity as? AppCompatActivity)?.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
            }
            emptyLayout.emptyText.text = "NO MODULES"
            ViewCompat.setOnApplyWindowInsetsListener(list) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                WindowInsetsCompat.CONSUMED
            }
            list.adapter = listAdapter
        }
        viewModel.agencies.observe(viewLifecycleOwner, { newAgencies ->
            listAdapter.submitList(newAgencies)
            binding?.apply {
                list.isVisible = !newAgencies.isNullOrEmpty()
                emptyLayout.isVisible = newAgencies.isNullOrEmpty()
            }
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