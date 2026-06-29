package org.mtransit.android.ui.favorites

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.DataSourceType.POIManagerTypeShortNameComparator
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.FavoriteFolder
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.toPOIM
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.provider.favorite.FavoritesUI
import org.mtransit.android.provider.favorite.FavoritesFolderDSTUtils
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareViewModel
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.sortWithAnd
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val adManager: IAdManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val favoriteRepository: FavoriteRepository,
    private val poiTypeShortNameComparator: POIManagerTypeShortNameComparator,
    private val pm: PackageManager,
) : MTViewModelWithLocation(),
    ModuleDisabledAwareViewModel {

    companion object {
        private val LOG_TAG: String = FavoritesViewModel::class.java.simpleName

        private val POI_ALPHA_COMPARATOR = POIAlphaComparator()
    }

    override fun getLogTag() = LOG_TAG

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModuleChanged

    val oneAgency: LiveData<AgencyBaseProperties?> = _allAgencies.map { // many users have only 1 agency installed
        if (it.size == 1) it[0] else null
    }.distinctUntilChanged()

    override val moduleDisabled = _allAgencies.map {
        it.filter { agency -> !agency.isEnabled }
    }.distinctUntilChanged()

    override val hasDisabledModule = moduleDisabled.map {
        it.any { agency -> !pm.isAppEnabled(agency.pkg) }
    }

    private val _hasFavoritesAgencyDisabled = MutableLiveData(false)
    val hasFavoritesAgencyDisabled: LiveData<Boolean> = _hasFavoritesAgencyDisabled.distinctUntilChanged()

    private val _homeScreenTypes = this.dataSourcesRepository.readingAllSupportedDataSourceTypes().map { // #onModulesUpdated
        it.filter { dst -> dst.isHomeScreen && dst != DataSourceType.TYPE_MODULE }
    }

    val favorites = this.favoriteRepository.readingAllFavorites

    val favoritePOIs: LiveData<List<POIManager>?> = MediatorLiveData3(favorites, _allAgencies, _homeScreenTypes)
        .switchMap { (favorites, allAgencies, homeScreenTypes) ->
            _hasFavoritesAgencyDisabled.value = false
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                val favorites = favorites ?: run { emit(null); return@liveData } // loading
                val allAgencies = allAgencies ?: run { emit(null); return@liveData } // loading
                val homeScreenTypes = homeScreenTypes ?: run { emit(null); return@liveData } // loading
                // 1 - cache only
                emit(getFavorites(favorites, allAgencies, homeScreenTypes, cacheOnly = true))
                // 3 - not cache only
                emit(getFavorites(favorites, allAgencies, homeScreenTypes, cacheOnly = false))
            }
        }.distinctUntilChanged()

    @WorkerThread
    private suspend fun getFavorites(
        favorites: Collection<Favorite>,
        allAgencies: List<AgencyBaseProperties>,
        homeScreenTypes: List<DataSourceType>,
        cacheOnly: Boolean,
    ): List<POIManager> {
        if (favorites.isEmpty()) {
            MTLog.d(this, "getFavorites() > SKIP (no favorites)")
            return emptyList() // empty (no favorites)
        }
        val pois = mutableListOf<POIManager>()
        val authorityToUUIDs = favorites.groupBy({ it.authority.orEmpty() }, { it.fkId })
        authorityToUUIDs
            .filterKeys { authority -> authority.isNotEmpty() && allAgencies.any { it.authority == authority } }
            .filterValues { it.isNotEmpty() }
            .forEach { (authority, authorityUUIDs) ->
                val agency = allAgencies.singleOrNull { it.authority == authority } ?: return@forEach
                if (!agency.isEnabled(pm)) {
                    _hasFavoritesAgencyDisabled.postValue(true)
                }
                val poiFilter = POIProviderContract.Filter.getNewUUIDsFilter(authorityUUIDs)
                    .copy(cacheOnly = cacheOnly) // POI_FILTER_EXTRA_AVOID_LOADING is similar
                this.poiRepository.findPOIMs(agency, poiFilter)
                    .let { agencyPOIs ->
                        if (agencyPOIs.isNotEmpty()) {
                            pois.addAll(
                                agencyPOIs.sortWithAnd(POI_ALPHA_COMPARATOR)
                            )
                        }
                    }
            }
        if (pois.isNotEmpty()) {
            pois.sortWith(poiTypeShortNameComparator)
        }
        // UPDATE favorite POI data source type ID with favorite folder data source type ID
        val uuidToFavoriteFolderId = favorites.associateBy({ it.fkId }, { it.folderId })
        val favFolderIds = mutableSetOf<Int>()
        pois.forEach { favPOIM ->
            val favFolderId = uuidToFavoriteFolderId[favPOIM.poi.uuid]
            if (favFolderId != null && favFolderId > FavoriteFolder.DEFAULT_FOLDER_ID) {
                favPOIM.poi.dataSourceTypeId = FavoritesFolderDSTUtils.generateFavoriteFolderDataSourceId(favFolderId)
                favFolderIds.add(favFolderId)
            }
        }
        // ADD empty favorite folders
        var textMessageId = UITimeUtils.currentTimeMillis()
        val favFolders = this.favoriteRepository.findFolders()
        favFolders
            .filter { favFolder -> favFolder.id > FavoriteFolder.DEFAULT_FOLDER_ID && !favFolderIds.contains(favFolder.id) }
            .forEach { favoriteFolder ->
                val dataSourceTypeId = FavoritesFolderDSTUtils.generateFavoriteFolderDataSourceId(favoriteFolder.id)
                pois.add(FavoritesUI.generateFavEmptyFavPOI(appContext, textMessageId++, dataSourceTypeId).toPOIM())
            }
        if (pois.isNotEmpty()) {
            pois.sortWith(FavoriteFolderNameComparator(favFolders))
        }
        // ADD missing data source type with empty at the end of list
        val favFolderTypeIds = pois.map { it.poi.dataSourceTypeId }.toSet()
        homeScreenTypes
            .filter { it.id !in favFolderTypeIds }
            .forEach {
                pois.add(FavoritesUI.generateFavEmptyFavPOI(appContext, textMessageId++, it.id).toPOIM())
            }
        return pois
    }

    class FavoriteFolderNameComparator(
        favFolders: Collection<FavoriteFolder>
    ) : Comparator<POIManager?> {

        private val folderIdToName = favFolders.associate { it.id to it.name }

        override fun compare(lhs: POIManager?, rhs: POIManager?): Int {
            val lhsPoi = lhs?.poi
            val rhsPoi = rhs?.poi
            if (lhsPoi == null && rhsPoi == null) {
                return ComparatorUtils.SAME
            } else if (lhsPoi == null) {
                return ComparatorUtils.BEFORE
            } else if (rhsPoi == null) {
                return ComparatorUtils.AFTER
            }
            val lFavFolderId = FavoritesFolderDSTUtils.getFavoriteFolderIdOrNull(lhsPoi.dataSourceTypeId)
            val lFavFolderName = folderIdToName[lFavFolderId]
            val rFavFolderId = FavoritesFolderDSTUtils.getFavoriteFolderIdOrNull(rhsPoi.dataSourceTypeId)
            val rFavFolderName = folderIdToName[rFavFolderId]
            if (lFavFolderName == null && rFavFolderName == null) {
                return ComparatorUtils.SAME
            } else if (lFavFolderName == null) {
                return ComparatorUtils.AFTER
            } else if (rFavFolderName == null) {
                return ComparatorUtils.BEFORE
            }
            return lFavFolderName.compareTo(rFavFolderName)
        }
    }

    override fun getAdBannerHeightInPx(activity: IAdScreenActivity?) = this.adManager.getBannerHeightInPx(activity)

    private val Favorite.authority: String?
        get() = POI.POIUtils.extractAuthorityFromUUID(fkId)
}
