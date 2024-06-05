package org.mtransit.android.ui.view.map.impl;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;

import androidx.collection.ArrayMap;

import com.google.android.gms.maps.model.LatLng;

import org.mtransit.android.ui.view.map.AnimationSettings;
import org.mtransit.android.ui.view.map.IMarker;

import java.util.Iterator;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class MarkerAnimator {

	private Handler handler = new Handler(msg -> {
		calculatePositions();
		return true;
	});

	private ArrayMap<DelegatingMarker, AnimationData> queue = new ArrayMap<>();

	private void calculatePositions() {
		long now = SystemClock.uptimeMillis();
		Iterator<DelegatingMarker> iterator = queue.keySet().iterator();
		while (iterator.hasNext()) {
			DelegatingMarker marker = iterator.next();
			AnimationData data = queue.get(marker);
			long time = now - data.start;
			if (time <= 0) {
				marker.setPositionDuringAnimation(data.from);
			} else if (time >= data.duration) {
				marker.setPositionDuringAnimation(data.to);
				if (data.callback != null) {
					data.callback.onFinish(marker);
				}
				iterator.remove();
			} else {
				float t = ((float) time) / data.duration;
				t = data.interpolator.getInterpolation(t);
				double lat = (1.0f - t) * data.from.latitude + t * data.to.latitude;
				double lng = (1.0f - t) * data.from.longitude + t * data.to.longitude;
				marker.setPositionDuringAnimation(new LatLng(lat, lng));
			}
		}
		if (queue.size() > 0) {
			handler.sendEmptyMessage(0);
		}
	}

	public void animate(DelegatingMarker marker, LatLng from, LatLng to, long start, AnimationSettings settings, IMarker.AnimationCallback callback) {
		AnimationData data = new AnimationData();
		data.from = from;
		data.to = to;
		data.start = start;
		data.duration = settings.getDuration();
		data.interpolator = settings.getInterpolator();
		data.callback = callback;
		queue.put(marker, data);
		handler.removeMessages(0);
		handler.sendEmptyMessage(0);
	}

	public void cancelAnimation(DelegatingMarker marker, IMarker.AnimationCallback.CancelReason reason) {
		AnimationData data = queue.remove(marker);
		if (data != null && data.callback != null) {
			data.callback.onCancel(marker, reason);
		}
	}

	private static class AnimationData {

		private LatLng from;

		private LatLng to;

		private long start;

		private long duration;

		private Interpolator interpolator;

		private IMarker.AnimationCallback callback;
	}
}
