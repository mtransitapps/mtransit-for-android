package org.mtransit.android.data

import androidx.lifecycle.LifecycleOwner

fun POIArrayAdapter.onCreateViewKt(viewLifecycleOwner: LifecycleOwner) {
    this.viewLifecycleOwner = viewLifecycleOwner
    this.dataSourcesRepository.readingAllAgencies().observe(viewLifecycleOwner) {
        resetModulesStatus()
    }
    this.favoriteRepository.readingAllFavorites.observe(viewLifecycleOwner) { it ->
        setFavorites(it)
    }
    this.favoriteRepository.readingAllFavoriteFkIds.observe(viewLifecycleOwner) { favoritesFkIds ->
        this.allFavoritesFkIds = favoritesFkIds
    }
    this.favoriteRepository.readingAllFavoriteFolders.observe(viewLifecycleOwner) { favoriteFolders ->
        this.favoriteFoldersByIds = favoriteFolders?.let {
            buildMap {
                it.forEach { folder ->
                    put(folder.id, folder)
                }
            }

        }
    }
    this.favoriteRepository.isUsingFavoriteFolders.observe(viewLifecycleOwner) { usingFavoriteFolders ->
        this.isUsingFavoriteFolders = usingFavoriteFolders
    }
}