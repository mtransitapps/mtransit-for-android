package org.mtransit.android.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType.POIManagerTypeShortNameComparator
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.di.Injection
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.util.UITimeUtils

class FavoritesViewModel : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = FavoritesViewModel::class.java.simpleName

        private val POI_ALPHA_COMPARATOR = POIAlphaComparator()
    }

    override fun getLogTag(): String = LOG_TAG

    private val app: IApplication by lazy { Injection.providesApplication() }

    private val dataSourcesRepository: DataSourcesRepository by lazy { Injection.providesDataSourcesRepository() }

    private val dataSourceRequestManager: DataSourceRequestManager by lazy { Injection.providesDataSourceRequestManager() }

    private val favoriteRepository: FavoriteRepository by lazy { Injection.providesFavoriteRepository() }

    private val poiTypeShortNameComparator: POIManagerTypeShortNameComparator by lazy {
        POIManagerTypeShortNameComparator(
            this.app.requireContext(),
            this.dataSourcesRepository
        )
    }

    fun onFavoriteUpdated() {
        _favoriteUpdatedTrigger.value = (_favoriteUpdatedTrigger.value ?: 0) + 1
    }

    private val _favoriteUpdatedTrigger = MutableLiveData(0)

    private val _agencies = this.dataSourcesRepository.readingAllAgenciesDistinct()

    val favoritePOIs: LiveData<List<POIManager>> =
        PairMediatorLiveData(_favoriteUpdatedTrigger, _agencies).switchMap { (_, agencies) ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getFavorites(agencies))
            }
        }

    private fun getFavorites(agencies: List<AgencyProperties>?): List<POIManager> {
        if (agencies.isNullOrEmpty()) {
            MTLog.d(this, "getFavorites() > SKIP (no agencies)")
            return emptyList()
        }
        val favorites = this.favoriteRepository.findFavorites()
        if (favorites.isNullOrEmpty()) {
            MTLog.d(this, "getFavorites() > SKIP (no favorites)")
            return emptyList()
        }
        val pois = mutableListOf<POIManager>()
        val authorityToUUIDs = favorites.groupBy({ POI.POIUtils.extractAuthorityFromUUID(it.fkId).orEmpty() }, { it.fkId })
        authorityToUUIDs
            .filterKeys { it.isNotEmpty() }
            .filterValues { it.isNotEmpty() }
            .forEach { (authority, authorityUUIDs) ->
                this.dataSourceRequestManager.findPOIs(authority, POIProviderContract.Filter.getNewUUIDsFilter(authorityUUIDs))
                    .takeIf { !it.isNullOrEmpty() }
                    ?.let { agencyPOIs ->
                        agencyPOIs.sortedWith(POI_ALPHA_COMPARATOR)
                        pois.addAll(agencyPOIs)
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
        return POIManager(this.favoriteRepository.generateFavEmptyFavPOI(textMessageId).also {
            it.dataSourceTypeId = this.favoriteRepository.generateFavoriteFolderId(favoriteFolderId)
        })
    }

    class POIAlphaComparator : Comparator<POIManager?> {
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
            return lhsPoi.compareToAlpha(null, rhsPoi)
        }
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
}