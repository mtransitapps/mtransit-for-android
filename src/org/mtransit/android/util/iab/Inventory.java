package org.mtransit.android.util.iab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// based on the Google IAB sample (Apache License, Version 2.0)
public class Inventory {

	Map<String, SkuDetails> mSkuMap = new HashMap<String, SkuDetails>();
	Map<String, Purchase> mPurchaseMap = new HashMap<String, Purchase>();

	Inventory() {
	}

	public SkuDetails getSkuDetails(String sku) {
		return mSkuMap.get(sku);
	}

	public Purchase getPurchase(String sku) {
		return mPurchaseMap.get(sku);
	}

	public boolean hasPurchase(String sku) {
		return mPurchaseMap.containsKey(sku);
	}

	public boolean hasDetails(String sku) {
		return mSkuMap.containsKey(sku);
	}

	public void erasePurchase(String sku) {
		if (mPurchaseMap.containsKey(sku)) mPurchaseMap.remove(sku);
	}

	public List<String> getAllOwnedSkus() {
		return new ArrayList<String>(mPurchaseMap.keySet());
	}

	public List<String> getAllOwnedSkus(String itemType) {
		List<String> result = new ArrayList<String>();
		for (Purchase p : mPurchaseMap.values()) {
			if (p.getItemType().equals(itemType)) result.add(p.getSku());
		}
		return result;
	}

	public List<Purchase> getAllPurchases() {
		return new ArrayList<Purchase>(mPurchaseMap.values());
	}

	public Set<String> getAllSkus() {
		return mSkuMap.keySet();
	}

	public void addSkuDetails(SkuDetails d) {
		mSkuMap.put(d.getSku(), d);
	}

	public void addPurchase(Purchase p) {
		mPurchaseMap.put(p.getSku(), p);
	}
}
