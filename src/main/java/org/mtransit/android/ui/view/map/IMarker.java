package org.mtransit.android.ui.view.map;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface IMarker {

	interface AnimationCallback {

		enum CancelReason {
			ANIMATE_POSITION, DRAG_START, REMOVE, SET_POSITION,
		}

		void onFinish(IMarker marker);

		void onCancel(IMarker marker, CancelReason reason);
	}

	void animatePosition(LatLng target);

	void animatePosition(LatLng target, AnimationSettings settings);

	void animatePosition(LatLng target, AnimationCallback callback);

	void animatePosition(LatLng target, AnimationSettings settings, AnimationCallback callback);

	float getAlpha();

	int getClusterGroup();

	<T> T getData();

	@Deprecated
	String getId();

	List<IMarker> getMarkers();

	LatLng getPosition();

	float getRotation();

	String getSnippet();

	String getTitle();

	void hideInfoWindow();

	boolean isCluster();

	boolean isDraggable();

	boolean isFlat();

	boolean isInfoWindowShown();

	boolean isVisible();

	void remove();

	void setAlpha(float alpha);

	void setAnchor(float anchorU, float anchorV);

	void setClusterGroup(int clusterGroup);

	void setData(Object data);

	void setDraggable(boolean draggable);

	void setFlat(boolean flat);

	@Deprecated
	void setIcon(BitmapDescriptor icon);

	void setIcon(@Nullable Context context,
				 @DrawableRes int iconResId,
				 @ColorInt int color,
				 @ColorInt @Nullable Integer secondaryColor,
				 @ColorInt int defaultColor);

	Integer getColor();

	Integer getSecondaryColor();

	Integer getDefaultColor();

	void setInfoWindowAnchor(float anchorU, float anchorV);

	void setPosition(LatLng position);

	void setRotation(float rotation);

	void setSnippet(String snippet);

	void setTitle(String title);

	void setVisible(boolean visible);

	void showInfoWindow();

}