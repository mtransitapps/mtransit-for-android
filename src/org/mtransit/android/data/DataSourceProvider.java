package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.task.MTAsyncTask;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.AsyncTask;
import android.util.SparseArray;

public class DataSourceProvider implements MTLog.Loggable {

	private static final String TAG = DataSourceProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static DataSourceProvider instance = null;


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
		DataSourceProvider newInstance = new DataSourceProvider();
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

	public static boolean resetIfNecessary(Context optContext) {
		if (instance != null) {
			if (optContext == null) { // cannot compare w/o context
				destroy();
				triggerModulesUpdated();
				return true;
			} else {
				if (hasChanged(instance, optContext)) {
					destroy();
					triggerModulesUpdated();
					return true;
				}
			}
		}
		return false;
	}

	private synchronized static boolean hasChanged(DataSourceProvider current, Context optContext) {
		if (current == null) {
			return true;
		}
		if (optContext == null) {
			return true;
		}
		String agencyProviderMetaData = optContext.getString(R.string.agency_provider);
		String scheduleProviderMetaData = optContext.getString(R.string.schedule_provider);
		String statusProviderMetaData = optContext.getString(R.string.status_provider);
		String serviceUpdateProviderMetaData = optContext.getString(R.string.service_update_provider);
		int nbAgencyProviders = 0, nbScheduleProviders = 0, nbStatusProviders = 0, nbServiceUpdateProviders = 0;
		PackageManager pm = optContext.getPackageManager();
		for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
			ProviderInfo[] providers = packageInfo.providers;
			if (providers != null) {
				for (ProviderInfo provider : providers) {
					if (provider.metaData != null) {
						if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
							if (instance.getAgency(provider.authority) == null) {
								return true;
							}
							nbAgencyProviders++;
						}
						if (statusProviderMetaData.equals(provider.metaData.getString(statusProviderMetaData))) {
							if (instance.getStatusProvider(provider.authority) == null) {
								return true;
							}
							nbStatusProviders++;
						}
						if (scheduleProviderMetaData.equals(provider.metaData.getString(scheduleProviderMetaData))) {
							if (instance.getScheduleProvider(provider.authority) == null) {
								return true;
							}
							nbScheduleProviders++;
						}
						if (serviceUpdateProviderMetaData.equals(provider.metaData.getString(serviceUpdateProviderMetaData))) {
							if (instance.getServiceUpdateProvider(provider.authority) == null) {
								return true;
							}
							nbServiceUpdateProviders++;
						}
					}
				}
			}
		}
		if (nbAgencyProviders != CollectionUtils.getSize(instance.allAgencies) //
				|| nbStatusProviders != CollectionUtils.getSize(instance.allStatusProviders) //
				|| nbScheduleProviders != CollectionUtils.getSize(instance.allScheduleProviders) //
				|| nbServiceUpdateProviders != CollectionUtils.getSize(instance.allServiceUpdateProviders)) {
			return true;
		}
		return false;
	}

	private ArrayList<DataSourceType> allAgencyTypes = new ArrayList<DataSourceType>();
	private ArrayList<AgencyProperties> allAgencies = new ArrayList<AgencyProperties>();
	private ArrayList<StatusProviderProperties> allStatusProviders = new ArrayList<StatusProviderProperties>();
	private ArrayList<ScheduleProviderProperties> allScheduleProviders = new ArrayList<ScheduleProviderProperties>();
	private ArrayList<ServiceUpdateProviderProperties> allServiceUpdateProviders = new ArrayList<ServiceUpdateProviderProperties>();
	private HashMap<String, AgencyProperties> allAgenciesByAuthority = new HashMap<String, AgencyProperties>();
	private HashMap<String, StatusProviderProperties> allStatusProvidersByAuthority = new HashMap<String, StatusProviderProperties>();
	private HashMap<String, ScheduleProviderProperties> allScheduleProvidersByAuthority = new HashMap<String, ScheduleProviderProperties>();
	private HashMap<String, ServiceUpdateProviderProperties> allServiceUpdateProvidersByAuthority = new HashMap<String, ServiceUpdateProviderProperties>();
	private SparseArray<ArrayList<AgencyProperties>> allAgenciesByTypeId = new SparseArray<ArrayList<AgencyProperties>>();
	private HashMap<String, HashSet<StatusProviderProperties>> statusProvidersByTargetAuthority = new HashMap<String, HashSet<StatusProviderProperties>>();
	private HashMap<String, HashSet<ScheduleProviderProperties>> scheduleProvidersByTargetAuthority = new HashMap<String, HashSet<ScheduleProviderProperties>>();
	private HashMap<String, HashSet<ServiceUpdateProviderProperties>> serviceUpdateProvidersByTargetAuthority = new HashMap<String, HashSet<ServiceUpdateProviderProperties>>();
	private HashMap<String, JPaths> rtsRouteLogoByAuthority = new HashMap<String, JPaths>();
	private DataSourceProvider() {
	}


	public ArrayList<DataSourceType> getAvailableAgencyTypes() {
		if (this.allAgencyTypes == null) {
			return null;
		}
		return new ArrayList<DataSourceType>(this.allAgencyTypes); // copy
	}

	public ArrayList<AgencyProperties> getAllAgencies() {
		if (this.allAgencies == null) {
			return null;
		}
		return new ArrayList<AgencyProperties>(this.allAgencies); // copy
	}

	public AgencyProperties getAgency(String authority) {
		if (this.allAgenciesByAuthority == null) {
			return null;
		}
		return this.allAgenciesByAuthority.get(authority);
	}

	public StatusProviderProperties getStatusProvider(String authority) {
		if (this.allStatusProvidersByAuthority == null) {
			return null;
		}
		return this.allStatusProvidersByAuthority.get(authority);
	}

	public ScheduleProviderProperties getScheduleProvider(String authority) {
		if (this.allScheduleProvidersByAuthority == null) {
			return null;
		}
		return this.allScheduleProvidersByAuthority.get(authority);
	}

	public ServiceUpdateProviderProperties getServiceUpdateProvider(String authority) {
		if (this.allServiceUpdateProvidersByAuthority == null) {
			return null;
		}
		return this.allServiceUpdateProvidersByAuthority.get(authority);
	}

	public ArrayList<AgencyProperties> getTypeDataSources(int typeId) {
		ArrayList<AgencyProperties> agencies = this.allAgenciesByTypeId == null ? null : this.allAgenciesByTypeId.get(typeId);
		if (agencies == null) {
			return null;
		}
		return new ArrayList<AgencyProperties>(agencies); // copy
	}

	public HashSet<StatusProviderProperties> getTargetAuthorityStatusProviders(String targetAuthority) {
		HashSet<StatusProviderProperties> statusProviders = this.statusProvidersByTargetAuthority == null ? null : this.statusProvidersByTargetAuthority
				.get(targetAuthority);
		if (statusProviders == null) {
			return null;
		}
		return new HashSet<StatusProviderProperties>(statusProviders); // copy
	}

	public HashSet<ScheduleProviderProperties> getTargetAuthorityScheduleProviders(String targetAuthority) {
		HashSet<ScheduleProviderProperties> scheduleProviders = this.scheduleProvidersByTargetAuthority == null ? null
				: this.scheduleProvidersByTargetAuthority.get(targetAuthority);
		if (scheduleProviders == null) {
			return null;
		}
		return new HashSet<ScheduleProviderProperties>(scheduleProviders); // copy
	}

	public HashSet<ServiceUpdateProviderProperties> getTargetAuthorityServiceUpdateProviders(String targetAuthority) {
		HashSet<ServiceUpdateProviderProperties> serviceUpdateProviders = this.serviceUpdateProvidersByTargetAuthority == null ? null
				: this.serviceUpdateProvidersByTargetAuthority.get(targetAuthority);
		if (serviceUpdateProviders == null) {
			return null;
		}
		return new HashSet<ServiceUpdateProviderProperties>(this.serviceUpdateProvidersByTargetAuthority.get(targetAuthority)); // copy
	}

	public JPaths getRTSRouteLogo(String authority) {
		if (this.rtsRouteLogoByAuthority == null) {
			return null;
		}
		return this.rtsRouteLogoByAuthority.get(authority);
	}

	public void onDestroy() {
		if (this.allAgencyTypes != null) {
			this.allAgencyTypes.clear();
		}
		if (this.allAgencies != null) {
			this.allAgencies.clear();
		}
		if (this.allAgenciesByTypeId != null) {
			this.allAgenciesByTypeId.clear();
		}
		if (this.allAgenciesByAuthority != null) {
			this.allAgenciesByAuthority.clear();
		}
		if (this.allStatusProviders != null) {
			this.allStatusProviders.clear();
		}
		if (this.allStatusProvidersByAuthority != null) {
			this.allStatusProvidersByAuthority.clear();
		}
		if (this.statusProvidersByTargetAuthority != null) {
			this.statusProvidersByTargetAuthority.clear();
		}
		if (this.rtsRouteLogoByAuthority != null) {
			this.rtsRouteLogoByAuthority.clear();
		}
		if (this.allScheduleProviders != null) {
			this.allScheduleProviders.clear();
		}
		if (this.allScheduleProvidersByAuthority != null) {
			this.allScheduleProvidersByAuthority.clear();
		}
		if (this.scheduleProvidersByTargetAuthority != null) {
			this.scheduleProvidersByTargetAuthority.clear();
		}
		if (this.allServiceUpdateProviders != null) {
			this.allServiceUpdateProviders.clear();
		}
		if (this.allServiceUpdateProvidersByAuthority != null) {
			this.allServiceUpdateProvidersByAuthority.clear();
		}
		if (this.serviceUpdateProvidersByTargetAuthority != null) {
			this.serviceUpdateProvidersByTargetAuthority.clear();
		}
	}

	private synchronized void init(Context context) {
		String agencyProviderMetaData = context.getString(R.string.agency_provider);
		String rtsProviderMetaData = context.getString(R.string.rts_provider);
		String scheduleProviderMetaData = context.getString(R.string.schedule_provider);
		String statusProviderMetaData = context.getString(R.string.status_provider);
		String serviceUpdateProviderMetaData = context.getString(R.string.service_update_provider);
		String statusProviderTargetMetaData = context.getString(R.string.status_provider_target);
		String scheduleProviderTargetMetaData = context.getString(R.string.schedule_provider_target);
		String serviceUpdateProviderTargetMetaData = context.getString(R.string.service_update_provider_target);
		PackageManager pm = context.getPackageManager();
		for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
			ProviderInfo[] providers = packageInfo.providers;
			if (providers != null) {
				for (ProviderInfo provider : providers) {
					if (provider.metaData != null) {
						if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
							String label = DataSourceManager.findAgencyLabel(context, provider.authority);
							String color = DataSourceManager.findAgencyColor(context, provider.authority);
							String shortName = DataSourceManager.findAgencyShortName(context, provider.authority);
							int typeId = DataSourceManager.findTypeId(context, provider.authority);
							DataSourceType type = DataSourceType.parseId(typeId);
							LocationUtils.Area area = DataSourceManager.findAgencyArea(context, provider.authority);
							boolean isRTS = rtsProviderMetaData.equals(provider.metaData.getString(rtsProviderMetaData));
							JPaths jPath = isRTS ? DataSourceManager.findAgencyRTSRouteLogo(context, provider.authority) : null;
							if (type != null && typeId >= 0) {
								AgencyProperties newAgency = new AgencyProperties(provider.authority, type, shortName, label, color, area, isRTS);
								addNewAgency(newAgency);
								if (jPath != null) {
									this.rtsRouteLogoByAuthority.put(newAgency.getAuthority(), jPath);
								}
							} else {
								MTLog.w(this, "Invalid type ID '%s' (%s), skipping agency provider.", typeId, type);
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
				int typeId = this.allAgenciesByTypeId.keyAt(i);
				CollectionUtils.sort(this.allAgenciesByTypeId.get(typeId), AgencyProperties.SHORT_NAME_COMPARATOR);
			}
		}
	}

	private void addNewStatusProvider(StatusProviderProperties newStatusProvider) {
		this.allStatusProviders.add(newStatusProvider);
		this.allStatusProvidersByAuthority.put(newStatusProvider.getAuthority(), newStatusProvider);
		String newScheduleProviderTargetAuthority = newStatusProvider.getTargetAuthority();
		if (!this.statusProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
			this.statusProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<StatusProviderProperties>());
		}
		this.statusProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newStatusProvider);
	}

	private void addNewScheduleProvider(ScheduleProviderProperties newScheduleProvider) {
		this.allScheduleProviders.add(newScheduleProvider);
		this.allScheduleProvidersByAuthority.put(newScheduleProvider.getAuthority(), newScheduleProvider);
		String newScheduleProviderTargetAuthority = newScheduleProvider.getTargetAuthority();
		if (!this.scheduleProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
			this.scheduleProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<ScheduleProviderProperties>());
		}
		this.scheduleProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newScheduleProvider);
	}

	private void addNewServiceUpdateProvider(ServiceUpdateProviderProperties newServiceUpdateProvider) {
		this.allServiceUpdateProviders.add(newServiceUpdateProvider);
		this.allServiceUpdateProvidersByAuthority.put(newServiceUpdateProvider.getAuthority(), newServiceUpdateProvider);
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
			WeakHashMap<ModulesUpdateListener, Object> listenersCopy = new WeakHashMap<ModulesUpdateListener, Object>(modulesUpdateListeners);
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
