package org.mtransit.android.receiver;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;

public class ModulesReceiver extends BroadcastReceiver implements MTLog.Loggable {

	private static final String TAG = ModulesReceiver.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String pkg = intent.getData().getSchemeSpecificPart();
		if (DataSourceProvider.isSet()) {
			if (DataSourceProvider.isProvider(context, pkg)) {
				DataSourceProvider.resetIfNecessary(context);
			}
		} else {
			ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg);
			if (providers != null) {
				String agencyProviderMetaData = DataSourceProvider.getAgencyProviderMetaData(context);
				for (ProviderInfo provider : providers) {
					if (provider.metaData != null) {
						if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
							DataSourceManager.ping(context, provider.authority);
						}
					}
				}
			}
		}
	}

}
