package org.mtransit.android.ui.view.map;

import androidx.annotation.NonNull;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface ClusterOptionsProvider {

	@NonNull
	ClusterOptions getClusterOptions(@NonNull List<IMarker> markers, float zoom);
}
