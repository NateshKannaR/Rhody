package com.roadpulse.app.ui.map

object MapConfig {
    /**
     * Default map style URL.
     * Uses OpenFreeMap Liberty - an open-source, free vector map tile service
     * that does not require an API key or account registration.
     *
     * Alternative providers (e.g. MapTiler, Stadia) can be configured here:
     * e.g. "https://api.maptiler.com/maps/streets-v2/style.json?key=YOUR_KEY"
     */
    const val DEFAULT_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    const val FALLBACK_STYLE_URL = "https://demotiles.maplibre.org/style.json"
}
