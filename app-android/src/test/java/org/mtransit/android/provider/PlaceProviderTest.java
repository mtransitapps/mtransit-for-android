package org.mtransit.android.provider;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.android.libraries.places.api.net.SearchByTextRequest;

import java.util.Arrays;
import java.util.Collections;

public class PlaceProviderTest {

	private static final Double LAT = 45.503937;
	private static final Double LNG = -73.588061;
	private static final Integer RADIUS_IN_METERS = 5000;

	@Test
	public void testGetTextSearchUrlString_null() {
		// Arrange
		Iterable<String> searchKeywords = null;
		// Act
		//noinspection ConstantConditions
		SearchByTextRequest result = PlaceProvider.getTextSearchRequest(LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_empty() {
		// Arrange
		Iterable<String> searchKeywords = Collections.emptyList();
		// Act
		SearchByTextRequest result = PlaceProvider.getTextSearchRequest(LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_emptyArray() {
		// Arrange
		Iterable<String> searchKeywords = Arrays.asList(null, "", " ");
		// Act
		SearchByTextRequest result = PlaceProvider.getTextSearchRequest(LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_digitOnly() {
		// Arrange
		Iterable<String> searchKeywords = Collections.singleton("1234");
		// Act
		SearchByTextRequest result = PlaceProvider.getTextSearchRequest(LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_digitAndString() {
		// Arrange
		Iterable<String> searchKeywords = Arrays.asList("1234", "Street st");
		// Act
		SearchByTextRequest result = PlaceProvider.getTextSearchRequest(LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNotNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_smallString() {
		// Arrange
		Iterable<String> searchKeywords = Collections.singleton("ab");
		// Act
		SearchByTextRequest result = PlaceProvider.getTextSearchRequest(LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNull(result);
	}

	@Test
	public void testGetTextSearchUrlString_bigString() {
		// Arrange
		Iterable<String> searchKeywords = Collections.singleton("abc");
		// Act
		SearchByTextRequest result = PlaceProvider.getTextSearchRequest(LAT, LNG, RADIUS_IN_METERS, searchKeywords);
		// Assert
		assertNotNull(result);
	}
}
