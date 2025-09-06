package org.mtransit.android.ui.map
//
//import androidx.lifecycle.SavedStateHandle
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.LatLngBounds
//import org.junit.Assert
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.mock
//import org.mtransit.android.common.repository.LocalPreferenceRepository
//import org.mtransit.android.datasource.DataSourcesRepository
//import org.mtransit.android.datasource.POIRepository
//import org.mtransit.commons.CommonsApp
//
//class MapViewModelTest {
//
// private val savedStateHandle: SavedStateHandle = mock()
//
// private val dataSourcesRepository: DataSourcesRepository = mock()
//
// private val poiRepository: POIRepository = mock()
//
// private val lclPrefRepository: LocalPreferenceRepository = mock()
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