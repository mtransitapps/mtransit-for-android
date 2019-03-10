package org.mtransit.android.billing;

import java.util.List;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

public interface IBillingManager {

	String SKU_SUBSCRIPTION = "_subscription_";

	String SKU_STARTS_WITH_F = "f_";

	String DEFAULT_PRICE_CAT = "1";

	String WEEKLY = "weekly";
	String MONTHLY = "monthly";
	String YEARLY = "yearly";

	String DEFAULT_PERIOD_CAT = MONTHLY;

	void refreshPurchases();

	void destroy();

	void addListener(@NonNull OnBillingResultListener listener);

	void removeListener(@NonNull OnBillingResultListener listener);

	@Nullable
	Boolean hasSubscription();

	String getSku(String periodCat, String priceCat);

	boolean isSkuAvailableForPurchase(@NonNull String sku);

	void queryInventoryAsync(@NonNull OnBillingInventoryListener listener);

	void launchPurchase(Activity activity, String sku);

	interface OnBillingResultListener {
		void onBillingResult(@Nullable Boolean hasSubscription);
	}

	interface OnBillingInventoryListener {
		void onBillingInventoryResult(@NonNull List<Pair<String, String>> skuDetailsList);

		void onUserCancelled();

		void onUnexpectedError();
	}
}
