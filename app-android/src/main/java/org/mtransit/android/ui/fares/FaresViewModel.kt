package org.mtransit.android.ui.fares

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.datasource.DataSourcesRepository
import javax.inject.Inject

@HiltViewModel
class FaresViewModel @Inject constructor(
    dataSourcesRepository: DataSourcesRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = FaresViewModel::class.java.simpleName

        private val UNSUPPORTED_TYPE = listOf(DataSourceType.TYPE_PLACE, DataSourceType.TYPE_MODULE)
    }

    override fun getLogTag(): String = LOG_TAG


    private val _filteredAgencies = dataSourcesRepository.readingAllAgencies().map { agencies ->
        agencies
            .ifEmpty { null } // difference between loading & loaded
            ?.filter { agency ->
                agency.getSupportedType() !in UNSUPPORTED_TYPE
            }
            ?.filter { it.hasFaresWebForLang }
            ?.sortedBy {
                it.shortNameLC
            }
    }

    val agencies: LiveData<List<AgencyProperties>?> = _filteredAgencies.distinctUntilChanged()
}