package org.mtransit.android.data;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.mtransit.android.R;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.datasource.DataSourcesRepository;

import java.lang.ref.WeakReference;
import java.util.Comparator;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@SuppressWarnings("WeakerAccess")
public enum DataSourceType {

	TYPE_LIGHT_RAIL(DataSourceTypeId.LIGHT_RAIL, false, // GTFS - LRT
			DataSourceStopType.STATION,
			R.string.agency_type_light_rail_short_name, R.string.agency_type_light_rail_all, //
			R.drawable.ic_light_rail_black_24dp, //
			R.id.root_nav_light_rail, //
			true, true, true, true, true), //
	TYPE_SUBWAY(DataSourceTypeId.SUBWAY, false, // GTFS - Metro
			DataSourceStopType.STATION,
			R.string.agency_type_subway_short_name, R.string.agency_type_subway_all, //
			R.drawable.ic_directions_subway_black_24dp, //
			R.id.root_nav_subway, //
			true, true, true, true, true), //
	TYPE_RAIL(DataSourceTypeId.RAIL, false, // GTFS - Train
			DataSourceStopType.TRAIN_STATION,
			R.string.agency_type_rail_short_name, R.string.agency_type_rail_all, //
			R.drawable.ic_directions_railway_black_24dp, //
			R.id.root_nav_rail, //
			true, true, true, true, true), //
	TYPE_BUS(DataSourceTypeId.BUS, false, // GTFS - Bus
			DataSourceStopType.STOP,
			R.string.agency_type_bus_short_name, R.string.agency_type_bus_all, //
			R.drawable.ic_directions_bus_black_24dp, //
			R.id.root_nav_bus, //
			true, true, true, true, true), //
	TYPE_FERRY(DataSourceTypeId.FERRY, false, // GTFS - Boat
			DataSourceStopType.TERMINAL,
			R.string.agency_type_ferry_short_name, R.string.agency_type_ferry_all, //
			R.drawable.ic_directions_boat_black_24dp, //
			R.id.root_nav_ferry, //
			true, true, true, true, true), //

	TYPE_TRAM(DataSourceTypeId.EX_TRAM, true, // GTFS - Tram, Streetcar
			DataSourceStopType.STOP,
			R.string.agency_type_tram_short_name, R.string.agency_type_tram_all, //
			R.drawable.ic_light_rail_black_24dp, //
			R.id.root_nav_tram, //
			true, true, true, true, true), //

	TYPE_BIKE(DataSourceTypeId.BIKE, false,// like BIXI, Velib
			DataSourceStopType.STATION,
			R.string.agency_type_bike_short_name, R.string.agency_type_bike_all, //
			R.drawable.ic_directions_bike_black_24dp, //
			R.id.root_nav_bike, //
			true, true, true, true, true), //

	TYPE_PLACE(DataSourceTypeId.PLACE, false,//
			DataSourceStopType.PLACE,
			R.string.agency_type_place_short_name, R.string.agency_type_place_all, //
			R.drawable.ic_place_black_24dp, //
			-1, // no nav ID
			false, false, false, false, true), //
	TYPE_MODULE(DataSourceTypeId.MODULE, false,//
			DataSourceStopType.MODULE,
			R.string.agency_type_module_short_name, R.string.agency_type_module_all, //
			R.drawable.ic_library_add_black_24dp, //
			R.id.root_nav_module, //
			true, true, true, false, false), //
	TYPE_FAVORITE(DataSourceTypeId.FAVORITE, false,//
			DataSourceStopType.FAVORITE,
			R.string.agency_type_favorite_short_name, R.string.agency_type_favorite_all, //
			R.drawable.ic_star_black_24dp, //
			R.id.root_nav_favorites, //
			false, false, false, false, false), //

	TYPE_NEWS(DataSourceTypeId.NEWS, false, //
			DataSourceStopType.NEWS_ARTICLE,
			R.string.agency_type_news_short_name, R.string.agency_type_news_all, //
			R.drawable.ic_newspaper_black_24dp, //
			R.id.root_nav_news, //
			false, false, false, false, false), //
	;

	private static final String LOG_TAG = DataSourceType.class.getSimpleName();

	public static final int MAX_ID = 1000;

	@DataSourceTypeId.DataSourceType
	private final int id;
	private final boolean extendedType;

	private final DataSourceStopType stopType;

	@StringRes
	private final int shortNameResId;
	@StringRes
	private final int shortNamesResId;

	@DrawableRes
	private final int iconResId;

	private final int navResId;

	private final boolean menuList;
	private final boolean homeScreen;
	private final boolean nearbyScreen;
	private final boolean mapScreen;
	private final boolean searchable;

	DataSourceType(@DataSourceTypeId.DataSourceType int id,
				   boolean extendedType,
				   @NonNull DataSourceStopType stopType,
				   @StringRes int shortNameResId, @StringRes int shortNamesResId,
				   @DrawableRes int iconResId,
				   @IdRes int navResId,
				   boolean menuList, boolean homeScreen, boolean nearbyScreen, boolean mapScreen, boolean searchable) {
		if (id >= MAX_ID) {
			throw new UnsupportedOperationException(String.format("Data source type ID '%s' must be lower than '%s'!", id, MAX_ID));
		}
		this.id = id;
		this.extendedType = extendedType;
		this.stopType = stopType;
		this.shortNameResId = shortNameResId;
		this.shortNamesResId = shortNamesResId;
		this.iconResId = iconResId;
		this.navResId = navResId;
		this.menuList = menuList;
		this.homeScreen = homeScreen;
		this.nearbyScreen = nearbyScreen;
		this.mapScreen = mapScreen;
		this.searchable = searchable;
	}

