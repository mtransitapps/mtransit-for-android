package org.mtransit.android.ui.view.map;

import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings({"unused", "WeakerAccess"})
public class AnimationSettings {

	public static final long DEFAULT_DURATION = 500L;

	public static final Interpolator DEFAULT_INTERPOLATOR = new LinearInterpolator();

	private long duration = DEFAULT_DURATION;

	@NonNull
	private Interpolator interpolator = DEFAULT_INTERPOLATOR;

	@NonNull
	public AnimationSettings duration(long duration) {
		if (duration <= 0L) {
			throw new IllegalArgumentException();
		}
		this.duration = duration;
		return this;
	}

	public long getDuration() {
		return duration;
	}

	@NonNull
	public Interpolator getInterpolator() {
		return interpolator;
	}

	@NonNull
	public AnimationSettings interpolator(@Nullable Interpolator interpolator) {
		if (interpolator == null) {
			throw new IllegalArgumentException();
		}
		this.interpolator = interpolator;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AnimationSettings that = (AnimationSettings) o;
		return duration == that.duration //
				&& ObjectsCompat.equals(interpolator, that.interpolator);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + Long.hashCode(duration);
		result = 31 * result + interpolator.hashCode();
		return result;
	}
}
