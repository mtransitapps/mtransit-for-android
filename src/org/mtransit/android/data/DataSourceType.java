package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.Comparator;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;

import android.content.Context;

public enum DataSourceType {

	TYPE_SUBWAY(1, // GTFS - Metro
			R.string.agency_type_subway_short_name, R.string.agency_type_subway_all, R.string.agency_type_subway_stations_short_name, //
			R.drawable.ic_menu_subway_holo_light, R.drawable.ic_menu_subway_holo_dark, //
			true), //
	TYPE_RAIL(2, // GTFS - Train
			R.string.agency_type_rail_short_name, R.string.agency_type_rail_all, R.string.agency_type_rail_stations_short_name, //
			R.drawable.ic_menu_train_holo_light, R.drawable.ic_menu_train_holo_dark, //
			true), //
	TYPE_BUS(3, // GTFS - Bus
			R.string.agency_type_bus_short_name, R.string.agency_type_bus_all, R.string.agency_type_bus_stops_short_name, //
			R.drawable.ic_menu_bus_holo_light, R.drawable.ic_menu_bus_holo_dark, //
			true), //
	TYPE_BIKE(100, // like Bixi, Velib
			R.string.agency_type_bike_short_name, R.string.agency_type_bike_all, R.string.agency_type_bike_stations_short_name, //
			R.drawable.ic_menu_bike_holo_light, R.drawable.ic_menu_bike_holo_dark, //
			true), //
	TYPE_MODULE(999, //
			R.string.agency_type_module_short_name, R.string.agency_type_module_all, R.string.agency_type_module_app_short_name, //
			R.drawable.ic_menu_play_store_holo_light, R.drawable.ic_menu_play_store_holo_dark, //
			false), //
	;

	private static final String TAG = DataSourceType.class.getSimpleName();

	private int id;

	private int shortNameResId;

	private int allStringResId;
	private int poiShortNameResId;
	private int menuResId;

	private int abIconResId;

	private boolean searchable;

	DataSourceType(int id, int shortNameResId, int allStringResId, int poiShortNameResId, int menuResId, int abIconResId, boolean searchable) {
		this.id = id;
		this.shortNameResId = shortNameResId;
		this.allStringResId = allStringResId;
		this.poiShortNameResId = poiShortNameResId;
		this.menuResId = menuResId;
		this.abIconResId = abIconResId;
		this.searchable = searchable;
	}

	public int getId() {
		return id;
	}

	public int getShortNameResId() {
		return shortNameResId;
	}

	public int getAllStringResId() {
		return allStringResId;
	}

	public int getPoiShortNameResId() {
		return poiShortNameResId;
	}

	public int getMenuResId() {
		return menuResId;
	}

	public int getAbIconResId() {
		return abIconResId;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public static DataSourceType parseId(int id) {
		switch (id) {
		case 1:
			return TYPE_SUBWAY;
		case 2:
			return TYPE_RAIL;
		case 3:
			return TYPE_BUS;
		case 100:
			return TYPE_BIKE;
		case 999:
			return TYPE_MODULE;
		default:
			MTLog.w(TAG, "ID '%s' doesn't match any type!", id);
			return null;
		}
	}

	public static class DataSourceTypeComparator implements Comparator<DataSourceType> {

		@Override
		public int compare(DataSourceType ldst, DataSourceType rdst) {
			return ldst.id - rdst.id;
		}

	}

	public static class DataSourceTypeShortNameComparator implements Comparator<DataSourceType> {

		private WeakReference<Context> contextWR;

		public DataSourceTypeShortNameComparator(Context context) {
			this.contextWR = new WeakReference<Context>(context);
		}

		@Override
		public int compare(DataSourceType ldst, DataSourceType rdst) {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return 0;
			}
			if (ldst.equals(rdst)) {
				return 0;
			}
			if (TYPE_MODULE.equals(ldst)) {
				return +1;
			} else if (TYPE_MODULE.equals(rdst)) {
				return -1;
			}
			String lShortName = context.getString(ldst.getShortNameResId());
			String rShortName = context.getString(rdst.getShortNameResId());
			return lShortName.compareTo(rShortName);
		}

	}

	public static class POIManagerTypeShortNameComparator implements Comparator<POIManager> {

		private WeakReference<Context> contextWR;

		public POIManagerTypeShortNameComparator(Context context) {
			this.contextWR = new WeakReference<Context>(context);
		}

		@Override
		public int compare(POIManager lpoim, POIManager rpoim) {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return 0;
			}
			AgencyProperties lagency = DataSourceProvider.get(context).getAgency(context, lpoim.poi.getAuthority());
			AgencyProperties ragency = DataSourceProvider.get(context).getAgency(context, rpoim.poi.getAuthority());
			if (lagency == null || ragency == null) {
				return 0;
			}
			int lshortNameResId = lagency.getType().getShortNameResId();
			int rshortNameResId = ragency.getType().getShortNameResId();
			String lShortName = context.getString(lshortNameResId);
			String rShortName = context.getString(rshortNameResId);
			return lShortName.compareTo(rShortName);
		}
	}
}
