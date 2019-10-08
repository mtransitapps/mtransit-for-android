package org.mtransit.android.data;

import java.util.ArrayList;

import org.junit.Test;
import org.mtransit.android.commons.CollectionUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class AgencyPropertiesTests {

	@SuppressWarnings("ConstantConditions")
	@Test
	public void testShortNameComparator() {
		ArrayList<AgencyProperties> agencies = new ArrayList<>();
		String shortName;
		//
		shortName = null;
		agencies.add(new AgencyProperties(null, null, shortName, null, null, null, false));
		shortName = "A";
		agencies.add(new AgencyProperties(null, null, shortName, null, null, null, false));
		shortName = "Z";
		agencies.add(new AgencyProperties(null, null, shortName, null, null, null, false));
		shortName = "b";
		agencies.add(new AgencyProperties(null, null, shortName, null, null, null, false));
		shortName = null;
		agencies.add(new AgencyProperties(null, null, shortName, null, null, null, false));
		//
		CollectionUtils.sort(agencies, AgencyProperties.SHORT_NAME_COMPARATOR);
		//
		assertNull(agencies.get(0).getShortName());
		assertNull(agencies.get(1).getShortName());
		assertEquals("A", agencies.get(2).getShortName());
		assertEquals("b", agencies.get(3).getShortName());
		assertEquals("Z", agencies.get(4).getShortName());
	}
}