package org.mtransit.android.util.iab;

// based on the Google IAB sample (Apache License, Version 2.0) based itself on Robert Harder code
public class Base64 {

	public final static boolean ENCODE = true;

	public final static boolean DECODE = false;

	private final static byte EQUALS_SIGN = (byte) '=';

	private final static byte NEW_LINE = (byte) '\n';

	private final static byte[] ALPHABET = { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I',
			(byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U',
			(byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
			(byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's',
			(byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4',
			(byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+', (byte) '/' };

	private final static byte[] WEBSAFE_ALPHABET = { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H',
			(byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T',
			(byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f',
			(byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r',
			(byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3',
			(byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '-', (byte) '_' };

	private final static byte[] DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
			62, // Plus sign at decimal 43
			-9, -9, -9, // Decimal 44 - 46
			63, // Slash at decimal 47
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through 'N'
			14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O' through 'Z'
			-9, -9, -9, -9, -9, -9, // Decimal 91 - 96
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a' through 'm'
			39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n' through 'z'
			-9, -9, -9, -9, -9 // Decimal 123 - 127
	};

	private final static byte[] WEBSAFE_DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 44
			62, // Dash '-' sign at decimal 45
			-9, -9, // Decimal 46-47
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through 'N'
			14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O' through 'Z'
			-9, -9, -9, -9, // Decimal 91-94
			63, // Underscore '_' at decimal 95
			-9, // Decimal 96
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a' through 'm'
			39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n' through 'z'
			-9, -9, -9, -9, -9 // Decimal 123 - 127
	};

	private final static byte WHITE_SPACE_ENC = -5;
	private final static byte EQUALS_SIGN_ENC = -1;

	private Base64() {
	}

	private static byte[] encode3to4(byte[] source, int srcOffset, int numSigBytes, byte[] destination, int destOffset, byte[] alphabet) {
		int inBuff = (numSigBytes > 0 ? ((source[srcOffset] << 24) >>> 8) : 0) | (numSigBytes > 1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
				| (numSigBytes > 2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);
		switch (numSigBytes) {
		case 3:
			destination[destOffset] = alphabet[(inBuff >>> 18)];
			destination[destOffset + 1] = alphabet[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = alphabet[(inBuff >>> 6) & 0x3f];
			destination[destOffset + 3] = alphabet[(inBuff) & 0x3f];
			return destination;
		case 2:
			destination[destOffset] = alphabet[(inBuff >>> 18)];
			destination[destOffset + 1] = alphabet[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = alphabet[(inBuff >>> 6) & 0x3f];
			destination[destOffset + 3] = EQUALS_SIGN;
			return destination;
		case 1:
			destination[destOffset] = alphabet[(inBuff >>> 18)];
			destination[destOffset + 1] = alphabet[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = EQUALS_SIGN;
			destination[destOffset + 3] = EQUALS_SIGN;
			return destination;
		default:
			return destination;
		} // end switch
	} // end encode3to4

	public static String encode(byte[] source) {
		return encode(source, 0, source.length, ALPHABET, true);
	}

	public static String encodeWebSafe(byte[] source, boolean doPadding) {
		return encode(source, 0, source.length, WEBSAFE_ALPHABET, doPadding);
	}

	public static String encode(byte[] source, int off, int len, byte[] alphabet, boolean doPadding) {
		byte[] outBuff = encode(source, off, len, alphabet, Integer.MAX_VALUE);
		int outLen = outBuff.length;
		while (doPadding == false && outLen > 0) {
			if (outBuff[outLen - 1] != '=') {
				break;
			}
			outLen -= 1;
		}
		return new String(outBuff, 0, outLen);
	}

	public static byte[] encode(byte[] source, int off, int len, byte[] alphabet, int maxLineLength) {
		int lenDiv3 = (len + 2) / 3; // ceil(len / 3)
		int len43 = lenDiv3 * 4;
		byte[] outBuff = new byte[len43 // Main 4:3
				+ (len43 / maxLineLength)]; // New lines
		int d = 0;
		int e = 0;
		int len2 = len - 2;
		int lineLength = 0;
		for (; d < len2; d += 3, e += 4) {
			int inBuff = ((source[d + off] << 24) >>> 8) | ((source[d + 1 + off] << 24) >>> 16) | ((source[d + 2 + off] << 24) >>> 24);
			outBuff[e] = alphabet[(inBuff >>> 18)];
			outBuff[e + 1] = alphabet[(inBuff >>> 12) & 0x3f];
			outBuff[e + 2] = alphabet[(inBuff >>> 6) & 0x3f];
			outBuff[e + 3] = alphabet[(inBuff) & 0x3f];
			lineLength += 4;
			if (lineLength == maxLineLength) {
				outBuff[e + 4] = NEW_LINE;
				e++;
				lineLength = 0;
			} // end if: end of line
		} // end for: each piece of array
		if (d < len) {
			encode3to4(source, d + off, len - d, outBuff, e, alphabet);
			lineLength += 4;
			if (lineLength == maxLineLength) {
				// Add a last newline
				outBuff[e + 4] = NEW_LINE;
				e++;
			}
			e += 4;
		}
		assert (e == outBuff.length);
		return outBuff;
	}

	private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset, byte[] decodabet) {
		if (source[srcOffset + 2] == EQUALS_SIGN) {
			int outBuff = ((decodabet[source[srcOffset]] << 24) >>> 6) | ((decodabet[source[srcOffset + 1]] << 24) >>> 12);
			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		} else if (source[srcOffset + 3] == EQUALS_SIGN) {
			int outBuff = ((decodabet[source[srcOffset]] << 24) >>> 6) | ((decodabet[source[srcOffset + 1]] << 24) >>> 12)
					| ((decodabet[source[srcOffset + 2]] << 24) >>> 18);
			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		} else {
			int outBuff = ((decodabet[source[srcOffset]] << 24) >>> 6) | ((decodabet[source[srcOffset + 1]] << 24) >>> 12)
					| ((decodabet[source[srcOffset + 2]] << 24) >>> 18) | ((decodabet[source[srcOffset + 3]] << 24) >>> 24);
			destination[destOffset] = (byte) (outBuff >> 16);
			destination[destOffset + 1] = (byte) (outBuff >> 8);
			destination[destOffset + 2] = (byte) (outBuff);
			return 3;
		}
	} // end decodeToBytes

	public static byte[] decode(String s) throws Base64DecoderException {
		byte[] bytes = s.getBytes();
		return decode(bytes, 0, bytes.length);
	}

	public static byte[] decodeWebSafe(String s) throws Base64DecoderException {
		byte[] bytes = s.getBytes();
		return decodeWebSafe(bytes, 0, bytes.length);
	}

	public static byte[] decode(byte[] source) throws Base64DecoderException {
		return decode(source, 0, source.length);
	}

	public static byte[] decodeWebSafe(byte[] source) throws Base64DecoderException {
		return decodeWebSafe(source, 0, source.length);
	}

	public static byte[] decode(byte[] source, int off, int len) throws Base64DecoderException {
		return decode(source, off, len, DECODABET);
	}

	public static byte[] decodeWebSafe(byte[] source, int off, int len) throws Base64DecoderException {
		return decode(source, off, len, WEBSAFE_DECODABET);
	}

	public static byte[] decode(byte[] source, int off, int len, byte[] decodabet) throws Base64DecoderException {
		int len34 = len * 3 / 4;
		byte[] outBuff = new byte[2 + len34]; // Upper limit on size of output
		int outBuffPosn = 0;
		byte[] b4 = new byte[4];
		int b4Posn = 0;
		int i = 0;
		byte sbiCrop = 0;
		byte sbiDecode = 0;
		for (i = 0; i < len; i++) {
			sbiCrop = (byte) (source[i + off] & 0x7f); // Only the low seven bits
			sbiDecode = decodabet[sbiCrop];
			if (sbiDecode >= WHITE_SPACE_ENC) { // White space Equals sign or better
				if (sbiDecode >= EQUALS_SIGN_ENC) {
					if (sbiCrop == EQUALS_SIGN) {
						int bytesLeft = len - i;
						byte lastByte = (byte) (source[len - 1 + off] & 0x7f);
						if (b4Posn == 0 || b4Posn == 1) {
							throw new Base64DecoderException("invalid padding byte '=' at byte offset " + i);
						} else if ((b4Posn == 3 && bytesLeft > 2) || (b4Posn == 4 && bytesLeft > 1)) {
							throw new Base64DecoderException("padding byte '=' falsely signals end of encoded value " + "at offset " + i);
						} else if (lastByte != EQUALS_SIGN && lastByte != NEW_LINE) {
							throw new Base64DecoderException("encoded value has invalid trailing byte");
						}
						break;
					}
					b4[b4Posn++] = sbiCrop;
					if (b4Posn == 4) {
						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn, decodabet);
						b4Posn = 0;
					}
				}
			} else {
				throw new Base64DecoderException("Bad Base64 input character at " + i + ": " + source[i + off] + "(decimal)");
			}
		}
		if (b4Posn != 0) {
			if (b4Posn == 1) {
				throw new Base64DecoderException("single trailing character at offset " + (len - 1));
			}
			b4[b4Posn++] = EQUALS_SIGN;
			outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn, decodabet);
		}
		byte[] out = new byte[outBuffPosn];
		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
		return out;
	}
}
