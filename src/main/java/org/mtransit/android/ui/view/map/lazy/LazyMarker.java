package org.mtransit.android.ui.view.map.lazy;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.util.MapUtils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Context;
import android.util.Log;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings({"unused", "WeakerAccess"})
public class LazyMarker implements MTLog.Loggable {

	private static final String LOG_TAG = LazyMarker.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public interface OnMarkerCreateListener {
		void onMarkerCreate(@NonNull LazyMarker marker);
	}

	private Marker marker;
	private GoogleMap map;
	private MarkerOptions markerOptions;
	private OnMarkerCreateListener listener;

	public LazyMarker(GoogleMap map, MarkerOptions options) {
		this(map, options, null);
	}

	public LazyMarker(GoogleMap map, MarkerOptions options, OnMarkerCreateListener listener) {
		this(map, options, null, null, null, null, null, listener);
	}

	public LazyMarker(GoogleMap map, MarkerOptions options, Integer optionsColor, Integer optionsSecondaryColor, Integer optionsDefaultColor,
			Integer optionsIconResId, Context optionsContext, OnMarkerCreateListener listener) {
		if (options.isVisible()) {
			createMarker(map, options, optionsColor, optionsSecondaryColor, optionsDefaultColor, optionsIconResId, optionsContext, listener);
		} else {
			this.map = map;
			this.markerOptions = copy(options);
			this.markerOptionsColor = optionsColor;
			this.markerOptionsSecondaryColor = optionsSecondaryColor;
			this.markerOptionsDefaultColor = optionsDefaultColor;
			this.markerOptionsIconResId = optionsIconResId;
			this.markerOptionsContextWR = new WeakReference<>(optionsContext);
			this.listener = listener;
		}
	}

	public float getAlpha() {
		if (marker != null) {
			return marker.getAlpha();
		} else {
			return markerOptions.getAlpha();
		}
	}

	@Deprecated
	public String getId() {
		createMarker();
		return marker.getId();
	}

	public Marker getMarker() {
		return marker;
	}

	public LatLng getPosition() {
		if (marker != null) {
			return marker.getPosition();
		} else {
			return markerOptions.getPosition();
		}
	}

	public float getRotation() {
		if (marker != null) {
			return marker.getRotation();
		} else {
			return markerOptions.getRotation();
		}
	}

	public String getSnippet() {
		if (marker != null) {
			return marker.getSnippet();
		} else {
			return markerOptions.getSnippet();
		}
	}

	public String getTitle() {
		if (marker != null) {
			return marker.getTitle();
		} else {
			return markerOptions.getTitle();
		}
	}

	public void hideInfoWindow() {
		if (marker != null) {
			marker.hideInfoWindow();
		}
	}

	public boolean isDraggable() {
		if (marker != null) {
			return marker.isDraggable();
		} else {
			return markerOptions.isDraggable();
		}
	}

	public boolean isFlat() {
		if (marker != null) {
			return marker.isFlat();
		} else {
			return markerOptions.isFlat();
		}
	}

	public boolean isInfoWindowShown() {
		if (marker != null) {
			return marker.isInfoWindowShown();
		} else {
			return false;
		}
	}

	public boolean isVisible() {
		if (marker != null) {
			return marker.isVisible();
		} else {
			return false;
		}
	}

	public void remove() {
		if (marker != null) {
			marker.remove();
			marker = null;
		} else {
			map = null;
			markerOptions = null;
			markerOptionsColor = null;
			markerOptionsSecondaryColor = null;
			markerOptionsDefaultColor = null;
			markerOptionsIconResId = null;
			if (markerOptionsContextWR != null) {
				markerOptionsContextWR.clear();
				markerOptionsContextWR = null;
			}
			listener = null;
		}
	}

	public void setAlpha(float alpha) {
		if (marker != null) {
			marker.setAlpha(alpha);
		} else {
			markerOptions.alpha(alpha);
		}
	}

	public void setAnchor(float anchorU, float anchorV) {
		if (marker != null) {
			marker.setAnchor(anchorU, anchorV);
		} else {
			markerOptions.anchor(anchorU, anchorV);
		}
	}

	public void setDraggable(boolean draggable) {
		if (marker != null) {
			marker.setDraggable(draggable);
		} else {
			markerOptions.draggable(draggable);
		}
	}

	public void setFlat(boolean flat) {
		if (marker != null) {
			marker.setFlat(flat);
		} else {
			markerOptions.flat(flat);
		}
	}

	@Deprecated
	public void setIcon(BitmapDescriptor icon) {
		if (marker != null) {
			marker.setIcon(icon);
		} else {
			markerOptions.icon(icon);
		}
		markerOptionsColor = null;
		markerOptionsSecondaryColor = null;
		markerOptionsDefaultColor = null;
		markerOptionsIconResId = null;
		if (markerOptionsContextWR != null) {
			markerOptionsContextWR.clear();
			markerOptionsContextWR = null;
		}
	}

