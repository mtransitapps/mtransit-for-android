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
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.FavoriteFolder
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.dstOrFavFolderId
import org.mtransit.android.data.toPOIM
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.provider.favorite.FavoritesFolderDSTUtils
import org.mtransit.android.provider.favorite.FavoritesUI
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
    }.distinctUntilChanged()

    val favorites = this.favoriteRepository.readingAllFavorites

    val favoritePOIs: LiveData<List<POIManager>?> = MediatorLiveData3(favorites, _allAgencies, _homeScreenTypes)
        .switchMap { (favorites, allAgencies, homeScreenTypes) ->
            _hasFavoritesAgencyDisabled.value = false
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                favorites ?: run { emit(null); return@liveData } // show loading
                allAgencies ?: run { emit(null); return@liveData } // show loading
                homeScreenTypes ?: run { emit(null); return@liveData } // show loading
                emit(getFavorites(favorites, allAgencies, homeScreenTypes))
            }
        }

    @WorkerThread
    private suspend fun getFavorites(
        favorites: Collection<Favorite>,
        allAgencies: List<AgencyBaseProperties>,
        homeScreenTypes: List<DataSourceType>,
    ) = buildList {
        if (favorites.isEmpty()) {
            MTLog.d(this, "getFavorites() > SKIP (no favorites)")
            return@buildList // empty (no favorites)
        }
        appendFavoritePOIs(allAgencies, favorites)
        // SET favorite POI DST favorite folder ID
        val uuidToFavoriteFolderId = favorites.associateBy({ it.fkId }, { it.folderId })
        val favFolderIds = mutableSetOf<Int>()
        forEach { favPOIM ->
            val favFolderId = uuidToFavoriteFolderId[favPOIM.poi.uuid]
            if (favFolderId != null && favFolderId > FavoriteFolder.DEFAULT_FOLDER_ID) {
                favPOIM.dstFavoriteFolderId = FavoritesFolderDSTUtils.generateDstFavoriteFolderId(favFolderId)
                favFolderIds.add(favFolderId)
            }
        }
        // ADD empty favorite folders
        var textMessageId = UITimeUtils.currentTimeMillis()
        val favFolders = favoriteRepository.findFolders()
        appendEmptyFavoriteFoldersPOIs(favFolderIds, favFolders, textMessageId).let { newTextMessageId ->
            textMessageId = newTextMessageId
        }
        val folderIdToName = favFolders.associate { it.id to it.name }
        val favoriteFolderNameComparator = compareBy<POIManager, String?>(nullsLast()) { poim ->
            val favFolderId = FavoritesFolderDSTUtils.getFavoriteFolderIdOrNull(poim.dstOrFavFolderId)
            folderIdToName[favFolderId]
        }
        sortWith(favoriteFolderNameComparator)
        // ADD missing data source type with empty at the end of list
        appendEmptyDataSourceTypesPOIs(homeScreenTypes, textMessageId)
    }

    private suspend fun MutableList<POIManager>.appendFavoritePOIs(
        allAgencies: Iterable<IAgencyProperties>,
        favorites: Collection<Favorite>
    ) {
        val authorityToTypeShortName = allAgencies.associate { agency ->
            agency.authority to appContext.getString(agency.getSupportedType().shortNameResId) // app context NOT compat w/ demo mode lang override
        }
        val poiTypeShortNameComparator = compareBy<POIManager> { poim ->
            authorityToTypeShortName[poim.poi.authority]
        }
        val favAuthorityToUUIDs = favorites.groupBy({ it.authority.orEmpty() }, { it.fkId })
        favAuthorityToUUIDs
            .filterKeys { authority -> authority.isNotEmpty() && allAgencies.any { it.authority == authority } }
            .filterValues { it.isNotEmpty() }
            .forEach { (authority, authorityUUIDs) ->
                val agency = allAgencies.singleOrNull { it.authority == authority } ?: return@forEach
                if (!agency.isEnabled(pm)) {
                    _hasFavoritesAgencyDisabled.postValue(true)
                }
                val poiFilter = POIProviderContract.Filter.getNewUUIDsFilter(authorityUUIDs)
                poiRepository.findPOIMs(agency, poiFilter)
                    .let { agencyPOIs ->
                        if (agencyPOIs.isNotEmpty()) {
                            addAll(
                                agencyPOIs.sortWithAnd(POI_ALPHA_COMPARATOR)
                            )
                        }
                    }
            }
        sortWith(poiTypeShortNameComparator)
    }

    private fun MutableList<POIManager>.appendEmptyFavoriteFoldersPOIs(
        favFolderIds: Iterable<Int>,
        favFolders: Set<FavoriteFolder>,
        textMessageId: Long,
    ): Long {
        var newTextMessageId = textMessageId
        favFolders
            .filter { favFolder -> favFolder.id > FavoriteFolder.DEFAULT_FOLDER_ID && !favFolderIds.contains(favFolder.id) }
            .forEach { favoriteFolder ->
                val dstFavoriteFolderId = FavoritesFolderDSTUtils.generateDstFavoriteFolderId(favoriteFolder.id)
                add(
                    FavoritesUI.generateFavEmptyFavPOI(appContext, newTextMessageId++, dstFavoriteFolderId)
                        .toPOIM()
                        .apply {
                            this.dstFavoriteFolderId = dstFavoriteFolderId
                        }
                )
            }
        return newTextMessageId
    }

    private fun MutableList<POIManager>.appendEmptyDataSourceTypesPOIs(
        homeScreenTypes: List<DataSourceType>,
        textMessageId: Long
    ) {
        var textMessageId1 = textMessageId
        val favoritePOIsDstIds = map { it.poi.dataSourceTypeId }.toSet()
        homeScreenTypes
            .filter { dst -> dst.id !in favoritePOIsDstIds }
            .forEach {
                add(FavoritesUI.generateFavEmptyFavPOI(appContext, textMessageId1++, it.id).toPOIM())
            }
    }

    override fun getAdBannerHeightInPx(activity: IAdScreenActivity?) = this.adManager.getBannerHeightInPx(activity)

    private val Favorite.authority: String?
        get() = POI.POIUtils.extractAuthorityFromUUID(fkId)
}
