package org.mtransit.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.di.Injection;

import java.util.Arrays;
import java.util.Collection;

import static org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES;

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
			Intent.ACTION_PACKAGE_VERIFIED
	);

	@Nullable
	private static String agencyProviderMetaData;

	@NonNull
	private static String getAgencyProviderMetaData(@NonNull Context context) {
		if (agencyProviderMetaData == null) {
			agencyProviderMetaData = context.getString(R.string.agency_provider);
		}
		return agencyProviderMetaData;
	}

	@NonNull
	private final CrashReporter crashReporter;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public ModulesReceiver() {
		super();
		this.crashReporter = Injection.providesCrashReporter();
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	@MainThread
	@Override
	public void onReceive(@Nullable Context context, @Nullable Intent intent) {
		if (context == null) {
			this.crashReporter.w(this, "Modules broadcast receiver with null context ignored!");
			return;
		}
		final String action = intent == null ? null : intent.getAction();
		if (!ACTIONS.contains(action)) {
			this.crashReporter.w(this, "Modules broadcast receiver with unexpected action '%s' ignored!", action);
			return;
		}
		final Uri data = intent == null ? null : intent.getData();
		final String pkg = data == null ? null : data.getSchemeSpecificPart();
		if (F_CACHE_DATA_SOURCES) {
			if (this.dataSourcesRepository.isAProvider(pkg)) {
				if (ping(context, pkg)) {
					MTLog.i(this, "Received broadcast %s for %s.", action, pkg);
				}
			}
			final boolean canBeAProvider = this.dataSourcesRepository.isAProvider(pkg)
					|| Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)
					|| Intent.ACTION_PACKAGE_REMOVED.equals(action);
			if (!canBeAProvider) {
				MTLog.d(this, "onReceive() > SKIP (can NOT be a provider: pkg:%s, action:%s)", pkg, action);
				return;
			}
			if (org.mtransit.android.data.DataSourceProvider.isSet()) {
			} else { // ELSE update cache for latter
				try {
					this.dataSourcesRepository.updateAsync().get(); // TODO ? filter by pkg? authority?
				} catch (Exception e) {
					MTLog.w(this, e, "Error while updating data-sources from repository!");
				}
			}
			return;
		}
		if (org.mtransit.android.data.DataSourceProvider.isSet()) {
			if (org.mtransit.android.data.DataSourceProvider.isProvider(context, pkg)) {
				MTLog.i(this, "Received broadcast %s for %s.", action, pkg);
				boolean didReset = org.mtransit.android.data.DataSourceProvider.resetIfNecessary(context);
				if (!didReset) {
					ping(context, pkg);
				}
			} else {
				if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action) //
						|| Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
					if (org.mtransit.android.data.DataSourceProvider.resetIfNecessary(context)) {
						MTLog.i(this, "Received broadcast %s for %s.", action, pkg);
					}
				}
			}
		} else {
			if (ping(context, pkg)) {
				MTLog.i(this, "Received broadcast %s for %s.", action, pkg);
			}
		}
	}

	private boolean ping(@NonNull Context context, @Nullable String pkg) {
		final ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg);
		if (providers != null) {
			final String agencyProviderMetaData = getAgencyProviderMetaData(context);
			for (ProviderInfo provider : providers) {
				if (provider != null && provider.metaData != null) {
					if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
						MTLog.i(this, "Ping: %s", provider.authority);
						DataSourceManager.ping(context, provider.authority);
						return true;
					}
				}
			}
		}
		return false;
	}
}
