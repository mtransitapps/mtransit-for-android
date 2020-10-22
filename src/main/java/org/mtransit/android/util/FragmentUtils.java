package org.mtransit.android.util;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.mtransit.android.commons.MTLog;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class FragmentUtils implements MTLog.Loggable {

	private static final String LOG_TAG = FragmentUtils.class.getSimpleName();

	public static final String DIALOG_TAG = "dialog";

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static boolean isCurrentFragmentVisible(@Nullable FragmentActivity fa,
												   @IdRes int currentFragmentId,
												   @Nullable Fragment fragment) {
		if (fragment == null) {
			MTLog.d(LOG_TAG, "isCurrentFragmentVisible() > SKIP (fragment null = invisible)");
			return false;
		}
		FragmentManager fm = fa == null ? null : fa.getSupportFragmentManager();
		Fragment currentFragment = fm == null ? null : fm.findFragmentById(currentFragmentId);
		return isFragmentReady(fragment) && fragment.equals(currentFragment);
	}

	public static boolean isFragmentReady(@Nullable Fragment fragment) {
		return fragment != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
	}

	public static boolean isFragmentReady(@Nullable FragmentActivity fa,
										  @NonNull @IdRes Integer fragmentResId) {
		Fragment fragment = getFragment(fa, fragmentResId);
		return fragment != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
	}

	@Nullable
	public static Fragment getFragment(@Nullable FragmentActivity fa,
									   @NonNull @IdRes Integer fragmentResId) {
		FragmentManager fm = fa == null ? null : fa.getSupportFragmentManager();
		return fm == null ? null : fm.findFragmentById(fragmentResId);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isFragmentReady(@Nullable android.app.Fragment fragment) {
		return fragment != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
	}

	public static void replaceFragment(@Nullable FragmentActivity fa,
									   @IdRes int containerViewId,
									   @NonNull Fragment fragment,
									   boolean addToStack,
									   @Nullable Fragment optSource) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "replaceFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(LOG_TAG, "replaceFragment() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			FragmentManager fm = fa.getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.replace(containerViewId, fragment);
			if (addToStack) {
				ft.addToBackStack(null);
			}
			ft.commit();
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while replacing fragment '%s' from '%s' in '%s'!", fragment, optSource, fa);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while replacing fragment '%s' from '%s' in '%s'!", fragment, optSource, fa);
		}
	}

	public static void replaceFragment(@Nullable FragmentActivity fa,
									   @IdRes int containerViewId,
									   @NonNull Fragment fragment,
									   boolean addToStack,
									   @Nullable @IdRes Integer optSourceResId) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "replaceFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSourceResId != null // optional check
					&& !isFragmentReady(fa, optSourceResId)) {
				MTLog.d(LOG_TAG, "replaceFragment() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			FragmentManager fm = fa.getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.replace(containerViewId, fragment);
			if (addToStack) {
				ft.addToBackStack(null);
			}
			ft.commit();
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while replacing fragment '%s'!", fragment);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while replacing fragment '%s'!", fragment);
		}
	}

	public static void replaceDialogFragment(@Nullable FragmentActivity fa,
											 @Nullable String tag,
											 @Nullable DialogFragment dialogFragment,
											 @Nullable Fragment optSource) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			FragmentManager fm = fa.getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment prev = fm.findFragmentByTag(tag);
			if (prev != null) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > remove old dialog %s", prev);
				ft.remove(prev);
			}
			ft.addToBackStack(null);
			if (dialogFragment != null) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > add new dialog %s", dialogFragment);
				dialogFragment.show(ft, tag);
			}
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while replacing support dialog fragment '%s'!", dialogFragment);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while replacing support dialog fragment '%s'!", dialogFragment);
		}
	}

	public static void replaceDialogFragment(@Nullable android.app.Activity fa,
											 @Nullable String tag,
											 @Nullable android.app.DialogFragment dialogFragment,
											 @Nullable android.app.Fragment optSource) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			android.app.FragmentManager fm = fa.getFragmentManager();
			android.app.FragmentTransaction ft = fm.beginTransaction();
			android.app.Fragment prev = fm.findFragmentByTag(tag);
			if (prev != null) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > remove old dialog %s", prev);
				ft.remove(prev);
			}
			ft.addToBackStack(null);
			if (dialogFragment != null) {
				MTLog.d(LOG_TAG, "replaceDialogFragment() > add new dialog %s", dialogFragment);
				dialogFragment.show(ft, tag);
			}
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while replacing dialog fragment '%s'!", dialogFragment);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while replacing dialog fragment '%s'!", dialogFragment);
		}
	}

	public static void executePendingTransactions(@Nullable FragmentActivity fa, @Nullable Fragment optSource) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "executePendingTransactions() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(LOG_TAG, "executePendingTransactions() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			fa.getSupportFragmentManager().executePendingTransactions();
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while executing pending transactions!");
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while executing pending transactions!");
		}
	}

	public static void clearFragmentBackStackImmediate(@Nullable FragmentActivity fa,
													   @Nullable Fragment optSource) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "clearFragmentBackStackImmediate() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(LOG_TAG, "clearFragmentBackStackImmediate() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			fa.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while clearing fragment back stack immediately!");
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while clearing fragment back stack immediately!");
		}
	}

	public static void popFragmentFromStack(@Nullable FragmentActivity fa,
											@Nullable Fragment fragment,
											@Nullable Fragment optSource) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "popFragmentFromStack() > SKIP (activity is null/finishing)");
				return;
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(LOG_TAG, "popFragmentFromStack() > SKIP (source fragment is !added/detached/removing)");
				return;
			}
			if (fragment != null) {
				fa.getSupportFragmentManager().popBackStack();
			}
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while popping fragment '%s' from stack!", fragment);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while popping fragment '%s' from stack!", fragment);
		}
	}

	public static boolean popLatestEntryFromStack(@Nullable FragmentActivity fa, @Nullable Fragment optSource) {
		try {
			if (fa == null || fa.isFinishing()) {
				MTLog.d(LOG_TAG, "popLatestEntryFromStack() > SKIP (activity is null/finishing)");
				return false; // not handled
			}
			if (optSource != null // optional check
					&& !isFragmentReady(optSource)) {
				MTLog.d(LOG_TAG, "popLatestEntryFromStack() > SKIP (source fragment is !added/detached/removing)");
				return false; // not handled
			}
			FragmentManager fm = fa.getSupportFragmentManager();
			if (fm.getBackStackEntryCount() > 0) {
				fm.popBackStack();
				return true; // handled
			}
			return false; // not handled
		} catch (IllegalStateException ise) {
			CrashUtils.w(LOG_TAG, ise, "Illegal State Exception while popping latest entry from stack!");
			return false; // not handled
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Unexpected error while popping fragment latest entry from stack!");
			return false; // not handled
		}
	}
}
