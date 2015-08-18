package org.mtransit.android.ui.view;

import android.view.View;
import android.widget.AdapterView;

public abstract class MTOnItemClickListener implements AdapterView.OnItemClickListener {

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		onItemClickS(parent, view, position, id, this);
	}

	public abstract void onItemClickMT(AdapterView<?> parent, View view, int position, long id);

	public static void onItemClickS(final AdapterView<?> parent, final View view, final int position, final long id, final MTOnItemClickListener listener) {
		if (listener == null) {
			return;
		}
		if (view == null) {
			listener.onItemClickMT(parent, null, position, id);
			return;
		}
		view.post(new Runnable() {
			@Override
			public void run() {
				listener.onItemClickMT(parent, view, position, id);
			}
		});
	}
}
