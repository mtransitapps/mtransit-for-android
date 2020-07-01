package org.mtransit.android.data.source

import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.DataSourceManager
import java.util.ArrayList

class DataSourceRepository(private val iApplication: IApplication) {

    fun findNews(
        authority: String,
        newsFilter: NewsProviderContract.Filter?
    ): ArrayList<NewsArticle>? {
        return DataSourceManager.findNews(iApplication.requireContext(), authority, newsFilter)
    }
    fun findNewsArticle(
        authority: String,
        newsFilter: NewsProviderContract.Filter?
    ): NewsArticle? {
        return DataSourceManager.findANews(iApplication.requireContext(), authority, newsFilter)
    }
}