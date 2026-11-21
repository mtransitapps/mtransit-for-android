package org.mtransit.android.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.getAppLongVersionCode
import org.mtransit.android.commons.getInstalledProviderWithMetaData
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.provider.InstagramNewsProvider
import org.mtransit.android.commons.provider.RSSNewsProvider
import org.mtransit.android.commons.provider.TwitterNewsProvider
import org.mtransit.android.commons.provider.YouTubeNewsProvider
import org.mtransit.android.commons.provider.common.ProviderConfig
import org.mtransit.android.commons.provider.common.ProviderConfig.Companion.toConfig
import org.mtransit.android.commons.provider.common.ProviderConfigContract
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.news.ConfigurableNewsProvider
import org.mtransit.android.commons.provider.news.NewsProviderContract
import org.mtransit.android.commons.provider.news.instagram.InstagramNewsConfig
import org.mtransit.android.commons.provider.news.rss.RssNewsConfig
import org.mtransit.android.commons.provider.news.twitter.TwitterNewsConfig
import org.mtransit.android.commons.provider.news.youtube.YouTubeNewsConfig
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.ProviderConfigEntity
import org.mtransit.commons.model.Secret
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderConfigRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val pm: PackageManager,
    dataSourcesDatabase: DataSourcesDatabase,
) : MTLogK.Loggable {

    companion object {
        private val LOG_TAG: String = ProviderConfigRepository::class.java.simpleName
        private val newsProviderTypes = mapOf(
            InstagramNewsProvider::class.java.name to ProviderConfig.TYPE_NEWS_INSTAGRAM,
            RSSNewsProvider::class.java.name to ProviderConfig.TYPE_NEWS_RSS,
            TwitterNewsProvider::class.java.name to ProviderConfig.TYPE_NEWS_TWITTER,
            YouTubeNewsProvider::class.java.name to ProviderConfig.TYPE_NEWS_YOUTUBE,
        )
    }

    override fun getLogTag() = LOG_TAG

    private val dao = dataSourcesDatabase.providerConfigDao()
    private val mutex = Mutex()
    private val newsRuntimes = ConcurrentHashMap<String, NewsRuntime>()

    @VisibleForTesting
    internal var newsProviderFactory: (String) -> ConfigurableNewsProvider = { type ->
        when (type) {
            ProviderConfig.TYPE_NEWS_INSTAGRAM -> InstagramNewsProvider()
            ProviderConfig.TYPE_NEWS_RSS -> RSSNewsProvider()
            ProviderConfig.TYPE_NEWS_TWITTER -> TwitterNewsProvider()
            ProviderConfig.TYPE_NEWS_YOUTUBE -> YouTubeNewsProvider()
            else -> throw IllegalArgumentException("Unsupported local news provider type")
        }
    }

    private class NewsRuntime(val configJson: String, val provider: ConfigurableNewsProvider) {
        var closed = false

        fun close() = synchronized(this) {
            closed = true
            provider.closeConfiguredRuntime()
        }
    }

    suspend fun refreshNewsConfigs(providers: List<NewsProviderProperties>) = mutex.withLock {
        val authorities = providers.map { it.authority }
        dao.deleteExcept(authorities)
        (newsRuntimes.keys - authorities.toSet()).forEach(::closeNewsRuntime)
        providers.forEach { provider ->
            yield()
            refresh(provider)
        }
    }

    private fun closeNewsRuntime(authority: String) {
        synchronized(newsRuntimes) {
            newsRuntimes.remove(authority)?.close()
        }
    }

    @VisibleForTesting
    internal var makeUri: (providerAuthority: String, path: String) -> Uri = { providerAuthority, path ->
        UriUtils.newContentUri(providerAuthority).buildUpon()
            .appendPath(path)
            .build()
    }

    suspend fun findNews(
        provider: NewsProviderProperties,
        filter: NewsProviderContract.Filter,
        mainAppEncodedKeys: Secret<Map<String, String>>?,
    ): ArrayList<News>? {
        if (provider.pkg == appContext.packageName) return null
        val entity = dao.get(provider.authority) ?: return null
        val currentVersionCode = pm.getAppLongVersionCode(provider.pkg)
        if (entity.pkg != provider.pkg
            || entity.moduleVersionCode != currentVersionCode
            || entity.schemaVersion != ProviderConfig.SCHEMA_VERSION
            || currentVersionCode < 0L
        ) {
            MTLogK.d(this, "Cached provider config is stale for '${provider.authority}'; use ContentProvider until refresh.")
            return null
        }
        val config = parseConfig(entity.configJson, provider) ?: return null
        val keys = buildMap {
            putAll(config.encSecrets)
            putAll(mainAppEncodedKeys?.data.orEmpty())
        }
        val newsFilter = filter.copy(providedEncryptKeysMap = Secret(keys))
        val uri = makeUri(provider.authority, NewsProviderContract.NEWS_PATH)
        return try {
            val runtime = getNewsRuntime(config)
            synchronized(runtime) {
                if (runtime.closed) {
                    MTLogK.d(this, "Local news runtime replaced for '${provider.authority}'; use ContentProvider.")
                    return@synchronized null
                }
                val selection = newsFilter.toJsonString()
                runtime.provider.queryMT(uri, null, selection, null, null)?.use { cursor ->
                    cursor.toNews(provider.authority)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SQLiteException) {
            MTLogK.w(this, "Local news cache unavailable for '${provider.authority}'; use ContentProvider.")
            null
        } catch (_: SecurityException) {
            MTLogK.w(this, "Local news permission denied for '${provider.authority}'; use ContentProvider.")
            null
        } catch (_: IllegalArgumentException) {
            // Provider/config exceptions can contain request keys.
            MTLogK.w(this, "Invalid local news request or config for '${provider.authority}'; use ContentProvider.")
            null
        } catch (_: IllegalStateException) {
            MTLogK.w(this, "Local news runtime unavailable for '${provider.authority}'; use ContentProvider.")
            null
        }
    }

    private fun getNewsRuntime(config: ProviderConfig): NewsRuntime {
        val configJson = config.toJson()
        synchronized(newsRuntimes) {
            val current = newsRuntimes[config.authority]
            if (current?.configJson == configJson) return current
            newsRuntimes.remove(config.authority)?.close()
            val provider = newsProviderFactory(config.type)
            provider.configureProviderConfig(config)
            val info = ProviderInfo().apply {
                authority = config.authority
                name = provider.javaClass.name
                packageName = appContext.packageName
            }
            provider.attachInfo(appContext, info)
            return NewsRuntime(configJson, provider).also { newsRuntimes[config.authority] = it }
        }
    }

    private fun Cursor.toNews(authority: String) = ArrayList<News>(count).apply {
        if (moveToFirst()) {
            do {
                add(News.fromCursorStatic(this@toNews, authority))
            } while (moveToNext())
        }
    }

    private suspend fun refresh(provider: NewsProviderProperties) {
        val installedProvider = pm.getInstalledProviderWithMetaData(provider.pkg, provider.authority)
        val expectedType = installedProvider?.name?.let(newsProviderTypes::get)
        if (provider.pkg == appContext.packageName
            || expectedType == null
            || !installedProvider.enabled
            || !pm.isAppEnabled(provider.pkg)
        ) {
            dao.delete(provider.authority)
            closeNewsRuntime(provider.authority)
            return
        }
        val versionCode = pm.getAppLongVersionCode(provider.pkg)
        val previous = dao.get(provider.authority)
        if (versionCode >= 0L
            && previous?.pkg == provider.pkg
            && previous.moduleVersionCode == versionCode
            && previous.schemaVersion == ProviderConfig.SCHEMA_VERSION
        ) {
            return
        }
        // Never retain config from a different module version if importing its replacement fails.
        dao.delete(provider.authority)
        closeNewsRuntime(provider.authority)
        if (versionCode < 0L) return // PackageManager already logged the missing package.
        importConfig(provider, versionCode, expectedType)
    }

    private suspend fun importConfig(provider: NewsProviderProperties, versionCode: Long, expectedType: String) {
        val configJson = readConfig(provider.authority) ?: return
        val config = parseConfig(configJson, provider) ?: return
        if (config.type != expectedType) {
            MTLogK.w(this, "Provider config type does not match the installed provider for '${provider.authority}'.")
            return
        }
        if (pm.getAppLongVersionCode(provider.pkg) != versionCode || !pm.isAppEnabled(provider.pkg)) {
            MTLogK.i(this, "Module changed while importing provider config for '${provider.authority}'; retry on next discovery.")
            return
        }
        dao.upsert(
            ProviderConfigEntity(
                authority = provider.authority,
                pkg = provider.pkg,
                moduleVersionCode = versionCode,
                schemaVersion = config.schemaVersion,
                configJson = config.toJson(),
            )
        )
    }

    private fun readConfig(authority: String): String? {
        val uri = makeUri(authority, ProviderConfigContract.CONFIG_PATH)
        try {
            return appContext.contentResolver.query(uri, arrayOf(ProviderConfigContract.COLUMN_JSON), null, null, null)?.use { cursor ->
                if (cursor.count != 1 || !cursor.moveToFirst()) {
                    MTLogK.w(this, "Invalid provider config cursor for '$authority'.")
                    return null
                }
                cursor.getString(cursor.getColumnIndexOrThrow(ProviderConfigContract.COLUMN_JSON))
            }.also {
                if (it == null) MTLogK.d(this, "Provider config unavailable for '$authority'.")
            }
        } catch (_: SecurityException) {
            MTLogK.w(this, "Provider config permission denied for '$authority'.")
        } catch (_: IllegalArgumentException) {
            MTLogK.w(this, "Provider config query unsupported or invalid for '$authority'.")
        } catch (_: IllegalStateException) {
            MTLogK.w(this, "Provider config unavailable while querying '$authority'.")
        } catch (_: SQLiteException) {
            MTLogK.w(this, "Provider config cursor could not be read for '$authority'.")
        }
        return null
    }

    private fun parseConfig(configJson: String, provider: NewsProviderProperties): ProviderConfig? {
        try {
            val config = configJson.toConfig()
            if (config.schemaVersion != ProviderConfig.SCHEMA_VERSION || config.type !in newsProviderTypes.values) {
                MTLogK.w(this, "Unsupported provider config schema or type for '${provider.authority}'.")
                return null
            }
            if (config.authority != provider.authority || config.targetAuthority != provider.targetAuthority || config.targetAuthority.isBlank()) {
                MTLogK.w(this, "Provider config identity mismatch for '${provider.authority}'.")
                return null
            }
            when (config.type) {
                ProviderConfig.TYPE_NEWS_INSTAGRAM -> sharedJson.decodeFromJsonElement<InstagramNewsConfig>(config.params)
                ProviderConfig.TYPE_NEWS_RSS -> RssNewsConfig.fromProviderConfig(config)
                ProviderConfig.TYPE_NEWS_TWITTER -> sharedJson.decodeFromJsonElement<TwitterNewsConfig>(config.params)
                ProviderConfig.TYPE_NEWS_YOUTUBE -> sharedJson.decodeFromJsonElement<YouTubeNewsConfig>(config.params)
            }
            return config
        } catch (_: SerializationException) {
            // Serialization exceptions can include the original JSON, including secrets.
            MTLogK.w(this, "Invalid provider config JSON for '${provider.authority}'.")
        } catch (_: IllegalArgumentException) {
            MTLogK.w(this, "Invalid provider config fields for '${provider.authority}'.")
        }
        return null
    }
}
