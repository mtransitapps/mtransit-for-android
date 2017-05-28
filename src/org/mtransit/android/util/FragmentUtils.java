package org.mtransit.android.util;

import org.mtransit.android.commons.MTLog;

import android.support.annotation.IdRes;
import android.support.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public final class FragmentUtils implements MTLog.Loggable {

	private static final String TAG = FragmentUtils.class.getSimpleName();

	public static final String DIALOG_TAG = "dialog";

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void replaceFragment(android.support.v4.app.FragmentActivity fa, @IdRes int containerViewId, android.support.v4.app.Fragment fragment,
			boolean addToStack, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "replaceFragment()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "showNewFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& (!optSource.isAdded() || optSource.isDetached() || optSource.isRemoving())) {
				MTLog.d(TAG, "showNewFragment() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			android.support.v4.app.FragmentManager fm = fa.getSupportFragmentManager();
			android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();
			ft.replace(containerViewId, fragment);
			if (addToStack) {
				ft.addToBackStack(null);
			}
			ft.commit();
		} catch (IllegalStateException ise) {
			CrashUtils.w(TAG, ise, "Illegal State Exception while replacing fragment '%s'!", fragment);
		} catch (Exception e) {
			CrashUtils.w(TAG, e, "Unexpected error while replacing fragment '%s'!", fragment);
		}
	}

	public static void replaceDialogFragment(android.support.v4.app.FragmentActivity fa, String tag, android.support.v4.app.DialogFragment dialogFragment,
			@Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "replaceDialogFragment()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "replaceDialogFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& (!optSource.isAdded() || optSource.isDetached() || optSource.isRemoving())) {
				MTLog.d(TAG, "replaceDialogFragment() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			android.support.v4.app.FragmentManager fm = fa.getSupportFragmentManager();
			android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();
			android.support.v4.app.Fragment prev = fm.findFragmentByTag(tag);
			if (prev != null) {
				MTLog.d(TAG, "replaceDialogFragment() > remove old dialog %s", prev);
				ft.remove(prev);
			}
			ft.addToBackStack(null);
			if (dialogFragment != null) {
				MTLog.d(TAG, "replaceDialogFragment() > add new dialog %s", dialogFragment);
				dialogFragment.show(ft, tag);
			}
		} catch (IllegalStateException ise) {
			CrashUtils.w(TAG, ise, "Illegal State Exception while replacing support dialog fragment '%s'!", dialogFragment);
		} catch (Exception e) {
			CrashUtils.w(TAG, e, "Unexpected error while replacing support dialog fragment '%s'!", dialogFragment);
		}
	}

	public static void replaceDialogFragment(android.app.Activity fa, String tag, android.app.DialogFragment dialogFragment,
			@Nullable android.app.Fragment optSource) {
		MTLog.v(TAG, "replaceDialogFragment()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "replaceDialogFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& (!optSource.isAdded() || optSource.isDetached() || optSource.isRemoving())) {
				MTLog.d(TAG, "replaceDialogFragment() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			android.app.FragmentManager fm = fa.getFragmentManager();
			android.app.FragmentTransaction ft = fm.beginTransaction();
			android.app.Fragment prev = fm.findFragmentByTag(tag);
			if (prev != null) {
				MTLog.d(TAG, "replaceDialogFragment() > remove old dialog %s", prev);
				ft.remove(prev);
			}
			ft.addToBackStack(null);
			if (dialogFragment != null) {
				MTLog.d(TAG, "replaceDialogFragment() > add new dialog %s", dialogFragment);
				dialogFragment.show(ft, tag);
			}
		} catch (IllegalStateException ise) {
			CrashUtils.w(TAG, ise, "Illegal State Exception while replacing dialog fragment '%s'!", dialogFragment);
		} catch (Exception e) {
			CrashUtils.w(TAG, e, "Unexpected error while replacing dialog fragment '%s'!", dialogFragment);
		}
	}

	public static void executePendingTransactions(android.support.v4.app.FragmentActivity fa, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "executePendingTransactions()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "executePendingTransactions() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& (!optSource.isAdded() || optSource.isDetached() || optSource.isRemoving())) {
				MTLog.d(TAG, "executePendingTransactions() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			fa.getSupportFragmentManager().executePendingTransactions();
		} catch (IllegalStateException ise) {
			CrashUtils.w(TAG, ise, "Illegal State Exception while executing pending transactions!");
		} catch (Exception e) {
			CrashUtils.w(TAG, e, "Unexpected error while executing pending transactions!");
		}
	}

	public static void clearFragmentBackStackImmediate(android.support.v4.app.FragmentActivity fa, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "clearFragmentBackStackImmediate()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "clearFragmentBackStackImmediate() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& (!optSource.isAdded() || optSource.isDetached() || optSource.isRemoving())) {
				MTLog.d(TAG, "clearFragmentBackStackImmediate() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			fa.getSupportFragmentManager().popBackStack(null, android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} catch (IllegalStateException ise) {
			CrashUtils.w(TAG, ise, "Illegal State Exception while clearing fragment back stack immediately!");
		} catch (Exception e) {
			CrashUtils.w(TAG, e, "Unexpected error while clearing fragment back stack immediately!");
		}
	}

	public static void popFragmentFromStack(android.support.v4.app.FragmentActivity fa, android.support.v4.app.Fragment fragment,
			@Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "popFragmentFromStack()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "popFragmentFromStack() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& (!optSource.isAdded() || optSource.isDetached() || optSource.isRemoving())) {
				MTLog.d(TAG, "popFragmentFromStack() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			if (fragment != null) {
				fa.getSupportFragmentManager().popBackStack();
			}
		} catch (IllegalStateException ise) {
			CrashUtils.w(TAG, ise, "Illegal State Exception while popping fragment '%s' from stack!", fragment);
		} catch (Exception e) {
			CrashUtils.w(TAG, e, "Unexpected error while popping fragment '%s' from stack!", fragment);
		}
	}

	public static boolean popLatestEntryFromStack(android.support.v4.app.FragmentActivity fa, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "popLatestEntryFromStack()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "popLatestEntryFromStack() > SKIP (activity is null/finishing)");
				return false; // not handled
			}
			if (optSource != null // optional check
					&& (!optSource.isAdded() || optSource.isDetached() || optSource.isRemoving())) {
				MTLog.d(TAG, "popLatestEntryFromStack() > SKIP (source fragment is !added/detached/removing)");
				return false; // not handled
			}
			android.support.v4.app.FragmentManager fm = fa.getSupportFragmentManager();
			if (fm.getBackStackEntryCount() > 0) {
				fm.popBackStack();
				return true; // handled
			}
			return false; // not handled
		} catch (IllegalStateException ise) {
			CrashUtils.w(TAG, ise, "Illegal State Exception while popping latest entry from stack!");
			return false; // not handled
		} catch (Exception e) {
			CrashUtils.w(TAG, e, "Unexpected error while popping fragment latest entry from stack!");
			return false; // not handled
		}
	}
}
