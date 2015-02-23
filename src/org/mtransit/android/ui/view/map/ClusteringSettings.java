package org.mtransit.android.ui.view.map;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class ClusteringSettings {

	public static final double DEFAULT_CLUSTER_SIZE = 90.0;

	private boolean addMarkersDynamically = false;

	private ClusterOptionsProvider clusterOptionsProvider = null;

	private double clusterSize = DEFAULT_CLUSTER_SIZE;

	private boolean enabled = true;

	public ClusteringSettings addMarkersDynamically(boolean addMarkersDynamically) {
		this.addMarkersDynamically = addMarkersDynamically;
		return this;
	}

	public ClusteringSettings clusterOptionsProvider(ClusterOptionsProvider clusterOptionsProvider) {
		this.clusterOptionsProvider = clusterOptionsProvider;
		return this;
	}

	public ClusteringSettings clusterSize(double clusterSize) {
		this.clusterSize = clusterSize;
		return this;
	}

	public ClusteringSettings enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public ClusterOptionsProvider getClusterOptionsProvider() {
		return clusterOptionsProvider;
	}

	public double getClusterSize() {
		return clusterSize;
	}

	public boolean isAddMarkersDynamically() {
		return addMarkersDynamically;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ClusteringSettings)) {
			return false;
		}
		ClusteringSettings other = (ClusteringSettings) o;
		if (enabled != other.enabled) {
			return false;
		}
		if (addMarkersDynamically != other.addMarkersDynamically) {
			return false;
		}
		if (!enabled && !other.enabled) {
			return true;
		}
		if (clusterSize != other.clusterSize) {
			return false;
		}
		if (!equals(clusterOptionsProvider, other.clusterOptionsProvider)) {
			return false;
		}
		return true;
	}

	private static boolean equals(Object objLeft, Object objRight) {
		if (objLeft == null) {
			return objRight == null;
		} else {
			return objLeft.equals(objRight);
		}
	}
}
