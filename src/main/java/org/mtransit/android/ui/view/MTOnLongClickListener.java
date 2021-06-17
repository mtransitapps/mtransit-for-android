package org.mtransit.android.ui.view;

import android.view.View;

public abstract class MTOnLongClickListener implements View.OnLongClickListener {

	@Override
	public boolean onLongClick(View view) {
		return onLongClickS(view, this);
	}

	public abstract boolean onLongClickMT(View view);

	public static boolean onLongClickS(final View view, final MTOnLongClickListener listener) {
		if (listener == null) {
			return false; // not handled
		}
		if (view == null) {
			return listener.onLongClickMT(null);
		}
		view.post(() ->
				listener.onLongClickMT(view)
		);
		return true; // handled
	}
}
