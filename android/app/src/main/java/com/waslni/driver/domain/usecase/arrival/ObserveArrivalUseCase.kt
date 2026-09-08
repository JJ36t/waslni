package com.waslni.driver.domain.usecase.arrival

import com.waslni.driver.core.location.ArrivalDetector
import com.waslni.driver.core.location.ArrivalState
import com.waslni.driver.domain.model.LatLng
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the driver's proximity to the customer's location.
 *
 * Returns a Flow of [ArrivalState] that:
 *   1. Emits the current distance to the customer on every GPS update.
 *   2. Emits `isArrived=true` when the smoothed distance drops below the
 *      arrival radius (default 50m, after at least 2 readings).
 *
 * Call this when the delivery becomes ON_THE_WAY. Cancel when it transitions
 * to ARRIVED (the driver confirmed arrival manually or via auto-detection).
 *
 * The UI uses this to:
 *   - Show "850m away" countdown.
 *   - Auto-suggest "Mark Arrived" when isArrived=true.
 *   - Show "جاري تحديد موقعك…" during warmup (readingsInBuffer < 2).
 */
class ObserveArrivalUseCase @Inject constructor(
    private val arrivalDetector: ArrivalDetector
) {
    operator fun invoke(
        customerLocation: LatLng,
        arrivalRadiusMeters: Double = ArrivalDetector.DEFAULT_ARRIVAL_RADIUS_METERS
    ): Flow<ArrivalState> =
        arrivalDetector.observeArrival(customerLocation, arrivalRadiusMeters)
}
