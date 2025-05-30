package org.mtransit.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.common.MTContinuationJ;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

@AndroidEntryPoint
public class ModulesReceiver extends BroadcastReceiver implements MTLog.Loggable {

	private static final String LOG_TAG = ModulesReceiver.class.getSimpleName();

	private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

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
			Intent.ACTION_PACKAGE_INSTALL, // deprecated (never been used)
			Intent.ACTION_PACKAGE_NEEDS_VERIFICATION,
			Intent.ACTION_PACKAGE_REMOVED,
			Intent.ACTION_PACKAGE_REPLACED,
			Intent.ACTION_PACKAGE_RESTARTED,
			Intent.ACTION_PACKAGE_VERIFIED
	);

	private boolean shouldPing(@Nullable Intent intent) {
		if (intent == null) {
			return false;
		}
		final String action = intent.getAction();
		if (action == null) {
			return false;
		}
		switch (action) {
			case Intent.ACTION_PACKAGE_ADDED:
				final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
				if (replacing) {
					return false; // will be followed by Intent.ACTION_PACKAGE_REPLACED
				}
				return true;
			case Intent.ACTION_PACKAGE_REPLACED:
				return true;
			default:
				return false;
		}
	}

	@NonNull
	public static IntentFilter getIntentFilter() {
		IntentFilter intentFilter = new IntentFilter();
		for (String action : ACTIONS) {
			intentFilter.addAction(action);
		}
		intentFilter.addDataScheme("package");
		return intentFilter;
	}

	@Nullable
	private static String agencyProviderMetaData;

	@NonNull
	private static String getAgencyProviderMetaData(@NonNull Context context) {
		if (agencyProviderMetaData == null) {
			agencyProviderMetaData = context.getString(org.mtransit.android.commons.R.string.agency_provider);
		}
		return agencyProviderMetaData;
	}

	@Inject
	CrashReporter crashReporter;
	@Inject
	DataSourcesRepository dataSourcesRepository;

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
		final boolean isAProvider = this.dataSourcesRepository.isAProvider(pkg, true);
		if (isAProvider && shouldPing(intent)) {
			if (ping(context, pkg)) { // TODO check if a GTFS provider?
				MTLog.i(this, "Received broadcast %s for %s.", action, pkg);
			}
		}
		final boolean canBeAProvider = isAProvider
				|| Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)
				|| Intent.ACTION_PACKAGE_REMOVED.equals(action);
		if (!canBeAProvider) {
			MTLog.d(this, "onReceive() > SKIP (can NOT be a provider: pkg:%s, action:%s)", pkg, action);
			return;
		}
		try {
			this.dataSourcesRepository.updateLock(new MTContinuationJ<Boolean>() {

				@NonNull
				@Override
				public CoroutineContext getContext() {
					return EmptyCoroutineContext.INSTANCE;
				}

				@Override
				public void resumeWithException(@NonNull Throwable t) {
					MTLog.w(ModulesReceiver.this, t, "Error while running update...");
				}

				@Override
				public void resume(Boolean result) {
					MTLog.d(ModulesReceiver.this, "Update run with result: %s", result);
				}
			});
		} catch (Exception e) {
			MTLog.w(this, e, "Error while updating data-sources from repository!");
		}
	}

	private boolean ping(@NonNull Context context, @Nullable String pkg) {
		final ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg);
		if (providers != null) {
			final String agencyProviderMetaData = getAgencyProviderMetaData(context);
			for (ProviderInfo provider : providers) {
				if (provider != null && provider.metaData != null) {
					if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
						final PendingResult pendingResult = goAsync();
						backgroundExecutor.execute(() -> {
							try {
								MTLog.i(this, "Ping: %s", provider.authority);
								DataSourceManager.ping(context, provider.authority);
							} finally {
								pendingResult.finish(); // Must call finish() so the BroadcastReceiver can be recycled
							}
						});
						return true;
					}
				}
			}
		}
		return false;
	}
}
