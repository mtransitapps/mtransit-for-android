package org.mtransit.android.data;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.DeviceUtils;
import org.mtransit.android.commons.LocationUtils.LocationPOI;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule.ScheduleStatusFilter;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.ServiceUpdateProviderContract;
import org.mtransit.android.commons.provider.StatusProviderContract;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTDialog;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.POIFragment;
import org.mtransit.android.ui.fragment.RTSRouteFragment;
import org.mtransit.android.ui.nearby.NearbyFragment;
import org.mtransit.android.ui.type.AgencyTypeFragment;
import org.mtransit.android.util.UITimeUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class POIManager implements LocationPOI, MTLog.Loggable {

	private static final String TAG = POIManager.class.getSimpleName();

	@SuppressWarnings("ConstantConditions")
	@NonNull
	@Override
	public String getLogTag() {
		if (this.poi != null) {
			return TAG + "-" + this.poi.getUUID();
		}
		return TAG;
	}

	public static final POIAlphaComparator POI_ALPHA_COMPARATOR = new POIAlphaComparator();

	@ColorInt
	public static int getDefaultDistanceAndCompassColor(@NonNull Context context) {
		return ColorUtils.getTextColorTertiary(context);
	}

	@NonNull
	public final POI poi;
	@Nullable
	private CharSequence distanceString = null;
	private float distance = -1;
	@Nullable
	private POIStatus status;
	@Nullable
	private ArrayList<ServiceUpdate> serviceUpdates = null;
	private boolean inFocus = false;

	private long lastFindStatusTimestampMs = -1L;

	@Nullable
	private WeakReference<StatusLoader.StatusLoaderListener> statusLoaderListenerWR;

	private int scheduleMaxDataRequests = ScheduleStatusFilter.MAX_DATA_REQUESTS_DEFAULT;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public POIManager(@NonNull POI poi) {
		this(poi, null);
	}

	public POIManager(@NonNull POI poi, @Nullable POIStatus status) {
		this.poi = poi;
		this.status = status;
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	@NonNull
	@Override
	public String toString() {
		return POIManager.class.getSimpleName() + '[' +//
				"poi:" + this.poi + ',' + //
				"status:" + this.status + ',' + //
				']';
	}

	public void resetLastFindTimestamps() {
		this.lastFindServiceUpdateTimestampMs = -1L;
		this.lastFindStatusTimestampMs = -1L;
	}

	public void setInFocus(boolean inFocus) {
		this.inFocus = inFocus;
	}

	@SuppressWarnings("unused")
	public boolean isInFocus() {
		return this.inFocus;
	}

	@Override
	public float getDistance() {
		return distance;
	}

	@Override
	public void setDistance(float distance) {
		this.distance = distance;
	}

	@Nullable
	@Override
	public CharSequence getDistanceString() {
		return distanceString;
	}

	@Override
	public void setDistanceString(@Nullable CharSequence distanceString) {
		this.distanceString = distanceString;
	}

	public void setStatusLoaderListener(@NonNull StatusLoader.StatusLoaderListener statusLoaderListener) {
		this.statusLoaderListenerWR = new WeakReference<>(statusLoaderListener);
	}

	@Nullable
	public String getLocation() {
		if (this.poi instanceof Module) {
			return ((Module) this.poi).getLocation();
		}
		return null;
	}

	public int getStatusType() {
		return this.poi.getStatusType();
	}

	@SuppressWarnings("unused")
	public boolean hasStatus() {
		return this.status != null;
	}

	public boolean setStatus(@NonNull POIStatus newStatus) {
		if (!newStatus.isUseful() && !newStatus.isNoData()) {
			return false; // no change
		}
		switch (getStatusType()) {
		case POI.ITEM_STATUS_TYPE_NONE:
			return false; // no change
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			if (!(newStatus instanceof UISchedule)) {
				MTLog.w(this, "setStatus() > Unexpected schedule status '%s'!", newStatus);
				return false; // no change
			}
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			if (!(newStatus instanceof AvailabilityPercent)) {
				MTLog.w(this, "setStatus() > Unexpected availability percent status '%s'!", newStatus);
				return false; // no change
			}
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			if (!(newStatus instanceof AppStatus)) {
				MTLog.w(this, "setStatus() > Unexpected app status '%s'!", newStatus);
				return false; // no change
			}
			break;
		default:
			MTLog.w(this, "setStatus() > Unexpected status '%s'!", newStatus);
			return false; // no change
		}
		if (this.status != null && this.status.getReadFromSourceAtInMs() > newStatus.getReadFromSourceAtInMs()) {
			return false; // no change
		}
		this.status = newStatus;
		return true; // change
	}

	@SuppressWarnings("unused")
	@Nullable
	public POIStatus getStatusOrNull() {
		return this.status;
	}

	@Nullable
	public POIStatus getStatus(@Nullable Context context) {
		if (this.status == null || this.lastFindStatusTimestampMs < 0L || this.inFocus || !this.status.isUseful()) {
			findStatus(context, false);
		}
		return this.status;
	}

	@SuppressWarnings("UnusedReturnValue")
	private boolean findStatus(@Nullable Context context,
							   @SuppressWarnings("SameParameterValue") boolean skipIfBusy) {
		long findStatusTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis();
		boolean isNotSkipped = false;
		if (this.lastFindStatusTimestampMs != findStatusTimestampMs) { // IF not same minute as last findStatus() call DO
			StatusProviderContract.Filter filter = getFilter();
			if (filter != null) {
				filter.setInFocus(this.inFocus);
				StatusLoader.StatusLoaderListener listener = this.statusLoaderListenerWR == null ? null : this.statusLoaderListenerWR.get();
				isNotSkipped = StatusLoader.get().findStatus(context, this, filter, listener, skipIfBusy);
				if (isNotSkipped) {
					this.lastFindStatusTimestampMs = findStatusTimestampMs;
				}
			}
		}
		return isNotSkipped;
	}

	public void setScheduleMaxDataRequests(int scheduleMaxDataRequests) {
		this.scheduleMaxDataRequests = scheduleMaxDataRequests;
	}

	@Nullable
	private StatusProviderContract.Filter getFilter() {
		switch (getStatusType()) {
		case POI.ITEM_STATUS_TYPE_NONE:
			return null;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			if (this.poi instanceof RouteTripStop) {
				RouteTripStop rts = (RouteTripStop) this.poi;
				ScheduleStatusFilter filter = new ScheduleStatusFilter(this.poi.getUUID(), rts);
				filter.setLookBehindInMs(UITimeUtils.RECENT_IN_MILLIS);
				filter.setMaxDataRequests(this.scheduleMaxDataRequests);
				return filter;
			} else {
				MTLog.w(this, "Schedule filter w/o '%s'!", this.poi);
				return null;
			}
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return new AvailabilityPercent.AvailabilityPercentStatusFilter(this.poi.getUUID());
		case POI.ITEM_STATUS_TYPE_APP:
			if (poi instanceof Module) {
				Module module = (Module) this.poi;
				return new AppStatus.AppStatusFilter(this.poi.getUUID(), module.getPkg());
			} else {
				MTLog.w(this, "App status filter w/o '%s'!", this.poi);
				return null;
			}
		default:
			MTLog.w(this, "Unexpected status type '%s´  for filter!", getStatusType());
			return null;
		}
	}

	@Nullable
	private WeakReference<ServiceUpdateLoader.ServiceUpdateLoaderListener> serviceUpdateLoaderListenerWR;

	public void setServiceUpdateLoaderListener(@NonNull ServiceUpdateLoader.ServiceUpdateLoaderListener serviceUpdateLoaderListener) {
		this.serviceUpdateLoaderListenerWR = new WeakReference<>(serviceUpdateLoaderListener);
	}

	@SuppressWarnings("unused")
	public boolean hasServiceUpdates() {
		return CollectionUtils.getSize(this.serviceUpdates) != 0;
	}

	public void setServiceUpdates(@Nullable Collection<ServiceUpdate> newServiceUpdates) {
		if (this.serviceUpdates == null) {
			this.serviceUpdates = new ArrayList<>();
		} else {
			this.serviceUpdates.clear();
		}
		if (newServiceUpdates != null) {
			this.serviceUpdates.addAll(newServiceUpdates);
			CollectionUtils.sort(this.serviceUpdates, ServiceUpdate.HIGHER_SEVERITY_FIRST_COMPARATOR);
		}
	}

	@Nullable
	public ArrayList<ServiceUpdate> getServiceUpdatesOrNull() {
		return this.serviceUpdates;
	}

	public boolean isServiceUpdateWarning(@NonNull Context context) {
		if (this.serviceUpdates == null || this.lastFindServiceUpdateTimestampMs < 0L || this.inFocus || !areServiceUpdatesUseful()) {
			findServiceUpdates(context, false);
		}
		return ServiceUpdate.isSeverityWarning(this.serviceUpdates);
	}

	@Nullable
	public ArrayList<ServiceUpdate> getServiceUpdates(@NonNull Context context) {
		if (this.serviceUpdates == null || this.lastFindServiceUpdateTimestampMs < 0L || this.inFocus || !areServiceUpdatesUseful()) {
			findServiceUpdates(context, false);
		}
		return this.serviceUpdates;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean areServiceUpdatesUseful() {
		if (this.serviceUpdates != null) {
			for (ServiceUpdate serviceUpdate : this.serviceUpdates) {
				if (serviceUpdate.isUseful()) {
					return true;
				}
			}
		}
		return false;
	}

	private long lastFindServiceUpdateTimestampMs = -1;

	@SuppressWarnings("UnusedReturnValue")
	private boolean findServiceUpdates(@NonNull Context context, @SuppressWarnings("SameParameterValue") boolean skipIfBusy) {
		long findServiceUpdateTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis();
		boolean isNotSkipped = false;
		if (this.lastFindServiceUpdateTimestampMs != findServiceUpdateTimestampMs) { // IF not same minute as last findStatus() call DO
			ServiceUpdateProviderContract.Filter filter = new ServiceUpdateProviderContract.Filter(this.poi);
			filter.setInFocus(this.inFocus);
			ServiceUpdateLoader.ServiceUpdateLoaderListener listener = //
					this.serviceUpdateLoaderListenerWR == null ? null : this.serviceUpdateLoaderListenerWR.get();
			isNotSkipped = ServiceUpdateLoader.get().findServiceUpdate(context, this, filter, listener, skipIfBusy);
			if (isNotSkipped) {
				this.lastFindServiceUpdateTimestampMs = findServiceUpdateTimestampMs;
			}
		}
		return isNotSkipped;
	}

	@NonNull
	private CharSequence[] getActionsItems(@NonNull Context context,
										   CharSequence defaultAction,
										   @SuppressWarnings("unused") SparseArrayCompat<Favorite.Folder> favoriteFolders) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_NONE:
			return new CharSequence[]{defaultAction};
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return new CharSequence[]{ //
					defaultAction, //
					FavoriteManager.isFavorite(context, this.poi.getUUID()) ? //
							FavoriteManager.get(context).isUsingFavoriteFolders() ? //
									context.getString(R.string.edit_fav) : //
									context.getString(R.string.remove_fav) : //
							context.getString(R.string.add_fav) //
			};
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			RouteTripStop rts = (RouteTripStop) this.poi;
			return new CharSequence[]{ //
					context.getString(R.string.view_stop), //
					TextUtils.isEmpty(rts.getRoute().getShortName()) ? //
							context.getString(R.string.view_stop_route) : //
							context.getString(R.string.view_stop_route_and_route, rts.getRoute().getShortName()), //
					FavoriteManager.isFavorite(context, this.poi.getUUID()) ? //
							FavoriteManager.get(context).isUsingFavoriteFolders() ? //
									context.getString(R.string.edit_fav) : //
									context.getString(R.string.remove_fav) : //
							context.getString(R.string.add_fav) //
			};
		case POI.ITEM_ACTION_TYPE_APP:
			if (PackageManagerUtils.isAppInstalled(context, ((Module) this.poi).getPkg())) {
				if (PackageManagerUtils.isAppEnabled(context, ((Module) this.poi).getPkg())) {
					return new CharSequence[]{ //
							context.getString(R.string.rate_on_store), //
							context.getString(R.string.manage_app), //
							context.getString(R.string.uninstall), //
					};
				} else {
					return new CharSequence[]{ //
							context.getString(R.string.re_enable_app), //
					};
				}
			} else {
				return new CharSequence[]{ //
						context.getString(R.string.download_on_store), //
				};
			}
		case POI.ITEM_ACTION_TYPE_PLACE:
			//noinspection DuplicateBranchesInSwitch
			return new CharSequence[]{defaultAction};
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return new CharSequence[]{defaultAction};
		}
	}

	private boolean onActionsItemClick(@NonNull Activity activity,
									   int itemClicked,
									   @SuppressWarnings("unused") SparseArrayCompat<Favorite.Folder> favoriteFolders,
									   FavoriteManager.FavoriteUpdateListener listener,
									   POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_NONE:
			return false; // NOT HANDLED
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return onActionsItemClickFavoritable(activity, itemClicked, listener, onClickHandledListener);
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return onActionsItemClickRTS(activity, itemClicked, listener, onClickHandledListener);
		case POI.ITEM_ACTION_TYPE_APP:
			return onActionsItemClickApp(activity, itemClicked, listener, onClickHandledListener);
		case POI.ITEM_ACTION_TYPE_PLACE:
			return onActionsItemClickPlace(activity, itemClicked, listener, onClickHandledListener);
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return false; // NOT HANDLED
		}
	}

	private boolean onActionsItemClickApp(@NonNull Activity activity, int itemClicked,
										  @SuppressWarnings("unused") FavoriteManager.FavoriteUpdateListener listener,
										  @Nullable POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (itemClicked) {
		case 0: // Rate on Google Play
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			StoreUtils.viewAppPage(activity, ((Module) poi).getPkg(), activity.getString(R.string.google_play));
			return true; // HANDLED
		case 1: // Manage App
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			DeviceUtils.showAppDetailsSettings(activity, ((Module) poi).getPkg());
			return true; // HANDLED
		case 2: // Uninstall
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			PackageManagerUtils.uninstallApp(activity, ((Module) poi).getPkg());
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickPlace(@NonNull Activity activity,
											int itemClicked,
											@SuppressWarnings("unused") FavoriteManager.FavoriteUpdateListener listener,
											POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (itemClicked) {
		case 0:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			((MainActivity) activity).addFragmentToStack( //
					NearbyFragment.newFixedOnInstance(
							null,
							this.poi.getLat(),
							this.poi.getLng(),
							getOneLineDescription(this.dataSourcesRepository, this.poi),
							getColor()
					)
			);
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	@Nullable
	@ColorInt
	private Integer color = null;

	@ColorInt
	public int getColor() {
		if (color == null) {
			color = getColor(this.dataSourcesRepository, this.poi, null);
		}
		if (color == null) {
			return Color.BLACK; // default
		}
		return color;
	}

	@Nullable
	@ColorInt
	public static Integer getColor(@NonNull DataSourcesRepository dataSourcesRepository,
								   @Nullable POI poi,
								   @Nullable Integer defaultColor) {
		if (poi != null) {
			if (poi instanceof RouteTripStop) {
				if (((RouteTripStop) poi).getRoute().hasColor()) {
					return ((RouteTripStop) poi).getRoute().getColorInt();
				}
			} else if (poi instanceof Module) {
				return ((Module) poi).getColorInt();
			}
			final AgencyProperties agency = dataSourcesRepository.getAgency(poi.getAuthority());
			if (agency != null) {
				return agency.getColorInt();
			}
		}
		return defaultColor;
	}

	@Nullable
	@ColorInt
	public static Integer getRouteColor(@NonNull DataSourcesRepository dataSourcesRepository,
										@Nullable Route route,
										@Nullable String authority,
										@Nullable Integer defaultColor) {
		if (route != null) {
			if (route.hasColor()) {
				return route.getColorInt();
			}
		}
		if (authority != null) {
			Integer agencyColorInt = dataSourcesRepository.getAgencyColorInt(authority);
			if (agencyColorInt != null) {
				return agencyColorInt;
			}
		}
		return defaultColor;
	}

	@ColorInt
	public static int getRouteColorNN(@NonNull DataSourcesRepository dataSourcesRepository,
									  @Nullable Route route,
									  @NonNull String authority,
									  int defaultColor) {
		if (route != null) {
			if (route.hasColor()) {
				return route.getColorInt();
			}
		}
		Integer agencyColorInt = dataSourcesRepository.getAgencyColorInt(authority);
		if (agencyColorInt != null) {
			return agencyColorInt;
		}
		return defaultColor;
	}

	@MainThread
	@NonNull
	public static String getOneLineDescription(@NonNull DataSourcesRepository dataSourcesRepository,
											   @NonNull POI poi) {
		return getOneLineDescription(
				dataSourcesRepository.getAgency(poi.getAuthority()),
				poi
		);
	}

	@MainThread
	@NonNull
	public static String getOneLineDescription(@Nullable AgencyProperties agency,
											   @NonNull POI poi) {
		StringBuilder sb = new StringBuilder();
		sb.append(poi.getName());
		if (poi instanceof RouteTripStop) {
			RouteTripStop rts = (RouteTripStop) poi;
			if (!TextUtils.isEmpty(rts.getRoute().getShortName())) {
				if (sb.length() > 0) {
					sb.append(StringUtils.SPACE_STRING).append("-").append(StringUtils.SPACE_STRING);
				}
				sb.append(rts.getRoute().getShortName());
			} else if (!TextUtils.isEmpty(rts.getRoute().getLongName())) {
				if (sb.length() > 0) {
					sb.append(StringUtils.SPACE_STRING).append("-").append(StringUtils.SPACE_STRING);
				}
				sb.append(rts.getRoute().getLongName());
			}
		}
		if (agency != null) {
			if (sb.length() > 0) {
				sb.append(StringUtils.SPACE_STRING).append("-").append(StringUtils.SPACE_STRING);
			}
			sb.append(agency.getShortName());
		}
		return sb.toString();
	}

	private boolean onActionsItemClickRTS(@NonNull Activity activity, int itemClicked, FavoriteManager.FavoriteUpdateListener listener,
										  POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (itemClicked) {
		case 1:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			RouteTripStop rts = (RouteTripStop) poi;
			((MainActivity) activity).addFragmentToStack( //
					RTSRouteFragment.newInstance(rts.getAuthority(), rts.getRoute().getId(), rts.getTrip().getId(), rts.getStop().getId(), rts.getRoute()));
			return true; // HANDLED
		case 2:
			return FavoriteManager.get(activity).addRemoveFavorite(activity, this.poi.getUUID(), listener);
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickFavoritable(Activity activity, int itemClicked, FavoriteManager.FavoriteUpdateListener listener,
												  @SuppressWarnings("unused") POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (itemClicked) {
		case 1:
			return FavoriteManager.get(activity).addRemoveFavorite(activity, this.poi.getUUID(), listener);
		}
		return false; // NOT HANDLED
	}

	public boolean isFavoritable() {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return true;
		case POI.ITEM_ACTION_TYPE_NONE:
		case POI.ITEM_ACTION_TYPE_APP:
		case POI.ITEM_ACTION_TYPE_PLACE:
			return false;
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return false;
		}
	}

	private boolean showPoiViewerScreen(Activity activity, POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		if (activity == null) {
			return false; // show long-click menu
		}
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_NONE:
			return false; // NOT HANDLED
		case POI.ITEM_ACTION_TYPE_APP:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			final Module module = (Module) this.poi;
			final String pkg = module.getPkg();
			if (PackageManagerUtils.isAppInstalled(activity, pkg)
					&& PackageManagerUtils.isAppEnabled(activity, pkg)) {
				final AgencyProperties agency = this.dataSourcesRepository.getAgencyForPkg(pkg);
				if (agency != null && !agency.getUpdateAvailable()) {
					PreferenceUtils.savePrefLcl(activity, PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(agency.getType().getId()), agency.getAuthority(), false);
					((MainActivity) activity).addFragmentToStack(AgencyTypeFragment.newInstance(agency.getType()));
					return true; // handled
				}
			}
			StoreUtils.viewAppPage(activity, pkg, activity.getString(R.string.google_play));
			return true; // handled
		case POI.ITEM_ACTION_TYPE_PLACE:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			((MainActivity) activity).addFragmentToStack(NearbyFragment.newFixedOnInstance(
					null,
					this.poi.getLat(),
					this.poi.getLng(),
					this.poi.getName(),
					null
			));
			return true; // nearby screen shown
		}
		if (activity instanceof MainActivity) {
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			AgencyProperties agencyProperties = this.dataSourcesRepository.getAgency(this.poi.getAuthority());
			((MainActivity) activity).addFragmentToStack(POIFragment.newInstance(this.poi.getUUID(), this.poi.getAuthority(), agencyProperties, this));
			// reset to defaults, so the POI is updated when coming back in the current screen
			resetLastFindTimestamps();
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	boolean onActionItemLongClick(Activity activity, SparseArrayCompat<Favorite.Folder> favoriteFolders,
								  FavoriteManager.FavoriteUpdateListener favoriteUpdateListener, POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		if (activity == null) {
			return false;
		}
		return showPoiMenu(activity, favoriteFolders, favoriteUpdateListener, onClickHandledListener);
	}

	boolean onActionItemClick(Activity activity, SparseArrayCompat<Favorite.Folder> favoriteFolders,
							  FavoriteManager.FavoriteUpdateListener favoriteUpdateListener, POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		if (activity == null) {
			return false;
		}
		boolean poiScreenShow = showPoiViewerScreen(activity, onClickHandledListener);
		if (!poiScreenShow) {
			poiScreenShow = showPoiMenu(activity, favoriteFolders, favoriteUpdateListener, onClickHandledListener);
		}
		return poiScreenShow;
	}

	private boolean showPoiMenu(final @NonNull Activity activity,
								final SparseArrayCompat<Favorite.Folder> favoriteFolders,
								final FavoriteManager.FavoriteUpdateListener favoriteUpdateListener,
								final POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (this.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return false; // no menu
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
		case POI.ITEM_VIEW_TYPE_MODULE:
			new MTDialog.Builder(activity) //
					.setTitle(this.poi.getName()) //
					.setItems( //
							getActionsItems( //
									activity, //
									activity.getString(R.string.view_details), //
									favoriteFolders //
							), //
							(dialog, item) -> {
								boolean handled = onActionsItemClick(activity, item, favoriteFolders, favoriteUpdateListener, onClickHandledListener);
								if (handled) {
									return;
								}
								switch (item) {
								case 0:
									showPoiViewerScreen(activity, onClickHandledListener);
									break;
								default:
									MTLog.w(POIManager.this, "Unexpected action item '%s'!", item);
									break;
								}
							})
					.create()
					.show();
			return true;
		default:
			MTLog.w(this, "Unknown view type '%s' for poi '%s'!", this.poi.getType(), this);
			return false;
		}
	}

	static POIManager fromCursorStatic(Cursor cursor, String authority) {
		switch (DefaultPOI.getTypeFromCursor(cursor)) {
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return new POIManager(DefaultPOI.fromCursorStatic(cursor, authority));
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return new POIManager(RouteTripStop.fromCursorStatic(cursor, authority));
		case POI.ITEM_VIEW_TYPE_MODULE:
			return new POIManager(Module.fromCursorStatic(cursor, authority));
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return new POIManager(TextMessage.fromCursorStatic(cursor, authority));
		default:
			MTLog.w(TAG, "Unexpected POI type '%s'! (using default)", DefaultPOI.getTypeFromCursor(cursor));
			return new POIManager(DefaultPOI.fromCursorStatic(cursor, authority));
		}
	}

	@Override
	public double getLat() {
		return this.poi.getLat();
	}

	@Override
	public double getLng() {
		return this.poi.getLng();
	}

	@Override
	public boolean hasLocation() {
		return this.poi.hasLocation();
	}

	private static class POIAlphaComparator implements Comparator<POIManager> {
		@Override
		public int compare(POIManager lhs, POIManager rhs) {
			POI lhsPoi = lhs == null ? null : lhs.poi;
			POI rhsPoi = rhs == null ? null : rhs.poi;
			if (lhsPoi == null && rhsPoi == null) {
				return ComparatorUtils.SAME;
			}
			if (lhsPoi == null) {
				return ComparatorUtils.BEFORE;
			} else if (rhsPoi == null) {
				return ComparatorUtils.AFTER;
			}
			return lhsPoi.compareToAlpha(null, rhsPoi);
		}
	}
}
