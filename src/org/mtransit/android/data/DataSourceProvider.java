package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.task.MTAsyncTask;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.AsyncTask;

public class DataSourceProvider implements MTLog.Loggable {

	private static final String TAG = DataSourceProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static DataSourceProvider instance = null;

	private List<AgencyProperties> allAgencies = null;

	private List<StatusProviderProperties> allStatusProviders = null;

	private List<ScheduleProviderProperties> allScheduleProviders = null;

	private List<DataSourceType> allAgencyTypes = null;

	private HashMap<Integer, List<AgencyProperties>> allAgenciesByTypeId = null;

	private HashMap<String, AgencyProperties> allAgenciesByAuthority = null;

	private HashMap<String, Set<StatusProviderProperties>> statusProvidersByTargetAuthority = null;

	private HashMap<String, Set<ScheduleProviderProperties>> scheduleProvidersByTargetAuthority = null;

	private HashMap<String, JPaths> rtsRouteLogoByAuthority = null;

	private HashMap<String, Uri> uriMap = new HashMap<String, Uri>();

	public static DataSourceProvider get() {
		if (instance == null) {
			instance = new DataSourceProvider();
		}
		return instance;
	}

	public static boolean isSet() {
		return instance != null;
	}

	public static void destroy() {
		if (instance != null) {
			instance.onDestroy();
			instance = null;
		}
	}

	public static boolean reset(Context context) {
		if (instance != null) {
			if (context == null) { // cannot compare w/o context
				destroy();
				triggerModulesUpdated();
				return true;
			} else {
				final DataSourceProvider newInstance = new DataSourceProvider();
				newInstance.init(context);
				if (CollectionUtils.getSize(newInstance.allAgencies) != CollectionUtils.getSize(instance.allAgencies)
						|| CollectionUtils.getSize(newInstance.allStatusProviders) != CollectionUtils.getSize(instance.allStatusProviders)
						|| CollectionUtils.getSize(newInstance.allScheduleProviders) != CollectionUtils.getSize(instance.allScheduleProviders)) {
					instance = newInstance;
					triggerModulesUpdated();
					return true;
				}
			}
		}
		return false;
	}

	private DataSourceProvider() {
	}

	public Uri getUri(String authority) {
		Uri uri = uriMap.get(authority);
		if (uri == null) {
			uri = UriUtils.newContentUri(authority);
			uriMap.put(authority, uri);
		}
		return uri;
	}

	public List<DataSourceType> getAvailableAgencyTypes(Context context) {
		if (this.allAgencyTypes == null) {
			init(context);
		}
		return this.allAgencyTypes;
	}

	public List<AgencyProperties> getAllAgencies(Context context) {
		if (this.allAgencies == null) {
			init(context);
		}
		return this.allAgencies;
	}

	public List<AgencyProperties> getTypeDataSources(Context context, int typeId) {
		if (this.allAgenciesByTypeId == null) {
			init(context);
		}
		return this.allAgenciesByTypeId.get(typeId);
	}

	public AgencyProperties getAgency(Context context, String authority) {
		if (this.allAgenciesByAuthority == null) {
			init(context);
		}
		return this.allAgenciesByAuthority.get(authority);
	}

	public Collection<StatusProviderProperties> getTargetAuthorityStatusProviders(Context context, String targetAuthority) {
		if (this.statusProvidersByTargetAuthority == null) {
			init(context);
		}
		return this.statusProvidersByTargetAuthority.get(targetAuthority);
	}

	public Collection<ScheduleProviderProperties> getTargetAuthorityScheduleProviders(Context context, String targetAuthority) {
		if (this.scheduleProvidersByTargetAuthority == null) {
			init(context);
		}
		return this.scheduleProvidersByTargetAuthority.get(targetAuthority);
	}

	public JPaths getRTSRouteLogo(Context context, String authority) {
		if (this.rtsRouteLogoByAuthority == null) {
			init(context);
		}
		return this.rtsRouteLogoByAuthority.get(authority);
	}

