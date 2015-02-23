package org.mtransit.android.ui.view.map;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface ClusterOptionsProvider {

	ClusterOptions getClusterOptions(List<IMarker> markers);
}
