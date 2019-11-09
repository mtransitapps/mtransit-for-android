package org.mtransit.android.provider;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PlaceProviderTest {

	private static final String API_KEY = "API_KEY";
	private static final Double LAT = 45.503937;
	private static final Double LNG = -73.588061;
	private static final Integer RADIUS_IN_METERS = 5000;

	@Test
	public void testGetTextSearchUrlString_null() {
		// Arrange
		String[] searchKeywords = null;
		// Act
		String result = PlaceProvider.getTextSearchUrlString(API_KEY, LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_empty() {
		// Arrange
		String[] searchKeywords = new String[0];
		// Act
		String result = PlaceProvider.getTextSearchUrlString(API_KEY, LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_emptyArray() {
		// Arrange
		String[] searchKeywords = new String[]{null, "", " "};
		// Act
		String result = PlaceProvider.getTextSearchUrlString(API_KEY, LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_digitOnly() {
		// Arrange
		String[] searchKeywords = new String[]{"1234"};
		// Act
		String result = PlaceProvider.getTextSearchUrlString(API_KEY, LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_digitAndString() {
		// Arrange
		String[] searchKeywords = new String[]{"1234", "Street st"};
		// Act
		String result = PlaceProvider.getTextSearchUrlString(API_KEY, LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNotNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_smallString() {
		// Arrange
		String[] searchKeywords = new String[]{"ab"};
		// Act
		String result = PlaceProvider.getTextSearchUrlString(API_KEY, LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_bigString() {
		// Arrange
		String[] searchKeywords = new String[]{"abc"};
		// Act
		String result = PlaceProvider.getTextSearchUrlString(API_KEY, LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNotNull(result);
	}

}