package org.mtransit.android.data

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.analytics.AnalyticsScreen
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.pref.liveData
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
            if (isNotEmpty() && allNewsProviders.isNotEmpty()) {
                add(this.size - 1, DataSourceType.TYPE_NEWS) // before LAST (MODULE)
            }
        }
    }
    readingAllHomeDST.observe(viewLifecycleOwner) { allHomeDST ->
        this.allHomeDST = allHomeDST
    }
    MediatorLiveData2(
        readingAllHomeDST,
        lclPrefRepository.pref.preferenceChangeLiveData(),
    ).switchMap { (allHomeDST, _) ->
        liveData(Dispatchers.IO) {
            allHomeDST ?: return@liveData
            allHomeDST
                .map { dst -> dst.id }
                .associateWith { dstId ->
                    lclPrefRepository.pref.getString(
                        LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(dstId),
                        LocalPreferenceRepository.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT
                    ).orEmpty()
                }.let {
                    emit(it)
                }
        }
    }.distinctUntilChanged() // listening all shared pref changes
        .observe(viewLifecycleOwner) { dstIdToSelectedAuthority ->
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
    this.defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_DISTANCE_UNITS, DefaultPreferenceRepository.PREFS_DISTANCE_UNITS_DEFAULT
    ).distinctUntilChanged().observe(viewLifecycleOwner) {
        this.distanceUnitsPref = it
        updateDistanceNowAsync(this.location)
    }
}

internal val POIArrayAdapter.analyticsScreen: AnalyticsScreen? get() = this.fragmentWR?.get() as? AnalyticsScreen

internal fun POIArrayAdapter.trackTypeHeaderButtonClick(buttonId: Int) {
    when (buttonId) {
        POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_MORE ->
            analyticsManager.trackButtonClick("header_more", analyticsScreen)
        POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_NEARBY ->
            analyticsManager.trackButtonClick("header_nearby", analyticsScreen)
        POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_ALL ->
            analyticsManager.trackButtonClick("header_all", analyticsScreen)
        else -> {
            MTLog.w(this, "trackTypeHeaderButtonClick() > Unexpected button ID '$buttonId'!")
            this.analyticsManager.trackButtonClick("header_unknown", analyticsScreen)
        }
    }
}
