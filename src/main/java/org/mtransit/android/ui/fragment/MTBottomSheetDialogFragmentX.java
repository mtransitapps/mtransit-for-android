package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTBottomSheetDialogFragmentX extends BottomSheetDialogFragment implements MTLog.Loggable {

	public MTBottomSheetDialogFragmentX() {
		super();
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	// ACTUALLY NOT WORKING (requires custom FragmentFactory)
	@SuppressWarnings("unused")
	private MTBottomSheetDialogFragmentX(@LayoutRes int contentLayoutId) {
		super(contentLayoutId);
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@Override
	public void dismiss() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "dismiss()");
		}
		super.dismiss();
	}

	@Override
	public void dismissAllowingStateLoss() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "dismissAllowingStateLoss()");
		}
		super.dismissAllowingStateLoss();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateDialog(%s)", savedInstanceState);
		}
		return super.onCreateDialog(savedInstanceState);
	}

	@Override
	public void show(@NonNull FragmentManager manager, @Nullable String tag) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "show(%s,%s)", manager, tag);
		}
		super.show(manager, tag);
	}

	@Override
	public int show(@NonNull FragmentTransaction transaction, @Nullable String tag) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "show(%s,%s)", transaction, tag);
		}
		return super.show(transaction, tag);
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCancel(%s)", dialog);
		}
		super.onCancel(dialog);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDismiss(%s)", dialog);
		}
		super.onDismiss(dialog);
	}

	// INHERITED FROM FRAGMENT
	@SuppressWarnings("deprecation")
	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onActivityCreated(%s)", savedInstanceState);
		}
		super.onActivityCreated(savedInstanceState);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onActivityResult(%s,%s,%s)", requestCode, resultCode, data);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(@NonNull Activity activity) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onAttach(%s)", activity);
		}
		super.onAttach(activity);
	}

	@Override
	public void onAttach(@NonNull Context context) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onAttach(%s)", context);
		}
		super.onAttach(context);
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onConfigurationChanged(%s)", newConfig);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreate(%s)", savedInstanceState);
		}
		super.onCreate(savedInstanceState);
	}

	@MainThread
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateView(%s,%s,%s)", inflater, container, savedInstanceState);
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onDestroy() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroy()");
		}
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroyView()");
		}
		super.onDestroyView();
	}

	@Override
	public void onDetach() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDetach()");
		}
		super.onDetach();
	}

	@SuppressWarnings("deprecation")
	@UiThread
	@CallSuper
	@Override
	public void onInflate(@NonNull Activity activity, @NonNull AttributeSet attrs, @Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onInflate(%s,%s,%s)", activity, attrs, savedInstanceState);
		}
		super.onInflate(activity, attrs, savedInstanceState);
	}

	@UiThread
	@CallSuper
	@Override
	public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs, @Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onInflate(%s,%s,%s)", context, attrs, savedInstanceState);
		}
		super.onInflate(context, attrs, savedInstanceState);
	}

	@Override
	public void onLowMemory() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onLowMemory()");
		}
		super.onLowMemory();
	}

	@Override
	public void onPause() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPause()");
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onResume()");
		}
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onSaveInstanceState(%s)", outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStart()");
		}
		super.onStart();
	}

	@Override
	public void onStop() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStop()");
		}
		super.onStop();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onViewCreated(%s, %s)", view, savedInstanceState);
		}
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onViewStateRestored(%s)", savedInstanceState);
		}
		super.onViewStateRestored(savedInstanceState);
	}
}
