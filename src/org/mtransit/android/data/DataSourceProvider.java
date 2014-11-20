package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import android.util.SparseArray;

public class DataSourceProvider implements MTLog.Loggable {

	private static final String TAG = DataSourceProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static DataSourceProvider instance = null;


	private ArrayList<DataSourceType> allAgencyTypes = null;

	private ArrayList<AgencyProperties> allAgencies = null;

	private ArrayList<StatusProviderProperties> allStatusProviders = null;

	private ArrayList<ScheduleProviderProperties> allScheduleProviders = null;

	private ArrayList<ServiceUpdateProviderProperties> allServiceUpdateProviders = null;

	private SparseArray<ArrayList<AgencyProperties>> allAgenciesByTypeId = null;

	private HashMap<String, AgencyProperties> allAgenciesByAuthority = null;

	private HashMap<String, HashSet<StatusProviderProperties>> statusProvidersByTargetAuthority = null;

	private HashMap<String, HashSet<ScheduleProviderProperties>> scheduleProvidersByTargetAuthority = null;

	private HashMap<String, HashSet<ServiceUpdateProviderProperties>> serviceUpdateProvidersByTargetAuthority = null;

	private HashMap<String, JPaths> rtsRouteLogoByAuthority = null;

	private HashMap<String, Uri> uriMap = new HashMap<String, Uri>();

	public static DataSourceProvider get(Context context) {
		if (instance == null) {
			initInstance(context);
		}
		return instance;
	}

