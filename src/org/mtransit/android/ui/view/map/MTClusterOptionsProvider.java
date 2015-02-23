package org.mtransit.android.ui.view.map;

import java.lang.ref.WeakReference;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.util.MapUtils;

import android.content.Context;
import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptor;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class MTClusterOptionsProvider implements ClusterOptionsProvider, MTLog.Loggable {

	private static final String TAG = MTClusterOptionsProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int CLUSETER_ICON_RES = R.drawable.ic_cluster_blur_white;

	private ClusterOptions clusterOptions = new ClusterOptions().anchor(0.5f, 0.5f);

	private WeakReference<Context> contextWR;

	public MTClusterOptionsProvider(Context context) {
		this.contextWR = new WeakReference<Context>(context);
	}

	@Override
	public ClusterOptions getClusterOptions(List<IMarker> markers) {
		BitmapDescriptor icon = getClusterIcon(markers);
		this.clusterOptions.icon(icon);
		return this.clusterOptions;
	}

	private BitmapDescriptor getClusterIcon(List<IMarker> markers) {
		Integer color = getColor(markers);
		Context context = this.contextWR == null ? null : this.contextWR.get();
		return MapUtils.getIcon(context, CLUSETER_ICON_RES, color);
	}

	private Integer getColor(List<IMarker> markers) {
		try {
			if (CollectionUtils.getSize(markers) == 0) {
				return Color.BLACK;
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
						color = Color.BLACK;
					}
				} else if (!color.equals(marker.getColor())) {
					color = Color.BLACK;
				}
				if (secondaryColor == null) {
					if (marker.getSecondaryColor() != null) {
						secondaryColor = Color.BLACK;
					}
				} else if (!secondaryColor.equals(marker.getSecondaryColor())) {
					secondaryColor = Color.BLACK;
				}
				if (defaultColor == null) {
					if (marker.getDefaultColor() != null) {
						defaultColor = Color.BLACK;
					}
				} else if (!defaultColor.equals(marker.getDefaultColor())) {
					defaultColor = Color.BLACK;
				}
				if (color != null && color.equals(Color.BLACK) && secondaryColor != null && secondaryColor.equals(Color.BLACK) && defaultColor != null
						&& defaultColor.equals(Color.BLACK)) {
					return Color.BLACK;
				}
			}
			if (color != null && !color.equals(Color.BLACK)) {
				return color;
			} else if (secondaryColor != null && !secondaryColor.equals(Color.BLACK)) {
				return secondaryColor;
			} else if (defaultColor != null && !defaultColor.equals(Color.BLACK)) {
				return defaultColor;
			} else {
				return Color.BLACK;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while finding color!");
			return Color.BLACK;
		}
	}
}
