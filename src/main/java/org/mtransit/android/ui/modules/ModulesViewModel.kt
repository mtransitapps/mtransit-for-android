package org.mtransit.android.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.di.Injection

class ModulesViewModel : ViewModel() {

    private val dataSourcesRepository: DataSourcesRepository by lazy { Injection.providesDataSourcesRepository() }

    val allAgencies = dataSourcesRepository.readingAllAgenciesDistinct().map { agencies ->
        agencies.filter { agency ->
            agency.type != DataSourceType.TYPE_PLACE
                    && agency.type != DataSourceType.TYPE_MODULE
        }.sortedBy { it.pkg }
    }
}