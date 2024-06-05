package org.mtransit.android.dev;

import android.location.Location;

import androidx.annotation.Nullable;

@SuppressWarnings("ConstantConditions")
public final class FakeLocation {

	public static final boolean ENABLED = false;
	// public static final boolean ENABLED = true; // DEBUG

	@Nullable
	public static Location getLocation() {
		// CANADA
		//noinspection UnusedAssignment,UnnecessaryLocalVariable
		Location location = null;
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.5131577, -73.4087376, 77F); // DEBUG
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.525191, -73.521961, 77F); // DEBUG - Station Longueuil
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.4789944, -73.449425, 77F); // DEBUG - RTL Line 5 - Grande Allée et mtée St-Hubert
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.514851, -73.559654, 77F); // DEBUG
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.4947620353203, -73.570805750666, 77F); // DEBUG
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.5254137,-73.5742727);
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.5159807, -73.5656846, 77F); // DEBUG
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.522018, -73.579445, 77F); // DEBUG (Montreal Office)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.518733, -73.581145, 77F); // DEBUG (Montreal ...)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.514841, -73.559646, 77F); // DEBUG (Montreal, Berri-UQAM)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.512597, -73.560596, 77F); // DEBUG (Montreal, UQAM)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.504774, -73.577123, 77F); // DEBUG (Montreal, McGill University)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.5, -73.566667, 77F); // DEBUG (Montreal)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.498570, -73.569077, 77F); // DEBUG (Montreal train)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.473916, -74.060423, 77F); // DEBUG (Deux-Montagnes)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.583333, -73.75, 77F); // DEBUG (Laval)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.558335, -73.721824, 77F); // DEBUG (Laval - Montmorency)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.566799, -73.744000, 77F); // DEBUG (Laval - Cosmodome)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.533333, -73.516667, 77F); // DEBUG (Longueuil)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.638366, -73.840007, 77F); // DEBUG (Laurentides)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.118697, -74.593228, 77F); // DEBUG (L'Inter des Laurentides)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.021284, -73.448818, 77F); // DEBUG (Lanaudière)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.547007, -73.229786, 77F); // DEBUG (Richelieu, Vallée du)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.088713, -74.173483, 77F); // DEBUG (Haut-St-Laurent)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.399002, -74.025913, 77F); // DEBUG (La Presqu'île)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.417042, -73.463271, 77F); // DEBUG (Le Richelain)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.714468, -73.474216, 77F); // DEBUG (L'Assomption)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.697795, -73.653956, 77F); // DEBUG (Les Moulins / Terrebonne-Mascouche)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.360619, -73.514753, 77F); // DEBUG (Candiac)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.447702, -73.278559, 77F); // DEBUG (Chambly-Richelieu-Carignan)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.385600, -73.567099, 77F); // DEBUG (Roussillon)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.360579, -73.732754, 77F); // DEBUG (Sud-Ouest)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.652719, -73.299551, 77F); // DEBUG (Sorel-Varennes)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.587540, -73.326283, 77F); // DEBUG (Ste-Julie)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.400023, -74.051315, 77F); // DEBUG (Vaudreuil)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.499370, -73.560304, 77F); // DEBUG (Orleans Express - Montreal)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.818937, -71.213429, 77F); // DEBUG (Orleans Express - Quebec)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.146602, -60.224134, 77F); // DEBUG (Maritime Bus - )
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.815010, -71.202920, 77F); // DEBUG (Quebec)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.748814, -71.232096, 77F); // DEBUG (Lévis)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.378654, -71.929601, 77F); // DEBUG (Sherbrooke)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.620383, -72.681895, 77F); // DEBUG (Shawinigan)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.430151, -75.709187, 77F); // DEBUG (Gatineau)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.425144, -75.699950, 77F); // DEBUG (Ottawa)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.387269, -75.689013, 77F); // DEBUG (Ottawa - Brewer Park)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.388951, -75.710554, 77F); // DEBUG (Ottawa - Nature/Agriculture/Food/Farm)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.016680, -74.722180, 77F); // DEBUG (Cornwall)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(44.233297, -76.479631, 77F); // DEBUG (Kingston)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(48.416896, -89.236989, 77F); // DEBUG (Thunder Bay)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.945374, -78.896391, 77F); // DEBUG (Durham Region)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.653425, -79.384080, 77F); // DEBUG (Toronto)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.667114, -79.397241, 77F); // DEBUG (Toronto - University)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.681321, -79.611455, 77F); // DEBUG (Toronto - Pearson)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.466476, -80.532906, 77F); // DEBUG (Waterloo - GRT)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.326095, -79.798886, 77F); // DEBUG (Burlington)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.706969, -79.724756, 77F); // DEBUG (Brampton)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.568954, -79.654336, 77F); // DEBUG (Mississauga)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.546694, -80.246579, 77F); // DEBUG (Guelph)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.255387, -79.871990, 77F); // DEBUG (Hamilton)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.009595, -81.273736, 77F); // DEBUG (London)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(42.305132, -83.067483, 77F); // DEBUG (Windsor)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.862927, -79.422701, 77F); // DEBUG (York Region)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.464729, -79.701734, 77F); // DEBUG (Oakville)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(44.412043, -79.667775, 77F); // DEBUG (Barrie)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.117566, -79.247697, 77F); // DEBUG (St Catharines)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.014299, -79.263982, 77F); // DEBUG (Welland)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.078902, -79.078168, 77F); // DEBUG (Niagara Falls)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.092208, -79.072202, 77F); // DEBUG (Niagara Falls WEGO)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(42.870485, -79.054661, 77F); // DEBUG (Fort Erie)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.466326, -80.972989, 77F); // DEBUG (Greater Sudbury)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(43.521473, -79.896475, 77F); // DEBUG (Milton)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.945428, -66.640688, 77F); // DEBUG (Fredericton)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.106415, -64.785663, 77F); // DEBUG (Moncton)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(44.647308, -63.580287, 77F); // DEBUG (Halifax)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(47.523474, -52.753646, 77F); // DEBUG (St John's)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.807523, -97.136600, 77F); // DEBUG (Winnipeg)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.845335, -99.963355, 77F); // DEBUG (Brandon)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(52.133398, -106.631368, 77F); // DEBUG (Saskatoon)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(50.418294, -104.587026, 77F); // DEBUG (Regina)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.678658, -112.859590, 77F); // DEBUG (Lethbridge)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(51.078164, -114.135795, 77F); // DEBUG (Calgary)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(51.277658, -114.013925, 77F); // DEBUG (Airdrie)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(52.248035, -113.822084, 77F); // DEBUG (Red Deer)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(51.178845, -115.569817, 77F); // DEBUG (Banff)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(53.523214, -113.526310, 77F); // DEBUG (Edmonton)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(53.631757, -113.630215, 77F); // DEBUG (St Albert)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(53.543450, -113.304297, 77F); // DEBUG (Strathcona County)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(55.137635, -118.783205, 77F); // DEBUG (Grande Prairie)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.280267, -123.114748, 77F); // DEBUG (Vancouver)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(48.463404, -123.311699, 77F); // DEBUG (Victoria)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(50.118816, -122.954990, 77F); // DEBUG (Whistler)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.705840, -123.150360, 77F); // DEBUG (Squamish)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.397239, -123.510916, 77F); // DEBUG (Sunshine Coast)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(55.756273, -120.221935, 77F); // DEBUG (Dawson Creek)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(56.245855, -120.847862, 77F); // DEBUG (Fort St. John)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(50.248307, -119.348325, 77F); // DEBUG (Vernon Regional)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.155939, -123.965865, 77F); // DEBUG (Nanaimo)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.028428, -122.284984, 77F); // DEBUG (Abbotsford)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.940752, -119.396298, 77F); // DEBUG (Kelowna)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(50.696690, -120.377338, 77F); // DEBUG (Kamloops)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(53.892376, -122.813684, 77F); // DEBUG (Prince George)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.671418, -124.927355, 77F); // DEBUG (Comox)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.101089, -121.972742, 77F); // DEBUG (Chilliwack)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(50.022539, -125.243029, 77F); // DEBUG (Campbell River)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.513780, -115.764318, 77F); // DEBUG (Cranbrook)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(49.311674, -117.652269, 77F); // DEBUG (West Kootenay - Selkirk College, Castlegar)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(48.784189, -123.706154, 77F); // DEBUG (Cowichan Valley, Duncan)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(60.720503, -135.052861, 77F); // DEBUG (Whitehorse)
		//
		// US
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(61.191010, -149.816301, 77F); // DEBUG (AK Anchorage)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(58.384999, -134.640329, 77F); // DEBUG (AK Juneau)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(47.921233, -122.279350, 77F); // DEBUG (WA Snohomish County Community Transit)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(45.626552, -122.674932, 77F); // DEBUG (WA Clark County C-TRAN)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(46.143465, -122.937214, 77F); // DEBUG (WA Longview RiverCities)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(47.921233, -122.279350, 77F); // DEBUG (WA Everett Transit)
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(48.462735, -123.009693, 77F); // DEBUG (WA Washington State Ferries)
		//
		// OTHER
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(0.0, 0.0); // DEBUG
		// location = org.mtransit.android.commons.LocationUtils.getNewLocation(1.0, 1.0); // DEBUG
		// location = null; // DEBUG (no location)
		return location;
	}
}
