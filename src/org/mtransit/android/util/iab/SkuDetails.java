package org.mtransit.android.util.iab;

import org.json.JSONException;
import org.json.JSONObject;

// based on the Google IAB sample (Apache License, Version 2.0)
public class SkuDetails {

	String mItemType;
	String mSku;
	String mType;
	String mPrice;
	String mTitle;
	String mDescription;
	String mJson;

	public SkuDetails(String jsonSkuDetails) throws JSONException {
		this(IabHelper.ITEM_TYPE_INAPP, jsonSkuDetails);
	}

	public SkuDetails(String itemType, String jsonSkuDetails) throws JSONException {
		mItemType = itemType;
		mJson = jsonSkuDetails;
		JSONObject o = new JSONObject(mJson);
		mSku = o.optString("productId");
		mType = o.optString("type");
		mPrice = o.optString("price");
		mTitle = o.optString("title");
		mDescription = o.optString("description");
	}

	public String getSku() {
		return mSku;
	}

	public String getType() {
		return mType;
	}

	public String getPrice() {
		return mPrice;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getDescription() {
		return mDescription;
	}

	@Override
	public String toString() {
		return SkuDetails.class.getSimpleName() + ":" + mJson;
	}
}
