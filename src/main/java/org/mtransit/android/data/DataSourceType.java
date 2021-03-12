package org.mtransit.android.data;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.mtransit.android.R;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;

import java.lang.ref.WeakReference;
import java.util.Comparator;

public enum DataSourceType {

	TYPE_LIGHT_RAIL(0, // GTFS - Tram, Streetcar
			R.string.agency_type_light_rail_short_name, R.string.agency_type_light_rail_all, //
			R.string.agency_type_light_rail_stations_short_name, R.string.agency_type_light_rail_nearby, //
			R.drawable.ic_tram_black_24dp, //
			R.id.nav_light_rail, //
			true, true, true, true, true), //
	TYPE_SUBWAY(1, // GTFS - Metro
			R.string.agency_type_subway_short_name, R.string.agency_type_subway_all, //
			R.string.agency_type_subway_stations_short_name, R.string.agency_type_subway_nearby, //
			R.drawable.ic_directions_subway_black_24dp, //
			R.id.nav_subway, //
			true, true, true, true, true), //
	TYPE_RAIL(2, // GTFS - Train
			R.string.agency_type_rail_short_name, R.string.agency_type_rail_all, //
			R.string.agency_type_rail_stations_short_name, R.string.agency_type_rail_nearby, //
			R.drawable.ic_directions_railway_black_24dp, //
			R.id.nav_rail, //
			true, true, true, true, true), //
	TYPE_BUS(3, // GTFS - Bus
			R.string.agency_type_bus_short_name, R.string.agency_type_bus_all, //
			R.string.agency_type_bus_stops_short_name, R.string.agency_type_bus_nearby, //
			R.drawable.ic_directions_bus_black_24dp, //
			R.id.nav_bus, //
			true, true, true, true, true), //
	TYPE_FERRY(4, // GTFS - Boat
			R.string.agency_type_ferry_short_name, R.string.agency_type_ferry_all, //
			R.string.agency_type_ferry_stations_short_name, R.string.agency_type_ferry_nearby, //
			R.drawable.ic_directions_boat_black_24dp, //
			R.id.nav_ferry, //
			true, true, true, true, true), //
	TYPE_BIKE(100, // like BIXI, Velib
			R.string.agency_type_bike_short_name, R.string.agency_type_bike_all, //
			R.string.agency_type_bike_stations_short_name, R.string.agency_type_bike_nearby, //
			R.drawable.ic_directions_bike_black_24dp, //
			R.id.nav_bike, //
			true, true, true, true, true), //
	TYPE_PLACE(666, //
			R.string.agency_type_place_short_name, R.string.agency_type_place_all, //
			R.string.agency_type_place_app_short_name, R.string.agency_type_place_nearby, //
			-1, //
			R.drawable.ic_place_black_24dp, //
			false, false, false, false, true), //
	TYPE_MODULE(999, //
			R.string.agency_type_module_short_name, R.string.agency_type_module_all, //
			R.string.agency_type_module_app_short_name, R.string.agency_type_module_nearby, //
			R.drawable.ic_library_add_black_24dp, //
			R.id.nav_module, //
			true, true, true, false, false), //
	;

	private static final String TAG = DataSourceType.class.getSimpleName();

	public static final int MAX_ID = 1000;

	private final int id;

	@StringRes
	private final int shortNameResId;
	@StringRes
	private final int allStringResId;
	@StringRes
	private final int poiShortNameResId;
	@StringRes
	private final int nearbyNameResId;

	@DrawableRes
	private final int iconResId;

	private final int navResId;

	private final boolean menuList;
	private final boolean homeScreen;
	private final boolean nearbyScreen;
	private final boolean mapScreen;
	private final boolean searchable;

	DataSourceType(int id,
				   @StringRes int shortNameResId, @StringRes int allStringResId, @StringRes int poiShortNameResId, @StringRes int nearbyNameResId,
				   @DrawableRes int iconResId,
				   int navResId,
				   boolean menuList, boolean homeScreen, boolean nearbyScreen, boolean mapScreen, boolean searchable) {
		if (id >= MAX_ID) {
			throw new UnsupportedOperationException(String.format("Data source type ID '%s' must be lower than '%s'!", id, MAX_ID));
		}
		this.id = id;
		this.shortNameResId = shortNameResId;
		this.allStringResId = allStringResId;
		this.poiShortNameResId = poiShortNameResId;
		this.nearbyNameResId = nearbyNameResId;
		this.iconResId = iconResId;
		this.navResId = navResId;
		this.menuList = menuList;
		this.homeScreen = homeScreen;
		this.nearbyScreen = nearbyScreen;
		this.mapScreen = mapScreen;
		this.searchable = searchable;
	}

	public int getId() {
		return id;
	}

	@StringRes
	public int getShortNameResId() {
		return shortNameResId;
	}

	@StringRes
	public int getAllStringResId() {
		return allStringResId;
	}

