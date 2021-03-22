package org.mtransit.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.receiver.DataChange;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.di.Injection;

import static org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES;

public class ModuleDataChangeReceiver extends BroadcastReceiver implements MTLog.Loggable {

	private static final String LOG_TAG = ModuleDataChangeReceiver.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final CrashReporter crashReporter;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public ModuleDataChangeReceiver() {
		super();
		this.crashReporter = Injection.providesCrashReporter();
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	@Override
	public void onReceive(@Nullable Context context, @Nullable Intent intent) {
		if (context == null) {
			this.crashReporter.w(this, "Modules data change broadcast receiver with null context ignored!");
			return;
		}
		String action = intent == null ? null : intent.getAction();
		if (!DataChange.ACTION_DATA_CHANGE.equals(action)) {
			crashReporter.shouldNotHappen("Wrong receiver action '%s'!", action);
			return;
		}
		MTLog.i(this, "Broadcast received: %s", action);
		Bundle extras = intent.getExtras();
		boolean forceReset = extras != null && extras.getBoolean(DataChange.FORCE, false);
		boolean repositoryUpdateTriggered = false;
		if (forceReset) {
			if (DataSourceProvider.isSet()) {
				DataSourceProvider.triggerModulesUpdated();
			}
		} else {
			repositoryUpdateTriggered = DataSourceProvider.resetIfNecessary(context);
		}
		if (F_CACHE_DATA_SOURCES) {
			if (!repositoryUpdateTriggered) { // DataSourceProvider already called method
				if (DataSourceProvider.isSet()) {
					DataSourceProvider.get().updateFromDataSourceRepository(false);
				} else { // ELSE update cache for latter
					try {
						this.dataSourcesRepository.updateAsync().get(); // TODO ? filter by pkg? authority?
					} catch (Exception e) {
						MTLog.w(this, e, "Error while updating data-sources from repository!");
					}
				}
			}
		}
	}
}
