package org.mtransit.android.ui.favorites

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
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType.POIManagerTypeShortNameComparator
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareViewModel
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.sortWithAnd
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val adManager: IAdManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val favoriteRepository: FavoriteRepository,
    private val poiTypeShortNameComparator: POIManagerTypeShortNameComparator,
    private val pm: PackageManager,
) : MTViewModelWithLocation(),
    ModuleDisabledAwareViewModel {

    companion object {
        private val LOG_TAG = FavoritesViewModel::class.java.simpleName

        private val POI_ALPHA_COMPARATOR = POIAlphaComparator()
    }

    override fun getLogTag(): String = LOG_TAG

    fun onFavoriteUpdated() {
        _favoriteUpdatedTrigger.value = (_favoriteUpdatedTrigger.value ?: 0) + 1
    }

    override val moduleDisabled = this.dataSourcesRepository.readingAllAgenciesBase().map {
        it.filter { agency -> !agency.isEnabled }
    }.distinctUntilChanged()

    override val hasDisabledModule = moduleDisabled.map {
        it.any { agency -> !pm.isAppEnabled(agency.pkg) }
    }

    private val _favoriteUpdatedTrigger = MutableLiveData(0)

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModuleChanged

    val oneAgency: LiveData<AgencyBaseProperties?> = _allAgencies.map { // many users have only 1 agency installed
        if (it.size == 1) it[0] else null
    }.distinctUntilChanged()

    private val _hasFavoritesAgencyDisabled = MutableLiveData(false)
    val hasFavoritesAgencyDisabled: LiveData<Boolean> = _hasFavoritesAgencyDisabled.distinctUntilChanged()

    val favoritePOIs: LiveData<List<POIManager>?> =
        PairMediatorLiveData(_favoriteUpdatedTrigger, _allAgencies).switchMap { (_, allAgencies) ->
            _hasFavoritesAgencyDisabled.value = false
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                allAgencies ?: return@liveData
                emit(getFavorites(allAgencies))
            }
        }

    @WorkerThread
    private suspend fun getFavorites(allAgencies: List<IAgencyProperties>): List<POIManager>? {
        val favorites = this.favoriteRepository.findFavorites()
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
                if (agency.isEnabled(pm)) {
                    _hasFavoritesAgencyDisabled.postValue(true)
                }
                this.poiRepository.findPOIMs(agency, POIProviderContract.Filter.getNewUUIDsFilter(authorityUUIDs))
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
        val uuidToFavoriteFolderId = favorites.associateBy({ it.fkId }, { it.folderId })
        val favFolderIds = mutableSetOf<Int>()
        pois.forEach { favPOIM ->
            val favFolderId = uuidToFavoriteFolderId[favPOIM.poi.uuid]
            if (favFolderId != null && favFolderId > FavoriteRepository.DEFAULT_FOLDER_ID) {
                favPOIM.poi.dataSourceTypeId = this.favoriteRepository.generateFavoriteFolderId(favFolderId)
                favFolderIds.add(favFolderId)
            }
        }
        var textMessageId = UITimeUtils.currentTimeMillis()
        val favFolders = this.favoriteRepository.findFoldersList()
        favFolders
            .filter { favFolder -> favFolder.id > FavoriteRepository.DEFAULT_FOLDER_ID && !favFolderIds.contains(favFolder.id) }
            .forEach { favoriteFolder ->
                pois.add(getNewEmptyFolder(textMessageId++, favoriteFolder.id))
            }
        if (pois.isNotEmpty()) {
            pois.sortWith(FavoriteFolderNameComparator(this.favoriteRepository, favFolders))
        }
        return pois
    }

    private fun getNewEmptyFolder(textMessageId: Long, favoriteFolderId: Int): POIManager {
        return POIManager(
            this.favoriteRepository.generateFavEmptyFavPOI(
                textMessageId,
                this.favoriteRepository.generateFavoriteFolderId(favoriteFolderId)
            )
        )
    }

    class FavoriteFolderNameComparator(
        private val favoriteRepository: FavoriteRepository,
        private val favFolders: Collection<Favorite.Folder>
    ) : Comparator<POIManager?> {

        override fun compare(lhs: POIManager?, rhs: POIManager?): Int {
            val lhsPoi = lhs?.poi
            val rhsPoi = rhs?.poi
            if (lhsPoi == null && rhsPoi == null) {
                return ComparatorUtils.SAME
            }
            if (lhsPoi == null) {
                return ComparatorUtils.BEFORE
            } else if (rhsPoi == null) {
                return ComparatorUtils.AFTER
            }
            val lFavFolderId = favoriteRepository.getFavoriteDataSourceIdOrNull(lhsPoi.dataSourceTypeId)
            val lFavFolderName = favFolders.singleOrNull { it.id == lFavFolderId }?.name ?: StringUtils.EMPTY
            val rFavFolderId = favoriteRepository.getFavoriteDataSourceIdOrNull(rhsPoi.dataSourceTypeId)
            val rFavFolderName = favFolders.singleOrNull { it.id == rFavFolderId }?.name ?: StringUtils.EMPTY
            return lFavFolderName.compareTo(rFavFolderName)
        }
    }

    override fun getAdBannerHeightInPx(activity: IAdScreenActivity?) = this.adManager.getBannerHeightInPx(activity)

    private val Favorite.authority: String?
        get() = POI.POIUtils.extractAuthorityFromUUID(fkId)
}