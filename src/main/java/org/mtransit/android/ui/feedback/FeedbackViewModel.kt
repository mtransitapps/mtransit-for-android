package org.mtransit.android.ui.feedback

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
class FeedbackViewModel @Inject constructor(
    dataSourcesRepository: DataSourcesRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = FeedbackViewModel::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG


    private val _filteredAgencies = dataSourcesRepository.readingAllAgencies().map { agencies ->
        agencies
            .ifEmpty { null } // difference between loading & loaded
            ?.filter { agency ->
                agency.type != DataSourceType.TYPE_PLACE
                        && agency.type != DataSourceType.TYPE_MODULE
            }
            ?.filter { it.hasContactUs() }
            ?.sortedBy {
                it.shortNameLC
            }
    }

    val agencies: LiveData<List<AgencyProperties>?> = _filteredAgencies.distinctUntilChanged()
}