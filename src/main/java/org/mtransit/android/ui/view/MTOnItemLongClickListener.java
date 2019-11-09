package org.mtransit.android.ui.view;

import android.view.View;
import android.widget.AdapterView;

public abstract class MTOnItemLongClickListener implements AdapterView.OnItemLongClickListener {

	@Override
	public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		return onItemLongClickS(parent, view, position, id, this);
	}

	public abstract boolean onItemLongClickMT(AdapterView<?> parent, View view, int position, long id);

	public static boolean onItemLongClickS(final AdapterView<?> parent, final View view, final int position, final long id,
			final MTOnItemLongClickListener listener) {
		if (listener == null) {
			return false; // not handled
		}
		if (view == null) {
			return listener.onItemLongClickMT(parent, null, position, id);
		}
		view.post(() ->
				listener.onItemLongClickMT(parent, view, position, id)
		);
		return true; // handled
	}
}
