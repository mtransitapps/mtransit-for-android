package org.mtransit.android.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.data.UISchedule;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.ui.schedule.day.ScheduleDayAdapter;

@SuppressWarnings("WeakerAccess")
public final class NightModeUtils implements MTLog.Loggable {

	private static final String LOG_TAG = NightModeUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void setDefaultNightMode(@NonNull Context context, @Nullable DemoModeManager demoModeManager) {
		setDefaultNightMode(
				getDefaultNightMode(context, demoModeManager)
		);
		resetColorCache();
	}

	public static void setDefaultNightMode(@AppCompatDelegate.NightMode int mode) {
		AppCompatDelegate.setDefaultNightMode(mode);
	}

	public static void recreate(@NonNull Activity activity) {
		activity.recreate();
	}

	public static void resetColorCache() {
		ColorUtils.resetColorCache();
		AppStatus.resetColorCache();
		UISchedule.resetColorCache();
		UITimeUtils.resetColorCache();
		AvailabilityPercent.resetColorCache();
		ScheduleDayAdapter.TimeViewHolder.resetColorCache();
	}

	@AppCompatDelegate.NightMode
	public static int getDefaultNightMode(@NonNull Context context,
										  @Nullable DemoModeManager demoModeManager) {
		String themePref = PreferenceUtils.getPrefDefaultNN(context,
				PreferenceUtils.PREFS_THEME, PreferenceUtils.PREFS_THEME_DEFAULT);
		if (demoModeManager != null && demoModeManager.getEnabled()) {
			themePref = PreferenceUtils.PREFS_THEME_LIGHT; // light for screenshots (demo mode ON)
		}
		switch (themePref) {
		case PreferenceUtils.PREFS_THEME_LIGHT:
			return AppCompatDelegate.MODE_NIGHT_NO;
		case PreferenceUtils.PREFS_THEME_DARK:
			return AppCompatDelegate.MODE_NIGHT_YES;
		case PreferenceUtils.PREFS_THEME_SYSTEM_DEFAULT:
		default:
			return getDefault();
		}
	}

	@AppCompatDelegate.NightMode
	private static int getDefault() {
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
			return AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
		} else { // Android 10 Q+
			return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
		}
	}
}
