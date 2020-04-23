package org.mtransit.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.di.Injection;

import java.util.Arrays;
import java.util.Collection;

public class ModulesReceiver extends BroadcastReceiver implements MTLog.Loggable {

	private static final String LOG_TAG = ModulesReceiver.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("deprecation")
	private static final Collection<String> ACTIONS = Arrays.asList(
			Intent.ACTION_PACKAGE_ADDED,
			Intent.ACTION_PACKAGE_CHANGED,
			Intent.ACTION_PACKAGE_DATA_CLEARED,
			Intent.ACTION_PACKAGE_FIRST_LAUNCH,
			Intent.ACTION_PACKAGE_FULLY_REMOVED,
			Intent.ACTION_PACKAGE_INSTALL,
			Intent.ACTION_PACKAGE_NEEDS_VERIFICATION,
			Intent.ACTION_PACKAGE_REMOVED,
			Intent.ACTION_PACKAGE_REPLACED,
			Intent.ACTION_PACKAGE_RESTARTED,
			"android.intent.action.PACKAGE_VERIFIED" // minSDK 17 = Intent.ACTION_PACKAGE_VERIFIED
	);

	@NonNull
	private final CrashReporter crashReporter;

	public ModulesReceiver() {
		super();
		this.crashReporter = Injection.providesCrashReporter();
	}

	@Override
	public void onReceive(@Nullable Context context, @Nullable Intent intent) {
		if (context == null) {
			this.crashReporter.w(this, "Modules broadcast receiver with null context ignored!");
			return;
		}
		String action = intent == null ? null : intent.getAction();
		if (!ACTIONS.contains(action)) {
			this.crashReporter.w(this, "Modules broadcast receiver with unexpected action '%s' ignored!", action);
			return;
		}
		Uri data = intent == null ? null : intent.getData();
		String pkg = data == null ? null : data.getSchemeSpecificPart();
		if (DataSourceProvider.isSet()) {
			if (DataSourceProvider.isProvider(context, pkg)) {
				MTLog.i(this, "Broadcast received: %s", action);
				boolean didReset = DataSourceProvider.resetIfNecessary(context);
				if (!didReset) {
					ping(context, pkg);
				}
			} else {
				if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action) //
						|| Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
					if (DataSourceProvider.resetIfNecessary(context)) {
						MTLog.i(this, "Broadcast received: %s", action);
					}
				}
			}
		} else {
			if (ping(context, pkg)) {
				MTLog.i(this, "Broadcast received: %s", action);
			}
		}
	}

	private boolean ping(@NonNull Context context, @Nullable String pkg) {
		ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg);
		if (providers != null) {
			String agencyProviderMetaData = DataSourceProvider.getAgencyProviderMetaData(context);
			for (ProviderInfo provider : providers) {
				if (provider != null && provider.metaData != null) {
					if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
						DataSourceManager.ping(context, provider.authority);
						return true;
					}
				}
			}
		}
		return false;
	}
}
