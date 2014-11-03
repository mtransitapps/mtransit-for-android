package org.mtransit.android.receiver;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.data.DataSourceProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

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
		} else {
			final String pkg = intent.getData().getSchemeSpecificPart();
			final ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg);
			if (providers != null) {
				final String agencyProviderMetaData = context.getString(R.string.agency_provider);
				for (ProviderInfo provider : providers) {
					if (provider.metaData != null) {
						if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
							ping(context, provider);
						}
					}
				}
			}
		}
	}

	private void ping(Context context, ProviderInfo provider) {
		final Uri contentUri = UriUtils.newContentUri(provider.authority);
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(contentUri, "ping");
			cursor = context.getContentResolver().query(uri, null, null, null, null);
		} catch (Throwable t) {
			MTLog.w(this, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
