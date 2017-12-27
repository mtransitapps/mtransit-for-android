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

	private static boolean equals(Object objLeft, Object objRight) {
		if (objLeft == null) {
			return objRight == null;
		} else {
			return objLeft.equals(objRight);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClusteringSettings that = (ClusteringSettings) o;
		return addMarkersDynamically == that.addMarkersDynamically //
				&& Double.compare(that.clusterSize, clusterSize) == 0 //
				&& enabled == that.enabled //
				&& (clusterOptionsProvider != null ? clusterOptionsProvider.equals(that.clusterOptionsProvider) : that.clusterOptionsProvider == null);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + (addMarkersDynamically ? 1 : 0);
		result = 31 * result + (clusterOptionsProvider != null ? clusterOptionsProvider.hashCode() : 0);
		long temp = Double.doubleToLongBits(clusterSize);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (enabled ? 1 : 0);
		return result;
	}
}
