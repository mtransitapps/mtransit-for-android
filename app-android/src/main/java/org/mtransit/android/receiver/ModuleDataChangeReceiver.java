package org.mtransit.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.receiver.DataChange;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.future.FutureKt;

/*
 TO TEST: REMOVE PERMISSION FROM MANIFEST
 adb shell am broadcast -a org.mtransit.android.intent.action.DATA_CHANGE \
 -n org.mtransit.android.debug/org.mtransit.android.receiver.ModuleDataChangeReceiver \
 --ez force true \
 --es pkg "org.mtransit.android.ca_montreal_stm_subway.debug"
 */
@AndroidEntryPoint
public class ModuleDataChangeReceiver extends BroadcastReceiver implements MTLog.Loggable {

	private static final String LOG_TAG = ModuleDataChangeReceiver.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Inject
	CrashReporter crashReporter;
	@Inject
	DataSourcesRepository dataSourcesRepository;

	@Override
	public void onReceive(@Nullable Context context, @Nullable Intent intent) {
		if (context == null) {
			this.crashReporter.w(this, "Modules data change broadcast receiver with null context ignored!");
			return;
		}
		final String action = intent == null ? null : intent.getAction();
		if (!DataChange.ACTION_DATA_CHANGE.equals(action)) {
			this.crashReporter.shouldNotHappen("Wrong receiver action '%s'!", action);
			return;
		}
		MTLog.i(this, "Broadcast received: %s", action);
		Bundle extras = intent.getExtras();
		final boolean forceReset = extras != null && extras.getBoolean(DataChange.FORCE, false);
		final String pkg = extras == null ? null : extras.getString(DataChange.PKG, null);
		try {
			FutureKt.future(
					CoroutineScopeKt.CoroutineScope(EmptyCoroutineContext.INSTANCE),
					Dispatchers.getIO(),
					CoroutineStart.DEFAULT,
					(scope, continuation) ->
							this.dataSourcesRepository.updateLock(forceReset ? pkg : null, continuation)
			).get();
		} catch (Exception e) {
			MTLog.w(this, e, "Error while updating data-sources from repository!");
		}
	}
}
