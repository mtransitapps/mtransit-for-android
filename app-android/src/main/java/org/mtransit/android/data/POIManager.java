package org.mtransit.android.data;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import org.mtransit.android.R;
import org.mtransit.android.commons.AppUpdateLauncher;
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
import org.mtransit.android.datasource.POIRepository;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTDialog;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.POIFragment;
import org.mtransit.android.ui.nearby.NearbyFragment;
import org.mtransit.android.ui.rts.route.RTSRouteFragment;
import org.mtransit.android.ui.type.AgencyTypeFragment;
import org.mtransit.android.ui.view.common.NavControllerExtKt;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.android.util.UITimeUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"unused", "WeakerAccess"})
public class POIManager implements LocationPOI, MTLog.Loggable {

	private static final String LOG_TAG = POIManager.class.getSimpleName();

	@SuppressWarnings("ConstantConditions")
	@NonNull
	@Override
	public String getLogTag() {
		if (this.poi != null) {
			final String uuid = this.poi.getUUID();
			final int index = uuid.indexOf(IAgencyProperties.PKG_COMMON);
			return LOG_TAG + "-" + (index == -1 ? uuid : uuid.substring(index + IAgencyProperties.PKG_COMMON.length()));
		}
		return LOG_TAG;
	}

	@SuppressWarnings("unused")
	public static final POIAlphaComparator POI_ALPHA_COMPARATOR = new POIAlphaComparator();

	@ColorInt
	public static int getDefaultDistanceAndCompassColor(@NonNull Context context) {
		return ColorUtils.getTextColorTertiary(context);
	}

	@NonNull
	public final POI poi;
	@Nullable
	private CharSequence distanceString = null;
	private float distance = -1f;
	@Nullable
	private POIStatus status = null;
	@Nullable
	private ArrayList<ServiceUpdate> serviceUpdates = null;
	private boolean inFocus = false;

	private long lastFindStatusTimestampMs = -1L;

	@Nullable
	private WeakReference<StatusLoader.StatusLoaderListener> statusLoaderListenerWR;

	private int scheduleMaxDataRequests = ScheduleStatusFilter.MAX_DATA_REQUESTS_DEFAULT;

	public POIManager(@NonNull POI poi) {
		this.poi = poi;
	}

	@NonNull
	@Override
	public String toString() {
		return POIManager.class.getSimpleName() + '[' +//
				"poi:" + this.poi + ',' + //
				"status:" + this.status + ',' + //
				']';
	}

