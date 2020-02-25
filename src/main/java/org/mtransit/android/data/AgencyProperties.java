package org.mtransit.android.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.text.TextUtils;

public class AgencyProperties implements MTLog.Loggable {

	private static final String LOG_TAG = AgencyProperties.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final AgencyPropertiesShortNameComparator SHORT_NAME_COMPARATOR = new AgencyPropertiesShortNameComparator();

	private String id;
	private DataSourceType type;
	private String shortName;
	private String longName;
	@ColorInt
	private Integer colorInt = null;
	private LocationUtils.Area area;
	private boolean isRTS;

	public AgencyProperties(String id, DataSourceType type, String shortName, String longName, String color, LocationUtils.Area area, boolean isRTS) {
		this.id = id;
		this.type = type;
		this.shortName = shortName;
		this.longName = longName;
		this.area = area;
		this.isRTS = isRTS;
		setColor(color);
	}

	@Nullable
	private JPaths logo = null;

	@Nullable
	public JPaths getLogo() {
		return logo;
	}

	public void setLogo(@Nullable JPaths logo) {
		this.logo = logo;
	}

	public void setColor(String color) {
		if (!TextUtils.isEmpty(color)) {
			this.colorInt = ColorUtils.parseColor(color);
		}
	}

	@ColorInt
	public int getColorInt() {
		if (colorInt == null) {
			return 0;
		}
		return colorInt;
	}

	public boolean hasColor() {
		return this.colorInt != null;
	}

	public boolean isRTS() {
		return isRTS;
	}

	public DataSourceType getType() {
		return type;
	}

	public String getShortName() {
		return shortName;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(AgencyProperties.class.getSimpleName()).append('{') //
				.append("id:").append(this.id).append(',') //
				.append("type:").append(this.type).append(',') //
				.append("shortName:").append(this.shortName).append(',') //
				.append("longName:").append(this.longName).append(',') //
				.append("area:").append(this.area).append(',') //
				.append('}').toString();
	}

	public String getAuthority() {
		return id;
	}

	public boolean isInArea(LocationUtils.Area area) {
		return LocationUtils.Area.areOverlapping(area, this.area);
	}

	public boolean isEntirelyInside(@Nullable LatLngBounds area) {
		return area != null //
				&& area.contains(new LatLng(this.area.minLat, this.area.minLng)) //
				&& area.contains(new LatLng(this.area.maxLat, this.area.maxLng));
	}

	public boolean isInArea(LatLngBounds area) {
		return areOverlapping(area, this.area);
	}

	private static boolean areOverlapping(@Nullable LatLngBounds area1, @Nullable LocationUtils.Area area2) {
		if (area1 == null || area2 == null) {
			return false; // no data to compare
		}
		if (LocationUtils.isInside(area1.southwest.latitude, area1.southwest.longitude, area2)) {
			return true; // min lat, min lng
		}
		if (LocationUtils.isInside(area1.southwest.latitude, area1.northeast.longitude, area2)) {
			return true; // min lat, max lng
		}
		if (LocationUtils.isInside(area1.northeast.latitude, area1.southwest.longitude, area2)) {
			return true; // max lat, min lng
		}
		if (LocationUtils.isInside(area1.northeast.latitude, area1.northeast.longitude, area2)) {
			return true; // max lat, max lng
		}
		if (isInside(area2.minLat, area2.minLng, area1)) {
			return true; // min lat, min lng
		}
		if (isInside(area2.minLat, area2.maxLng, area1)) {
			return true; // min lat, max lng
		}
		if (isInside(area2.maxLat, area2.minLng, area1)) {
			return true; // max lat, min lng
		}
		//noinspection SimplifiableIfStatement
		if (isInside(area2.maxLat, area2.maxLng, area1)) {
			return true; // max lat, max lng
		}
		return areCompletelyOverlapping(area1, area2);
	}

	public static boolean isInside(double lat, double lng, LatLngBounds area) {
		if (area == null) {
			return false;
		}
		double minLat = Math.min(area.southwest.latitude, area.northeast.latitude);
		double maxLat = Math.max(area.southwest.latitude, area.northeast.latitude);
		double minLng = Math.min(area.southwest.longitude, area.northeast.longitude);
		double maxLng = Math.max(area.southwest.longitude, area.northeast.longitude);
		return LocationUtils.isInside(lat, lng, minLat, maxLat, minLng, maxLng);
	}

	private static boolean areCompletelyOverlapping(LatLngBounds area1, LocationUtils.Area area2) {
		double area1MinLat = Math.min(area1.southwest.latitude, area1.northeast.latitude);
		double area1MaxLat = Math.max(area1.southwest.latitude, area1.northeast.latitude);
		double area1MinLng = Math.min(area1.southwest.longitude, area1.northeast.longitude);
		double area1MaxLng = Math.max(area1.southwest.longitude, area1.northeast.longitude);
		if (area1MinLat >= area2.minLat && area1MaxLat <= area2.maxLat //
				&& area2.minLng >= area1MinLng && area2.maxLng <= area1MaxLng) {
			return true; // area 1 wider than area 2 but area 2 higher than area 1
		}
		//noinspection RedundantIfStatement
		if (area2.minLat >= area1MinLat && area2.maxLat <= area1MaxLat //
				&& area1MinLng >= area2.minLng && area1MaxLng <= area2.maxLng) {
			return true; // area 2 wider than area 1 but area 1 higher than area 2
		}
		return false;
	}

	public boolean isEntirelyInside(LocationUtils.Area otherArea) {
		return this.area == null || this.area.isEntirelyInside(otherArea);
	}

	public static void removeType(Collection<AgencyProperties> agencies, DataSourceType typeToRemove) {
		if (agencies != null) {
			Iterator<AgencyProperties> it = agencies.iterator();
			while (it.hasNext()) {
				if (it.next().type == typeToRemove) {
					it.remove();
				}
			}
		}
	}

	protected static class AgencyPropertiesShortNameComparator implements Comparator<AgencyProperties> {
		@Override
		public int compare(AgencyProperties lap, AgencyProperties rap) {
			String lShortName = lap.getShortName();
			String rShortName = rap.getShortName();
			if (lShortName == null) {
				return rShortName == null ? 0 : -1;
			} else if (rShortName == null) {
				return +1;
			}
			return lShortName.toLowerCase(Locale.getDefault()).compareTo(rShortName.toLowerCase(Locale.getDefault()));
		}
	}
}
