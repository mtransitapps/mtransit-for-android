package org.mtransit.android.ui.map
//
//import androidx.lifecycle.SavedStateHandle
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.LatLngBounds
//import org.junit.Assert
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.mockito.Mock
//import org.mockito.junit.MockitoJUnitRunner
//import org.mtransit.android.common.repository.LocalPreferenceRepository
//import org.mtransit.android.datasource.DataSourcesRepository
//import org.mtransit.android.datasource.POIRepository
//import org.mtransit.commons.CommonsApp
//
//@RunWith(MockitoJUnitRunner::class)
//class MapViewModelTest {
//
// @Mock
// private lateinit var savedStateHandle: SavedStateHandle
//
// @Mock
// private lateinit var dataSourcesRepository: DataSourcesRepository
//
// @Mock
// private lateinit var poiRepository: POIRepository
//
// @Mock
// private lateinit var lclPrefRepository: LocalPreferenceRepository
//
// @Before
// fun setUp() {
// CommonsApp.setup(false)
// }
//
// @Test
// fun test_onCameraChange() {
// // Arrange
// val mapViewModel = MapViewModel(this.savedStateHandle, this.dataSourcesRepository, this.poiRepository, this.lclPrefRepository)
// val newVisibleArea = LatLngBounds(LatLng(-1.0, -1.0), LatLng(+1.0, +1.0))
// // Act
// val result = mapViewModel.onCameraChange(newVisibleArea, getBigCameraPosition = {
// null
// })
// // Assert
// Assert.assertEquals(true, result)
// }
//}