	public void onDestroy() {
		if (this.allAgencies != null) {
			this.allAgencies.clear();
			this.allAgencies = null;
		}
		if (this.allAgencyTypes != null) {
			this.allAgencyTypes.clear();
			this.allAgencyTypes = null;
		}
		if (this.allAgenciesByTypeId != null) {
			this.allAgenciesByTypeId.clear();
			this.allAgenciesByTypeId = null;
		}
		if (this.allAgenciesByAuthority != null) {
			this.allAgenciesByAuthority.clear();
			this.allAgenciesByAuthority = null;
		}
		if (this.allStatusProviders != null) {
			this.allStatusProviders.clear();
			this.allStatusProviders = null;
		}
		if (this.statusProvidersByTargetAuthority != null) {
			this.statusProvidersByTargetAuthority.clear();
			this.statusProvidersByTargetAuthority = null;
		}
		if (this.rtsRouteLogoByAuthority != null) {
			this.rtsRouteLogoByAuthority.clear();
			this.rtsRouteLogoByAuthority = null;
		}
		if (this.allScheduleProviders != null) {
			this.allScheduleProviders.clear();
			this.allScheduleProviders = null;
		}
		if (this.scheduleProvidersByTargetAuthority != null) {
			this.scheduleProvidersByTargetAuthority.clear();
			this.scheduleProvidersByTargetAuthority = null;
		}
	}

