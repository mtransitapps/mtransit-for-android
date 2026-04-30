package org.mtransit.android.data

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.pref.preferenceChangeLiveData
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData4

fun POIArrayAdapter.onCreateViewKt(viewLifecycleOwner: LifecycleOwner) {
    this.viewLifecycleOwner = viewLifecycleOwner
    this.dataSourcesRepository.readingAllAgenciesByType().observe(viewLifecycleOwner) { allAgenciesByType ->
        this.allAgenciesByType = allAgenciesByType
        resetModulesStatus()
    }
    val readingAllHomeDST = MediatorLiveData4(
        this.favoriteRepository.readingHasFavorites,
        this.dataSourcesRepository.readingAllSupportedDataSourceTypes(),
        this.dataSourcesRepository.readingHasAgenciesEnabled(),
        this.dataSourcesRepository.readingAllNewsProviders()
    ).map { (hasFavorites, allSupportedDST, hasAgenciesEnabled, allNewsProviders) ->
        hasFavorites ?: return@map null
        allSupportedDST ?: return@map null
        hasAgenciesEnabled ?: return@map null
        allNewsProviders ?: return@map null
        buildList<DataSourceType> {
            addAll(allSupportedDST.filter { it.isHomeScreen })
            if (hasFavorites && hasAgenciesEnabled) {
                add(0, DataSourceType.TYPE_FAVORITE) // 1st
            }
            if (allNewsProviders.isNotEmpty()) {
                add(this.size - 1, DataSourceType.TYPE_NEWS) // before LAST (MODULE)
            }
        }
    }
    readingAllHomeDST.observe(viewLifecycleOwner) { allHomeDST ->
        this.allHomeDST = allHomeDST
    }
    MediatorLiveData2(
        readingAllHomeDST,
        localPreferenceRepository.pref.preferenceChangeLiveData(),
    ).switchMap { (allHomeDST, _) ->
        liveData(Dispatchers.IO) {
            allHomeDST ?: return@liveData
            allHomeDST
                .map { dst -> dst.id }
                .associateWith { dstId ->
                    localPreferenceRepository.getValueNN(
                        LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(dstId),
                        LocalPreferenceRepository.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT
                    )
                }.let {
                    emit(it)
                }
        }
    }.observe(viewLifecycleOwner) { dstIdToSelectedAuthority ->
        this.dstIdToSelectedAuthority = dstIdToSelectedAuthority
        if (this.showBrowseHeaderSection || this.showTypeHeader == POIArrayAdapter.TYPE_HEADER_ALL_NEARBY) {
            this.nbDisplayedAgencyTypes = -1 // reset
            notifyDataSetChanged()
        }
    }
    this.favoriteRepository.readingAllFavorites.observe(viewLifecycleOwner) { allFavorites ->
        setFavorites(allFavorites)
    }
    this.favoriteRepository.readingAllFavoriteFkIds.observe(viewLifecycleOwner) { allFavoritesFkIds ->
        this.allFavoritesFkIds = allFavoritesFkIds
    }
    this.favoriteRepository.readingAllFolders.observe(viewLifecycleOwner) { allFavoriteFolders ->
        this.favoriteFoldersByIds = allFavoriteFolders?.associateBy { it.id }
    }
    this.favoriteRepository.isUsingFolders.observe(viewLifecycleOwner) { usingFavoriteFolders ->
        this.isUsingFavoriteFolders = usingFavoriteFolders
    }
}
