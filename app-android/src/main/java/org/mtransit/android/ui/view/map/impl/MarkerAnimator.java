package org.mtransit.android.ui.view.map.impl;

import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.android.gms.maps.model.LatLng;

import org.mtransit.android.ui.view.map.AnimationSettings;
import org.mtransit.android.ui.view.map.IMarker;

import java.util.Iterator;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings("WeakerAccess")
class MarkerAnimator {

	private final Handler handler = new Handler(msg -> {
		calculatePositions();
		return true;
	});

	private final ArrayMap<DelegatingMarker, MarkerAnimatorData> queue = new ArrayMap<>();

	private void calculatePositions() {
		long now = SystemClock.uptimeMillis();
		Iterator<DelegatingMarker> iterator = queue.keySet().iterator();
		while (iterator.hasNext()) {
			DelegatingMarker marker = iterator.next();
			MarkerAnimatorData data = queue.get(marker);
			if (data == null) continue;
			long time = now - data.getStart();
			if (time <= 0) {
				marker.setPositionDuringAnimation(data.getFrom());
			} else if (time >= data.getDuration()) {
				marker.setPositionDuringAnimation(data.getTo());
				if (data.getCallback() != null) {
					data.getCallback().onFinish(marker);
				}
				iterator.remove();
			} else {
				float t = ((float) time) / data.getDuration();
				t = data.getInterpolator().getInterpolation(t);
				double lat = (1.0f - t) * data.getFrom().latitude + t * data.getTo().latitude;
				double lng = (1.0f - t) * data.getFrom().longitude + t * data.getTo().longitude;
				marker.setPositionDuringAnimation(new LatLng(lat, lng));
			}
		}
		if (!queue.isEmpty()) {
			handler.sendEmptyMessage(0);
		}
	}

	public void animate(
			DelegatingMarker marker,
			@NonNull LatLng from,
			@NonNull LatLng to,
			long start,
			@NonNull AnimationSettings settings,
			@Nullable IMarker.AnimationCallback callback
	) {
		final MarkerAnimatorData data = new MarkerAnimatorData(
				from,
				to,
				start,
				settings.getDuration(),
				settings.getInterpolator(),
				callback
		);
		queue.put(marker, data);
		handler.removeMessages(0);
		handler.sendEmptyMessage(0);
	}

	public void cancelAnimation(@NonNull DelegatingMarker marker, @NonNull IMarker.AnimationCallback.CancelReason reason) {
		MarkerAnimatorData data = queue.remove(marker);
		if (data != null && data.getCallback() != null) {
			data.getCallback().onCancel(marker, reason);
		}
	}
}
