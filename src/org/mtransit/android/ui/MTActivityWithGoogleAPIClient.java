package org.mtransit.android.ui;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.fragment.MTDialogFragmentV4;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

public abstract class MTActivityWithGoogleAPIClient extends MTActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
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

	public GoogleApiClient getGoogleApiClientOrNull() {
		return this.googleApiClient;
	}

	public GoogleApiClient getGoogleApiClientOrInit() {
		if (this.useGooglePlayServices && this.googleApiClient == null) {
			initGoogleApiClient();
		}
		return this.googleApiClient;
	}

	private synchronized void initGoogleApiClient() {
		if (this.googleApiClient != null) {
			return;
		}
		final GoogleApiClient.Builder googleApiClientBuilder = new GoogleApiClient.Builder(this);
		addGoogleAPIs(googleApiClientBuilder);
		googleApiClientBuilder //
				.addConnectionCallbacks(this)//
				.addOnConnectionFailedListener(this);
		this.googleApiClient = googleApiClientBuilder.build();
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
				final GoogleApiClient googleApiClient = getGoogleApiClientOrInit();
				if (googleApiClient != null && !googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
					googleApiClient.connect();
				}
				break;
			default:
				MTLog.w(this, "Unexpected artivity result code '%s'.", resultCode);
			}
			break;
		default:
			MTLog.w(this, "Unexpected artivity request code '%s'.", requestCode);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!this.resolvingError) {
			new StartGoogleApiClientTask(this).execute();
		}
	}

	private static class StartGoogleApiClientTask extends MTAsyncTask<Void, Void, Void> {

		private static final String TAG = MTActivityWithGoogleAPIClient.class.getSimpleName() + ">" + StartGoogleApiClientTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<MTActivityWithGoogleAPIClient> activityWR;

		public StartGoogleApiClientTask(MTActivityWithGoogleAPIClient activity) {
			this.activityWR = new WeakReference<MTActivityWithGoogleAPIClient>(activity);
		}

		@Override
		protected Void doInBackgroundMT(Void... params) {
			MTActivityWithGoogleAPIClient activity = this.activityWR == null ? null : this.activityWR.get();
			if (activity == null) {
				return null;
			}
			final GoogleApiClient googleApiClient = activity.getGoogleApiClientOrInit();
			if (googleApiClient != null) {
				googleApiClient.connect();
			}
			return null;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		new StopGoogleApiClientTask(this).execute();
	}

	private static class StopGoogleApiClientTask extends MTAsyncTask<Void, Void, Void> {

		private static final String TAG = MTActivityWithGoogleAPIClient.class.getSimpleName() + ">" + StopGoogleApiClientTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<MTActivityWithGoogleAPIClient> activityWR;

		public StopGoogleApiClientTask(MTActivityWithGoogleAPIClient activity) {
			this.activityWR = new WeakReference<MTActivityWithGoogleAPIClient>(activity);
		}

		@Override
		protected Void doInBackgroundMT(Void... params) {
			MTActivityWithGoogleAPIClient activity = this.activityWR == null ? null : this.activityWR.get();
			if (activity == null) {
				return null;
			}
			activity.onBeforeClientDisconnected();
			final GoogleApiClient googleApiClient = activity.getGoogleApiClientOrNull();
			if (googleApiClient != null) {
				googleApiClient.disconnect();
			}
			return null;
		}
	}

	public abstract void onBeforeClientDisconnected();

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (this.googleApiClient != null) {
			this.googleApiClient.unregisterConnectionCallbacks(this);
			this.googleApiClient.unregisterConnectionFailedListener(this);
			this.googleApiClient = null;
		}
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
	public void onConnectionFailed(ConnectionResult result) {
		if (this.resolvingError) {
			return;
		} else if (result.hasResolution()) {
			try {
				this.resolvingError = true;
				result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
			} catch (SendIntentException sie) {
				MTLog.w(this, sie, "Error while resolving Google Play Services error!");
				final GoogleApiClient googleApiClient = getGoogleApiClientOrInit();
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
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment(this);
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getSupportFragmentManager(), "errordialog");
	}

	public void onDialogDismissed() {
		this.resolvingError = false;
	}

	public static class ErrorDialogFragment extends MTDialogFragmentV4 {

		private static final String TAG = ErrorDialogFragment.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<MTActivityWithGoogleAPIClient> activityWR;

		public ErrorDialogFragment(MTActivityWithGoogleAPIClient activity) {
			this.activityWR = new WeakReference<MTActivityWithGoogleAPIClient>(activity);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode, this.getActivity(), REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			MTActivityWithGoogleAPIClient activity = this.activityWR == null ? null : this.activityWR.get();
			if (activity != null) {
				activity.onDialogDismissed();
			}
		}

	}

}