	private synchronized void init(Context context) {
		this.allAgencies = new ArrayList<AgencyProperties>();
		this.allAgencyTypes = new ArrayList<DataSourceType>();
		this.allAgenciesByTypeId = new HashMap<Integer, List<AgencyProperties>>();
		this.allAgenciesByAuthority = new HashMap<String, AgencyProperties>();
		this.allStatusProviders = new ArrayList<StatusProviderProperties>();
		this.statusProvidersByTargetAuthority = new HashMap<String, Set<StatusProviderProperties>>();
		this.rtsRouteLogoByAuthority = new HashMap<String, JPaths>();
		this.allScheduleProviders = new ArrayList<ScheduleProviderProperties>();
		this.scheduleProvidersByTargetAuthority = new HashMap<String, Set<ScheduleProviderProperties>>();
		String agencyProviderMetaData = context.getString(R.string.agency_provider);
		String rtsProviderMetaData = context.getString(R.string.rts_provider);
		String scheduleProviderMetaData = context.getString(R.string.schedule_provider);
		String statusProviderMetaData = context.getString(R.string.status_provider);
		String statusProviderTargetMetaData = context.getString(R.string.status_provider_target);
		String scheduleProviderTargetMetaData = context.getString(R.string.schedule_provider_target);
		final PackageManager pm = context.getPackageManager();
		for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
			ProviderInfo[] providers = packageInfo.providers;
			if (providers != null) {
				for (ProviderInfo provider : providers) {
					if (provider.metaData != null) {
						if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
							final Uri contentUri = getUri(provider.authority);
							final String label = DataSourceManager.findAgencyLabel(context, contentUri);
							final String shortName = DataSourceManager.findAgencyShortName(context, contentUri);
							final int typeId = DataSourceManager.findTypeId(context, contentUri);
							final DataSourceType type = DataSourceType.parseId(typeId);
							final Area area = DataSourceManager.findAgencyArea(context, contentUri);
							final boolean isRTS = rtsProviderMetaData.equals(provider.metaData.getString(rtsProviderMetaData));
							final JPaths jPath = isRTS ? DataSourceManager.findAgencyRTSRouteLogo(context, contentUri) : null;
							if (type != null && typeId >= 0) {
								final AgencyProperties newAgency = new AgencyProperties(provider.authority, type, shortName, label, area, isRTS);
								addNewAgency(newAgency);
								if (jPath != null) {
									this.rtsRouteLogoByAuthority.put(newAgency.getAuthority(), jPath);
								}
							} else {
								MTLog.d(this, "Invalid type, skipping agency provider.");
							}
						}
						if (statusProviderMetaData.equals(provider.metaData.getString(statusProviderMetaData))) {
							String targetAuthority = provider.metaData.getString(statusProviderTargetMetaData);
							StatusProviderProperties newStatusProvider = new StatusProviderProperties(provider.authority, targetAuthority);
							addNewStatusProvider(newStatusProvider);
						}
						if (scheduleProviderMetaData.equals(provider.metaData.getString(scheduleProviderMetaData))) {
							String targetAuthority = provider.metaData.getString(scheduleProviderTargetMetaData);
							ScheduleProviderProperties newScheduleProvider = new ScheduleProviderProperties(provider.authority, targetAuthority);
							addNewScheduleProvider(newScheduleProvider);
						}
					}
				}
			}
		}
		CollectionUtils.sort(this.allAgencyTypes, new DataSourceType.DataSourceTypeShortNameComparator(context));
		CollectionUtils.sort(this.allAgencies, AgencyProperties.SHORT_NAME_COMPARATOR);

		if (this.allAgenciesByTypeId != null) {
			for (Integer typeId : this.allAgenciesByTypeId.keySet()) {
				CollectionUtils.sort(this.allAgenciesByTypeId.get(typeId), AgencyProperties.SHORT_NAME_COMPARATOR);
			}
		}
	}

	private void addNewStatusProvider(StatusProviderProperties newStatusProvider) {
		this.allStatusProviders.add(newStatusProvider);
		String newScheduleProviderTargetAuthority = newStatusProvider.getTargetAuthority();
		if (!this.statusProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
			this.statusProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<StatusProviderProperties>());
		}
		this.statusProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newStatusProvider);
	}

	private void addNewScheduleProvider(ScheduleProviderProperties newScheduleProvider) {
		this.allScheduleProviders.add(newScheduleProvider);
		String newScheduleProviderTargetAuthority = newScheduleProvider.getTargetAuthority();
		if (!this.scheduleProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
			this.scheduleProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<ScheduleProviderProperties>());
		}
		this.scheduleProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newScheduleProvider);
	}

	private void addNewAgency(AgencyProperties newAgency) {
		this.allAgencies.add(newAgency);
		this.allAgenciesByAuthority.put(newAgency.getAuthority(), newAgency);
		DataSourceType newAgencyType = newAgency.getType();
		if (!this.allAgencyTypes.contains(newAgencyType)) {
			this.allAgencyTypes.add(newAgencyType);
		}
		if (!this.allAgenciesByTypeId.containsKey(newAgencyType.getId())) {
			this.allAgenciesByTypeId.put(newAgencyType.getId(), new ArrayList<AgencyProperties>());
		}
		this.allAgenciesByTypeId.get(newAgencyType.getId()).add(newAgency);
	}

	private static WeakHashMap<ModulesUpdateListener, Object> modulesUpdateListeners = new WeakHashMap<ModulesUpdateListener, Object>();

	public static void addModulesUpdateListerner(ModulesUpdateListener listener) {
		try {
			if (!modulesUpdateListeners.containsKey(listener)) {
				modulesUpdateListeners.put(listener, null);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "addModulesUpdateListerner() > error while adding listerner '%s'!", listener);
		}
	}

	public static void removeModulesUpdateListerner(ModulesUpdateListener listener) {
		try {
			if (modulesUpdateListeners.containsKey(listener)) {
				modulesUpdateListeners.remove(listener);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "removeModulesUpdateListerner() > error while removing listerner '%s'!", listener);
		}
	}

	private static TriggerModulesUpdatedTask triggerModulesUpdatedTask = null;

	public static void triggerModulesUpdated() {
		if (modulesUpdateListeners != null) {
			if (triggerModulesUpdatedTask != null && triggerModulesUpdatedTask.getStatus() != AsyncTask.Status.RUNNING) {
				triggerModulesUpdatedTask.cancel(true);
			}
			final WeakHashMap<ModulesUpdateListener, Object> listenersCopy = new WeakHashMap<ModulesUpdateListener, Object>(modulesUpdateListeners);
			triggerModulesUpdatedTask = new TriggerModulesUpdatedTask(listenersCopy);
			triggerModulesUpdatedTask.execute();
		}
	}

	public static interface ModulesUpdateListener {
		public void onModulesUpdated();
	}

	private static class TriggerModulesUpdatedTask extends MTAsyncTask<Void, ModulesUpdateListener, Void> {

		private static final String TAG = TriggerModulesUpdatedTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakHashMap<ModulesUpdateListener, Object> listerners;

		public TriggerModulesUpdatedTask(WeakHashMap<ModulesUpdateListener, Object> listeners) {
			this.listerners = listeners;
		}

		@Override
		protected Void doInBackgroundMT(Void... params) {
			if (this.listerners != null) {
				Iterator<ModulesUpdateListener> it = this.listerners.keySet().iterator();
				while (it.hasNext()) {
					ModulesUpdateListener listener = it.next();
					if (listener != null) {
						publishProgress(listener);
					}
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(ModulesUpdateListener... values) {
			if (values != null && values.length > 0) {
				values[0].onModulesUpdated();
			}
		}
	}
}
