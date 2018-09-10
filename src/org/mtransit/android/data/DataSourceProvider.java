package org.mtransit.android.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.task.MTAsyncTask;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.SparseArray;

public class DataSourceProvider implements MTLog.Loggable {

	private static final String TAG = DataSourceProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Nullable
	private static DataSourceProvider instance = null;

	public static DataSourceProvider get(@Nullable Context context) {
		if (instance == null) {
			initInstance(context);
		}
		return instance;
	}

	@Nullable
	public static DataSourceProvider get() {
		return instance;
	}

	private synchronized static void initInstance(@Nullable Context context) {
		if (instance != null) {
			return;
		}
		if (context == null) {
			return;
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

	private static boolean resumed = false;

	public static void onPause() {
		resumed = false;
	}

	public static void onResume() {
		resumed = true;
		if (triggerModulesUpdated) {
			triggerModulesUpdated();
		}
	}

	@Nullable
	private static String agencyProviderMetaData;

	@NonNull
	public static String getAgencyProviderMetaData(@NonNull Context context) {
		if (agencyProviderMetaData == null) {
			agencyProviderMetaData = context.getString(R.string.agency_provider);
		}
		return agencyProviderMetaData;
	}

	@Nullable
	private static String scheduleProviderMetaData;

	@NonNull
	private static String getScheduleProviderMetaData(@NonNull Context context) {
		if (scheduleProviderMetaData == null) {
			scheduleProviderMetaData = context.getString(R.string.schedule_provider);
		}
		return scheduleProviderMetaData;
	}

	@Nullable
	private static String statusProviderMetaData;

	@NonNull
	private static String getStatusProviderMetaData(@NonNull Context context) {
		if (statusProviderMetaData == null) {
			statusProviderMetaData = context.getString(R.string.status_provider);
		}
		return statusProviderMetaData;
	}

	@Nullable
	private static String serviceUpdateProviderMetaData;

	@NonNull
	private static String getServiceUpdateProviderMetaData(@NonNull Context context) {
		if (serviceUpdateProviderMetaData == null) {
			serviceUpdateProviderMetaData = context.getString(R.string.service_update_provider);
		}
		return serviceUpdateProviderMetaData;
	}

	@Nullable
	private static String newsProviderMetaData;

	@NonNull
	private static String getNewsProviderMetaData(@NonNull Context context) {
		if (newsProviderMetaData == null) {
			newsProviderMetaData = context.getString(R.string.news_provider);
		}
		return newsProviderMetaData;
	}

	public static boolean isProvider(@NonNull Context context, String pkg) {
		if (TextUtils.isEmpty(pkg)) {
			return false;
		}
		ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg);
		if (providers == null || providers.length == 0) {
			return false;
		}
		String agencyProviderMetaData = getAgencyProviderMetaData(context);
		String scheduleProviderMetaData = getScheduleProviderMetaData(context);
		String statusProviderMetaData = getStatusProviderMetaData(context);
		String serviceUpdateProviderMetaData = getServiceUpdateProviderMetaData(context);
		String newsProviderMetaData = getNewsProviderMetaData(context);
		for (ProviderInfo provider : providers) {
			if (provider.metaData != null) {
				if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
					return true;
				}
				if (scheduleProviderMetaData.equals(provider.metaData.getString(scheduleProviderMetaData))) {
					return true;
				}
				if (statusProviderMetaData.equals(provider.metaData.getString(statusProviderMetaData))) {
					return true;
				}
				if (serviceUpdateProviderMetaData.equals(provider.metaData.getString(serviceUpdateProviderMetaData))) {
					return true;
				}
				if (newsProviderMetaData.equals(provider.metaData.getString(newsProviderMetaData))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean resetIfNecessary(@NonNull Context context) {
		if (instance != null) {
			if (hasChanged(instance, context)) {
				destroy();
				triggerModulesUpdated();
				return true;
			}
		}
		return false;
	}

	private synchronized static boolean hasChanged(DataSourceProvider current, @NonNull Context context) {
		if (current == null) {
			return true;
		}
		String agencyProviderMetaData = getAgencyProviderMetaData(context);
		String scheduleProviderMetaData = getScheduleProviderMetaData(context);
		String statusProviderMetaData = getStatusProviderMetaData(context);
		String serviceUpdateProviderMetaData = getServiceUpdateProviderMetaData(context);
		String newsProviderMetaData = getNewsProviderMetaData(context);
		int nbAgencyProviders = 0, nbScheduleProviders = 0, nbStatusProviders = 0, nbServiceUpdateProviders = 0, nbNewsProviders = 0;
		PackageManager pm = context.getPackageManager();
		for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
			ProviderInfo[] providers = packageInfo.providers;
			if (providers != null) {
				for (ProviderInfo provider : providers) {
					if (provider != null && provider.metaData != null) {
						if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
							if (!current.hasAgency(provider.authority)) {
								return true;
							}
							nbAgencyProviders++;
						}
						if (statusProviderMetaData.equals(provider.metaData.getString(statusProviderMetaData))) {
							if (current.getStatusProvider(provider.authority) == null) {
								return true;
							}
							nbStatusProviders++;
						}
						if (scheduleProviderMetaData.equals(provider.metaData.getString(scheduleProviderMetaData))) {
							if (current.getScheduleProvider(provider.authority) == null) {
								return true;
							}
							nbScheduleProviders++;
						}
						if (serviceUpdateProviderMetaData.equals(provider.metaData.getString(serviceUpdateProviderMetaData))) {
							if (current.getServiceUpdateProvider(provider.authority) == null) {
								return true;
							}
							nbServiceUpdateProviders++;
						}
						if (newsProviderMetaData.equals(provider.metaData.getString(newsProviderMetaData))) {
							if (current.getNewsProvider(provider.authority) == null) {
								return true;
							}
							nbNewsProviders++;
						}
					}
				}
			}
		}
		if (nbAgencyProviders != CollectionUtils.getSize(current.allAgenciesAuthority) //
				|| nbStatusProviders != CollectionUtils.getSize(current.allStatusProviders) //
				|| nbScheduleProviders != CollectionUtils.getSize(current.allScheduleProviders) //
				|| nbServiceUpdateProviders != CollectionUtils.getSize(current.allServiceUpdateProviders) //
				|| nbNewsProviders != CollectionUtils.getSize(current.allNewsProviders)) {
			return true;
		}
		return false;
	}

	@NonNull
	private HashSet<String> allAgenciesAuthority = new HashSet<String>();

	@NonNull
	private ArrayMap<String, Integer> agenciesAuthorityTypeId = new ArrayMap<String, Integer>();

	@NonNull
	private ArrayMap<String, String> agenciesAuthorityPkg = new ArrayMap<String, String>();

	@NonNull
	private ArrayMap<String, Boolean> agenciesAuthorityIsRts = new ArrayMap<String, Boolean>();

	private ArrayList<DataSourceType> allAgencyTypes = new ArrayList<DataSourceType>();

	@Nullable
	private ArrayList<AgencyProperties> allAgencies = null;

	private ArrayList<StatusProviderProperties> allStatusProviders = new ArrayList<StatusProviderProperties>();

	private ArrayList<ScheduleProviderProperties> allScheduleProviders = new ArrayList<ScheduleProviderProperties>();

	private ArrayList<ServiceUpdateProviderProperties> allServiceUpdateProviders = new ArrayList<ServiceUpdateProviderProperties>();

	private ArrayList<NewsProviderProperties> allNewsProviders = new ArrayList<NewsProviderProperties>();

	@Nullable
	private ArrayMap<String, AgencyProperties> allAgenciesByAuthority = null;

	private ArrayMap<String, StatusProviderProperties> allStatusProvidersByAuthority = new ArrayMap<String, StatusProviderProperties>();

	private ArrayMap<String, ScheduleProviderProperties> allScheduleProvidersByAuthority = new ArrayMap<String, ScheduleProviderProperties>();

	private ArrayMap<String, ServiceUpdateProviderProperties> allServiceUpdateProvidersByAuthority = new ArrayMap<String, ServiceUpdateProviderProperties>();

	private ArrayMap<String, NewsProviderProperties> allNewsProvidersByAuthority = new ArrayMap<String, NewsProviderProperties>();

	@Nullable
	private SparseArray<ArrayList<AgencyProperties>> allAgenciesByTypeId = null;

	private ArrayMap<String, HashSet<StatusProviderProperties>> statusProvidersByTargetAuthority = new ArrayMap<String, HashSet<StatusProviderProperties>>();

	private ArrayMap<String, HashSet<ScheduleProviderProperties>> scheduleProvidersByTargetAuthority =
			new ArrayMap<String, HashSet<ScheduleProviderProperties>>();

	private ArrayMap<String, HashSet<ServiceUpdateProviderProperties>> serviceUpdateProvidersByTargetAuthority =
			new ArrayMap<String, HashSet<ServiceUpdateProviderProperties>>();

	private ArrayMap<String, HashSet<NewsProviderProperties>> newsProvidersByTargetAuthority = new ArrayMap<String, HashSet<NewsProviderProperties>>();

	private ArrayMap<String, Integer> allAgenciesColorInts = new ArrayMap<String, Integer>();

	private DataSourceProvider() {
	}

	public ArrayList<DataSourceType> getAvailableAgencyTypes() {
		return new ArrayList<DataSourceType>(this.allAgencyTypes); // copy
	}

	public int getAllAgenciesCount() {
		return CollectionUtils.getSize(this.allAgenciesAuthority);
	}

	public ArrayList<AgencyProperties> getAllAgencies(Context context) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties(context);
		}
		if (this.allAgencies == null) {
			return null;
		}
		return new ArrayList<AgencyProperties>(this.allAgencies); // copy
	}

