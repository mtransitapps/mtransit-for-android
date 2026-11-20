package org.mtransit.android.datasource

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.res.Resources
import android.database.Cursor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mtransit.android.commons.FakeSharedPreferences
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.SecureStringUtils
import org.mtransit.android.commons.provider.InstagramNewsProvider
import org.mtransit.android.commons.provider.RSSNewsProvider
import org.mtransit.android.commons.provider.TwitterNewsProvider
import org.mtransit.android.commons.provider.YouTubeNewsProvider
import org.mtransit.android.commons.provider.common.ProviderConfig
import org.mtransit.android.commons.provider.common.ProviderConfig.Companion.toConfig
import org.mtransit.android.commons.provider.common.ProviderConfigContract
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.news.NewsProviderContract
import org.mtransit.android.commons.provider.news.NewsProviderContract.Filter.Companion.toFilter
import org.mtransit.android.commons.provider.news.instagram.InstagramNewsConfig
import org.mtransit.android.commons.provider.news.rss.RssNewProviderUtils
import org.mtransit.android.commons.provider.news.rss.RssNewsConfig
import org.mtransit.android.commons.provider.news.rss.RssStorage
import org.mtransit.android.commons.provider.news.twitter.TwitterNewsConfig
import org.mtransit.android.commons.provider.news.twitter.TwitterStorage
import org.mtransit.android.commons.provider.news.youtube.YouTubeNewsConfig
import org.mtransit.android.commons.provider.news.youtube.YouTubeStorage
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.ProviderConfigEntity
import org.mtransit.commons.model.Secret
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import org.mtransit.android.commons.R as commonsR

@SuppressLint("DoNotMockPlatformTypes")
class ProviderConfigRepositoryTest {

    companion object {
        private const val PKG = "org.mtransit.test.module"
        private const val AUTHORITY = "$PKG.news.twitter"
        private const val TARGET = "$PKG.gtfs"
        private const val SECRET = "test-api-key"
        private const val ENCODED_SECRET = "3nc0d3d$3cr3t$\n"
        private val PROPERTIES = NewsProviderProperties(AUTHORITY, TARGET, PKG)
    }

    private val dao = FakeProviderConfigDao()
    private val packageInfo = PackageInfo().apply {
        packageName = PKG
        @Suppress("DEPRECATION")
        versionCode = 1
        providers = arrayOf(
            ProviderInfo().apply {
                authority = AUTHORITY
                name = TwitterNewsProvider::class.java.name
                enabled = true
            }
        )
    }
    private var appEnabled = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    private val pm = mock<PackageManager> {
        on { getPackageInfo(eq(PKG), any<Int>()) } doReturn packageInfo
        on { getApplicationEnabledSetting(PKG) } doAnswer { appEnabled }
    }
    private var configJson = newConfig().toJson()
    private var configAvailable = true
    private val cursor = mock<Cursor> {
        on { count } doReturn 1
        on { moveToFirst() } doReturn true
        on { getColumnIndexOrThrow(ProviderConfigContract.COLUMN_JSON) } doReturn 0
        on { getString(0) } doAnswer { configJson }
    }
    private val resolver = mock<ContentResolver> {
        on { query(any(), eq(arrayOf(ProviderConfigContract.COLUMN_JSON)), isNull(), isNull(), isNull()) } doAnswer {
            if (configAvailable) cursor else null
        }
    }
    private val context = mock<Context> {
        on { contentResolver } doReturn resolver
    }
    private val repository = ProviderConfigRepository(
        appContext = context,
        pm = pm,
        dataSourcesDatabase = mock { on { providerConfigDao() } doReturn dao },
    ).apply {
        this.makeUri = { _, _ -> mock {} }
    }

    @Test
    fun exportUsesSecureStringUtilsAndImportPreservesEncodedSecrets() = runTest {
        val encoder: (String) -> String? = {
            if (it == SECRET) ENCODED_SECRET else null
        }
        val config = newConfig(mapOf(KeysIds.TWITTER_BEARER_TOKEN to SECRET), encoder)
        configJson = config.toJson()
        assertFalse(configJson.contains(SECRET))
        assertEquals(ENCODED_SECRET, config.encSecrets[KeysIds.TWITTER_BEARER_TOKEN])
        assertFalse(config.toString().contains(ENCODED_SECRET))

        repository.refreshNewsConfigs(listOf(PROPERTIES))

        val stored = assertNotNull(dao.get(AUTHORITY))
        assertEquals(1L, stored.moduleVersionCode)
        assertEquals(configJson, stored.configJson)
        assertFalse(stored.toString().contains(ENCODED_SECRET))
        assertEquals(
            ENCODED_SECRET,
            stored.configJson.toConfig().encSecrets[KeysIds.TWITTER_BEARER_TOKEN]
        )
        verify(cursor).close()
    }