	private synchronized static void initInstance(Context context) {
		if (instance != null) {
			return;
		}
		if (context == null) {
			throw new IllegalArgumentException("Cannot instantiate provider w/o context!");
		}
		final DataSourceProvider newInstance = new DataSourceProvider();
		newInstance.init(context);
		instance = newInstance;
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

	public static boolean reset(Context optContext) {
		if (instance != null) {
			if (optContext == null) { // cannot compare w/o context
				destroy();
				triggerModulesUpdated();
				return true;
			} else {
				final DataSourceProvider newInstance = new DataSourceProvider();
				newInstance.init(optContext);
				if (areDifferents(instance, newInstance)) {
					instance = newInstance;
					triggerModulesUpdated();
					return true;
				}
			}
		}
		return false;
	}

	private static boolean areDifferents(DataSourceProvider dsp1, DataSourceProvider dsp2) {
		return CollectionUtils.getSize(dsp1.allAgencyTypes) != CollectionUtils.getSize(dsp2.allAgencyTypes)
				|| CollectionUtils.getSize(dsp1.allAgencies) != CollectionUtils.getSize(dsp2.allAgencies)
				|| CollectionUtils.getSize(dsp1.allStatusProviders) != CollectionUtils.getSize(dsp2.allStatusProviders)
				|| CollectionUtils.getSize(dsp1.allScheduleProviders) != CollectionUtils.getSize(dsp2.allScheduleProviders)
				|| CollectionUtils.getSize(dsp1.allServiceUpdateProviders) != CollectionUtils.getSize(dsp2.allServiceUpdateProviders);
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

	public ArrayList<DataSourceType> getAvailableAgencyTypes() {
		return this.allAgencyTypes == null ? null : new ArrayList<DataSourceType>(this.allAgencyTypes);
	}

	public ArrayList<AgencyProperties> getAllAgencies() {
		return this.allAgencies == null ? null : new ArrayList<AgencyProperties>(this.allAgencies);
	}

	public ArrayList<AgencyProperties> getTypeDataSources(int typeId) {
		return this.allAgenciesByTypeId.get(typeId);
	}

	public AgencyProperties getAgency(String authority) {
		return this.allAgenciesByAuthority.get(authority);
	}

	public Collection<StatusProviderProperties> getTargetAuthorityStatusProviders(String targetAuthority) {
		return this.statusProvidersByTargetAuthority.get(targetAuthority);
	}

	public Collection<ScheduleProviderProperties> getTargetAuthorityScheduleProviders(String targetAuthority) {
		return this.scheduleProvidersByTargetAuthority.get(targetAuthority);
	}

	public JPaths getRTSRouteLogo(String authority) {
		return this.rtsRouteLogoByAuthority.get(authority);
	}

	public Collection<ServiceUpdateProviderProperties> getTargetAuthorityServiceUpdateProviders(String targetAuthority) {
		return this.serviceUpdateProvidersByTargetAuthority.get(targetAuthority);
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
		if (this.allServiceUpdateProviders != null) {
			this.allServiceUpdateProviders.clear();
			this.allServiceUpdateProviders = null;
		}
		if (this.serviceUpdateProvidersByTargetAuthority != null) {
			this.serviceUpdateProvidersByTargetAuthority.clear();
			this.serviceUpdateProvidersByTargetAuthority = null;
		}
	}

	private synchronized void init(Context context) {
		this.allAgencies = new ArrayList<AgencyProperties>();
		this.allAgencyTypes = new ArrayList<DataSourceType>();
		this.allAgenciesByTypeId = new SparseArray<ArrayList<AgencyProperties>>();
		this.allAgenciesByAuthority = new HashMap<String, AgencyProperties>();
		this.allStatusProviders = new ArrayList<StatusProviderProperties>();
		this.statusProvidersByTargetAuthority = new HashMap<String, HashSet<StatusProviderProperties>>();
		this.rtsRouteLogoByAuthority = new HashMap<String, JPaths>();
		this.allScheduleProviders = new ArrayList<ScheduleProviderProperties>();
		this.scheduleProvidersByTargetAuthority = new HashMap<String, HashSet<ScheduleProviderProperties>>();
		this.allServiceUpdateProviders = new ArrayList<ServiceUpdateProviderProperties>();
		this.serviceUpdateProvidersByTargetAuthority = new HashMap<String, HashSet<ServiceUpdateProviderProperties>>();
		String agencyProviderMetaData = context.getString(R.string.agency_provider);
		String rtsProviderMetaData = context.getString(R.string.rts_provider);
		String scheduleProviderMetaData = context.getString(R.string.schedule_provider);
		String statusProviderMetaData = context.getString(R.string.status_provider);
		String serviceUpdateProviderMetaData = context.getString(R.string.service_update_provider);
		String statusProviderTargetMetaData = context.getString(R.string.status_provider_target);
		String scheduleProviderTargetMetaData = context.getString(R.string.schedule_provider_target);
		String serviceUpdateProviderTargetMetaData = context.getString(R.string.service_update_provider_target);
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
						if (serviceUpdateProviderMetaData.equals(provider.metaData.getString(serviceUpdateProviderMetaData))) {
							String targetAuthority = provider.metaData.getString(serviceUpdateProviderTargetMetaData);
							ServiceUpdateProviderProperties newServiceUpdateProvider = new ServiceUpdateProviderProperties(provider.authority, targetAuthority);
							addNewServiceUpdateProvider(newServiceUpdateProvider);
						}
					}
				}
			}
		}
		CollectionUtils.sort(this.allAgencyTypes, new DataSourceType.DataSourceTypeShortNameComparator(context));
		CollectionUtils.sort(this.allAgencies, AgencyProperties.SHORT_NAME_COMPARATOR);

		if (this.allAgenciesByTypeId != null) {
			for (int i = 0; i < this.allAgenciesByTypeId.size(); i++) {
				final int typeId = this.allAgenciesByTypeId.keyAt(i);
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

	private void addNewServiceUpdateProvider(ServiceUpdateProviderProperties newServiceUpdateProvider) {
		this.allServiceUpdateProviders.add(newServiceUpdateProvider);
		String newServiceUpdateProviderTargetAuthority = newServiceUpdateProvider.getTargetAuthority();
		if (!this.serviceUpdateProvidersByTargetAuthority.containsKey(newServiceUpdateProviderTargetAuthority)) {
			this.serviceUpdateProvidersByTargetAuthority.put(newServiceUpdateProviderTargetAuthority, new HashSet<ServiceUpdateProviderProperties>());
		}
		this.serviceUpdateProvidersByTargetAuthority.get(newServiceUpdateProviderTargetAuthority).add(newServiceUpdateProvider);
	}

	private void addNewAgency(AgencyProperties newAgency) {
		this.allAgencies.add(newAgency);
		this.allAgenciesByAuthority.put(newAgency.getAuthority(), newAgency);
		DataSourceType newAgencyType = newAgency.getType();
		if (!this.allAgencyTypes.contains(newAgencyType)) {
			this.allAgencyTypes.add(newAgencyType);
		}
		if (this.allAgenciesByTypeId.get(newAgencyType.getId()) == null) {
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
