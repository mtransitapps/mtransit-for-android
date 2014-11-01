package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.Comparator;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;

import android.content.Context;

public enum DataSourceType {

	TYPE_SUBWAY(1, R.string.agency_type_subway_short_name, R.drawable.ic_menu_subway, R.drawable.ic_menu_subway_holo_light), // GTFS - Metro
	TYPE_RAIL(2, R.string.agency_type_rail_short_name, R.drawable.ic_menu_train, R.drawable.ic_menu_train_holo_light), // GTFS - Train
	TYPE_BUS(3, R.string.agency_type_bus_short_name, R.drawable.ic_menu_bus, R.drawable.ic_menu_bus_holo_light), // GTFS - Bus
	TYPE_BIKE(100, R.string.agency_type_bike_short_name, R.drawable.ic_menu_bike, R.drawable.ic_menu_bike_holo_light), // like Bixi, Velib
	TYPE_MODULE(999, R.string.agency_type_module_short_name, R.drawable.ic_menu_play_store, R.drawable.ic_menu_play_store_holo_light), //
	;

	private static final String TAG = DataSourceType.class.getSimpleName();

	private int id;

	private int shortNameResId;

	private int menuResId;

	private int abIconResId;

	DataSourceType(int id, int shortNameResId, int menuResId, int abIconResId) {
		this.id = id;
		this.shortNameResId = shortNameResId;
		this.menuResId = menuResId;
		this.abIconResId = abIconResId;
	}

	public int getId() {
		return id;
	}

	public int getShortNameResId() {
		return shortNameResId;
	}

	public int getMenuResId() {
		return menuResId;
	}

	public int getAbIconResId() {
		return abIconResId;
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
			final Context context = this.contextWR == null ? null : this.contextWR.get();
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
			final Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return 0;
			}
			final AgencyProperties lagency = DataSourceProvider.get().getAgency(context, lpoim.poi.getAuthority());
			final AgencyProperties ragency = DataSourceProvider.get().getAgency(context, rpoim.poi.getAuthority());
			final int lshortNameResId = lagency.getType().getShortNameResId();
			final int rshortNameResId = ragency.getType().getShortNameResId();
			final String lShortName = context.getString(lshortNameResId);
			final String rShortName = context.getString(rshortNameResId);
			return lShortName.compareTo(rShortName);
		}
	}
}
