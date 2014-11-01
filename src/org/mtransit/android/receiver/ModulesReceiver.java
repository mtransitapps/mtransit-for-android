package org.mtransit.android.receiver;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.DataSourceProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ModulesReceiver extends BroadcastReceiver implements MTLog.Loggable {

	private static final String TAG = ModulesReceiver.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (DataSourceProvider.isSet()) {
			DataSourceProvider.reset(context);
		}
	}
}