	@NonNull
	public String toStringSimple() {
		return POIManager.class.getSimpleName() + '[' +//
				"poi:" + this.poi.getUUID() + ',' + //
				"status:" + this.hasStatus() + ',' + //
				"service updated:" + this.hasServiceUpdates() + ',' + //
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

	@NonNull
	@Override
	public POIManager setDistance(float distance) {
		this.distance = distance;
		return this;
	}

	@Nullable
	@Override
	public CharSequence getDistanceString() {
		return distanceString;
	}

	@NonNull
	@Override
	public POIManager setDistanceString(@Nullable CharSequence distanceString) {
		this.distanceString = distanceString;
		return this;
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
		if (this.status != null && !this.status.isNoData() && newStatus.isNoData()) {
			return false; // keep status w/ data
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
	public POIStatus getStatus(@Nullable Context context,
							   @NonNull StatusLoader statusLoader) {
		if (this.status == null || this.lastFindStatusTimestampMs < 0L || this.inFocus || !this.status.isUseful()) {
			findStatus(context, statusLoader, false);
		}
		return this.status;
	}

	@SuppressWarnings("UnusedReturnValue")
	private boolean findStatus(@Nullable Context context,
							   @NonNull StatusLoader statusLoader,
							   @SuppressWarnings("SameParameterValue") boolean skipIfBusy) {
		long findStatusTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis();
		boolean isNotSkipped = false;
		if (this.lastFindStatusTimestampMs != findStatusTimestampMs) { // IF not same minute as last findStatus() call DO
			StatusProviderContract.Filter filter = getFilter();
			if (filter != null) {
				filter.setInFocus(this.inFocus);
				StatusLoader.StatusLoaderListener listener = this.statusLoaderListenerWR == null ? null : this.statusLoaderListenerWR.get();
				isNotSkipped = statusLoader.findStatus(context, this, filter, listener, skipIfBusy);
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
	public List<ServiceUpdate> getServiceUpdatesOrNull() {
		return this.serviceUpdates;
	}

	public boolean isServiceUpdateWarning(@Nullable Context context,
										  @NonNull ServiceUpdateLoader serviceUpdateLoader) {
		if (this.serviceUpdates == null || this.lastFindServiceUpdateTimestampMs < 0L || this.inFocus || !areServiceUpdatesUseful()) {
			findServiceUpdates(context, serviceUpdateLoader, false);
		}
		return ServiceUpdate.isSeverityWarning(this.serviceUpdates);
	}

	@Nullable
	public ArrayList<ServiceUpdate> getServiceUpdates(@Nullable Context context,
													  @NonNull ServiceUpdateLoader serviceUpdateLoader) {
		if (this.serviceUpdates == null || this.lastFindServiceUpdateTimestampMs < 0L || this.inFocus || !areServiceUpdatesUseful()) {
			findServiceUpdates(context, serviceUpdateLoader, false);
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
	private boolean findServiceUpdates(@Nullable Context context,
									   @NonNull ServiceUpdateLoader serviceUpdateLoader,
									   @SuppressWarnings("SameParameterValue") boolean skipIfBusy) {
		long findServiceUpdateTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis();
		boolean isNotSkipped = false;
		if (this.lastFindServiceUpdateTimestampMs != findServiceUpdateTimestampMs) { // IF not same minute as last findStatus() call DO
			ServiceUpdateProviderContract.Filter filter = new ServiceUpdateProviderContract.Filter(this.poi);
			filter.setInFocus(this.inFocus);
			ServiceUpdateLoader.ServiceUpdateLoaderListener listener = //
					this.serviceUpdateLoaderListenerWR == null ? null : this.serviceUpdateLoaderListenerWR.get();
			isNotSkipped = serviceUpdateLoader.findServiceUpdate(context, this, filter, listener, skipIfBusy);
			if (isNotSkipped) {
				this.lastFindServiceUpdateTimestampMs = findServiceUpdateTimestampMs;
			}
		}
		return isNotSkipped;
	}

	@NonNull
	private CharSequence[] getActionsItems(@NonNull Context context,
										   @NonNull DataSourcesRepository dataSourcesRepository,
										   @NonNull FavoriteManager favoriteManager,
										   CharSequence defaultAction,
										   @SuppressWarnings("unused") SparseArrayCompat<Favorite.Folder> favoriteFolders) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_NONE:
			return new CharSequence[]{defaultAction};
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return new CharSequence[]{ //
					defaultAction, //
					favoriteManager.isFavorite(context, this.poi.getUUID()) ? //
							favoriteManager.isUsingFavoriteFolders() ? //
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
					favoriteManager.isFavorite(context, this.poi.getUUID()) ? //
							favoriteManager.isUsingFavoriteFolders() ? //
									context.getString(R.string.edit_fav) : //
									context.getString(R.string.remove_fav) : //
							context.getString(R.string.add_fav) //
			};
		case POI.ITEM_ACTION_TYPE_APP:
			final String pkg = ((Module) this.poi).getPkg();
			if (PackageManagerUtils.isAppInstalled(context, pkg)) {
				if (PackageManagerUtils.isAppEnabled(context, pkg)) {
					final AgencyProperties agencyProperties = dataSourcesRepository.getAgencyForPkg(pkg);
					if (agencyProperties != null && agencyProperties.hasContactUs()) {
						return new CharSequence[]{
								context.getString(R.string.view_on_store),
								context.getString(R.string.manage_app),
								context.getString(R.string.uninstall),
								context.getString(R.string.customer_service),
						};
					} else {
						return new CharSequence[]{ //
								context.getString(R.string.view_on_store), //
								context.getString(R.string.manage_app), //
								context.getString(R.string.uninstall), //
						};
					}
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
									   @NonNull View view,
									   @NonNull FavoriteManager favoriteManager,
									   @NonNull DataSourcesRepository dataSourcesRepository,
									   int itemClicked,
									   @SuppressWarnings("unused") SparseArrayCompat<Favorite.Folder> favoriteFolders,
									   FavoriteManager.FavoriteUpdateListener listener,
									   POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (this.poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_NONE:
			return false; // NOT HANDLED
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
			return onActionsItemClickFavoritable(activity, favoriteManager, itemClicked, listener, onClickHandledListener);
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return onActionsItemClickRTS(activity, view, favoriteManager, itemClicked, listener, onClickHandledListener);
		case POI.ITEM_ACTION_TYPE_APP:
			return onActionsItemClickApp(activity, view, dataSourcesRepository, itemClicked, listener, onClickHandledListener);
		case POI.ITEM_ACTION_TYPE_PLACE:
			return onActionsItemClickPlace(activity, view, dataSourcesRepository, itemClicked, listener, onClickHandledListener);
		default:
			MTLog.w(this, "unexpected action type '%s'!", this.poi.getActionsType());
			return false; // NOT HANDLED
		}
	}

	private boolean onActionsItemClickApp(@NonNull Activity activity,
										  @NonNull View view,
										  @NonNull DataSourcesRepository dataSourcesRepository,
										  int itemClicked,
										  @SuppressWarnings("unused") FavoriteManager.FavoriteUpdateListener listener,
										  @Nullable POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		final String pkg = ((Module) poi).getPkg();
		switch (itemClicked) {
		case 0: // Rate on Google Play
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			StoreUtils.viewAppPage(activity, pkg, activity.getString(org.mtransit.android.commons.R.string.google_play));
			return true; // HANDLED
		case 1: // Manage App
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			DeviceUtils.showAppDetailsSettings(activity, pkg);
			return true; // HANDLED
		case 2: // Uninstall
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			PackageManagerUtils.uninstallApp(activity, pkg);
			return true; // HANDLED
		case 3: // Customer service
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			final AgencyProperties agencyProperties = dataSourcesRepository.getAgencyForPkg(pkg);
			if (agencyProperties != null && agencyProperties.hasContactUs()) {
				LinkUtils.open(view, activity, agencyProperties.getContactUsWebForLang(), activity.getString(org.mtransit.android.commons.R.string.web_browser), false); // force external web browser
			}
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickPlace(@NonNull Activity activity,
											@NonNull View view,
											@NonNull DataSourcesRepository dataSourcesRepository,
											int itemClicked,
											@SuppressWarnings("unused") FavoriteManager.FavoriteUpdateListener listener,
											POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (itemClicked) {
		case 0:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_nearby_screen,
						NearbyFragment.newFixedOnPOIInstanceArgs(this, dataSourcesRepository, true), // PLACE SEARCH RESULT
						null,
						extras
				);
			} else {
				((MainActivity) activity).addFragmentToStack(
						NearbyFragment.newFixedOnPOIInstance(this, dataSourcesRepository, true) // PLACE SEARCH RESULT
				);
			}
			return true; // HANDLED
		}
		return false; // NOT HANDLED
	}

	@Nullable
	@ColorInt
	private Integer color = null;

	@ColorInt
	public int getColor(@Nullable DataSourcesRepository dataSourcesRepository) {
		return getColor(() -> dataSourcesRepository != null ? dataSourcesRepository.getAgency(poi.getAuthority()) : null);
	}

	@ColorInt
	public int getColor(@NonNull AgencyResolver agencyResolver) {
		if (this.color == null) {
			this.color = getNewColor(this.poi, agencyResolver);
		}
		return this.color;
	}

	@ColorInt
	public static int getNewColor(@NonNull POI poi, @NonNull AgencyResolver agencyResolver) {
		final Integer newColor = getNewColor(poi, agencyResolver, null);
		if (newColor == null) {
			return Color.BLACK; // default
		}
		return newColor;
	}

	@Nullable
	@ColorInt
	public static Integer getNewColor(@Nullable POI poi,
									  @Nullable DataSourcesRepository dataSourcesRepository,
									  @Nullable Integer defaultColor) {
		return getNewColor(
				poi,
				() -> dataSourcesRepository != null && poi != null ? dataSourcesRepository.getAgency(poi.getAuthority()) : null,
				defaultColor
		);
	}

	@Nullable
	@ColorInt
	public static Integer getNewColor(@Nullable POI poi,
									  @Nullable AgencyProperties agency,
									  @Nullable Integer defaultColor) {
		return getNewColor(
				poi,
				() -> agency,
				defaultColor
		);
	}

	@Nullable
	@ColorInt
	public static Integer getNewColor(@Nullable POI poi,
									  @NonNull AgencyResolver agencyResolver,
									  @Nullable Integer defaultColor) {
		if (poi != null) {
			if (poi instanceof RouteTripStop) {
				if (((RouteTripStop) poi).getRoute().hasColor()) {
					return ((RouteTripStop) poi).getRoute().getColorInt();
				}
			} else if (poi instanceof Module) {
				return ((Module) poi).getColorInt();
			}
			final IAgencyUIProperties agency = agencyResolver.getAgency();
			if (agency != null) {
				return agency.getColorInt();
			}
		}
		return defaultColor;
	}

	@Nullable
	@ColorInt
	public static Integer getNewRouteColor(@Nullable DataSourcesRepository dataSourcesRepository,
										   @Nullable Route route,
										   @Nullable String authority,
										   @Nullable Integer defaultColor) {
		return getNewRouteColor(route, defaultColor, () -> dataSourcesRepository != null && authority != null ? dataSourcesRepository.getAgency(authority) : null);
	}

	@Nullable
	@ColorInt
	public static Integer getNewRouteColor(@Nullable Route route,
										   @Nullable Integer defaultColor,
										   @NonNull AgencyResolver agencyResolver) {
		if (route != null) {
			if (route.hasColor()) {
				return route.getColorInt();
			}
		}
		final IAgencyUIProperties agency = agencyResolver.getAgency();
		final Integer agencyColorInt = agency == null ? null : agency.getColorInt();
		if (agencyColorInt != null) {
			return agencyColorInt;
		}
		return defaultColor;
	}

	@ColorInt
	public static int getRouteColorNN(@Nullable DataSourcesRepository dataSourcesRepository,
									  @Nullable Route route,
									  @Nullable String authority,
									  int defaultColor) {
		return getRouteColorNN(route, defaultColor, () -> dataSourcesRepository != null && authority != null ? dataSourcesRepository.getAgency(authority) : null);
	}

	@ColorInt
	public static int getRouteColorNN(@Nullable Route route,
									  int defaultColor,
									  @NonNull AgencyResolver agencyResolver) {
		if (route != null) {
			if (route.hasColor()) {
				return route.getColorInt();
			}
		}
		final IAgencyUIProperties agency = agencyResolver.getAgency();
		final Integer agencyColorInt = agency == null ? null : agency.getColorInt();
		if (agencyColorInt != null) {
			return agencyColorInt;
		}
		return defaultColor;
	}

	@MainThread
	@NonNull
	public String getNewOneLineDescription(@NonNull DataSourcesRepository dataSourcesRepository) {
		return getNewOneLineDescription(this.poi, dataSourcesRepository);
	}

	@MainThread
	@NonNull
	public static String getNewOneLineDescription(@NonNull POI poi, @NonNull DataSourcesRepository dataSourcesRepository) {
		return getNewOneLineDescription(poi, () -> dataSourcesRepository.getAgency(poi.getAuthority()));
	}

	@MainThread
	@NonNull
	public String getNewOneLineDescription(@Nullable IAgencyUIProperties agency) {
		return getNewOneLineDescription(this.poi, agency);
	}

	@NonNull
	public static String getNewOneLineDescription(@NonNull POI poi, @Nullable IAgencyUIProperties agency) {
		return getNewOneLineDescription(poi, () -> agency);
	}

	@NonNull
	public static String getNewOneLineDescription(@NonNull POI poi, @NonNull AgencyResolver agencyResolver) {
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
		final IAgencyUIProperties agency = agencyResolver.getAgency();
		if (agency != null) {
			if (sb.length() > 0) {
				sb.append(StringUtils.SPACE_STRING).append("-").append(StringUtils.SPACE_STRING);
			}
			sb.append(agency.getShortName());
		}
		return sb.toString();
	}

	private boolean onActionsItemClickRTS(@NonNull Activity activity,
										  @NonNull View view,
										  @NonNull FavoriteManager favoriteManager,
										  int itemClicked,
										  FavoriteManager.FavoriteUpdateListener listener,
										  POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (itemClicked) {
		case 1:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			final RouteTripStop rts = (RouteTripStop) poi;
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_rts_route_screen,
						RTSRouteFragment.newInstanceArgs(rts),
						null,
						extras
				);
			} else {
				((MainActivity) activity).addFragmentToStack(
						RTSRouteFragment.newInstance(rts),
						view
				);
			}
			return true; // HANDLED
		case 2:
			return favoriteManager.addRemoveFavorite(activity, this.poi.getUUID(), listener);
		}
		return false; // NOT HANDLED
	}

	private boolean onActionsItemClickFavoritable(Activity activity,
												  @NonNull FavoriteManager favoriteManager,
												  int itemClicked,
												  FavoriteManager.FavoriteUpdateListener listener,
												  @SuppressWarnings("unused") POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		switch (itemClicked) {
		case 1:
			return favoriteManager.addRemoveFavorite(activity, this.poi.getUUID(), listener);
		}
		return false; // NOT HANDLED
	}

	public boolean isFavoritable() {
		return isFavoritable(this.poi);
	}

	public static boolean isFavoritable(@NonNull POI poi) {
		switch (poi.getActionsType()) {
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
			return true;
		case POI.ITEM_ACTION_TYPE_NONE:
		case POI.ITEM_ACTION_TYPE_APP:
		case POI.ITEM_ACTION_TYPE_PLACE:
			return false;
		default:
			MTLog.w(LOG_TAG, "unexpected action type '%s'!", poi.getActionsType());
			return false;
		}
	}

	private boolean showPoiViewerScreen(Activity activity,
										@NonNull View view,
										DataSourcesRepository dataSourcesRepository,
										POIRepository poiRepository,
										POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
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
				final AgencyProperties agency = dataSourcesRepository.getAgencyForPkg(pkg);
				if (agency != null) {
					if (agency.isUpdateAvailable(activity.getPackageManager())) {
						AppUpdateLauncher.launchAppUpdate(activity, pkg);
					} else { // navigate to agency type screen
						PreferenceUtils.savePrefLclAsync(activity, PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(agency.getSupportedType().getId()), agency.getAuthority());
						if (FeatureFlags.F_NAVIGATION) {
							final NavController navController = Navigation.findNavController(view);
							FragmentNavigator.Extras extras = null;
							if (FeatureFlags.F_TRANSITION) {
								extras = new FragmentNavigator.Extras.Builder()
										.addSharedElement(view, view.getTransitionName())
										.build();
							}
							NavControllerExtKt.navigateF(navController,
									R.id.nav_to_type_screen,
									AgencyTypeFragment.newInstanceArgs(agency.getSupportedType()),
									null,
									extras
							);
						} else {
							((MainActivity) activity).addFragmentToStack(
									AgencyTypeFragment.newInstance(agency.getSupportedType())
							);
						}
					}
					return true; // handled
				}
			}
			StoreUtils.viewAppPage(activity, pkg, activity.getString(org.mtransit.android.commons.R.string.google_play));
			return true; // handled
		case POI.ITEM_ACTION_TYPE_PLACE:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_nearby_screen,
						NearbyFragment.newFixedOnPOIInstanceArgs(this, dataSourcesRepository, true), // PLACE SEARCH RESULT
						null,
						extras
				);
			} else {
				((MainActivity) activity).addFragmentToStack(
						NearbyFragment.newFixedOnPOIInstance(this, dataSourcesRepository, true) // PLACE SEARCH RESULT
				);
			}
			return true; // nearby screen shown
		case POI.ITEM_ACTION_TYPE_FAVORITABLE:
		case POI.ITEM_ACTION_TYPE_ROUTE_TRIP_STOP:
		default:
			if (onClickHandledListener != null) {
				onClickHandledListener.onLeaving();
			}
			poiRepository.push(this);
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_poi_screen,
						POIFragment.newInstanceArgs(this),
						null,
						extras
				);
			} else {
				((MainActivity) activity).addFragmentToStack(
						POIFragment.newInstance(this),
						view
				);
			}
			// reset to defaults, so the POI is updated when coming back in the current screen
			resetLastFindTimestamps();
			return true; // HANDLED
		}
	}

	boolean onActionItemLongClick(Activity activity,
								  View view,
								  FavoriteManager favoriteManager,
								  DataSourcesRepository dataSourcesRepository,
								  POIRepository poiRepository,
								  SparseArrayCompat<Favorite.Folder> favoriteFolders,
								  FavoriteManager.FavoriteUpdateListener favoriteUpdateListener,
								  POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		if (activity == null) {
			return false;
		}
		return showPoiMenu(activity, view, favoriteManager, dataSourcesRepository, poiRepository, favoriteFolders, favoriteUpdateListener, onClickHandledListener);
	}

	boolean onActionItemClick(Activity activity,
							  View view,
							  FavoriteManager favoriteManager,
							  DataSourcesRepository dataSourcesRepository,
							  POIRepository poiRepository,
							  SparseArrayCompat<Favorite.Folder> favoriteFolders,
							  FavoriteManager.FavoriteUpdateListener favoriteUpdateListener,
							  POIArrayAdapter.OnClickHandledListener onClickHandledListener) {
		if (activity == null) {
			return false;
		}
		boolean poiScreenShow = showPoiViewerScreen(activity, view, dataSourcesRepository, poiRepository, onClickHandledListener);
		if (!poiScreenShow) {
			poiScreenShow = showPoiMenu(activity, view, favoriteManager, dataSourcesRepository, poiRepository, favoriteFolders, favoriteUpdateListener, onClickHandledListener);
		}
		return poiScreenShow;
	}

	private boolean showPoiMenu(final @NonNull Activity activity,
								final @NonNull View view,
								final @NonNull FavoriteManager favoriteManager,
								final @NonNull DataSourcesRepository dataSourcesRepository,
								final @NonNull POIRepository poiRepository,
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
									dataSourcesRepository,
									favoriteManager,
									activity.getString(R.string.view_details), //
									favoriteFolders //
							), //
							(dialog, item) -> {
								boolean handled = onActionsItemClick(
										activity,
										view,
										favoriteManager,
										dataSourcesRepository,
										item,
										favoriteFolders,
										favoriteUpdateListener,
										onClickHandledListener
								);
								if (handled) {
									return;
								}
								switch (item) {
								case 0:
									showPoiViewerScreen(activity, view, dataSourcesRepository, poiRepository, onClickHandledListener);
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

	@NonNull
	static POIManager fromCursorStatic(@NonNull Cursor cursor, @NonNull String authority) {
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
			MTLog.w(LOG_TAG, "Unexpected POI type '%s'! (using default)", DefaultPOI.getTypeFromCursor(cursor));
			return new POIManager(DefaultPOI.fromCursorStatic(cursor, authority));
		}
	}

	@NonNull
	@Override
	public POI getPOI() {
		return this.poi;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		POIManager that = (POIManager) o;

		if (Float.compare(that.distance, distance) != 0) return false;
		if (inFocus != that.inFocus) return false;
		if (lastFindStatusTimestampMs != that.lastFindStatusTimestampMs) return false;
		if (scheduleMaxDataRequests != that.scheduleMaxDataRequests) return false;
		if (lastFindServiceUpdateTimestampMs != that.lastFindServiceUpdateTimestampMs) return false;
		if (!poi.equals(that.poi)) return false;
		if (!Objects.equals(distanceString, that.distanceString)) return false;
		if (!Objects.equals(status, that.status)) return false;
		if (!Objects.equals(serviceUpdates, that.serviceUpdates)) return false;
		if (!Objects.equals(statusLoaderListenerWR, that.statusLoaderListenerWR)) return false;
		if (!Objects.equals(serviceUpdateLoaderListenerWR, that.serviceUpdateLoaderListenerWR))
			return false;
		return Objects.equals(color, that.color);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + poi.hashCode();
		result = 31 * result + (distanceString != null ? distanceString.hashCode() : 0);
		result = 31 * result + (distance != 0.0f ? Float.floatToIntBits(distance) : 0);
		result = 31 * result + (status != null ? status.hashCode() : 0);
		result = 31 * result + (serviceUpdates != null ? serviceUpdates.hashCode() : 0);
		result = 31 * result + (inFocus ? 1 : 0);
		result = 31 * result + (int) (lastFindStatusTimestampMs ^ (lastFindStatusTimestampMs >>> 32));
		result = 31 * result + (statusLoaderListenerWR != null ? statusLoaderListenerWR.hashCode() : 0);
		result = 31 * result + scheduleMaxDataRequests;
		result = 31 * result + (serviceUpdateLoaderListenerWR != null ? serviceUpdateLoaderListenerWR.hashCode() : 0);
		result = 31 * result + (int) (lastFindServiceUpdateTimestampMs ^ (lastFindServiceUpdateTimestampMs >>> 32));
		result = 31 * result + (color != null ? color.hashCode() : 0);
		return result;
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

	public static class POISameRouteComparator implements Comparator<POIManager> {

		@Nullable
		private RouteTripStop targetedRouteTripStop = null;

		public void setTargetedRouteTripStop(@Nullable RouteTripStop targetedRouteTripStop) {
			this.targetedRouteTripStop = targetedRouteTripStop;
		}

		@Nullable
		public RouteTripStop getTargetedRouteTripStop() {
			return targetedRouteTripStop;
		}

		@Override
		public int compare(@Nullable POIManager poim1, @Nullable POIManager poim2) {
			if (this.targetedRouteTripStop != null
					&& poim1 != null
					&& poim2 != null
					&& poim1.poi instanceof RouteTripStop
					&& poim2.poi instanceof RouteTripStop
			) {
				if (isSameLocation(poim1, poim2)) {
					final boolean poim1SameRoute = isTargetedRouteAndStop(poim1.poi);
					final boolean poim2SameRoute = isTargetedRouteAndStop(poim2.poi);
					if (poim1SameRoute && !poim2SameRoute) {
						return ComparatorUtils.BEFORE;
					} else if (!poim1SameRoute && poim2SameRoute) {
						return ComparatorUtils.AFTER;
					}
				}
			}
			return ComparatorUtils.SAME;
		}

		private boolean isSameLocation(@NonNull POIManager poim1, @NonNull POIManager poim2) {
			if (poim1.hasLocation() && poim2.hasLocation()) {
				return poim1.getLat() == poim2.getLat()
						&& poim1.getLng() == poim2.getLng();
			}
			return false;
		}

		private boolean isTargetedRouteAndStop(@NonNull POI poi) {
			if (poi instanceof RouteTripStop && targetedRouteTripStop != null) {
				final RouteTripStop rts = (RouteTripStop) poi;
				return rts.getRoute().equals(targetedRouteTripStop.getRoute())
						&& rts.getStop().equals(targetedRouteTripStop.getStop());
			}
			return false;
		}
	}

	public interface AgencyResolver {
		@Nullable
		IAgencyUIProperties getAgency();
	}
}