	@StringRes
	public int getPoiShortNameResId() {
		return poiShortNameResId;
	}

	@StringRes
	public int getNearbyNameResId() {
		return nearbyNameResId;
	}

	@DrawableRes
	public int getIconResId() {
		return iconResId;
	}

	public int getNavResId() {
		return navResId;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isMenuList() {
		return this.menuList;
	}

	public boolean isHomeScreen() {
		return this.homeScreen;
	}

	public boolean isNearbyScreen() {
		return this.nearbyScreen;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isMapScreen() {
		return this.mapScreen;
	}

	public boolean isSearchable() {
		return searchable;
	}

	@Nullable
	public static DataSourceType parseId(@Nullable Integer id) {
		if (id == null) {
			MTLog.w(TAG, "ID 'null' doesn't match any type!");
			return null;
		}
		switch (id) {
		case 0:
			return TYPE_LIGHT_RAIL;
		case 1:
			return TYPE_SUBWAY;
		case 2:
			return TYPE_RAIL;
		case 3:
			return TYPE_BUS;
		case 4:
			return TYPE_FERRY;
		case 100:
			return TYPE_BIKE;
		case 666:
			return TYPE_PLACE;
		case 999:
			return TYPE_MODULE;
		default:
			MTLog.w(TAG, "ID '%s' doesn't match any type!", id);
			return null;
		}
	}

	@Nullable
	public static DataSourceType parseNavResId(@Nullable Integer navResId) {
		if (navResId == null) {
			MTLog.w(TAG, "Nav res ID 'null' doesn't match any type!");
			return null;
		}
		if (navResId == R.id.nav_light_rail) {
			return TYPE_LIGHT_RAIL;
		} else if (navResId == R.id.nav_subway) {
			return TYPE_SUBWAY;
		} else if (navResId == R.id.nav_rail) {
			return TYPE_RAIL;
		} else if (navResId == R.id.nav_bus) {
			return TYPE_BUS;
		} else if (navResId == R.id.nav_ferry) {
			return TYPE_FERRY;
		} else if (navResId == R.id.nav_bike) {
			return TYPE_BIKE;
		} else if (navResId == R.id.nav_module) {
			return TYPE_MODULE;
		}
		MTLog.w(TAG, "Nav res ID '5s' doesn't match any type!", navResId);
		return null;
	}

	@SuppressWarnings("unused")
	public static class DataSourceTypeComparator implements Comparator<DataSourceType> {

		@Override
		public int compare(@NonNull DataSourceType lType, @NonNull DataSourceType rType) {
			return lType.id - rType.id;
		}
	}

	public static class DataSourceTypeShortNameComparator implements Comparator<DataSourceType> {

		@NonNull
		private final WeakReference<Context> contextWR;

		DataSourceTypeShortNameComparator(@NonNull Context context) {
			this.contextWR = new WeakReference<>(context);
		}

		@Override
		public int compare(@NonNull DataSourceType lType, @NonNull DataSourceType rType) {
			Context context = this.contextWR.get();
			if (context == null) {
				return ComparatorUtils.SAME;
			}
			if (lType.equals(rType)) {
				return ComparatorUtils.SAME;
			}
			if (TYPE_MODULE.equals(lType)) {
				return ComparatorUtils.AFTER;
			} else if (TYPE_MODULE.equals(rType)) {
				return ComparatorUtils.BEFORE;
			} else if (TYPE_PLACE.equals(lType)) {
				return ComparatorUtils.AFTER;
			} else if (TYPE_PLACE.equals(rType)) {
				return ComparatorUtils.BEFORE;
			}
			String lShortName = context.getString(lType.getShortNameResId());
			String rShortName = context.getString(rType.getShortNameResId());
			return lShortName.compareTo(rShortName);
		}
	}

	public static class POIManagerTypeShortNameComparator implements Comparator<POIManager> {

		@NonNull
		private final WeakReference<Context> contextWR;

		public POIManagerTypeShortNameComparator(@NonNull Context context) {
			this.contextWR = new WeakReference<>(context);
		}

		@Override
		public int compare(@NonNull POIManager lPoim, @NonNull POIManager rPoim) {
			Context context = this.contextWR.get();
			if (context == null) {
				return 0;
			}
			AgencyProperties lAgency = DataSourceProvider.get(context).getAgency(context, lPoim.poi.getAuthority());
			AgencyProperties rAgency = DataSourceProvider.get(context).getAgency(context, rPoim.poi.getAuthority());
			if (lAgency == null || rAgency == null) {
				return 0;
			}
			int lShortNameResId = lAgency.getType().getShortNameResId();
			int rShortNameResId = rAgency.getType().getShortNameResId();
			String lShortName = context.getString(lShortNameResId);
			String rShortName = context.getString(rShortNameResId);
			return lShortName.compareTo(rShortName);
		}
	}
}
