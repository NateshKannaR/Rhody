package com.roadpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.ui.map.MapConfig
import com.roadpulse.app.ui.theme.CardBorder
import com.roadpulse.app.ui.theme.ElectricBlue
import com.roadpulse.app.ui.theme.MidnightCard
import com.roadpulse.app.ui.theme.NeonCyan
import com.roadpulse.app.ui.theme.TextPrimary
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun RouteMapView(
    points: List<TrackPointEntity>,
    currentLat: Double?,
    currentLon: Double?,
    isLiveTracking: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentPolyline by remember { mutableStateOf<Polyline?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var startMarker by remember { mutableStateOf<Marker?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            addOnDidFailLoadingMapListener {
                mapInstance?.setStyle(MapConfig.FALLBACK_STYLE_URL) {
                    isMapLoaded = true
                }
            }
            getMapAsync { map ->
                mapInstance = map
                map.setStyle(MapConfig.DEFAULT_STYLE_URL) {
                    isMapLoaded = true
                    hasError = false
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Update Polyline and Markers when points or current location change
    LaunchedEffect(points, currentLat, currentLon, mapInstance, isMapLoaded) {
        val map = mapInstance ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect

        // Redraw polyline
        currentPolyline?.let { map.removePolyline(it) }
        if (points.size >= 2) {
            val latLngs = points.map { LatLng(it.latitude, it.longitude) }
            val options = PolylineOptions()
                .addAll(latLngs)
                .color(android.graphics.Color.parseColor("#00E5FF"))
                .width(5f)
            currentPolyline = map.addPolyline(options)

            // Add start pin if not yet placed
            if (startMarker == null && points.isNotEmpty()) {
                val firstPt = points.first()
                startMarker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(firstPt.latitude, firstPt.longitude))
                        .title("Trip Start")
                )
            }
        }

        // Update current position marker
        if (currentLat != null && currentLon != null) {
            val currentLatLng = LatLng(currentLat, currentLon)
            if (currentMarker == null) {
                currentMarker = map.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Current Position")
                )
            } else {
                currentMarker?.position = currentLatLng
            }

            if (isLiveTracking) {
                val camera = CameraPosition.Builder()
                    .target(currentLatLng)
                    .zoom(16.0)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 800)
            }
        } else if (!isLiveTracking && points.size >= 2) {
            // Zoom bounds to fit historical route
            val boundsBuilder = LatLngBounds.Builder()
            points.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
            try {
                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 60), 600)
            } catch (ignored: Exception) {
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Recenter / Follow Button
        if (currentLat != null && currentLon != null) {
            FloatingActionButton(
                onClick = {
                    mapInstance?.let { map ->
                        val camera = CameraPosition.Builder()
                            .target(LatLng(currentLat, currentLon))
                            .zoom(16.5)
                            .build()
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 600)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp),
                containerColor = MidnightCard,
                contentColor = NeonCyan,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
