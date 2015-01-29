package org.mtransit.android.util.iab;

// based on the Google IAB sample (Apache License, Version 2.0)
public class IabResult {

	int mResponse;
	String mMessage;

	public IabResult(int response, String message) {
		mResponse = response;
		if (message == null || message.trim().length() == 0) {
			mMessage = IabHelper.getResponseDesc(response);
		} else {
			mMessage = message + " (response: " + IabHelper.getResponseDesc(response) + ")";
		}
	}

	public int getResponse() {
		return mResponse;
	}

	public String getMessage() {
		return mMessage;
	}

	public boolean isSuccess() {
		return mResponse == IabHelper.BILLING_RESPONSE_RESULT_OK;
	}

	public boolean isFailure() {
		return !isSuccess();
	}

	@Override
	public String toString() {
		return IabResult.class.getSimpleName() + ": " + getMessage();
	}
}
