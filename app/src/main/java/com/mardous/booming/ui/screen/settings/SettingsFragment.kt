/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.screen.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentSettingsBinding
import com.mardous.booming.extensions.applyHorizontalWindowInsets
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.materialSharedAxis
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.screen.settings.search.SettingsSearchAdapter
import com.mardous.booming.ui.screen.settings.search.SettingsSearchHelper
import androidx.appcompat.widget.SearchView

/**
 * @author Christians M. A. (mardous)
 */
class SettingsFragment : AbsMainActivityFragment(R.layout.fragment_settings), NavController.OnDestinationChangedListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var childNavController: NavController? = null
    private lateinit var searchAdapter: SettingsSearchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        with(binding.appBarLayout.toolbar) {
            setNavigationIcon(R.drawable.ic_back_24dp)
            isTitleCentered = false
            setNavigationOnClickListener {
                getOnBackPressedDispatcher().onBackPressed()
            }
            
            menu.clear()
            inflateMenu(R.menu.menu_settings_search)
            
            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem?.actionView as? SearchView
            searchView?.queryHint = getString(R.string.search_label)
            
            searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrBlank()) {
                        binding.searchResults.visibility = View.GONE
                    } else {
                        val results = SettingsSearchHelper.search(newText)
                        searchAdapter.submitList(results)
                        binding.searchResults.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                    return true
                }
            })
            
            searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    binding.searchResults.visibility = View.GONE
                    return true
                }
            })
        }

        materialSharedAxis(view)
        view.applyHorizontalWindowInsets()

        val navHostFragment = childFragmentManager.findFragmentById(R.id.contentFrame) as NavHostFragment
        childNavController = navHostFragment.navController.apply {
            addOnDestinationChangedListener(this@SettingsFragment)
        }

        // Setup Search
        SettingsSearchHelper.indexSettings(requireContext())
        searchAdapter = SettingsSearchAdapter { result ->
            val searchView = binding.appBarLayout.toolbar.menu.findItem(R.id.action_search)?.actionView as? SearchView
            searchView?.setQuery("", false)
            searchView?.isIconified = true
            binding.searchResults.visibility = View.GONE
            
            // Navigate to target and highlight
            val args = Bundle().apply { putString("highlightKey", result.key) }
            childNavController?.navigate(result.destinationId, args)
        }
        
        binding.searchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val isComposeScreenWithInternalToolbar = destination.id == R.id.nav_pending_scrobbles || 
                destination.id == R.id.nav_network_preferences || 
                destination.id == R.id.nav_lastfm_profile ||
                destination.id == R.id.nav_advanced_preferences ||
                destination.id == R.id.nav_library_preferences
        
        binding.appBarLayout.visibility = if (isComposeScreenWithInternalToolbar) View.GONE else View.VISIBLE
        binding.appBarLayout.title = destination.label ?: getString(R.string.settings_title)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    override fun onResume() {
        super.onResume()
        getOnBackPressedDispatcher().addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
        childNavController?.removeOnDestinationChangedListener(this)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (mainActivity.panelState != BottomSheetBehavior.STATE_COLLAPSED) {
                mainActivity.collapsePanel()
                return
            }
            if (childNavController?.popBackStack() == false) {
                remove()
                getOnBackPressedDispatcher().onBackPressed()
                return
            }
        }
    }
}