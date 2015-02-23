package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface Polyline {

	int getColor();

	<T> T getData();

	@Deprecated
	String getId();

	List<LatLng> getPoints();

	float getWidth();

	float getZIndex();

	boolean isGeodesic();

	boolean isVisible();

	void remove();

	void setColor(int color);

	void setData(Object data);

	void setGeodesic(boolean geodesic);

	void setPoints(List<LatLng> points);

	void setVisible(boolean visible);

	void setWidth(float width);

	void setZIndex(float zIndex);
}