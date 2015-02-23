package org.mtransit.android.ui.view.map;

import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class AnimationSettings {

	public static final long DEFAULT_DURATION = 500L;

	public static final Interpolator DEFAULT_INTERPOLATOR = new LinearInterpolator();

	private long duration = DEFAULT_DURATION;

	private Interpolator interpolator = DEFAULT_INTERPOLATOR;

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

	public Interpolator getInterpolator() {
		return interpolator;
	}

	public AnimationSettings interpolator(Interpolator interpolator) {
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
		if (!(o instanceof AnimationSettings)) {
			return false;
		}
		AnimationSettings other = (AnimationSettings) o;
		if (duration != other.duration) {
			return false;
		}
		return interpolator.equals(other.interpolator);
	}
}