	private boolean isAgencyPropertiesSet() {
		return this.allAgencies != null;
	}

	private synchronized void initAgencyProperties(Context context) {
		if (this.allAgencies != null) {
			return; // too late
		}
		if (context == null) {
			return;
		}
		try {
			if (CollectionUtils.getSize(this.allAgenciesAuthority) > 0) {
				ArrayList<AgencyProperties> allAgencies = new ArrayList<AgencyProperties>();
				ArrayMap<String, AgencyProperties> allAgenciesByAuthority = new ArrayMap<String, AgencyProperties>();
				SparseArray<ArrayList<AgencyProperties>> allAgenciesByTypeId = new SparseArray<ArrayList<AgencyProperties>>();
				for (String authority : this.allAgenciesAuthority) {
					boolean isRTS = this.agenciesAuthorityIsRts.get(authority);
					Integer typeId = this.agenciesAuthorityTypeId.get(authority);
					if (typeId != null && typeId >= 0) {
						DataSourceType type = DataSourceType.parseId(typeId);
						if (type != null) {
							AgencyProperties agency = DataSourceManager.findAgencyProperties(context, authority, type, isRTS);
							if (agency != null) {
								if (isRTS) {
									agency.setLogo(DataSourceManager.findAgencyRTSRouteLogo(context, authority));
								}
								allAgencies.add(agency);
								allAgenciesByAuthority.put(agency.getAuthority(), agency);
								DataSourceType newAgencyType = agency.getType();
								if (allAgenciesByTypeId.get(newAgencyType.getId()) == null) {
									allAgenciesByTypeId.put(newAgencyType.getId(), new ArrayList<AgencyProperties>());
								}
								allAgenciesByTypeId.get(newAgencyType.getId()).add(agency);
								this.allAgenciesColorInts.put(agency.getAuthority(), agency.hasColor() ? agency.getColorInt() : null);
							} else {
								MTLog.w(this, "Invalid agency '%s'!", authority);
							}
						} else {
							MTLog.w(this, "Invalid type for ID '%s', skipping agency provider '%s'!", typeId, authority);
						}
					} else {
						MTLog.w(this, "Invalid type ID '%s', skipping agency provider '%s'!.", typeId, authority);
					}
				}
				CollectionUtils.sort(allAgencies, AgencyProperties.SHORT_NAME_COMPARATOR);
				for (int i = 0; i < allAgenciesByTypeId.size(); i++) {
					CollectionUtils.sort(allAgenciesByTypeId.get(allAgenciesByTypeId.keyAt(i)), AgencyProperties.SHORT_NAME_COMPARATOR);
				}
				this.allAgencies = allAgencies;
				this.allAgenciesByAuthority = allAgenciesByAuthority;
				this.allAgenciesByTypeId = allAgenciesByTypeId;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while initializing agencies properties!");
			this.allAgencies = null; // reset
			this.allAgenciesByAuthority = null; // reset
			this.allAgenciesByTypeId = null; // reset
		}
	}

	public boolean hasAgency(String authority) {
		return this.allAgenciesAuthority.contains(authority);
	}

	@Nullable
	public AgencyProperties getAgency(Context context, String authority) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties(context);
		}
		if (this.allAgenciesByAuthority == null) {
			return null;
		}
		return this.allAgenciesByAuthority.get(authority);
	}

	public ArrayList<AgencyProperties> getTypeDataSources(Context context, int typeId) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties(context);
		}
		ArrayList<AgencyProperties> agencies = this.allAgenciesByTypeId == null ? null : this.allAgenciesByTypeId.get(typeId);
		if (agencies == null) {
			return null;
		}
		return new ArrayList<AgencyProperties>(agencies); // copy
	}

	@ColorInt
	public Integer getAgencyColorInt(Context context, String authority) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties(context);
		}
		return this.allAgenciesColorInts.get(authority);
	}

	@Nullable
	public String getAgencyPkg(String authority) {
		return this.agenciesAuthorityPkg.get(authority);
	}

	private StatusProviderProperties getStatusProvider(String authority) {
		return this.allStatusProvidersByAuthority.get(authority);
	}

	private ScheduleProviderProperties getScheduleProvider(String authority) {
		return this.allScheduleProvidersByAuthority.get(authority);
	}

	private ServiceUpdateProviderProperties getServiceUpdateProvider(String authority) {
		return this.allServiceUpdateProvidersByAuthority.get(authority);
	}

	public ArrayList<NewsProviderProperties> getAllNewsProvider() {
		return new ArrayList<NewsProviderProperties>(this.allNewsProviders); // copy
	}

	private NewsProviderProperties getNewsProvider(String authority) {
		return this.allNewsProvidersByAuthority.get(authority);
	}

	public HashSet<StatusProviderProperties> getTargetAuthorityStatusProviders(String targetAuthority) {
		HashSet<StatusProviderProperties> statusProviders = this.statusProvidersByTargetAuthority.get(targetAuthority);
		if (statusProviders == null) {
			return null;
		}
		return new HashSet<StatusProviderProperties>(statusProviders); // copy
	}

	public HashSet<ScheduleProviderProperties> getTargetAuthorityScheduleProviders(String targetAuthority) {
		HashSet<ScheduleProviderProperties> scheduleProviders = this.scheduleProvidersByTargetAuthority.get(targetAuthority);
		if (scheduleProviders == null) {
			return null;
		}
		return new HashSet<ScheduleProviderProperties>(scheduleProviders); // copy
	}

	public HashSet<ServiceUpdateProviderProperties> getTargetAuthorityServiceUpdateProviders(String targetAuthority) {
		HashSet<ServiceUpdateProviderProperties> serviceUpdateProviders = this.serviceUpdateProvidersByTargetAuthority.get(targetAuthority);
		if (serviceUpdateProviders == null) {
			return null;
		}
		return new HashSet<ServiceUpdateProviderProperties>(serviceUpdateProviders); // copy
	}

	public HashSet<NewsProviderProperties> getTargetAuthorityNewsProviders(String targetAuthority) {
		HashSet<NewsProviderProperties> newsProviders = this.newsProvidersByTargetAuthority.get(targetAuthority);
		if (newsProviders == null) {
			return null;
		}
		return new HashSet<NewsProviderProperties>(newsProviders); // copy
	}

	public void onDestroy() {
		if (this.allAgencies != null) {
			this.allAgencies.clear();
			this.allAgencies = null; // reset
		}
		if (this.allAgenciesByTypeId != null) {
			this.allAgenciesByTypeId.clear();
			this.allAgenciesByTypeId = null; // reset
		}
		if (this.allAgenciesByAuthority != null) {
			this.allAgenciesByAuthority.clear();
			this.allAgenciesByAuthority = null; // reset
		}
		this.allAgencyTypes.clear();
		this.allAgenciesAuthority.clear();
		this.agenciesAuthorityIsRts.clear();
		this.agenciesAuthorityTypeId.clear();
		this.agenciesAuthorityPkg.clear();
		this.allStatusProviders.clear();
		this.allStatusProvidersByAuthority.clear();
		this.statusProvidersByTargetAuthority.clear();
		this.allScheduleProviders.clear();
		this.allScheduleProvidersByAuthority.clear();
		this.scheduleProvidersByTargetAuthority.clear();
		this.allServiceUpdateProviders.clear();
		this.allServiceUpdateProvidersByAuthority.clear();
		this.serviceUpdateProvidersByTargetAuthority.clear();
		this.allNewsProviders.clear();
		this.allNewsProvidersByAuthority.clear();
		this.newsProvidersByTargetAuthority.clear();
		this.allAgenciesColorInts.clear();
	}

	private synchronized void init(@NonNull Context context) {
		try {
			String agencyProviderMetaData = getAgencyProviderMetaData(context);
			String agencyProviderTypeMetaData = context.getString(R.string.agency_provider_type);
			String rtsProviderMetaData = context.getString(R.string.rts_provider);
			String scheduleProviderMetaData = getScheduleProviderMetaData(context);
			String statusProviderMetaData = getStatusProviderMetaData(context);
			String serviceUpdateProviderMetaData = getServiceUpdateProviderMetaData(context);
			String newsProviderMetaData = getNewsProviderMetaData(context);
			String statusProviderTargetMetaData = context.getString(R.string.status_provider_target);
			String scheduleProviderTargetMetaData = context.getString(R.string.schedule_provider_target);
			String serviceUpdateProviderTargetMetaData = context.getString(R.string.service_update_provider_target);
			String newsProviderTargetMetaData = context.getString(R.string.news_provider_target);
			PackageManager pm = context.getPackageManager();
			for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
				ProviderInfo[] providers = packageInfo.providers;
				if (providers != null) {
					for (ProviderInfo provider : providers) {
						if (provider.metaData != null) {
							if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
								this.agenciesAuthorityPkg.put(provider.authority, provider.packageName);
								int agencyTypeId = provider.metaData.getInt(agencyProviderTypeMetaData, -1);
								if (agencyTypeId >= 0) {
									DataSourceType newAgencyType = DataSourceType.parseId(agencyTypeId);
									if (newAgencyType != null) {
										if (!this.allAgencyTypes.contains(newAgencyType)) {
											this.allAgencyTypes.add(newAgencyType);
										}
									}
									this.agenciesAuthorityTypeId.put(provider.authority, agencyTypeId);
								}
								this.agenciesAuthorityIsRts.put( //
										provider.authority, rtsProviderMetaData.equals(provider.metaData.getString(rtsProviderMetaData)));
								this.allAgenciesAuthority.add(provider.authority);
							}
							if (statusProviderMetaData.equals(provider.metaData.getString(statusProviderMetaData))) {
								String targetAuthority = provider.metaData.getString(statusProviderTargetMetaData);
								StatusProviderProperties newStatusProvider = new StatusProviderProperties(provider.authority, targetAuthority);
								this.allStatusProviders.add(newStatusProvider);
								this.allStatusProvidersByAuthority.put(newStatusProvider.getAuthority(), newStatusProvider);
								String newScheduleProviderTargetAuthority = newStatusProvider.getTargetAuthority();
								if (!this.statusProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
									this.statusProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<StatusProviderProperties>());
								}
								this.statusProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newStatusProvider);
							}
							if (scheduleProviderMetaData.equals(provider.metaData.getString(scheduleProviderMetaData))) {
								String targetAuthority = provider.metaData.getString(scheduleProviderTargetMetaData);
								ScheduleProviderProperties newScheduleProvider = new ScheduleProviderProperties(provider.authority, targetAuthority);
								this.allScheduleProviders.add(newScheduleProvider);
								this.allScheduleProvidersByAuthority.put(newScheduleProvider.getAuthority(), newScheduleProvider);
								String newScheduleProviderTargetAuthority = newScheduleProvider.getTargetAuthority();
								if (!this.scheduleProvidersByTargetAuthority.containsKey(newScheduleProviderTargetAuthority)) {
									this.scheduleProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, new HashSet<ScheduleProviderProperties>());
								}
								this.scheduleProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority).add(newScheduleProvider);
							}
							if (serviceUpdateProviderMetaData.equals(provider.metaData.getString(serviceUpdateProviderMetaData))) {
								String targetAuthority = provider.metaData.getString(serviceUpdateProviderTargetMetaData);
								ServiceUpdateProviderProperties newServiceUpdateProvider =
										new ServiceUpdateProviderProperties(provider.authority, targetAuthority);
								this.allServiceUpdateProviders.add(newServiceUpdateProvider);
								this.allServiceUpdateProvidersByAuthority.put(newServiceUpdateProvider.getAuthority(), newServiceUpdateProvider);
								String newServiceUpdateProviderTargetAuthority = newServiceUpdateProvider.getTargetAuthority();
								if (!this.serviceUpdateProvidersByTargetAuthority.containsKey(newServiceUpdateProviderTargetAuthority)) {
									this.serviceUpdateProvidersByTargetAuthority.put( //
											newServiceUpdateProviderTargetAuthority, new HashSet<ServiceUpdateProviderProperties>());
								}
								this.serviceUpdateProvidersByTargetAuthority.get(newServiceUpdateProviderTargetAuthority).add(newServiceUpdateProvider);
							}
							if (newsProviderMetaData.equals(provider.metaData.getString(newsProviderMetaData))) {
								String targetAuthority = provider.metaData.getString(newsProviderTargetMetaData);
								NewsProviderProperties newNewsProvider = new NewsProviderProperties(provider.authority, targetAuthority);
								this.allNewsProviders.add(newNewsProvider);
								this.allNewsProvidersByAuthority.put(newNewsProvider.getAuthority(), newNewsProvider);
								String newNewsProviderTargetAuthority = newNewsProvider.getTargetAuthority();
								if (!this.newsProvidersByTargetAuthority.containsKey(newNewsProviderTargetAuthority)) {
									this.newsProvidersByTargetAuthority.put(newNewsProviderTargetAuthority, new HashSet<NewsProviderProperties>());
								}
								this.newsProvidersByTargetAuthority.get(newNewsProviderTargetAuthority).add(newNewsProvider);
							}
						}
					}
				}
			}
			CollectionUtils.sort(this.allAgencyTypes, new DataSourceType.DataSourceTypeShortNameComparator(context));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while initializing properties!");
			destroy();
		}
	}

	private static WeakHashMap<ModulesUpdateListener, Object> modulesUpdateListeners = new WeakHashMap<ModulesUpdateListener, Object>();

	public static void addModulesUpdateListener(ModulesUpdateListener listener) {
		try {
			if (!modulesUpdateListeners.containsKey(listener)) {
				modulesUpdateListeners.put(listener, null);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "addModulesUpdateListener() > error while adding listener '%s'!", listener);
		}
	}

	public static void removeModulesUpdateListener(ModulesUpdateListener listener) {
		try {
			if (modulesUpdateListeners.containsKey(listener)) {
				modulesUpdateListeners.remove(listener);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "removeModulesUpdateListener() > error while removing listener '%s'!", listener);
		}
	}

	private static TriggerModulesUpdatedTask triggerModulesUpdatedTask = null;

	private static boolean triggerModulesUpdated = false;

	public static void triggerModulesUpdated() {
		triggerModulesUpdated = true;
		if (!resumed) {
			return;
		}
		if (modulesUpdateListeners != null) {
			if (triggerModulesUpdatedTask != null && triggerModulesUpdatedTask.getStatus() != AsyncTask.Status.RUNNING) {
				triggerModulesUpdatedTask.cancel(true);
			}
			WeakHashMap<ModulesUpdateListener, Object> listenersCopy = new WeakHashMap<ModulesUpdateListener, Object>(modulesUpdateListeners);
			triggerModulesUpdatedTask = new TriggerModulesUpdatedTask(listenersCopy);
			TaskUtils.execute(triggerModulesUpdatedTask);
			triggerModulesUpdated = false; // processed
		} else {
			triggerModulesUpdated = false; // processed
		}
	}

	public interface ModulesUpdateListener {
		void onModulesUpdated();
	}

	private static class TriggerModulesUpdatedTask extends MTAsyncTask<Void, ModulesUpdateListener, Void> {

		private static final String TAG = DataSourceProvider.class.getSimpleName() + ">" + TriggerModulesUpdatedTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakHashMap<ModulesUpdateListener, Object> listeners;

		public TriggerModulesUpdatedTask(WeakHashMap<ModulesUpdateListener, Object> listeners) {
			this.listeners = listeners;
		}

		@Override
		protected Void doInBackgroundMT(Void... params) {
			if (this.listeners != null) {
				Iterator<ModulesUpdateListener> it = this.listeners.keySet().iterator();
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
				try {
					values[0].onModulesUpdated();
				} catch (Throwable t) {
					MTLog.w(this, t, "Error while broadcasting module updated for %!", values[0]);
				}
			}
		}
	}
}