	@DataSourceTypeId.DataSourceType
	public int getId() {
		return id;
	}

	public boolean isExtendedType() {
		return extendedType;
	}

	@StringRes
	public int getShortNameResId() {
		return shortNameResId;
	}

	@StringRes
	public int getShortNamesResId() {
		return shortNamesResId;
	}

	@NonNull
	public CharSequence getPoiShortName(@NonNull Context context) {
		return context.getString(R.string.agency_type_stops_short_name,
				context.getString(getShortNamesResId()),
				context.getString(this.stopType.getStopsStringResId())
		);
	}

	@NonNull
	public CharSequence getNearbyName(@NonNull Context context) {
		return context.getString(R.string.agency_type_stops_nearby,
				context.getString(this.stopType.getStopsStringResId())
		);
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
	public static DataSourceType parseId(@DataSourceTypeId.DataSourceType @Nullable Integer id) {
		if (id == null) {
			MTLog.w(LOG_TAG, "ID 'null' doesn't match any type!");
			return null;
		}
		switch (id) {
		case DataSourceTypeId.LIGHT_RAIL:
			return TYPE_LIGHT_RAIL;
		case DataSourceTypeId.SUBWAY:
			return TYPE_SUBWAY;
		case DataSourceTypeId.RAIL:
			return TYPE_RAIL;
		case DataSourceTypeId.BUS:
			return TYPE_BUS;
		case DataSourceTypeId.FERRY:
			return TYPE_FERRY;
		case DataSourceTypeId.EX_TRAM:
			return TYPE_TRAM;
		case DataSourceTypeId.BIKE:
			return TYPE_BIKE;
		case DataSourceTypeId.PLACE:
			return TYPE_PLACE;
		case DataSourceTypeId.MODULE:
			return TYPE_MODULE;
		case DataSourceTypeId.NEWS:
			return TYPE_NEWS;
		case DataSourceTypeId.INVALID:
			return null;
		default:
			MTLog.w(LOG_TAG, "ID '%s' doesn't match any type!", id);
			return null;
		}
	}

	@Nullable
	public static DataSourceType parseNavResId(@Nullable Integer navResId) {
		if (navResId == null) {
			MTLog.w(LOG_TAG, "Nav res ID 'null' doesn't match any type!");
			return null;
		}
		if (navResId == R.id.root_nav_light_rail) {
			return TYPE_LIGHT_RAIL;
		} else if (navResId == R.id.root_nav_tram) {
			return TYPE_TRAM;
		} else if (navResId == R.id.root_nav_subway) {
			return TYPE_SUBWAY;
		} else if (navResId == R.id.root_nav_rail) {
			return TYPE_RAIL;
		} else if (navResId == R.id.root_nav_bus) {
			return TYPE_BUS;
		} else if (navResId == R.id.root_nav_ferry) {
			return TYPE_FERRY;
		} else if (navResId == R.id.root_nav_bike) {
			return TYPE_BIKE;
		} else if (navResId == R.id.root_nav_module) {
			return TYPE_MODULE;
		}
		MTLog.w(LOG_TAG, "Nav res ID '5s' doesn't match any type!", navResId);
		return null;
	}

	public static class DataSourceTypeShortNameComparator implements Comparator<DataSourceType> {

		@NonNull
		private final WeakReference<Context> contextWR;

		public DataSourceTypeShortNameComparator(@NonNull Context context) {
			this.contextWR = new WeakReference<>(context);
		}

		@Override
		public int compare(@NonNull DataSourceType lType, @NonNull DataSourceType rType) {
			final Context context = this.contextWR.get();
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
			final String lShortName = context.getString(lType.getShortNameResId());
			final String rShortName = context.getString(rType.getShortNameResId());
			return lShortName.compareTo(rShortName);
		}
	}

	@Singleton
	public static class POIManagerTypeShortNameComparator implements Comparator<POIManager> {

		@NonNull
		private final Context appContext;
		@NonNull
		private final DataSourcesRepository dataSourcesRepository;

		@Inject
		POIManagerTypeShortNameComparator(@NonNull @ApplicationContext Context appContext,
										  @NonNull DataSourcesRepository dataSourcesRepository) {
			this.appContext = appContext;
			this.dataSourcesRepository = dataSourcesRepository;
		}

		@Override
		public int compare(@NonNull POIManager lPoim, @NonNull POIManager rPoim) {
			final AgencyProperties lAgency = this.dataSourcesRepository.getAgency(lPoim.poi.getAuthority());
			final AgencyProperties rAgency = this.dataSourcesRepository.getAgency(rPoim.poi.getAuthority());
			if (lAgency == null || rAgency == null) {
				return 0;
			}
			final int lShortNameResId = lAgency.getSupportedType().getShortNameResId();
			final int rShortNameResId = rAgency.getSupportedType().getShortNameResId();
			final String lShortName = this.appContext.getString(lShortNameResId);
			final String rShortName = this.appContext.getString(rShortNameResId);
			return lShortName.compareTo(rShortName);
		}
	}
}
