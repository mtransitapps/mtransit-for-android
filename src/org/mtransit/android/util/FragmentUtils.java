package org.mtransit.android.util;

import org.mtransit.android.commons.MTLog;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public final class FragmentUtils implements MTLog.Loggable {

	private static final String TAG = FragmentUtils.class.getSimpleName();

	public static final String DIALOG_TAG = "dialog";

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static boolean isCurrentFragmentVisible(@Nullable android.support.v4.app.FragmentActivity fa, @IdRes int currentFragmentId,
			@Nullable android.support.v4.app.Fragment fragment) {
		MTLog.v(TAG, "isCurrentFragmentVisible(%s)", fragment);
		if (fragment == null) {
			MTLog.d(TAG, "isCurrentFragmentVisible() > SKIP (fragment null = invisible)");
			return false;
		}
		android.support.v4.app.FragmentManager fm = fa == null ? null : fa.getSupportFragmentManager();
		android.support.v4.app.Fragment currentFragment = fm == null ? null : fm.findFragmentById(currentFragmentId);
		return isFragmentReady(fragment) && fragment.equals(currentFragment);
	}

	public static boolean isFragmentReady(@Nullable android.support.v4.app.Fragment fragment) {
		return fragment != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
	}

	public static boolean isFragmentReady(@Nullable android.support.v4.app.FragmentActivity fa, @NonNull @IdRes Integer fragmentResId) {
		android.support.v4.app.Fragment fragment = getFragment(fa, fragmentResId);
		return fragment != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
	}

	@Nullable
	public static android.support.v4.app.Fragment getFragment(@Nullable android.support.v4.app.FragmentActivity fa, @NonNull @IdRes Integer fragmentResId) {
		android.support.v4.app.FragmentManager fm = fa == null ? null : fa.getSupportFragmentManager();
		return fm == null ? null : fm.findFragmentById(fragmentResId);
	}

	public static boolean isFragmentReady(@Nullable android.app.Fragment fragment) {
		return fragment != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
	}

	public static void replaceFragment(@Nullable android.support.v4.app.FragmentActivity fa, @IdRes int containerViewId,
			android.support.v4.app.Fragment fragment, boolean addToStack, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "replaceFragment()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "replaceFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(TAG, "replaceFragment() > SKIP (source fragment is !added/detached/removing)");
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

	public static void replaceFragment(@Nullable android.support.v4.app.FragmentActivity fa, @IdRes int containerViewId,
			android.support.v4.app.Fragment fragment, boolean addToStack, @Nullable @IdRes Integer optSourceResId) {
		MTLog.v(TAG, "replaceFragment()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "replaceFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSourceResId != null // optional check
					&& !isFragmentReady(fa, optSourceResId)) {
				MTLog.d(TAG, "replaceFragment() > SKIP (source fragment is !added/detached/removing)");
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

	public static void replaceDialogFragment(@Nullable android.support.v4.app.FragmentActivity fa, String tag,
			android.support.v4.app.DialogFragment dialogFragment, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "replaceDialogFragment()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "replaceDialogFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
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

	public static void replaceDialogFragment(@Nullable android.app.Activity fa, String tag, android.app.DialogFragment dialogFragment,
			@Nullable android.app.Fragment optSource) {
		MTLog.v(TAG, "replaceDialogFragment()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "replaceDialogFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
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

	public static void executePendingTransactions(@Nullable android.support.v4.app.FragmentActivity fa, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "executePendingTransactions()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "executePendingTransactions() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
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

	public static void clearFragmentBackStackImmediate(@Nullable android.support.v4.app.FragmentActivity fa,
			@Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "clearFragmentBackStackImmediate()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "clearFragmentBackStackImmediate() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
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

	public static void popFragmentFromStack(@Nullable android.support.v4.app.FragmentActivity fa, android.support.v4.app.Fragment fragment,
			@Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "popFragmentFromStack()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "popFragmentFromStack() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
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

	public static boolean popLatestEntryFromStack(@Nullable android.support.v4.app.FragmentActivity fa, @Nullable android.support.v4.app.Fragment optSource) {
		MTLog.v(TAG, "popLatestEntryFromStack()");
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(TAG, "popLatestEntryFromStack() > SKIP (activity is null/finishing)");
				return false; // not handled
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
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
