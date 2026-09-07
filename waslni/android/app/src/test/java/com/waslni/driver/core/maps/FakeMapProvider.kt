package com.waslni.driver.core.maps

import com.waslni.driver.core.maps.model.CameraTarget
import com.waslni.driver.core.maps.model.MapMarker
import com.waslni.driver.core.maps.model.MapTapResult
import com.waslni.driver.core.maps.model.MarkerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [MapProvider].
 *
 * Captures the last [setMarkers] / [moveCamera] calls so tests can assert
 * on what would have been rendered. Also lets tests push synthetic tap
 * events via [emitTap].
 *
 * No Android or Mapbox dependencies — pure Kotlin.
 */
class FakeMapProvider : MapProvider {

    var attachCallCount: Int = 0
        private set

    var detachCallCount: Int = 0
        private set

    var lastMarkers: List<MapMarker> = emptyList()
        private set

    var lastCameraTarget: CameraTarget? = null
        private set

    var setMarkersCallCount: Int = 0
        private set

    var moveCameraCallCount: Int = 0
        private set

    private val _isReady = MutableStateFlow(false)
    private val _tapEvents = MutableSharedFlow<MapTapResult>(replay = 0, extraBufferCapacity = 10)

    fun setReady(value: Boolean) {
        _isReady.value = value
    }

    suspend fun emitTap(result: MapTapResult) {
        _tapEvents.emit(result)
    }

    override suspend fun attach() {
        attachCallCount++
    }

    override fun detach() {
        detachCallCount++
    }

    override fun setMarkers(markers: List<MapMarker>) {
        lastMarkers = markers
        setMarkersCallCount++
    }

    override fun moveCamera(target: CameraTarget) {
        lastCameraTarget = target
        moveCameraCallCount++
    }

    override fun observeTaps(): Flow<MapTapResult> = _tapEvents.asSharedFlow()

    override fun isReady() = _isReady.asStateFlow()
}
