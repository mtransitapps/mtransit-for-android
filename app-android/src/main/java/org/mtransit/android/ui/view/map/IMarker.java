package org.mtransit.android.ui.view.map;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings("unused")
public interface IMarker {

	interface AnimationCallback {

		enum CancelReason {
			ANIMATE_POSITION, DRAG_START, REMOVE, SET_POSITION,
		}

		void onFinish(@NonNull IMarker marker);

		void onCancel(@NonNull IMarker marker, @NonNull CancelReason reason);
	}

	void animatePosition(@Nullable LatLng target);

	void animatePosition(@Nullable LatLng target, @Nullable AnimationSettings settings);

	void animatePosition(@Nullable LatLng target, @Nullable AnimationCallback callback);

	void animatePosition(@Nullable LatLng target, @Nullable AnimationSettings settings, @Nullable AnimationCallback callback);

	float getAlpha();

	int getClusterGroup();

	<T> T getData();

	@Deprecated
	@NonNull
	String getId();

	@Nullable
	List<IMarker> getMarkers();

	@NonNull
	LatLng getPosition();

	float getRotation();

	float getZIndex();

	@Nullable
	String getSnippet();

	@Nullable
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

	void setData(@Nullable Object data);

	void setDraggable(boolean draggable);

	void setFlat(boolean flat);

	@Deprecated
	void setIcon(@Nullable BitmapDescriptor icon);

	void setIcon(@Nullable Context context,
				 @DrawableRes @Nullable Integer iconResId,
				 @Nullable Boolean replaceColor,
				 @ColorInt @Nullable Integer color,
				 @ColorInt @Nullable Integer secondaryColor,
				 @ColorInt @Nullable Integer defaultColor);

	@Nullable
	Integer getColor();

	@Nullable
	Integer getSecondaryColor();

	@Nullable
	Integer getDefaultColor();

	void setInfoWindowAnchor(float anchorU, float anchorV);

	void setPosition(@NonNull LatLng position);

	void setRotation(float rotation);

	void setZIndex(float zIndex);

	void setSnippet(@Nullable String snippet);

	void setTitle(@Nullable String title);

	void setVisible(boolean visible);

	void showInfoWindow();

}