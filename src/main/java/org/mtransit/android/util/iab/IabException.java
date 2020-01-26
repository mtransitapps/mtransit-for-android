package org.mtransit.android.util.iab;

// based on the Google IAB sample (Apache License, Version 2.0)
public class IabException extends Exception {

	private static final long serialVersionUID = 1L;

	IabResult mResult;

	public IabException(IabResult r) {
		this(r, null);
	}

	public IabException(int response, String message) {
		this(new IabResult(response, message));
	}

	public IabException(IabResult r, Exception cause) {
		super(r.getMessage(), cause);
		mResult = r;
	}

	public IabException(int response, String message, Exception cause) {
		this(new IabResult(response, message), cause);
	}

	public IabResult getResult() {
		return mResult;
	}
}