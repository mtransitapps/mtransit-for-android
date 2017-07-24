package org.mtransit.android.ui;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.MTDialogFragmentV4;
import org.mtransit.android.util.FragmentUtils;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

public abstract class MTActivityWithGoogleAPIClient extends MTActivity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

	private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final String DIALOG_ERROR = "dialog_error";
	private static final String STATE_RESOLVING_ERROR = "resolving_error";

	private boolean resolvingError = false;
	private boolean useGooglePlayServices = false;
	private GoogleApiClient googleApiClient;

	public MTActivityWithGoogleAPIClient(boolean useGooglePlayServices) {
		this.useGooglePlayServices = useGooglePlayServices;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.resolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
	}

	@Nullable
	public GoogleApiClient getGoogleApiClientOrNull() {
		return this.googleApiClient;
	}

	private GoogleApiClient getGoogleApiClientOrInit() {
		if (this.useGooglePlayServices && this.googleApiClient == null) {
			initGoogleApiClient();
		}
		return this.googleApiClient;
	}

	private synchronized void initGoogleApiClient() {
		if (this.googleApiClient != null) {
			return;
		}
		GoogleApiClient.Builder googleApiClientBuilder = new GoogleApiClient.Builder(this);
		addGoogleAPIs(googleApiClientBuilder);
		googleApiClientBuilder //
				.addConnectionCallbacks(this)//
				.addOnConnectionFailedListener(this);
		this.googleApiClient = googleApiClientBuilder.build();
	}

	private void destroyGoogleApiClient() {
		this.googleApiClient = null;
	}

	protected abstract void addGoogleAPIs(GoogleApiClient.Builder googleApiClientBuilder);

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_RESOLVING_ERROR, this.resolvingError);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_RESOLVE_ERROR:
			switch (resultCode) {
			case Activity.RESULT_OK:
				GoogleApiClient googleApiClient = getGoogleApiClientOrInit();
				if (googleApiClient != null && !googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
					googleApiClient.connect();
				}
				break;
			default:
				MTLog.w(this, "Unexpected activity result code '%s'.", resultCode);
			}
			break;
		default:
			MTLog.w(this, "Unexpected activity request code '%s'.", requestCode);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!this.resolvingError) {
			GoogleApiClient googleApiClient = getGoogleApiClientOrInit();
			if (googleApiClient != null && !googleApiClient.isConnected()) {
				googleApiClient.connect();
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		GoogleApiClient googleApiClient = getGoogleApiClientOrNull();
		if (googleApiClient != null) {
			googleApiClient.unregisterConnectionCallbacks(this);
			googleApiClient.unregisterConnectionFailedListener(this);
			googleApiClient.disconnect();
		}
		destroyGoogleApiClient();
	}

	public abstract void onBeforeClientDisconnected();

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		onClientConnected();
	}

	public abstract void onClientConnected();

	@Override
	public void onConnectionSuspended(int cause) {
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		if (this.resolvingError) {
			return;
		}
		if (result.hasResolution()) {
			try {
				this.resolvingError = true;
				result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
			} catch (SendIntentException sie) {
				MTLog.w(this, sie, "Error while resolving Google Play Services error!");
				GoogleApiClient googleApiClient = getGoogleApiClientOrInit();
				if (googleApiClient != null) {
					googleApiClient.connect();
				}
			}
		} else {
			showErrorDialog(result.getErrorCode());
			this.resolvingError = true;
		}
	}

	private void showErrorDialog(int errorCode) {
		FragmentUtils.replaceDialogFragment(this, FragmentUtils.DIALOG_TAG, ErrorDialogFragment.newInstance(errorCode), null);
	}

	private void onDialogDismissed() {
		this.resolvingError = false;
	}

	public static class ErrorDialogFragment extends MTDialogFragmentV4 {

		private static final String TAG = ErrorDialogFragment.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public static ErrorDialogFragment newInstance(int errorCode) {
			ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
			Bundle args = new Bundle();
			args.putInt(DIALOG_ERROR, errorCode);
			dialogFragment.setArguments(args);
			return dialogFragment;
		}

		public ErrorDialogFragment() {
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int errorCode = getArguments().getInt(DIALOG_ERROR);
			return GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			FragmentActivity activity = getActivity();
			if (activity != null && activity instanceof MTActivityWithGoogleAPIClient) {
				((MTActivityWithGoogleAPIClient) activity).onDialogDismissed();
			}
		}
	}
}
