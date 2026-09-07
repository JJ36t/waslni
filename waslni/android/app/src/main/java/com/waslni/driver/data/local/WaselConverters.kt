package com.waslni.driver.data.local

import androidx.room.TypeConverter

/**
 * Room type converters.
 *
 * Currently we don't actually need any — all entity fields are primitives
 * or Strings. This class is declared and wired so that future additions
 * (e.g. storing a List<LatLng> as JSON for a route snapshot) are trivial.
 *
 * When you add a converter, also register it in [WaselDatabase] via
 * @TypeConverters(WaselConverters::class).
 */
class WaselConverters {

    // === Future use ===
    // Example: store List<LatLng> as JSON
    //
    // @TypeConverter
    // fun latLngListToJson(value: List<LatLng>): String =
    //     Json.encodeToString(value)
    //
    // @TypeConverter
    // fun jsonToLatLngList(value: String): List<LatLng> =
    //     Json.decodeFromString(value)
}