	@DrawableRes
	private Integer markerOptionsIconResId = null;
	@ColorInt
	private Integer markerOptionsColor = null;
	@ColorInt
	private Integer markerOptionsSecondaryColor = null;
	@ColorInt
	private Integer markerOptionsDefaultColor = null;
	private WeakReference<Context> markerOptionsContextWR = null;

	public void setIcon(Context context, @DrawableRes Integer iconResId, @ColorInt Integer color, @ColorInt Integer secondaryColor, @ColorInt Integer defaultColor) {
		if (marker != null) {
			marker.setIcon(MapUtils.getIcon(context, iconResId, color, true));
		} else {
			markerOptionsIconResId = iconResId;
			markerOptionsColor = color;
			markerOptionsSecondaryColor = secondaryColor;
			markerOptionsDefaultColor = defaultColor;
			markerOptionsContextWR = new WeakReference<>(context);
			markerOptions.icon(null);
		}
	}

	@ColorInt
	public Integer getColor() {
		return this.markerOptionsColor;
	}

	@ColorInt
	public Integer getSecondaryColor() {
		return this.markerOptionsSecondaryColor;
	}

	@ColorInt
	public Integer getDefaultColor() {
		return this.markerOptionsDefaultColor;
	}

	public void setInfoWindowAnchor(float anchorU, float anchorV) {
		if (marker != null) {
			marker.setInfoWindowAnchor(anchorU, anchorV);
		} else {
			markerOptions.infoWindowAnchor(anchorU, anchorV);
		}
	}

	public void setPosition(LatLng position) {
		if (marker != null) {
			marker.setPosition(position);
		} else {
			markerOptions.position(position);
		}
	}

	public void setRotation(float rotation) {
		if (marker != null) {
			marker.setRotation(rotation);
		} else {
			markerOptions.rotation(rotation);
		}
	}

	public void setSnippet(String snippet) {
		if (marker != null) {
			marker.setSnippet(snippet);
		} else {
			markerOptions.snippet(snippet);
		}
	}

	public void setTitle(String title) {
		if (marker != null) {
			marker.setTitle(title);
		} else {
			markerOptions.title(title);
		}
	}

	public void setVisible(boolean visible) {
		if (marker != null) {
			marker.setVisible(visible);
		} else if (visible) {
			markerOptions.visible(true);
			createMarker();
		}
	}

	public void showInfoWindow() {
		if (marker != null) {
			marker.showInfoWindow();
		}
	}

	private void createMarker() {
		if (marker == null) {
			Context markerOptionsContext = markerOptionsContextWR == null ? null : markerOptionsContextWR.get();
			createMarker(map, markerOptions, markerOptionsColor, markerOptionsSecondaryColor, markerOptionsDefaultColor, markerOptionsIconResId,
					markerOptionsContext, listener);
			map = null;
			markerOptions = null;
			markerOptionsIconResId = null;
			if (markerOptionsContextWR != null) {
				markerOptionsContextWR.clear();
				markerOptionsContextWR = null;
			}
			listener = null;
		}
	}

	private void createMarker(@NonNull GoogleMap map, @NonNull MarkerOptions options,
			@ColorInt Integer markerOptionsColor, @ColorInt Integer markerOptionsSecondaryColor, @ColorInt Integer markerOptionsDefaultColor,
			@DrawableRes Integer markerOptionsIconResId,
			@Nullable Context markerOptionsContext,
			@Nullable OnMarkerCreateListener listener) {
		if (markerOptionsDefaultColor != null && markerOptionsIconResId != null && markerOptionsContext != null) {
			int color = markerOptionsColor == null ? markerOptionsSecondaryColor == null ? markerOptionsDefaultColor : markerOptionsSecondaryColor
					: markerOptionsColor;
			options.icon(MapUtils.getIcon(markerOptionsContext, markerOptionsIconResId, color, true));
		}
		marker = map.addMarker(options);
		if (listener != null) {
			listener.onMarkerCreate(this);
		}
	}

	private static MarkerOptions copy(MarkerOptions options) {
		MarkerOptions copy = new MarkerOptions();
		copy.alpha(options.getAlpha());
		copy.anchor(options.getAnchorU(), options.getAnchorV());
		copy.draggable(options.isDraggable());
		copy.flat(options.isFlat());
		copy.icon(options.getIcon());
		copy.infoWindowAnchor(options.getInfoWindowAnchorU(), options.getInfoWindowAnchorV());
		copy.position(options.getPosition());
		copy.rotation(options.getRotation());
		copy.snippet(options.getSnippet());
		copy.title(options.getTitle());
		copy.visible(options.isVisible());
		return copy;
	}
}
