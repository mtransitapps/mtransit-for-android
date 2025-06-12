package org.mtransit.android.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.pref.liveData
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("MemberVisibilityCanBePrivate", "unused")
@Singleton
class LanguageManager @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
) {

    companion object {
        val LANG_EN: Locale = Locale.forLanguageTag("en")
        val LANG_FR: Locale = Locale.forLanguageTag("fr")
    }

    val langUserPref: LiveData<String> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_LANG, DefaultPreferenceRepository.PREFS_LANG_DEFAULT
    ).distinctUntilChanged()


    fun updateUserPrefFromAppLocale() {
        val appLocale = AppCompatDelegate.getApplicationLocales()[0] // not using Locale.getDefault() because it's not user pref
        val userPref = when (appLocale?.language) {
            LANG_EN.language -> DefaultPreferenceRepository.PREFS_LANG_EN
            LANG_FR.language -> DefaultPreferenceRepository.PREFS_LANG_FR
            else -> DefaultPreferenceRepository.PREFS_LANG_SYSTEM_DEFAULT
        }
        defaultPrefRepository.pref.edit {
            putString(DefaultPreferenceRepository.PREFS_LANG, userPref)
        }
    }

    fun updateAppLocaleFromUserPref() {
        updateAppLocale(
            when (langUserPref.value) {
                DefaultPreferenceRepository.PREFS_LANG_EN -> LocaleListCompat.forLanguageTags("en")
                DefaultPreferenceRepository.PREFS_LANG_FR -> LocaleListCompat.forLanguageTags("fr")
                DefaultPreferenceRepository.PREFS_LANG_SYSTEM_DEFAULT -> LocaleListCompat.getEmptyLocaleList()
                else -> LocaleListCompat.getEmptyLocaleList()
            }
        )
    }

    fun updateAppLocaleFromLanguageTag(langTag: String) {
        updateAppLocale(LocaleListCompat.forLanguageTags(langTag))
    }

    fun updateAppLocale(newLocaleList: LocaleListCompat) {
        val currentLocaleList = AppCompatDelegate.getApplicationLocales()
        if (currentLocaleList == newLocaleList) {
            return // SKIP
        }
        AppCompatDelegate.setApplicationLocales(newLocaleList)
    }
}

