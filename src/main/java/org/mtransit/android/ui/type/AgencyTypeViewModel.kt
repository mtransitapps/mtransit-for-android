package org.mtransit.android.ui.type

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareViewModel
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.util.UIFeatureFlags
import javax.inject.Inject

@HiltViewModel
class AgencyTypeViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val adManager: IAdManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val statusLoader: StatusLoader,
    private val serviceUpdateLoader: ServiceUpdateLoader,
    private val pm: PackageManager,
) : MTViewModelWithLocation(),
    ModuleDisabledAwareViewModel {

    companion object {
        private val LOG_TAG = AgencyTypeViewModel::class.java.simpleName

        internal const val EXTRA_TYPE_ID = "extra_type_id"
    }

    override fun getLogTag(): String = LOG_TAG

    private val _typeId = savedStateHandle.getLiveDataDistinct<Int>(EXTRA_TYPE_ID)

    val type: LiveData<DataSourceType?> = _typeId.map { typeId ->
        DataSourceType.parseId(typeId)
    }

    private val allAvailableAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModulesUpdated

    val typeAgencies = PairMediatorLiveData(type, allAvailableAgencies).map { (dst, allAgencies) ->
        allAgencies?.filter { it.getSupportedType() == dst }
    }

    override val moduleDisabled = typeAgencies.map {
        it?.filter { agency -> !agency.isEnabled } ?: emptyList()
    }.distinctUntilChanged()

    val title: LiveData<String?> = PairMediatorLiveData(type, typeAgencies).map { (dst, agencies) ->
        if (UIFeatureFlags.F_HIDE_ONE_AGENCY_TYPE_TABS) {
            if (agencies?.size == 1) {
                return@map agencies[0].shortName
            }
        }
        return@map dst?.shortNamesResId?.let { appContext.getString(it) }
    }

    val tabsVisible: LiveData<Boolean> = PairMediatorLiveData(type, typeAgencies).map { (dst, agencies) ->
        if (UIFeatureFlags.F_HIDE_ONE_AGENCY_TYPE_TABS) {
            if (agencies?.size == 1) {
                return@map false
            }
        }
        return@map dst != DataSourceType.TYPE_MODULE
    }

    override val hasDisabledModule = moduleDisabled.map {
        it.any { agency -> !pm.isAppEnabled(agency.pkg) }
    }

    override fun getAdBannerHeightInPx(activity: IActivity?) = this.adManager.getBannerHeightInPx(activity)

    private val selectedTypeAgencyAuthority: LiveData<String> = _typeId.switchMap { typeId ->
        liveData {
            emitSource(
                lclPrefRepository.pref.liveData(
                    LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(typeId),
                    LocalPreferenceRepository.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT
                )
            )
        }
    }.distinctUntilChanged()

    val selectedTypeAgencyPosition: LiveData<Int?> = PairMediatorLiveData(selectedTypeAgencyAuthority, typeAgencies).map { (agencyAuthority, agencies) ->
        if (agencyAuthority == null || agencies == null) {
            null
        } else {
            agencies.indexOfFirst { it.authority == agencyAuthority }.coerceAtLeast(0)
        }
    }

    fun onPageSelected(position: Int) {
        this.statusLoader.clearAllTasks()
        this.serviceUpdateLoader.clearAllTasks()
        saveSelectedTypeAgency(position)
    }

    private fun saveSelectedTypeAgency(position: Int) {
        saveSelectedTypeAgency(
            typeAgencies.value?.getOrNull(position) ?: return
        )
    }

    private fun saveSelectedTypeAgency(agency: IAgencyProperties) {
        val typeId: Int = _typeId.value ?: return
        lclPrefRepository.pref.edit {
            putString(LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(typeId), agency.authority)
        }
    }
}