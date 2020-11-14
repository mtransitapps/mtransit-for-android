package org.mtransit.android.data.source

import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.DataSourceManager
import java.util.ArrayList

class DataSourceRepository(private val app: IApplication) {

    fun findNews(
        authority: String,
        newsFilter: NewsProviderContract.Filter?
    ): ArrayList<NewsArticle>? {
        return DataSourceManager.findNews(app.requireContext(), authority, newsFilter)
    }

    fun findNewsArticle(
        authority: String,
        newsFilter: NewsProviderContract.Filter?
    ): NewsArticle? {
        return DataSourceManager.findANews(app.requireContext(), authority, newsFilter)
    }
}