package org.mtransit.android.ui.view.map;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.util.MapUtils;

import com.google.android.gms.maps.model.BitmapDescriptor;

import android.content.Context;
import android.graphics.Color;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class MTClusterOptionsProvider implements ClusterOptionsProvider, MTLog.Loggable {

	private static final String LOG_TAG = MTClusterOptionsProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@DrawableRes
	private static final int CLUSTER_ICON_RES = R.drawable.map_icon_cluster_blur_white;

	@NonNull
	private ClusterOptions clusterOptions = new ClusterOptions().anchor(0.5f, 0.5f);

	@NonNull
	private final WeakReference<Context> contextWR;

	public MTClusterOptionsProvider(@Nullable Context context) {
		this.contextWR = new WeakReference<>(context);
	}

	@NonNull
	@Override
	public ClusterOptions getClusterOptions(@NonNull List<IMarker> markers) {
		BitmapDescriptor icon = getClusterIcon(markers);
		this.clusterOptions.icon(icon);
		return this.clusterOptions;
	}

	private BitmapDescriptor getClusterIcon(@NonNull List<IMarker> markers) {
		Context context = this.contextWR.get();
		Integer color = getColor(context, markers);
		return MapUtils.getIcon(context, CLUSTER_ICON_RES, color);
	}

	@ColorInt
	private Integer getColor(@Nullable Context context, @NonNull List<IMarker> markers) {
		final int defaultMapColor = getDefaultColor(context);
		try {
			if (CollectionUtils.getSize(markers) == 0) {
				return defaultMapColor;
			}
			Integer color = null;
			Integer secondaryColor = null;
			Integer defaultColor = null;
			boolean first = true;
			for (IMarker marker : markers) {
				if (first) {
					color = marker.getColor();
					secondaryColor = marker.getSecondaryColor();
					defaultColor = marker.getDefaultColor();
					first = false;
					continue;
				}
				if (color == null) {
					if (marker.getColor() != null) {
						color = defaultMapColor;
					}
				} else if (!color.equals(marker.getColor())) {
					color = defaultMapColor;
				}
				if (secondaryColor == null) {
					if (marker.getSecondaryColor() != null) {
						secondaryColor = defaultMapColor;
					}
				} else if (!secondaryColor.equals(marker.getSecondaryColor())) {
					secondaryColor = defaultMapColor;
				}
				if (defaultColor == null) {
					if (marker.getDefaultColor() != null) {
						defaultColor = defaultMapColor;
					}
				} else if (!defaultColor.equals(marker.getDefaultColor())) {
					defaultColor = defaultMapColor;
				}
				if (color != null && color.equals(defaultMapColor)
						&& secondaryColor != null && secondaryColor.equals(defaultMapColor)
						&& defaultColor != null && defaultColor.equals(defaultMapColor)) {
					return defaultMapColor;
				}
			}
			if (color != null && !color.equals(defaultMapColor)) {
				return color;
			} else if (secondaryColor != null && !secondaryColor.equals(defaultMapColor)) {
				return secondaryColor;
			} else if (defaultColor != null && !defaultColor.equals(defaultMapColor)) {
				return defaultColor;
			} else {
				return defaultMapColor;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while finding color!");
			return defaultMapColor;
		}
	}

	@ColorInt
	private int getDefaultColor(@Nullable Context context) {
		MTLog.d(this, "getDefaultColor()");
		if (context != null && ColorUtils.isDarkTheme(context)) {
			return Color.WHITE;
		} else {
			return Color.BLACK;
		}
	}
}
