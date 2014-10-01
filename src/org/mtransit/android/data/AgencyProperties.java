package org.mtransit.android.data;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;

public class AgencyProperties implements MTLog.Loggable {

	private static final String TAG = AgencyProperties.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private String id;
	private DataSourceType type;
	private String shortName;
	private String longName;

	private Area area;

	public AgencyProperties(String id, DataSourceType type, String shortName, String longName, Area area) {
		this.id = id;
		this.type = type;
		this.shortName = shortName;
		this.longName = longName;
		this.area = area;
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
		final boolean isInArea = Area.areOverlapping(area, this.area);
		return isInArea;
	}

}
