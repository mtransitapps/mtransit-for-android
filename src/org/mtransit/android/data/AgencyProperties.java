package org.mtransit.android.data;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;

import android.text.TextUtils;
public class AgencyProperties implements MTLog.Loggable {

	private static final String TAG = AgencyProperties.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final AgencyPropertiesShortNameComparator SHORT_NAME_COMPARATOR = new AgencyPropertiesShortNameComparator();

	private String id;
	private DataSourceType type;
	private String shortName;
	private String longName;

	private Integer colorInt = null;

	private Area area;

	private boolean isRTS = false;

	public AgencyProperties(String id, DataSourceType type, String shortName, String longName, String color, Area area, boolean isRTS) {
		this.id = id;
		this.type = type;
		this.shortName = shortName;
		this.longName = longName;
		setColor(color);
		this.area = area;
		this.isRTS = isRTS;
	}

	public void setColor(String color) {
		if (!TextUtils.isEmpty(color)) {
			this.colorInt = ColorUtils.parseColor(color);
		}
	}

	public int getColorInt() {
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
				.append("id:").append(id).append(',') //
				.append("type:").append(type).append(',') //
				.append("shortName:").append(shortName).append(',') //
				.append("longName:").append(longName).append(',') //
				.append("area:").append(area).append(',') //
				.append('}').toString();
	}

	public String getAuthority() {
		return id;
	}

	public boolean isInArea(double lat, double lng, double aroundDiff) {
		Area area = LocationUtils.getArea(lat, lng, aroundDiff);
		boolean isInArea = Area.areOverlapping(area, this.area);
		return isInArea;
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

	private static class AgencyPropertiesShortNameComparator implements Comparator<AgencyProperties> {

		@Override
		public int compare(AgencyProperties lap, AgencyProperties rap) {
			String lShortName = lap.getShortName();
			String rShortName = rap.getShortName();
			if (lShortName == null) {
				return rShortName == null ? 0 : -1;
			} else if (rShortName == null) {
				return +1;
			}
			return lShortName.compareTo(rShortName);
		}
	}
}