    @Test
    fun blankModuleSecretIsOmitted() {
        val config = newConfig(mapOf(KeysIds.TWITTER_BEARER_TOKEN to " "))
        assertEquals(emptyMap(), config.toJson().toConfig().encSecrets)
    }

    @Test
    fun twitterRuntimePreferencesAreNamespacedWithoutChangingLegacyKeys() {
        val preferences = FakeSharedPreferences()
        val storageContext = mock<Context>()
        whenever(storageContext.applicationContext).thenReturn(storageContext)
        whenever(storageContext.getSharedPreferences("lcl", Context.MODE_PRIVATE)).thenReturn(preferences)
        val first = TwitterStorage(storageContext, AUTHORITY)
        val second = TwitterStorage(storageContext, "$AUTHORITY.other")
        val legacy = TwitterStorage(storageContext)

        first.saveLastUpdateMs(1L)
        second.saveLastUpdateMs(2L)
        legacy.saveLastUpdateMs(3L)

        assertEquals(1L, first.getLastUpdateMs(0L))
        assertEquals(2L, second.getLastUpdateMs(0L))
        assertEquals(3L, legacy.getLastUpdateMs(0L))
    }

    @Test
    fun unchangedVersionSkipsExportAndUpgradesAndDowngradesRefresh() = runTest {
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        verify(cursor, times(1)).getString(0)
        for (version in listOf(3, 2)) {
            @Suppress("DEPRECATION")
            packageInfo.versionCode = version
            repository.refreshNewsConfigs(listOf(PROPERTIES))
            assertEquals(version.toLong(), assertNotNull(dao.get(AUTHORITY)).moduleVersionCode)
        }
        verify(cursor, times(3)).getString(0)
    }

    @Test
    fun missingOrInvalidReplacementRemovesStaleConfigAndAllowsRetry() = runTest {
        val validJson = configJson
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        @Suppress("DEPRECATION")
        packageInfo.versionCode = 2
        configAvailable = false
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        assertNull(dao.get(AUTHORITY))
        configAvailable = true
        val validObject = sharedJson.parseToJsonElement(validJson).jsonObject
        for (invalid in listOf(
            "{invalid-json",
            JsonObject(validObject + ("version" to JsonPrimitive(999))).toString(),
            JsonObject(validObject + ("type" to JsonPrimitive("unknown"))).toString(),
            JsonObject(validObject + ("authority" to JsonPrimitive("wrong.authority"))).toString(),
            JsonObject(validObject + ("target_authority" to JsonPrimitive("wrong.target"))).toString(),
            JsonObject(validObject + ("params" to JsonObject(emptyMap()))).toString(),
        )) {
            configJson = invalid
            repository.refreshNewsConfigs(listOf(PROPERTIES))
            assertNull(dao.get(AUTHORITY))
        }
        configJson = validJson
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        assertEquals(2L, assertNotNull(dao.get(AUTHORITY)).moduleVersionCode)
    }

    @Test
    fun disabledAndRemovedProvidersAreCleanedUp() = runTest {
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        appEnabled = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        assertNull(dao.get(AUTHORITY))
        appEnabled = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        assertNotNull(dao.get(AUTHORITY))
        repository.refreshNewsConfigs(emptyList())
        assertNull(dao.get(AUTHORITY))
    }

