/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mtransit.android.util.iab;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.mtransit.android.commons.MTLog;

import android.text.TextUtils;

// based on the Google IAB sample (Apache License, Version 2.0)
public class Security implements MTLog.Loggable {

	private static final String TAG = Security.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String KEY_FACTORY_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

	public static boolean verifyPurchase(String base64PublicKey, String signedData, String signature) {
		if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) || TextUtils.isEmpty(signature)) {
			MTLog.e(TAG, "Purchase verification failed: missing data.");
			return false;
		}
		PublicKey key = Security.generatePublicKey(base64PublicKey);
		return Security.verify(key, signedData, signature);
	}

	public static PublicKey generatePublicKey(String encodedPublicKey) {
		try {
			byte[] decodedKey = Base64.decode(encodedPublicKey);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeySpecException e) {
			MTLog.e(TAG, "Invalid key specification.");
			throw new IllegalArgumentException(e);
		} catch (Base64DecoderException e) {
			MTLog.e(TAG, "Base64 decoding failed.");
			throw new IllegalArgumentException(e);
		}
	}

	public static boolean verify(PublicKey publicKey, String signedData, String signature) {
		Signature sig;
		try {
			sig = Signature.getInstance(SIGNATURE_ALGORITHM);
			sig.initVerify(publicKey);
			sig.update(signedData.getBytes());
			if (!sig.verify(Base64.decode(signature))) {
				MTLog.e(TAG, "Signature verification failed.");
				return false;
			}
			return true;
		} catch (NoSuchAlgorithmException e) {
			MTLog.e(TAG, "NoSuchAlgorithmException.");
		} catch (InvalidKeyException e) {
			MTLog.e(TAG, "Invalid key specification.");
		} catch (SignatureException e) {
			MTLog.e(TAG, "Signature exception.");
		} catch (Base64DecoderException e) {
			MTLog.e(TAG, "Base64 decoding failed.");
		}
		return false;
	}
}