    @Test
    fun importedTwitterConfigLoadsNewsLocallyAndMergesEncodedKeys() = runTest(timeout = 10.hours) {
        val encoder: (String) -> String? = {
            if (it == SECRET) ENCODED_SECRET else null
        }
        configJson = newConfig(mapOf(KeysIds.TWITTER_BEARER_TOKEN to SECRET), encoder).toJson()
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        val localCursor = mock<Cursor> {
            on { count } doReturn 0
            on { moveToFirst() } doReturn false
        }
        val runtime = mock<TwitterNewsProvider> {
            on { queryMT(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn localCursor
        }
        repository.newsProviderFactory = { runtime }

        val result = repository.findNews(
            provider = PROPERTIES,
            filter = NewsProviderContract.Filter(),
            mainAppEncodedKeys = Secret(mapOf(KeysIds.TWITTER_CACHED_API_URL to "encoded-main-key")),
        )

        assertNotNull(result)
        assertEquals(0, result.size)
        val configCaptor = argumentCaptor<ProviderConfig>()
        verify(runtime).configureProviderConfig(configCaptor.capture())
        val capturedConfig = configCaptor.firstValue
        assertEquals(ENCODED_SECRET, capturedConfig.encSecrets[KeysIds.TWITTER_BEARER_TOKEN])
        val selectionCaptor = argumentCaptor<String>()
        verify(runtime).queryMT(any(), isNull(), selectionCaptor.capture(), isNull(), isNull())
        val xx = assertNotNull(selectionCaptor.firstValue.toFilter())
        assertEquals("encoded-main-key", xx.providedEncryptKeysMap?.data[KeysIds.TWITTER_CACHED_API_URL])
    }

    @Test
    fun instagramExportsModuleConfigAndRoutesLocallyUntilDisabled() = runTest {
        val authority = "$PKG.news.instagram"
        val properties = NewsProviderProperties(authority, TARGET, PKG)
        val installed = assertNotNull(packageInfo.providers).single().apply {
            this.authority = authority
            name = InstagramNewsProvider::class.java.name
        }
        val moduleResources = mock<Resources> {
            on { getStringArray(any()) } doReturn emptyArray()
            on { getStringArray(commonsR.array.instagram_user_names) } doReturn arrayOf("transit")
            on { getStringArray(commonsR.array.instagram_user_names_lang) } doReturn arrayOf("fr")
            on { getIntArray(any()) } doReturn intArrayOf()
            on { getInteger(commonsR.integer.news_provider_severity_info_agency) } doReturn 2
        }
        val moduleContext = mock<Context> {
            on { resources } doReturn moduleResources
            on { getString(commonsR.string.instagram_authority) } doReturn authority
            on { getString(commonsR.string.instagram_target_for_poi_authority) } doReturn TARGET
            on { getString(commonsR.string.instagram_color) } doReturn "112233"
            on { getString(commonsR.string.news_provider_noteworthy_info) } doReturn "60000"
            on { getString(commonsR.string.instagram_consumer_key) } doReturn SECRET
            on { getString(commonsR.string.instagram_consumer_secret) } doReturn SECRET
        }
        val exporter = mock<InstagramNewsProvider> {
            on { getProviderConfig() } doAnswer { // Answers.CALLS_REAL_METHODS
                ProviderConfig.create(
                    type = ProviderConfig.TYPE_NEWS_INSTAGRAM,
                    authority = authority,
                    targetAuthority = TARGET,
                    params = sharedJson.encodeToJsonElement(
                        InstagramNewsConfig(
                            color = "112233",
                            userNames = listOf("transit"),
                            userNamesLang = listOf("fr"),
                            userNamesColors = emptyList(),
                            userNamesTarget = emptyList(),
                            userNamesSeverity = emptyList(),
                            userNamesNoteworthy = emptyList(),
                            defaultSeverity = 2,
                            defaultNoteworthy = 60_000L,
                        )
                    ).jsonObject,
                    secrets = mapOf(
                        KeysIds.INSTAGRAM_CONSUMER_KEY to SECRET,
                        KeysIds.INSTAGRAM_CONSUMER_SECRET to SECRET,
                    ),
                    encoder = { clear ->
                        if (clear == SECRET) ENCODED_SECRET else null
                    }
                )
            }
        }
        doReturn(moduleContext).whenever(exporter).context
        configJson = exporter.getProviderConfig().toJson()
        assertFalse(configJson.contains(SECRET))
        repository.refreshNewsConfigs(listOf(properties))
        val stored = assertNotNull(dao.get(authority)).configJson.toConfig()
        assertEquals(
            mapOf(KeysIds.INSTAGRAM_CONSUMER_KEY to ENCODED_SECRET, KeysIds.INSTAGRAM_CONSUMER_SECRET to ENCODED_SECRET),
            stored.encSecrets,
        )
        val params = sharedJson.decodeFromJsonElement<InstagramNewsConfig>(stored.params)
        assertEquals(listOf("transit"), params.userNames)
        assertEquals(listOf("fr"), params.userNamesLang)
        assertEquals(2, params.defaultSeverity)
        assertEquals(60_000L, params.defaultNoteworthy)
        val configured = InstagramNewsProvider().apply { configureProviderConfig(stored) }
        assertEquals(authority, configured.authority)
        assertEquals(configJson, configured.getProviderConfig().toJson())

        val localCursor = mock<Cursor> {
            on { count } doReturn 0
            on { moveToFirst() } doReturn false
        }
        val runtime = mock<InstagramNewsProvider> {
            on { queryMT(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn localCursor
        }
        repository.newsProviderFactory = { type ->
            assertEquals(ProviderConfig.TYPE_NEWS_INSTAGRAM, type)
            runtime
        }
        assertEquals(0, assertNotNull(repository.findNews(properties, NewsProviderContract.Filter(), null)).size)
        verify(runtime).queryMT(any(), isNull(), anyOrNull(), isNull(), isNull())
        installed.enabled = false
        repository.refreshNewsConfigs(listOf(properties))
        assertNull(dao.get(authority))
        assertNull(repository.findNews(properties, NewsProviderContract.Filter(), null))
        verify(runtime).closeConfiguredRuntime()
    }

    @Test
    fun youtubeConfigImportsEncodedKeysAndReusesRuntimeUntilModuleChanges() = runTest {
        val authority = "$PKG.news.youtube"
        val properties = NewsProviderProperties(authority, TARGET, PKG)
        assertNotNull(packageInfo.providers).single().apply {
            this.authority = authority
            name = YouTubeNewsProvider::class.java.name
        }
        val encoder: (String) -> String? = {
            if (it == SECRET) ENCODED_SECRET else null
        }
        val config = ProviderConfig.create(
            type = ProviderConfig.TYPE_NEWS_YOUTUBE,
            authority = authority,
            targetAuthority = TARGET,
            params = sharedJson.encodeToJsonElement(
                YouTubeNewsConfig(
                    color = "112233",
                    channelsUsername = emptyList(),
                    channelsId = listOf("channel-id"),
                    channelsHandle = listOf("@transit"),
                    channelsAuthorUrl = listOf("https://www.youtube.com/@transit"),
                    channelsLang = listOf("en"),
                    channelsColors = emptyList(),
                    channelsTarget = emptyList(),
                    channelsSeverity = emptyList(),
                    channelsNoteworthy = emptyList(),
                    defaultSeverity = 1,
                    defaultNoteworthy = 60_000L,
                )
            ).jsonObject,
            secrets = mapOf(KeysIds.YOUTUBE_API_KEY to SECRET),
            encoder = encoder,
        )
        configJson = config.toJson()
        repository.refreshNewsConfigs(listOf(properties))
        val stored = assertNotNull(dao.get(authority)).configJson.toConfig()
        assertEquals(ENCODED_SECRET, stored.encSecrets[KeysIds.YOUTUBE_API_KEY])
        // Exercise the actual config decoder without attaching an Android provider.
        val configured = YouTubeNewsProvider().apply { configureProviderConfig(stored) }
        assertEquals(authority, configured.authority)
        assertEquals(configJson, configured.getProviderConfig().toJson())

        val localCursor = mock<Cursor> {
            on { count } doReturn 0
            on { moveToFirst() } doReturn false
        }
        val runtime = mock<YouTubeNewsProvider> {
            on { queryMT(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn localCursor
        }
        repository.newsProviderFactory = { type ->
            assertEquals(ProviderConfig.TYPE_NEWS_YOUTUBE, type)
            runtime
        }
        repeat(2) {
            assertEquals(0, assertNotNull(repository.findNews(properties, NewsProviderContract.Filter(), null)).size)
        }
        verify(runtime, times(1)).configureProviderConfig(any())
        @Suppress("DEPRECATION")
        packageInfo.versionCode = 2
        assertNull(repository.findNews(properties, NewsProviderContract.Filter(), null))
        repository.refreshNewsConfigs(listOf(properties))
        verify(runtime).closeConfiguredRuntime()
    }

    @Test
    fun youtubePreferenceResetDoesNotAffectOtherProvidersOrLegacyKeys() {
        val preferences = FakeSharedPreferences()
        val storageContext = mock<Context>()
        whenever(storageContext.applicationContext).thenReturn(storageContext)
        whenever(storageContext.getSharedPreferences("lcl", Context.MODE_PRIVATE)).thenReturn(preferences)
        val first = YouTubeStorage(storageContext, "first")
        val second = YouTubeStorage(storageContext, "second")
        val legacy = YouTubeStorage(storageContext)
        first.saveLastUpdateMs(1L)
        assertEquals(1L, first.getLastUpdateMs(0L))
        first.saveLastUpdateLang("fr")
        second.saveLastUpdateMs(2L)
        second.saveLastUpdateLang("en")
        legacy.saveLastUpdateMs(3L)
        first.saveLastUpdateMs(null)
        first.saveLastUpdateLang(null)
        assertEquals(0L, first.getLastUpdateMs(0L))
        assertEquals("", first.getLastUpdateLang(""))
        assertEquals(2L, second.getLastUpdateMs(0L))
        assertEquals("en", second.getLastUpdateLang(""))
        assertEquals(3L, legacy.getLastUpdateMs(0L))
    }

    @Test
    fun rssImportUsesModuleParsingOptionsWithoutReadingMainAppResources() = runTest {
        val authority = "$PKG.news.rss"
        val properties = NewsProviderProperties(authority, TARGET, PKG)
        assertNotNull(packageInfo.providers).single().apply {
            this.authority = authority
            name = RSSNewsProvider::class.java.name
        }
        val params = newRssConfig()
        configJson = params.toProviderConfig(authority, TARGET).toJson()
        repository.refreshNewsConfigs(listOf(properties))
        val stored = assertNotNull(dao.get(authority)).configJson.toConfig()
        val imported = RssNewsConfig.fromProviderConfig(stored)
        assertEquals(params, imported)
        val configured = RSSNewsProvider().apply { configureProviderConfig(stored) }
        assertEquals(authority, configured.authority)
        assertEquals(configJson, configured.getProviderConfig().toJson())

        val hostContext = mock<Context>()
        assertEquals("112233", RssNewProviderUtils.pickColor(hostContext, 0, imported))
        assertEquals("Agency", RssNewProviderUtils.pickAuthorName(hostContext, 0, imported))
        assertEquals("fr", RssNewProviderUtils.pickLang(hostContext, 0, imported))
        assertEquals(TARGET, RssNewProviderUtils.pickTarget(hostContext, 0, imported, TARGET))
        assertEquals("specific.target", RssNewProviderUtils.pickTarget(hostContext, 1, imported, TARGET))
        assertEquals(2, RssNewProviderUtils.pickSeverity(hostContext, 0, imported))
        assertEquals(60_000L, RssNewProviderUtils.pickNoteworthy(hostContext, 0, imported))
        assertTrue(RssNewProviderUtils.pickIgnoreGUID(hostContext, 0, imported))
        assertFalse(RssNewProviderUtils.pickIgnoreLink(hostContext, 0, imported))
        assertEquals("ISO-8859-1", RssNewProviderUtils.pickEncoding(hostContext, imported))
        assertEquals("(\\d{4}-\\d{2}-\\d{2})" to "yyyy-MM-dd", RssNewProviderUtils.pickDateLinkFallback(hostContext, 0, imported))
        verifyNoInteractions(hostContext)
        assertFailsWith<IllegalArgumentException> { params.copy(useCustomSslCertificate = true) }

        val localCursor = mock<Cursor> {
            on { count } doReturn 0
            on { moveToFirst() } doReturn false
        }
        val runtime = mock<RSSNewsProvider> {
            on { queryMT(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn localCursor
        }
        repository.newsProviderFactory = { type ->
            assertEquals(ProviderConfig.TYPE_NEWS_RSS, type)
            runtime
        }
        assertEquals(0, assertNotNull(repository.findNews(properties, NewsProviderContract.Filter(), null)).size)
        verify(runtime).queryMT(any(), isNull(), anyOrNull(), isNull(), isNull())
    }

    @Test
    fun mainAppOwnedNewsKeepsExistingProviderPath() = runTest {
        whenever(context.packageName).thenReturn(PKG)
        assertNotNull(packageInfo.providers).single().name = RSSNewsProvider::class.java.name
        repository.refreshNewsConfigs(listOf(PROPERTIES))
        assertNull(dao.get(AUTHORITY))
        assertNull(repository.findNews(PROPERTIES, NewsProviderContract.Filter(), null))
        // verifyNoInteractions(resolver)
    }

    @Test
    fun rssPreferenceResetOnlyClearsItsOwnNamespace() {
        val preferences = FakeSharedPreferences()
        val storageContext = mock<Context>()
        whenever(storageContext.applicationContext).thenReturn(storageContext)
        whenever(storageContext.getSharedPreferences("lcl", Context.MODE_PRIVATE)).thenReturn(preferences)
        val first = RssStorage(storageContext, "first")
        val second = RssStorage(storageContext, "second")
        val legacy = RssStorage(storageContext)
        first.saveLastUpdate(1L, "fr")
        second.saveLastUpdate(2L, "en")
        legacy.saveLastUpdate(3L, "en")
        first.saveLastUpdate(null, null)
        assertEquals(0L, first.getLastUpdateMs(0L))
        assertEquals("", first.getLastUpdateLang(""))
        assertEquals(2L, second.getLastUpdateMs(0L))
        assertEquals("en", second.getLastUpdateLang(""))
        assertEquals(3L, legacy.getLastUpdateMs(0L))
        assertEquals("en", legacy.getLastUpdateLang(""))
    }

    private fun newRssConfig() = RssNewsConfig(
        color = "112233",
        feeds = listOf("https://example.org/feed"),
        feedsAuthorIcon = emptyList(),
        feedsAuthorName = listOf(""),
        feedsAuthorUrl = listOf("https://example.org"),
        feedsLabel = emptyList(),
        feedsLang = listOf("fr"),
        feedsColors = listOf(""),
        feedsTarget = listOf("", "specific.target"),
        feedsSeverity = emptyList(),
        feedsNoteworthy = emptyList(),
        feedsIgnoreGuid = listOf(true),
        feedsIgnoreLink = emptyList(),
        feedsDateLinkFallbackRegex = listOf("(\\d{4}-\\d{2}-\\d{2})"),
        feedsDateLinkFallbackFormat = listOf("yyyy-MM-dd"),
        defaultAuthorName = "Agency",
        defaultSeverity = 2,
        defaultNoteworthy = 60_000L,
        timeZone = "America/Montreal",
        copyToFileInsteadOfStreaming = true,
        encoding = "ISO-8859-1",
        fileCleaningRegex = listOf("^\\s+"),
        fileCleaningReplacement = listOf(""),
        useCustomSslCertificate = false,
        customSslCertificate = null,
    )

    private fun newConfig(
        secrets: Map<String, String> = emptyMap(),
        encoder: (clear: String) -> String? = SecureStringUtils::enc
    ) = ProviderConfig.create(
        type = ProviderConfig.TYPE_NEWS_TWITTER,
        authority = AUTHORITY,
        targetAuthority = TARGET,
        params = sharedJson.encodeToJsonElement(
            TwitterNewsConfig(
                color = "112233",
                screenNames = listOf("transit"),
                screenNamesId = emptyList(),
                screenNamesLang = emptyList(),
                screenNamesColors = emptyList(),
                screenNamesTarget = emptyList(),
                screenNamesSeverity = emptyList(),
                screenNamesNoteworthy = emptyList(),
                defaultSeverity = 1,
                defaultNoteworthy = 60_000L,
            )
        ).jsonObject,
        secrets = secrets,
        encoder = encoder,
    )

    private class FakeProviderConfigDao : ProviderConfigDao {
        private val configs = mutableMapOf<String, ProviderConfigEntity>()

        override suspend fun get(authority: String) = configs[authority]

        override suspend fun upsert(config: ProviderConfigEntity) {
            configs[config.authority] = config
        }

        override suspend fun delete(authority: String) {
            configs.remove(authority)
        }

        override suspend fun deleteExcept(authorities: List<String>) {
            configs.keys.retainAll(authorities.toSet())
        }
    }
}
