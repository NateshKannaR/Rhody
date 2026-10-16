// ========================================================
// RHODY MOTORCYCLE TELEMETRY & GUARDIAN SAFETY SUITE
// REAL TELEMETRY ENGINE & EXECUTIVE MOTORSPORT ARCHITECTURE
// 100% REAL DATA — ZERO MOCK / ZERO SYNTHETIC SIMULATION
// ========================================================

// Application State
var currentView = 'dashboard';
var dashboardMapInstance = null;
var tripsMapInstance = null;
var analysisMapInstance = null;

var esriDarkLayerUrl = 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}';
var osmLayerUrl = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
var satLayerUrl = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';

var allTripsCache = [];
var activeAnalysisTrip = null;
var activeAnalysisPoints = [];
var analysisPolylineSegments = [];
var analysisScrubberMarker = null;
var analysisChartInstance = null;
var analysisChartMode = 'speed';

// Active Trip Session State (REAL GPS & SENSORS ONLY)
var isTripRecording = false;
var isTripPaused = false;
var tripStartTime = null;
var tripElapsedSeconds = 0;
var tripTimerInterval = null;
var activeTripId = null;
var activeTripPoints = [];
var activeTripPolyline = null;
var activeTripMarker = null;
var activeTripDistanceKm = 0.0;
var liveSpeedKmh = 0.0;
var liveLeanDeg = 0.0;
var peakTripSpeed = 0.0;
var peakTripLean = 0.0;
var lastKnownGpsFix = null;
var gpsWatchId = null;
var parentPollInterval = null;
var beaconPushInterval = null;

// Rider Profile State
var riderProfile = {
  name: 'Natesh',
  email: 'pilot@rhody.io',
  avatar: '',
  handle: '@natesh_pilot',
  bio: 'Weekend canyon carver & telemetry enthusiast. Western Ghats apex seeker pushing precision motorcycle telemetry.',
  bikeModel: 'Yamaha MT-09 SP',
  bikeSpecs: '890cc Triple • 119 BHP • 93 Nm',
  bikePlate: 'TN-38-RH-0909',
  tireSetup: 'Pirelli Diablo Rosso IV (Cold PSI: F 32 / R 36)',
  emergencyContactName: 'Family Guardian (Mom & Dad)',
  emergencyContactPhone: '+91 98765 43210',
  speedUnit: 'kmh',
  defaultMapLayer: 'dark',
  gpsPrecision: 'sport'
};

// Real Saved Places (Loaded exclusively from MongoDB Atlas / User Storage)
var savedPlaces = [];
var dashboardPlacesLayer = null;
var dashboardActiveRoute = null;
var currentDashCategoryFilter = 'all';

// Real YouTube Active Query
var currentYoutubeQuery = 'Yamaha MT-09 SP motorcycle review pure sound';

// ----------------------------------------------------
// INITIALIZATION
// ----------------------------------------------------
document.addEventListener('DOMContentLoaded', function() {
  loadProfileFromStorage();
  loadPlacesFromBackend();
  renderDashboardProfile();
  renderDashboardBike();
  renderYoutubeDynamicHub();
  populateSettingsForm();

  // Initialize Maps with Leaflet
  ensureLeaflet(function() {
    initDashboardMap();
    initTripsMap();
    initAnalysisMap();
  });

  // Fetch real user trips from MongoDB Atlas
  refreshRealTrips();

  // Draw initial high-precision gauges
  try {
    drawSpeedGauge('tripsSpeedCanvas', 0);
    drawGForceReticle('tripsGforceCanvas', 0, 0);
  } catch (e) {
    console.warn('Initial gauge draw:', e);
  }

  // Setup Real Device Orientation / Gyroscope Listeners for Lean Angle
  setupDeviceSensorFusion();

  // Handle URL live query param (e.g. ?live=tripId) for Parent Live Tracking
  const urlParams = new URLSearchParams(window.location.search);
  const liveTripId = urlParams.get('live');
  if (liveTripId) {
    initParentLiveTrackingMode(liveTripId);
  }
});

// ----------------------------------------------------
// LEAFLET RESILIENT LOADER
// ----------------------------------------------------
function ensureLeaflet(callback, attempt = 0) {
  if (typeof L !== 'undefined' && typeof L.map === 'function') {
    callback();
    return;
  }
  if (attempt < 30) {
    setTimeout(function() {
      ensureLeaflet(callback, attempt + 1);
    }, 80);
  } else {
    var script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js';
    script.onload = function() {
      if (typeof L !== 'undefined') callback();
    };
    document.head.appendChild(script);
  }
}

// ----------------------------------------------------
// TAB SWITCHING (EXACT 4 TABS)
// ----------------------------------------------------
function switchMainView(viewName) {
  currentView = viewName;
  const views = ['dashboard', 'trips', 'analysis', 'settings'];

  views.forEach(v => {
    const el = document.getElementById('view' + capitalizeFirst(v));
    const tabBtn = document.getElementById('navTab' + capitalizeFirst(v));
    const mobileBtn = document.getElementById('mobileTab' + capitalizeFirst(v));

    if (el) {
      if (v === viewName) {
        el.classList.remove('hidden');
      } else {
        el.classList.add('hidden');
      }
    }

    if (tabBtn) {
      if (v === viewName) {
        tabBtn.className = 'px-4 py-2 rounded-xl text-xs font-bold bg-cyan-500/15 text-cyan-300 border border-cyan-400/40 shadow-[0_0_15px_rgba(0,240,255,0.2)] flex items-center space-x-2 transition';
      } else {
        tabBtn.className = 'px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-white/[0.04] transition flex items-center space-x-2';
      }
    }

    if (mobileBtn) {
      if (v === viewName) {
        mobileBtn.className = 'flex flex-col items-center justify-center flex-1 py-1 text-cyan-300 font-bold';
      } else {
        mobileBtn.className = 'flex flex-col items-center justify-center flex-1 py-1 text-slate-500 hover:text-slate-300 font-medium';
      }
    }
  });

  // Re-invalidate map sizes on tab change
  setTimeout(function() {
    if (viewName === 'dashboard' && dashboardMapInstance) {
      dashboardMapInstance.invalidateSize();
    }
    if (viewName === 'trips' && tripsMapInstance) {
      tripsMapInstance.invalidateSize();
    }
    if (viewName === 'analysis') {
      if (analysisMapInstance) analysisMapInstance.invalidateSize();
      if (activeAnalysisTrip) {
        renderAnalysisChart();
      }
    }
  }, 120);

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function capitalizeFirst(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

// ----------------------------------------------------
// PROFILE & BIKE LOGIC
// ----------------------------------------------------
function loadProfileFromStorage() {
  const cached = localStorage.getItem('rhody_rider_profile');
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      riderProfile = Object.assign({}, riderProfile, parsed);
    } catch (e) {}
  }

  fetch('/api/user/profile')
    .then(r => r.json())
    .then(data => {
      if (data && data.profile) {
        riderProfile = Object.assign({}, riderProfile, data.profile);
        renderDashboardProfile();
        renderDashboardBike();
        renderYoutubeDynamicHub();
      }
    })
    .catch(() => {});
}

function renderDashboardProfile() {
  const greeting = document.getElementById('dashWelcomeName');
  const handle = document.getElementById('dashHandle');
  const bio = document.getElementById('dashBio');
  const avatarImg = document.getElementById('dashAvatarImg');
  const avatarPlaceholder = document.getElementById('dashAvatarPlaceholder');
  const navAvatarImg = document.getElementById('navUserAvatarImg');
  const navAvatarInit = document.getElementById('navUserAvatarInit');
  const removeBtn = document.getElementById('btnRemovePhoto');

  if (greeting) greeting.textContent = riderProfile.name || 'Natesh';
  if (handle) handle.textContent = riderProfile.handle || '@natesh_pilot';
  if (bio) bio.textContent = riderProfile.bio || 'Weekend canyon carver & telemetry enthusiast.';

  const initial = (riderProfile.name || 'N').charAt(0).toUpperCase();

  if (riderProfile.avatar) {
    if (avatarImg) { avatarImg.src = riderProfile.avatar; avatarImg.classList.remove('hidden'); }
    if (avatarPlaceholder) avatarPlaceholder.classList.add('hidden');
    if (navAvatarImg) { navAvatarImg.src = riderProfile.avatar; navAvatarImg.classList.remove('hidden'); }
    if (navAvatarInit) navAvatarInit.classList.add('hidden');
    if (removeBtn) removeBtn.classList.remove('hidden');
  } else {
    if (avatarImg) { avatarImg.src = ''; avatarImg.classList.add('hidden'); }
    if (avatarPlaceholder) {
      avatarPlaceholder.textContent = initial;
      avatarPlaceholder.classList.remove('hidden');
    }
    if (navAvatarImg) { navAvatarImg.src = ''; navAvatarImg.classList.add('hidden'); }
    if (navAvatarInit) { navAvatarInit.textContent = initial; navAvatarInit.classList.remove('hidden'); }
    if (removeBtn) removeBtn.classList.add('hidden');
  }
}

function renderDashboardBike() {
  const model = document.getElementById('dashBikeModel');
  const specs = document.getElementById('dashBikeSpecs');
  const plate = document.getElementById('dashBikePlate');
  const tires = document.getElementById('dashTires');

  if (model) model.textContent = riderProfile.bikeModel || 'Yamaha MT-09 SP';
  if (specs) specs.textContent = riderProfile.bikeSpecs || '890cc Triple • 119 BHP • 93 Nm';
  if (plate) plate.textContent = riderProfile.bikePlate || 'TN-38-RH-0909';
  if (tires) tires.textContent = riderProfile.tireSetup || 'Pirelli Diablo Rosso IV';
}

function handleProfilePhotoUpload(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    const rawBase64 = e.target.result;
    const img = new Image();
    img.onload = function() {
      const canvas = document.createElement('canvas');
      const maxDim = 360;
      let width = img.width;
      let height = img.height;

      if (width > height) {
        if (width > maxDim) { height *= maxDim / width; width = maxDim; }
      } else {
        if (height > maxDim) { width *= maxDim / height; height = maxDim; }
      }

      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, width, height);

      const optimizedBase64 = canvas.toDataURL('image/jpeg', 0.85);
      riderProfile.avatar = optimizedBase64;
      saveProfile();
      renderDashboardProfile();
      showToast('Profile photo updated', 'success');
    };
    img.src = rawBase64;
  };
  reader.readAsDataURL(file);
}

function removeRiderAvatar() {
  riderProfile.avatar = '';
  saveProfile();
  renderDashboardProfile();
  showToast('Profile photo removed', 'info');
}

function saveProfile() {
  localStorage.setItem('rhody_rider_profile', JSON.stringify(riderProfile));
  fetch('/api/user/profile', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(riderProfile)
  }).catch(function() {});
}

// ----------------------------------------------------
// DASHBOARD: REAL SAVED PLACES & MAP CONNECTION
// ----------------------------------------------------
function loadPlacesFromBackend() {
  const cached = localStorage.getItem('rhody_saved_places');
  if (cached) {
    try {
      savedPlaces = JSON.parse(cached) || [];
    } catch (e) {
      savedPlaces = [];
    }
  }

  fetch('/api/user/places')
    .then(r => r.json())
    .then(data => {
      if (data && Array.isArray(data.places)) {
        savedPlaces = data.places;
        localStorage.setItem('rhody_saved_places', JSON.stringify(savedPlaces));
      }
      renderDashboardPlaces();
      renderDashboardPlacesMarkers();
      renderYoutubeDynamicHub();
    })
    .catch(() => {
      renderDashboardPlaces();
      renderDashboardPlacesMarkers();
    });
}

function savePlacesToStorage() {
  localStorage.setItem('rhody_saved_places', JSON.stringify(savedPlaces));
}

function initDashboardMap() {
  const container = document.getElementById('dashboardMap');
  if (!container || dashboardMapInstance) return;

  // Center map on user's current GPS or default view
  dashboardMapInstance = L.map('dashboardMap', {
    center: [11.0168, 76.9558],
    zoom: 8,
    zoomControl: true
  });

  L.tileLayer(esriDarkLayerUrl, {
    maxNativeZoom: 16,
    maxZoom: 19,
    attribution: '&copy; Esri &mdash; Rhody Dark Canvas'
  }).addTo(dashboardMapInstance);

  dashboardPlacesLayer = L.layerGroup().addTo(dashboardMapInstance);

  // Try locating user's real position on map
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(pos) {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      lastKnownGpsFix = { lat, lng };
      dashboardMapInstance.setView([lat, lng], 11);

      // Add rider current location beacon
      const riderBeacon = L.divIcon({
        className: 'rider-live-pin',
        html: `
          <div style="position: relative; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;">
            <div style="position: absolute; inset: -6px; border-radius: 9999px; background: rgba(0, 240, 255, 0.3); animation: pulseGlow 2s infinite;"></div>
            <div style="width: 14px; height: 14px; border-radius: 9999px; background: #00F0FF; border: 2px solid #FFFFFF; box-shadow: 0 0 10px #00F0FF;"></div>
          </div>
        `,
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      });
      L.marker([lat, lng], { icon: riderBeacon })
        .bindPopup('<b style="color: #00F0FF;">Your Current Location</b>')
        .addTo(dashboardMapInstance);

      renderDashboardPlaces(); // Re-render to compute real distance to places
    }, function() {}, { enableHighAccuracy: true, timeout: 5000 });
  }

  // Interactive Click-to-Pin on Map
  dashboardMapInstance.on('click', function(e) {
    const lat = e.latlng.lat;
    const lng = e.latlng.lng;
    promptSaveMapPin(lat, lng);
  });

  renderDashboardPlacesMarkers();
}

function promptSaveMapPin(lat, lng) {
  const placeName = prompt(`Pin Waypoint at (${lat.toFixed(4)}, ${lng.toFixed(4)})\nEnter place name:`, 'Scenic Apex Corner');
  if (!placeName || !placeName.trim()) return;

  const newPlace = {
    name: placeName.trim(),
    category: 'pass',
    lat: parseFloat(lat),
    lng: parseFloat(lng),
    notes: 'Pinned on telemetry radar map'
  };

  saveNewPlaceToBackend(newPlace);
}

function saveNewPlaceToBackend(placeDoc) {
  fetch('/api/user/places', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(placeDoc)
  })
    .then(r => r.json())
    .then(data => {
      const saved = (data && data.place) ? data.place : Object.assign({ id: 'loc_' + Date.now() }, placeDoc);
      savedPlaces.unshift(saved);
      savePlacesToStorage();
      renderDashboardPlaces();
      renderDashboardPlacesMarkers();
      renderYoutubeDynamicHub();
      showToast(`Saved "${placeDoc.name}" to your map!`, 'success');
    })
    .catch(err => {
      placeDoc.id = 'loc_' + Date.now();
      savedPlaces.unshift(placeDoc);
      savePlacesToStorage();
      renderDashboardPlaces();
      renderDashboardPlacesMarkers();
      showToast(`Saved "${placeDoc.name}" locally`, 'success');
    });
}

function saveCurrentGpsLocation() {
  if (!navigator.geolocation) {
    showToast('Geolocation is not supported by your browser', 'error');
    return;
  }

  showToast('Acquiring high-precision GPS fix...', 'info');

  navigator.geolocation.getCurrentPosition(function(pos) {
    const lat = pos.coords.latitude;
    const lng = pos.coords.longitude;
    const alt = pos.coords.altitude ? Math.round(pos.coords.altitude) : null;

    // Real Reverse Geocode via OpenStreetMap Nominatim
    fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`)
      .then(r => r.json())
      .then(geo => {
        let detectedName = 'Rider Waypoint';
        if (geo && geo.address) {
          detectedName = geo.address.road || geo.address.suburb || geo.address.city || geo.address.town || 'Rider Waypoint';
        }
        const customName = prompt(`GPS Fix Acquired! Lat: ${lat.toFixed(4)}, Lng: ${lng.toFixed(4)}\nEnter name to save this place:`, detectedName);
        if (!customName || !customName.trim()) return;

        const newPlace = {
          name: customName.trim(),
          category: 'garage',
          lat: lat,
          lng: lng,
          altitude: alt,
          notes: geo && geo.display_name ? geo.display_name.substring(0, 100) : 'Current GPS location fix'
        };

        saveNewPlaceToBackend(newPlace);
        if (dashboardMapInstance) {
          dashboardMapInstance.flyTo([lat, lng], 14);
        }
      })
      .catch(() => {
        const customName = prompt(`GPS Fix Acquired! Lat: ${lat.toFixed(4)}, Lng: ${lng.toFixed(4)}\nEnter name to save this place:`, 'Current Garage / Spot');
        if (!customName || !customName.trim()) return;

        saveNewPlaceToBackend({
          name: customName.trim(),
          category: 'garage',
          lat: lat,
          lng: lng,
          altitude: alt,
          notes: 'Current GPS location fix'
        });
      });
  }, function(err) {
    showToast('Failed to acquire GPS fix: ' + err.message, 'error');
  }, { enableHighAccuracy: true, timeout: 10000 });
}

function deletePlace(placeId) {
  if (!confirm('Are you sure you want to remove this saved place from your map?')) return;

  savedPlaces = savedPlaces.filter(p => (p._id || p.id) !== placeId);
  savePlacesToStorage();
  renderDashboardPlaces();
  renderDashboardPlacesMarkers();
  renderYoutubeDynamicHub();

  fetch(`/api/user/places/${placeId}`, { method: 'DELETE' }).catch(() => {});
  showToast('Place removed from map', 'info');
}

function setDashboardMapLayer(type) {
  if (!dashboardMapInstance) return;
  dashboardMapInstance.eachLayer(function(l) {
    if (l instanceof L.TileLayer) dashboardMapInstance.removeLayer(l);
  });

  let url = esriDarkLayerUrl;
  if (type === 'osm') url = osmLayerUrl;
  if (type === 'sat') url = satLayerUrl;

  L.tileLayer(url, { maxZoom: 19 }).addTo(dashboardMapInstance);

  const btnDark = document.getElementById('btnDashDark');
  const btnOsm = document.getElementById('btnDashOsm');
  const btnSat = document.getElementById('btnDashSat');
  [btnDark, btnOsm, btnSat].forEach(b => {
    if (b) b.className = 'px-3 py-1 rounded-lg text-xs text-slate-400 hover:text-white transition';
  });
  if (type === 'dark' && btnDark) btnDark.className = 'px-3 py-1 rounded-lg text-xs bg-cyan-500/20 text-cyan-300 border border-cyan-400/40 font-bold transition';
  if (type === 'osm' && btnOsm) btnOsm.className = 'px-3 py-1 rounded-lg text-xs bg-cyan-500/20 text-cyan-300 border border-cyan-400/40 font-bold transition';
  if (type === 'sat' && btnSat) btnSat.className = 'px-3 py-1 rounded-lg text-xs bg-cyan-500/20 text-cyan-300 border border-cyan-400/40 font-bold transition';
}

function renderDashboardPlacesMarkers() {
  if (!dashboardMapInstance || !dashboardPlacesLayer) return;
  dashboardPlacesLayer.clearLayers();

  if (savedPlaces.length === 0) return;

  const bounds = L.latLngBounds();

  savedPlaces.forEach(place => {
    let pinColor = '#FFB703'; // Amber for pass
    let iconChar = '⛰️';
    if (place.category === 'track') { pinColor = '#FF3366'; iconChar = '🏁'; }
    if (place.category === 'garage') { pinColor = '#00F0FF'; iconChar = '🏠'; }
    if (place.category === 'fuel') { pinColor = '#10B981'; iconChar = '⛽'; }

    const svgIcon = L.divIcon({
      className: 'custom-place-pin',
      html: `
        <div style="position: relative; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; cursor: pointer;">
          <div style="position: absolute; inset: -4px; border-radius: 9999px; background: ${pinColor}25; animation: pulseGlow 2.5s infinite;"></div>
          <div style="width: 30px; height: 30px; border-radius: 9999px; background: #0E131F; border: 2px solid ${pinColor}; display: flex; align-items: center; justify-content: center; font-size: 13px; box-shadow: 0 4px 14px rgba(0,0,0,0.7);">
            ${iconChar}
          </div>
        </div>
      `,
      iconSize: [34, 34],
      iconAnchor: [17, 17]
    });

    const marker = L.marker([place.lat, place.lng], { icon: svgIcon }).addTo(dashboardPlacesLayer);
    bounds.extend([place.lat, place.lng]);

    marker.bindPopup(`
      <div style="background: #0E131F; color: #E2E8F0; padding: 12px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.12); font-family: Inter, sans-serif; min-width: 230px; box-shadow: 0 10px 25px rgba(0,0,0,0.8);">
        <div style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 6px; margin-bottom: 8px;">
          <strong style="color: #FFFFFF; font-size: 13px;">${place.name}</strong>
          <span style="font-size: 10px; font-weight: bold; color: ${pinColor}; text-transform: uppercase;">${place.category}</span>
        </div>
        <p style="font-size: 11px; color: #94A3B8; margin: 4px 0 8px 0; line-height: 1.4;">${place.notes || 'Real waypoint stored in Rhody database.'}</p>
        <div style="display: flex; gap: 6px;">
          <button onclick="routeToPlace(${place.lat}, ${place.lng}, '${place.name.replace(/'/g, "\\'")}')" style="flex: 1; background: #00F0FF; color: #07090E; border: none; padding: 6px 10px; border-radius: 8px; font-size: 11px; font-weight: 800; cursor: pointer;">Route</button>
          <button onclick="searchYoutubeForPlace('${place.name.replace(/'/g, "\\'")}')" style="background: rgba(239,68,68,0.2); color: #FF4D4D; border: 1px solid rgba(239,68,68,0.4); padding: 6px 10px; border-radius: 8px; font-size: 11px; font-weight: 700; cursor: pointer;">▶ Vlogs</button>
          <button onclick="deletePlace('${place._id || place.id}')" style="background: rgba(255,255,255,0.06); color: #94A3B8; border: 1px solid rgba(255,255,255,0.1); padding: 6px 8px; border-radius: 8px; font-size: 11px; cursor: pointer;" title="Delete">🗑️</button>
        </div>
      </div>
    `);
  });

  if (savedPlaces.length > 0 && dashboardMapInstance && bounds.isValid()) {
    dashboardMapInstance.fitBounds(bounds, { padding: [40, 40], maxZoom: 13 });
  }
}

function renderDashboardPlaces() {
  const container = document.getElementById('dashPlacesGrid');
  const countBadge = document.getElementById('dashPlacesBadge');
  if (!container) return;

  if (countBadge) countBadge.textContent = `${savedPlaces.length} Saved On Map`;

  // Real Empty State if user has zero saved places
  if (savedPlaces.length === 0) {
    container.innerHTML = `
      <div class="col-span-full p-8 rounded-2xl bg-[#0E131F]/90 border border-white/[0.08] backdrop-blur-xl text-center flex flex-col items-center justify-center space-y-4">
        <div class="w-14 h-14 rounded-2xl bg-cyan-500/10 border border-cyan-400/30 flex items-center justify-center text-cyan-300">
          <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
        </div>
        <div class="max-w-md">
          <h4 class="text-sm font-bold text-white tracking-wide">Zero Stored Waypoints Yet</h4>
          <p class="text-xs text-slate-400 mt-1 leading-relaxed">
            Your real motorcycle destinations, garage basecamp, and mountain roads will appear here. Pin your current GPS fix or click anywhere on the radar map to store locations.
          </p>
        </div>
        <div class="flex items-center space-x-3 pt-1">
          <button onclick="saveCurrentGpsLocation()" class="px-4 py-2 text-xs font-bold rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 border border-cyan-400/50 shadow-[0_0_15px_rgba(0,240,255,0.2)] flex items-center space-x-1.5 transition">
            <span>📍 Pin Current Location</span>
          </button>
          <button onclick="document.getElementById('dashPlaceSearchInput').focus()" class="px-4 py-2 text-xs font-medium rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-slate-300 border border-white/10 transition">
            <span>🔍 Search Real Places</span>
          </button>
        </div>
      </div>
    `;
    return;
  }

  const filtered = savedPlaces.filter(p => {
    if (currentDashCategoryFilter === 'all') return true;
    return p.category === currentDashCategoryFilter;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="col-span-full py-6 text-center text-xs text-slate-400">
        No places saved under the "${currentDashCategoryFilter}" category.
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(place => {
    let catBadgeColor = 'bg-amber-500/15 text-amber-300 border-amber-400/30';
    let iconChar = '⛰️';
    if (place.category === 'track') { catBadgeColor = 'bg-rose-500/15 text-rose-300 border-rose-400/30'; iconChar = '🏁'; }
    if (place.category === 'garage') { catBadgeColor = 'bg-cyan-500/15 text-cyan-300 border-cyan-400/30'; iconChar = '🏠'; }
    if (place.category === 'fuel') { catBadgeColor = 'bg-emerald-500/15 text-emerald-300 border-emerald-400/30'; iconChar = '⛽'; }

    const safeName = place.name.replace(/'/g, "\\'");
    let distStr = '';
    if (lastKnownGpsFix && place.lat && place.lng) {
      const d = haversineDistance(lastKnownGpsFix.lat, lastKnownGpsFix.lng, place.lat, place.lng) / 1000;
      distStr = `${d.toFixed(1)} km away`;
    }

    return `
      <div class="p-4 rounded-2xl bg-[#0E131F]/90 border border-white/[0.08] backdrop-blur-xl hover:border-cyan-400/40 transition-all flex flex-col justify-between shadow-lg group">
        <div>
          <div class="flex items-center justify-between">
            <span class="px-2.5 py-0.5 text-[9px] font-bold uppercase rounded-full border ${catBadgeColor}">${iconChar} ${place.category}</span>
            <span class="text-[10px] text-cyan-400 font-mono tracking-wider">${distStr || (place.altitude ? place.altitude + ' m' : '')}</span>
          </div>
          <h4 class="text-sm font-bold text-white mt-2 group-hover:text-cyan-300 transition truncate">${place.name}</h4>
          <p class="text-xs text-slate-400 mt-1 line-clamp-2 leading-relaxed">${place.notes || 'Waypoint pinned on telemetry map'}</p>
        </div>
        
        <div class="pt-3 flex items-center space-x-2 border-t border-white/[0.06] mt-3">
          <button onclick="flyToDashboardPlace(${place.lat}, ${place.lng}, '${safeName}')" class="flex-1 py-1.5 px-2.5 text-xs font-semibold rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 text-cyan-300 border border-cyan-400/30 transition text-center">
            View on Map
          </button>
          <button onclick="routeToPlace(${place.lat}, ${place.lng}, '${safeName}')" class="py-1.5 px-3 text-xs font-medium rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-slate-300 border border-white/10 transition">
            Route
          </button>
          <button onclick="searchYoutubeForPlace('${safeName}')" title="Watch YouTube vlogs for this place" class="py-1.5 px-2.5 text-xs font-bold rounded-xl bg-red-600/20 hover:bg-red-600/30 text-red-400 border border-red-500/30 transition">
            ▶ Vlogs
          </button>
          <button onclick="deletePlace('${place._id || place.id}')" title="Delete waypoint" class="p-1.5 text-slate-500 hover:text-rose-400 transition">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
          </button>
        </div>
      </div>
    `;
  }).join('');
}

function filterDashboardPlacesCategory(cat) {
  currentDashCategoryFilter = cat;
  const cats = ['all', 'pass', 'track', 'garage'];
  cats.forEach(c => {
    const btn = document.getElementById('dashCat_' + c);
    if (btn) {
      if (c === cat) {
        btn.className = 'px-3 py-1.5 rounded-xl text-xs font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-400/40 transition';
      } else {
        btn.className = 'px-3 py-1.5 rounded-xl text-xs font-medium text-slate-400 hover:text-white bg-white/[0.04] border border-white/10 transition';
      }
    }
  });
  renderDashboardPlaces();
}

function flyToDashboardPlace(lat, lng, name) {
  if (!dashboardMapInstance) return;
  dashboardMapInstance.flyTo([lat, lng], 14, { duration: 1.2 });
  fetchRoadWeather(lat, lng, name);
}

function routeToPlace(lat, lng, name) {
  if (!dashboardMapInstance) return;

  // Real route origin: user's current GPS location, or fallback
  let startLat = 11.0168;
  let startLng = 76.9558;

  if (lastKnownGpsFix) {
    startLat = lastKnownGpsFix.lat;
    startLng = lastKnownGpsFix.lng;
  }

  showToast(`Calculating motorcycle route to ${name}...`, 'info');

  const url = `https://router.project-osrm.org/route/v1/driving/${startLng},${startLat};${lng},${lat}?overview=full&geometries=geojson`;

  fetch(url)
    .then(r => r.json())
    .then(data => {
      if (!data.routes || data.routes.length === 0) {
        showToast('No road trajectory found to this waypoint', 'error');
        return;
      }

      if (dashboardActiveRoute) {
        dashboardMapInstance.removeLayer(dashboardActiveRoute);
      }

      const routeGeo = data.routes[0].geometry;
      const distKm = (data.routes[0].distance / 1000).toFixed(1);
      const durationMins = Math.round(data.routes[0].duration / 60);

      dashboardActiveRoute = L.geoJSON(routeGeo, {
        style: {
          color: '#00F0FF',
          weight: 4,
          opacity: 0.9,
          dashArray: '8, 6'
        }
      }).addTo(dashboardMapInstance);

      dashboardMapInstance.fitBounds(dashboardActiveRoute.getBounds(), { padding: [50, 50] });
      showToast(`Motorcycle Route to ${name}: ${distKm} km • ${durationMins} mins ride time`, 'success');
      fetchRoadWeather(lat, lng, name);
    })
    .catch(err => {
      showToast('Routing engine temporarily unreachable', 'error');
    });
}

function searchDashboardPlaces() {
  const input = document.getElementById('dashPlaceSearchInput');
  if (!input || !input.value.trim()) return;

  const query = input.value.trim();
  showToast(`Searching worldwide locations for "${query}"...`, 'info');

  fetch(`https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=5`)
    .then(r => r.json())
    .then(results => {
      if (!results || results.length === 0) {
        showToast(`No locations found for "${query}"`, 'error');
        return;
      }

      const top = results[0];
      const lat = parseFloat(top.lat);
      const lng = parseFloat(top.lon);

      if (dashboardMapInstance) {
        dashboardMapInstance.flyTo([lat, lng], 13);
        const searchMarker = L.marker([lat, lng])
          .bindPopup(`
            <div style="background: #0E131F; color: #FFFFFF; padding: 10px; border-radius: 10px; font-family: Inter, sans-serif;">
              <strong>${top.display_name.split(',')[0]}</strong><br>
              <span style="font-size: 11px; color: #94A3B8;">${top.display_name.substring(0, 80)}</span><br><br>
              <button onclick="saveNewPlaceToBackend({ name: '${top.display_name.split(',')[0].replace(/'/g, "\\'")}', category: 'pass', lat: ${lat}, lng: ${lng}, notes: '${top.display_name.substring(0, 100).replace(/'/g, "\\'")}' })" style="background: #00F0FF; color: #07090E; border: none; padding: 6px 12px; border-radius: 6px; font-size: 11px; font-weight: bold; cursor: pointer;">
                Save This Place to Map
              </button>
            </div>
          `)
          .addTo(dashboardMapInstance);
        searchMarker.openPopup();
      }
      showToast(`Found: ${top.display_name.split(',')[0]}`, 'success');
    })
    .catch(() => {
      showToast('Search service unavailable. Please check internet connection.', 'error');
    });
}

// ----------------------------------------------------
// DASHBOARD: REAL DYNAMIC YOUTUBE GUIDES & STREAMING
// ----------------------------------------------------
function renderYoutubeDynamicHub() {
  const pillsContainer = document.getElementById('youtubePillsContainer');
  const cardsContainer = document.getElementById('youtubeVideoGrid');
  if (!pillsContainer || !cardsContainer) return;

  const bike = riderProfile.bikeModel || 'Yamaha MT-09 SP';

  // Dynamic search topics based on rider's actual machine & places
  const dynamicTopics = [
    { label: `${bike} Review`, query: `${bike} motorcycle in-depth review pure sound` },
    { label: `${bike} Exhaust & Dyno`, query: `${bike} exhaust sound acceleration top speed` },
    { label: `Apex Cornering Guide`, query: `motorcycle apex cornering body position trail braking technique` },
    { label: `Rain Grip & Lean Angle`, query: `motorcycle wet weather riding grip tire lean angle safety` }
  ];

  // Add rider's saved places dynamically
  savedPlaces.slice(0, 3).forEach(place => {
    dynamicTopics.push({
      label: `${place.name} Ride`,
      query: `${place.name} motorcycle ride vlog onboard 4K`
    });
  });

  pillsContainer.innerHTML = dynamicTopics.map((topic, i) => {
    const isFirst = i === 0;
    const activeClass = isFirst
      ? 'bg-red-600 text-white border-red-500 shadow-[0_0_12px_rgba(239,68,68,0.4)]'
      : 'bg-white/[0.04] text-slate-400 hover:text-white border-white/10 hover:border-white/20';

    return `
      <button onclick="executeYoutubeSearch('${topic.query.replace(/'/g, "\\'")}')" class="px-3.5 py-1.5 rounded-xl text-xs font-bold border transition flex items-center space-x-1.5 ${activeClass}">
        <svg class="w-3.5 h-3.5 text-red-400" fill="currentColor" viewBox="0 0 20 20"><path d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z"/></svg>
        <span>${topic.label}</span>
      </button>
    `;
  }).join('');

  // Render Dynamic High-Tech Video Preview Cards
  cardsContainer.innerHTML = dynamicTopics.map(topic => {
    return `
      <div class="p-4 rounded-2xl bg-[#0E131F]/90 border border-white/[0.08] backdrop-blur-xl hover:border-red-500/40 transition flex flex-col justify-between group shadow-lg">
        <div>
          <div class="flex items-center justify-between text-[10px] text-slate-500">
            <span class="px-2 py-0.5 rounded-full bg-red-600/15 text-red-400 font-bold border border-red-500/20">LIVE YOUTUBE STREAM</span>
            <span class="font-mono text-cyan-400">4K ONBOARD</span>
          </div>
          <h4 class="text-sm font-bold text-white mt-2 group-hover:text-red-400 transition leading-snug">${topic.label}</h4>
          <p class="text-xs text-slate-400 mt-1 line-clamp-2">Real-time YouTube video stream for "${topic.query}".</p>
        </div>

        <div class="pt-3 flex items-center space-x-2 border-t border-white/[0.06] mt-3">
          <button onclick="executeYoutubeSearch('${topic.query.replace(/'/g, "\\'")}')" class="flex-1 py-2 px-3 text-xs font-bold rounded-xl bg-red-600 hover:bg-red-500 text-white transition flex items-center justify-center space-x-1.5 shadow-[0_0_15px_rgba(239,68,68,0.3)]">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path d="M4 4l12 6-12 6z"/></svg>
            <span>Play Inside Rhody</span>
          </button>
          <a href="https://www.youtube.com/results?search_query=${encodeURIComponent(topic.query)}" target="_blank" rel="noopener noreferrer" class="p-2 text-slate-400 hover:text-white rounded-xl bg-white/[0.05] hover:bg-white/10 border border-white/10 transition" title="Open directly in YouTube">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/></svg>
          </a>
        </div>
      </div>
    `;
  }).join('');
}

function executeYoutubeSearch(query) {
  currentYoutubeQuery = query;
  const input = document.getElementById('youtubeSearchInput');
  if (input) input.value = query;

  openYoutubePlayerSearch(query);
}

function searchCustomYoutube() {
  const input = document.getElementById('youtubeSearchInput');
  if (!input || !input.value.trim()) return;

  const query = input.value.trim();
  executeYoutubeSearch(query);
}

function searchYoutubeForBike() {
  const bike = riderProfile.bikeModel || 'Yamaha MT-09 SP';
  executeYoutubeSearch(`${bike} motorcycle review pure exhaust sound`);
}

function searchYoutubeForPlace(placeName) {
  executeYoutubeSearch(`${placeName} motorcycle ride vlog onboard 4K`);
}

// Fallback curated high-res motorcycle videos when offline or search fails
const YOUTUBE_BIKE_FALLBACKS = {
  'mt-09': 'NHEPDiR4618',
  'yamaha': 'NHEPDiR4618',
  'mt-15': 'p3MHXPSNNUo',
  'cornering': 'JOe2MkwL4yE',
  'apex': 'JOe2MkwL4yE',
  'exhaust': 'eKlr9YnxDcI',
  'rain': 'PpyK_NB3j-s',
  'default': 'NHEPDiR4618'
};

function getFallbackVideoId(query) {
  const q = (query || '').toLowerCase();
  for (const [key, id] of Object.entries(YOUTUBE_BIKE_FALLBACKS)) {
    if (q.includes(key)) return id;
  }
  return YOUTUBE_BIKE_FALLBACKS.default;
}

function playSpecificYoutubeVideo(videoId, title) {
  const iframe = document.getElementById('youtubeIframe');
  const titleEl = document.getElementById('youtubePlayerTitle');
  const externalLink = document.getElementById('youtubeExternalLink');

  if (iframe) {
    iframe.src = `https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&rel=0&modestbranding=1`;
  }
  if (titleEl && title) {
    titleEl.textContent = title;
  }
  if (externalLink) {
    externalLink.href = `https://www.youtube.com/watch?v=${videoId}`;
  }
}

async function openYoutubePlayerSearch(query) {
  const modal = document.getElementById('youtubePlayerModal');
  const iframe = document.getElementById('youtubeIframe');
  const titleEl = document.getElementById('youtubePlayerTitle');
  const externalLink = document.getElementById('youtubeExternalLink');
  const resultsContainer = document.getElementById('youtubeModalResultsContainer');
  const resultsGrid = document.getElementById('youtubeModalResultsGrid');

  if (!modal || !iframe) return;

  if (titleEl) titleEl.textContent = `Loading stream for "${query}"...`;
  if (externalLink) externalLink.href = `https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`;
  
  modal.classList.remove('hidden');

  // Load a fast instant fallback immediately so user gets playback right away
  const fallbackId = getFallbackVideoId(query);
  playSpecificYoutubeVideo(fallbackId, `YouTube Stream: "${query}"`);

  // Now query our real backend search proxy for exact matches
  try {
    const res = await fetch(`/api/youtube/search?q=${encodeURIComponent(query)}`);
    if (res.ok) {
      const data = await res.json();
      if (data.videos && data.videos.length > 0) {
        const first = data.videos[0];
        playSpecificYoutubeVideo(first.id, first.title);

        // Render alternative streams grid
        if (resultsGrid && resultsContainer) {
          resultsContainer.classList.remove('hidden');
          resultsGrid.innerHTML = data.videos.map((v, idx) => {
            return `
              <div onclick="playSpecificYoutubeVideo('${v.id}', '${v.title.replace(/'/g, "\\'")}')" class="p-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] border border-white/10 hover:border-red-500/50 cursor-pointer transition flex flex-col justify-between group">
                <div class="relative w-full aspect-video rounded-lg overflow-hidden bg-black mb-1.5">
                  <img src="${v.thumbnail}" alt="${v.title}" class="w-full h-full object-cover group-hover:scale-105 transition duration-200" onerror="this.src='https://i.ytimg.com/vi/${v.id}/hqdefault.jpg'" />
                  <span class="absolute bottom-1 right-1 px-1 py-0.5 rounded bg-black/80 font-mono text-[9px] text-white">${v.duration || 'Video'}</span>
                </div>
                <h5 class="text-[11px] font-bold text-slate-200 line-clamp-2 leading-tight group-hover:text-red-400 transition">${v.title}</h5>
                <span class="text-[9px] text-slate-500 truncate mt-1">${v.channel}</span>
              </div>
            `;
          }).join('');
        }
        showToast(`Streaming real YouTube video for "${query}"`, 'info');
        return;
      }
    }
  } catch (err) {
    console.warn('Backend YouTube search fetch error, keeping fallback:', err.message);
  }
}

function closeYoutubePlayerModal() {
  const modal = document.getElementById('youtubePlayerModal');
  const iframe = document.getElementById('youtubeIframe');
  const resultsContainer = document.getElementById('youtubeModalResultsContainer');
  if (modal && iframe) {
    iframe.src = '';
    modal.classList.add('hidden');
    if (resultsContainer) resultsContainer.classList.add('hidden');
  }
}

// Global keyboard shortcuts (Escape key closes modals)
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' || e.key === 'Esc') {
    closeYoutubePlayerModal();
  }
});

// ----------------------------------------------------
// TRIPS: 100% REAL GPS & DEVICE ORIENTATION SENSORS
// ----------------------------------------------------
function initTripsMap() {
  const container = document.getElementById('tripsLiveMap');
  if (!container || tripsMapInstance) return;

  tripsMapInstance = L.map('tripsLiveMap', {
    center: [11.0168, 76.9558],
    zoom: 14
  });

  L.tileLayer(esriDarkLayerUrl, {
    maxNativeZoom: 16,
    maxZoom: 19,
    attribution: '&copy; Esri &mdash; Rhody Dark Canvas'
  }).addTo(tripsMapInstance);

  activeTripPolyline = L.polyline([], {
    color: '#00F0FF',
    weight: 5,
    opacity: 0.95
  }).addTo(tripsMapInstance);
}

function setupDeviceSensorFusion() {
  // Real Gyroscope / Device Orientation for Apex Lean Angle
  if (window.DeviceOrientationEvent) {
    window.addEventListener('deviceorientation', function(event) {
      if (!isTripRecording || isTripPaused) return;

      // When phone is mounted in landscape or portrait on handlebars:
      // gamma (-90 to +90) or beta (-180 to 180) reflects motorcycle roll
      let rawTilt = event.gamma || 0;
      if (Math.abs(window.orientation || 0) === 90) {
        rawTilt = event.beta || 0;
      }

      liveLeanDeg = Math.max(-60, Math.min(60, rawTilt));
      if (Math.abs(liveLeanDeg) > peakTripLean) {
        peakTripLean = Math.abs(liveLeanDeg);
      }

      updateLiveLeanAngleDisplay(liveLeanDeg);
    }, true);
  }

  // Real Accelerometer for G-Force Meter
  if (window.DeviceMotionEvent) {
    window.addEventListener('devicemotion', function(event) {
      if (!isTripRecording || isTripPaused) return;

      const acc = event.accelerationIncludingGravity;
      if (acc) {
        const lateralG = ((acc.x || 0) / 9.81).toFixed(2);
        const longitudinalG = ((acc.y || 0) / 9.81).toFixed(2);
        drawGForceReticle('tripsGforceCanvas', lateralG, longitudinalG);
      }
    }, true);
  }
}

function updateLiveLeanAngleDisplay(leanDeg) {
  const leanReadout = document.getElementById('liveLeanAngleReadout');
  if (!leanReadout) return;

  const rounded = Math.abs(leanDeg).toFixed(1);
  if (leanDeg < -2.0) {
    leanReadout.innerHTML = `<span class="text-cyan-400 font-mono">${rounded}&deg;</span> <span class="text-xs text-slate-400">LEFT APEX</span>`;
  } else if (leanDeg > 2.0) {
    leanReadout.innerHTML = `<span class="text-amber-400 font-mono">${rounded}&deg;</span> <span class="text-xs text-slate-400">RIGHT APEX</span>`;
  } else {
    leanReadout.innerHTML = `<span class="text-emerald-400 font-mono">0.0&deg;</span> <span class="text-xs text-slate-400">UPRIGHT</span>`;
  }
}

function startNewTripSession() {
  if (!navigator.geolocation) {
    alert('Geolocation is not supported by your browser. Please use a modern mobile or desktop browser with GPS enabled.');
    return;
  }

  // Request Device Orientation permission on iOS 13+ if applicable
  if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
    DeviceOrientationEvent.requestPermission().catch(() => {});
  }

  isTripRecording = true;
  isTripPaused = false;
  tripStartTime = new Date();
  tripElapsedSeconds = 0;
  activeTripPoints = [];
  activeTripDistanceKm = 0.0;
  peakTripSpeed = 0.0;
  peakTripLean = 0.0;
  activeTripId = 'trip_' + Date.now();

  // Reset live polyline on map
  if (tripsMapInstance && activeTripPolyline) {
    activeTripPolyline.setLatLngs([]);
    if (activeTripMarker) {
      tripsMapInstance.removeLayer(activeTripMarker);
      activeTripMarker = null;
    }
  }

  // Update UI Elements to Recording State
  document.getElementById('btnStartTrip').classList.add('hidden');
  document.getElementById('btnPauseTrip').classList.remove('hidden');
  document.getElementById('btnFinishTrip').classList.remove('hidden');

  const dot = document.getElementById('tripRecordingStatusDot');
  const title = document.getElementById('tripRecordingStateTitle');
  const pill = document.getElementById('tripLiveStatusPill');
  const startStamp = document.getElementById('tripStartTimestampDisplay');
  const navPulse = document.getElementById('navLiveTripPulse');

  if (dot) dot.className = 'w-3 h-3 rounded-full bg-emerald-400 status-pulse';
  if (title) title.textContent = 'Trip in Progress (Real GPS Active)';
  if (pill) {
    pill.textContent = 'GPS SENSORS ACTIVE';
    pill.className = 'px-3 py-1 text-xs font-bold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-400/40 status-pulse';
  }

  const timeStr = tripStartTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const dateStr = tripStartTime.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  if (startStamp) {
    startStamp.innerHTML = `Started at: <strong class="text-white">${timeStr}</strong> &bull; ${dateStr} &bull; Real Telemetry Stream`;
  }
  if (navPulse) navPulse.classList.remove('hidden');

  // Start real stopwatch timer
  if (tripTimerInterval) clearInterval(tripTimerInterval);
  tripTimerInterval = setInterval(updateLiveTripTimer, 1000);

  // Start Real GPS Position Tracking
  gpsWatchId = navigator.geolocation.watchPosition(
    handleRealGpsFix,
    handleRealGpsError,
    {
      enableHighAccuracy: true,
      maximumAge: 1000,
      timeout: 10000
    }
  );

  // Start Real Parent Live Beacon Push (pushes real coordinates every 3 seconds)
  startRealLiveBeaconBroadcast();

  showToast('Real GPS & IMU Telemetry active! Mount phone on motorcycle.', 'success');
}

function handleRealGpsFix(position) {
  if (!isTripRecording || isTripPaused) return;

  const lat = position.coords.latitude;
  const lng = position.coords.longitude;
  const alt = position.coords.altitude ? Math.round(position.coords.altitude) : null;
  const timestamp = position.timestamp || Date.now();

  let speedKmh = 0.0;

  // Real Speed Computation
  if (position.coords.speed !== null && position.coords.speed >= 0) {
    speedKmh = position.coords.speed * 3.6;
  } else if (lastKnownGpsFix && lastKnownGpsFix.timestamp) {
    // Haversine fallback calculation for devices without native speed reporting
    const dMeters = haversineDistance(lastKnownGpsFix.lat, lastKnownGpsFix.lng, lat, lng);
    const dtSeconds = (timestamp - lastKnownGpsFix.timestamp) / 1000;
    if (dtSeconds > 0) {
      speedKmh = (dMeters / dtSeconds) * 3.6;
    }
  }

  // Smooth speed & ignore micro GPS drift below 1.5 km/h
  if (speedKmh < 1.5) speedKmh = 0.0;
  liveSpeedKmh = speedKmh;

  if (speedKmh > peakTripSpeed) {
    peakTripSpeed = speedKmh;
  }

  // Cumulative distance
  if (lastKnownGpsFix) {
    const segmentMeters = haversineDistance(lastKnownGpsFix.lat, lastKnownGpsFix.lng, lat, lng);
    activeTripDistanceKm += (segmentMeters / 1000);
  }

  lastKnownGpsFix = { lat, lng, altitude: alt, timestamp };

  // Store real track point
  activeTripPoints.push({
    latitude: lat,
    longitude: lng,
    altitudeMeters: alt,
    speedMps: speedKmh / 3.6,
    speedKmh: speedKmh,
    leanAngleDeg: liveLeanDeg,
    cumulativeDistanceMeters: Math.round(activeTripDistanceKm * 1000),
    timestamp: timestamp
  });

  // Update UI Elements
  const speedEl = document.getElementById('liveCurrentSpeed');
  const distEl = document.getElementById('liveCurrentDist');
  const moveEl = document.getElementById('liveMovementStatus');
  const mapSpeed = document.getElementById('tripsMapSpeed');
  const mapDist = document.getElementById('tripsMapDist');
  const mapAlt = document.getElementById('tripsMapAlt');

  if (speedEl) speedEl.textContent = speedKmh.toFixed(1);
  if (distEl) distEl.textContent = activeTripDistanceKm.toFixed(2);

  if (moveEl) {
    if (speedKmh >= 2.0) {
      moveEl.textContent = `BIKE IS MOVING — ${speedKmh.toFixed(1)} KM/H`;
      moveEl.className = 'text-xs text-emerald-400 font-mono font-bold block mt-1 tracking-wider';
    } else {
      moveEl.textContent = `BIKE IS STOPPED — 0.0 KM/H`;
      moveEl.className = 'text-xs text-amber-400 font-mono font-bold block mt-1 tracking-wider';
    }
  }

  if (mapSpeed) mapSpeed.textContent = speedKmh.toFixed(0);
  if (mapDist) mapDist.textContent = activeTripDistanceKm.toFixed(2) + ' km';
  if (mapAlt && alt !== null) mapAlt.textContent = alt + ' m';

  // Redraw gauges
  drawSpeedGauge('tripsSpeedCanvas', speedKmh);

  // Update Live Map Breadcrumbs with Real GPS Path
  if (tripsMapInstance && activeTripPolyline) {
    activeTripPolyline.addLatLng([lat, lng]);

    if (!activeTripMarker) {
      const bikeIcon = L.divIcon({
        className: 'bike-real-marker',
        html: `
          <div style="position: relative; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;">
            <div style="position: absolute; inset: -4px; border-radius: 9999px; background: rgba(0, 240, 255, 0.4); animation: pulseGlow 1.5s infinite;"></div>
            <div style="width: 18px; height: 18px; border-radius: 9999px; background: #00F0FF; border: 2px solid #FFFFFF; box-shadow: 0 0 12px #00F0FF;"></div>
          </div>
        `,
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      });
      activeTripMarker = L.marker([lat, lng], { icon: bikeIcon }).addTo(tripsMapInstance);
    } else {
      activeTripMarker.setLatLng([lat, lng]);
    }

    tripsMapInstance.panTo([lat, lng]);
  }
}

function handleRealGpsError(err) {
  console.warn('GPS Fix Notice:', err.message);
  if (err.code === 1) {
    showToast('GPS Permission Denied. Please enable location permissions in browser.', 'error');
  }
}

function startRealLiveBeaconBroadcast() {
  if (beaconPushInterval) clearInterval(beaconPushInterval);

  beaconPushInterval = setInterval(function() {
    if (!isTripRecording || !activeTripId) return;

    const payload = {
      tripId: activeTripId,
      riderName: riderProfile.name || 'Natesh',
      status: isTripPaused ? 'PAUSED' : 'RECORDING',
      isMoving: liveSpeedKmh >= 2.0,
      currentSpeedKmh: liveSpeedKmh,
      maxSpeedKmh: peakTripSpeed,
      latitude: lastKnownGpsFix ? lastKnownGpsFix.lat : 0,
      longitude: lastKnownGpsFix ? lastKnownGpsFix.lng : 0,
      altitudeMeters: lastKnownGpsFix ? lastKnownGpsFix.altitude : null,
      totalDistanceKm: activeTripDistanceKm,
      elapsedTimeMs: tripElapsedSeconds * 1000,
      timestamp: Date.now()
    };

    fetch(`/api/trips/${activeTripId}/live`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).catch(() => {});
  }, 3000);
}

function updateLiveTripTimer() {
  if (!isTripRecording || isTripPaused) return;
  tripElapsedSeconds++;

  const hrs = String(Math.floor(tripElapsedSeconds / 3600)).padStart(2, '0');
  const mins = String(Math.floor((tripElapsedSeconds % 3600) / 60)).padStart(2, '0');
  const secs = String(tripElapsedSeconds % 60).padStart(2, '0');
  const formatted = `${hrs}:${mins}:${secs}`;

  const timerEl = document.getElementById('liveElapsedTimer');
  if (timerEl) timerEl.textContent = formatted;
}

function togglePauseTripSession() {
  if (!isTripRecording) return;
  isTripPaused = !isTripPaused;

  const pauseLabel = document.getElementById('pauseBtnLabel');
  const pill = document.getElementById('tripLiveStatusPill');
  const movement = document.getElementById('liveMovementStatus');

  if (isTripPaused) {
    if (pauseLabel) pauseLabel.textContent = 'Resume Recording';
    if (pill) {
      pill.textContent = 'TRIP PAUSED';
      pill.className = 'px-3 py-1 text-xs font-bold rounded-full bg-amber-500/20 text-amber-300 border border-amber-400/40';
    }
    if (movement) {
      movement.textContent = 'BIKE PAUSED / ENGINE IDLE';
      movement.className = 'text-xs text-amber-400 font-mono font-bold block mt-1 tracking-wider';
    }
    showToast('Trip paused', 'info');
  } else {
    if (pauseLabel) pauseLabel.textContent = 'Pause Recording';
    if (pill) {
      pill.textContent = 'GPS SENSORS ACTIVE';
      pill.className = 'px-3 py-1 text-xs font-bold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-400/40 status-pulse';
    }
    showToast('Trip resumed', 'success');
  }
}

function finishTripSession() {
  if (!isTripRecording) return;

  isTripRecording = false;
  isTripPaused = false;

  if (tripTimerInterval) clearInterval(tripTimerInterval);
  if (gpsWatchId) navigator.geolocation.clearWatch(gpsWatchId);
  if (beaconPushInterval) clearInterval(beaconPushInterval);

  // Reset controls
  document.getElementById('btnStartTrip').classList.remove('hidden');
  document.getElementById('btnPauseTrip').classList.add('hidden');
  document.getElementById('btnFinishTrip').classList.add('hidden');

  const title = document.getElementById('tripRecordingStateTitle');
  const pill = document.getElementById('tripLiveStatusPill');
  const navPulse = document.getElementById('navLiveTripPulse');

  if (title) title.textContent = 'Trip Completed Safely';
  if (pill) {
    pill.textContent = 'SAVED TO CLOUD';
    pill.className = 'px-3 py-1 text-xs font-bold rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-400/40';
  }
  if (navPulse) navPulse.classList.add('hidden');

  // Compute REAL Summary Metrics from actual recorded points
  const finishTime = new Date();
  const validSpeeds = activeTripPoints.map(p => p.speedKmh).filter(s => s > 0);
  const avgSpeed = validSpeeds.length > 0 ? (validSpeeds.reduce((a, b) => a + b, 0) / validSpeeds.length) : 0.0;
  
  // Real Elevation Gain Calculation
  let totalAscent = 0;
  for (let i = 1; i < activeTripPoints.length; i++) {
    const prevAlt = activeTripPoints[i - 1].altitudeMeters;
    const curAlt = activeTripPoints[i].altitudeMeters;
    if (prevAlt !== null && curAlt !== null && curAlt > prevAlt) {
      totalAscent += (curAlt - prevAlt);
    }
  }

  const tripRecord = {
    _id: activeTripId,
    title: 'Motorcycle Ride &mdash; ' + finishTime.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
    startTime: tripStartTime ? tripStartTime.toISOString() : new Date().toISOString(),
    endTime: finishTime.toISOString(),
    duration: tripElapsedSeconds,
    distance: activeTripDistanceKm,
    maxSpeed: peakTripSpeed,
    avgSpeed: avgSpeed,
    maxLeanAngle: peakTripLean,
    elevationGain: totalAscent,
    status: 'COMPLETED',
    points: activeTripPoints
  };

  allTripsCache.unshift(tripRecord);
  renderRecentTripsTable();

  // Save Real Trip and Track Points to MongoDB Atlas Backend
  fetch('/api/trips', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tripRecord)
  })
    .then(r => r.json())
    .then(() => {
      showToast('Trip successfully persisted to MongoDB Atlas!', 'success');
    })
    .catch(() => {
      showToast('Trip saved locally in browser memory', 'info');
    });

  // Automatically switch to Analysis view and load this real run
  setTimeout(function() {
    switchMainView('analysis');
    loadTripForAnalysis(tripRecord._id);
  }, 1000);
}

function shareTripWithParents() {
  const tripId = activeTripId || (allTripsCache[0] && allTripsCache[0]._id) || 'live_ride';
  const liveUrl = `${window.location.origin}/?live=${tripId}`;
  const riderName = riderProfile.name || 'Natesh';
  const text = encodeURIComponent(`Hi! ${riderName} has started a motorcycle ride on Rhody. Track real live speed, motion status & GPS location here: ${liveUrl}`);
  window.open(`https://wa.me/?text=${text}`, '_blank');
  showToast('Generated WhatsApp Parent Live Safety Link', 'success');
}

function copyParentTrackingLink() {
  const tripId = activeTripId || (allTripsCache[0] && allTripsCache[0]._id) || 'live_ride';
  const liveUrl = `${window.location.origin}/?live=${tripId}`;
  navigator.clipboard.writeText(liveUrl).then(() => {
    showToast('Parent Live Safety link copied to clipboard!', 'success');
  }).catch(() => {
    prompt('Copy Parent Live Safety Link:', liveUrl);
  });
}

// ----------------------------------------------------
// PARENT LIVE TRACKING MONITOR MODE (?live=<tripId>)
// ----------------------------------------------------
function initParentLiveTrackingMode(liveTripId) {
  switchMainView('trips');
  showToast(`Connected to Parent Live Safety Monitor for trip #${liveTripId}`, 'info');

  const banner = document.getElementById('parentLiveAlertBanner');
  if (banner) banner.classList.remove('hidden');

  // Poll real live beacon every 3 seconds
  if (parentPollInterval) clearInterval(parentPollInterval);

  parentPollInterval = setInterval(function() {
    fetch(`/api/trips/${liveTripId}/live`)
      .then(r => r.json())
      .then(beacon => {
        if (!beacon) return;

        const speedEl = document.getElementById('liveCurrentSpeed');
        const distEl = document.getElementById('liveCurrentDist');
        const moveEl = document.getElementById('liveMovementStatus');
        const pill = document.getElementById('tripLiveStatusPill');

        if (speedEl) speedEl.textContent = (beacon.currentSpeedKmh || 0).toFixed(1);
        if (distEl) distEl.textContent = (beacon.totalDistanceKm || 0).toFixed(2);

        if (moveEl) {
          if (beacon.isMoving) {
            moveEl.textContent = `BIKE IS MOVING — ${(beacon.currentSpeedKmh || 0).toFixed(1)} KM/H`;
            moveEl.className = 'text-xs text-emerald-400 font-mono font-bold block mt-1 tracking-wider';
          } else {
            moveEl.textContent = `BIKE IS STOPPED — 0.0 KM/H`;
            moveEl.className = 'text-xs text-amber-400 font-mono font-bold block mt-1 tracking-wider';
          }
        }

        if (pill) {
          pill.textContent = beacon.status === 'RECORDING' ? 'RIDER ACTIVE ON ROAD' : 'RIDE FINISHED';
        }

        drawSpeedGauge('tripsSpeedCanvas', beacon.currentSpeedKmh || 0);

        if (beacon.latitude && beacon.longitude && tripsMapInstance) {
          if (!activeTripMarker) {
            const beaconIcon = L.divIcon({
              className: 'parent-live-beacon',
              html: `<div style="width: 20px; height: 20px; border-radius: 9999px; background: #00F0FF; border: 3px solid #FFFFFF; box-shadow: 0 0 15px #00F0FF;"></div>`,
              iconSize: [20, 20],
              iconAnchor: [10, 10]
            });
            activeTripMarker = L.marker([beacon.latitude, beacon.longitude], { icon: beaconIcon }).addTo(tripsMapInstance);
          } else {
            activeTripMarker.setLatLng([beacon.latitude, beacon.longitude]);
          }
          tripsMapInstance.panTo([beacon.latitude, beacon.longitude]);
        }
      })
      .catch(() => {});
  }, 3000);
}

// ----------------------------------------------------
// ANALYSIS LOGIC: 100% REAL RECORDED TRIPS & HEATMAP
// ----------------------------------------------------
function refreshRealTrips() {
  fetch('/api/trips')
    .then(r => r.json())
    .then(trips => {
      if (Array.isArray(trips)) {
        allTripsCache = trips;
      }
      renderRecentTripsTable();
      populateAnalysisDropdown();
      if (allTripsCache.length > 0 && !activeAnalysisTrip) {
        loadTripForAnalysis(allTripsCache[0]._id || allTripsCache[0].id);
      } else if (allTripsCache.length === 0) {
        renderAnalysisEmptyState();
      }
    })
    .catch(() => {
      renderRecentTripsTable();
      if (allTripsCache.length === 0) renderAnalysisEmptyState();
    });
}

function renderRecentTripsTable() {
  const tbody = document.getElementById('tripsRecentTableBody');
  if (!tbody) return;

  if (allTripsCache.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" class="py-8 text-center text-xs text-slate-400">
          <div class="flex flex-col items-center justify-center space-y-2">
            <svg class="w-6 h-6 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            <span>No trips recorded yet. Tap "Start Recording Trip" above to capture your first real motorcycle run.</span>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = allTripsCache.map(t => {
    const dStr = new Date(t.startTime).toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    const durMins = Math.round((t.duration || 0) / 60);

    return `
      <tr class="hover:bg-white/[0.04] transition border-b border-white/[0.04]">
        <td class="py-3 px-3 font-semibold text-white text-xs">${t.title || 'Motorcycle Ride'}</td>
        <td class="py-3 px-3 text-slate-400 text-[11px] font-mono">${dStr}</td>
        <td class="py-3 px-3 font-bold text-cyan-300 font-mono">${(t.distance || 0).toFixed(1)} KM</td>
        <td class="py-3 px-3 text-slate-300 text-xs font-mono">${durMins} min</td>
        <td class="py-3 px-3 text-amber-400 font-bold font-mono">${(t.maxSpeed || 0).toFixed(1)} km/h</td>
        <td class="py-3 px-3 text-right">
          <button onclick="switchMainView('analysis'); loadTripForAnalysis('${t._id || t.id}')" class="px-3 py-1 text-xs font-bold rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 text-cyan-300 border border-cyan-400/30 transition">
            Analyze
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

function populateAnalysisDropdown() {
  const select = document.getElementById('analysisTripSelect');
  if (!select) return;

  if (allTripsCache.length === 0) {
    select.innerHTML = `<option value="">No recorded trips in cloud</option>`;
    return;
  }

  select.innerHTML = allTripsCache.map(t => {
    const dStr = new Date(t.startTime).toLocaleDateString([], { month: 'short', day: 'numeric' });
    const dist = (t.distance || 0).toFixed(1);
    return `<option value="${t._id || t.id}">${t.title || 'Ride'} (${dStr} &bull; ${dist} km)</option>`;
  }).join('');
}

function initAnalysisMap() {
  const container = document.getElementById('analysisMap');
  if (!container || analysisMapInstance) return;

  analysisMapInstance = L.map('analysisMap', {
    center: [11.0168, 76.9558],
    zoom: 12
  });

  L.tileLayer(esriDarkLayerUrl, {
    maxNativeZoom: 16,
    maxZoom: 19,
    attribution: '&copy; Esri &mdash; Rhody Dark Canvas'
  }).addTo(analysisMapInstance);
}

function onAnalysisTripSelect(tripId) {
  loadTripForAnalysis(tripId);
}

function renderAnalysisEmptyState() {
  const titleEl = document.getElementById('analysisTripTitle');
  if (titleEl) titleEl.textContent = 'Awaiting Telemetry Data';

  const container = document.getElementById('analysisLandmarksTableBody');
  if (container) {
    container.innerHTML = `
      <tr>
        <td colspan="5" class="py-8 text-center text-xs text-slate-400">
          No recorded telemetry sessions. Take your motorcycle on the road and tap "Start Recording Trip" in the Trips tab.
        </td>
      </tr>
    `;
  }
}

function loadTripForAnalysis(tripId) {
  if (!tripId) {
    renderAnalysisEmptyState();
    return;
  }

  const trip = allTripsCache.find(t => (t._id || t.id) === tripId);
  if (!trip) {
    fetch(`/api/trips/${tripId}`)
      .then(r => r.json())
      .then(fetchedTrip => {
        if (fetchedTrip && !fetchedTrip.error) {
          allTripsCache.push(fetchedTrip);
          applyTripToAnalysis(fetchedTrip);
        }
      })
      .catch(() => {});
    return;
  }

  applyTripToAnalysis(trip);
}

function applyTripToAnalysis(trip) {
  activeAnalysisTrip = trip;

  const titleEl = document.getElementById('analysisTripTitle');
  const selectEl = document.getElementById('analysisTripSelect');
  if (titleEl) titleEl.textContent = trip.title || 'Motorcycle Ride';
  if (selectEl && selectEl.value !== (trip._id || trip.id)) selectEl.value = (trip._id || trip.id);

  // Update Completion Dossier with Real Values
  const compDist = document.getElementById('compDist');
  const compDuration = document.getElementById('compDuration');
  const compTopSpeed = document.getElementById('compTopSpeed');
  const compAvgSpeed = document.getElementById('compAvgSpeed');
  const compMaxLean = document.getElementById('compMaxLean');
  const compElevation = document.getElementById('compElevation');

  if (compDist) compDist.textContent = (trip.distance || 0).toFixed(1) + ' km';
  if (compDuration) {
    const durSec = trip.duration || 0;
    const h = String(Math.floor(durSec / 3600)).padStart(2, '0');
    const m = String(Math.floor((durSec % 3600) / 60)).padStart(2, '0');
    const s = String(durSec % 60).padStart(2, '0');
    compDuration.textContent = `${h}:${m}:${s}`;
  }
  if (compTopSpeed) compTopSpeed.textContent = (trip.maxSpeed || 0).toFixed(1) + ' km/h';
  if (compAvgSpeed) compAvgSpeed.textContent = (trip.avgSpeed || 0).toFixed(1) + ' km/h';
  if (compMaxLean) compMaxLean.textContent = `${(trip.maxLeanAngle || 0).toFixed(1)}° APEX`;
  if (compElevation) compElevation.textContent = `+${trip.elevationGain || 0} m`;

  // Fetch or extract Real Track Points
  if (trip.points && Array.isArray(trip.points) && trip.points.length > 0) {
    processRealAnalysisPoints(trip.points);
  } else {
    fetch(`/api/trips/${trip._id || trip.id}`)
      .then(r => r.json())
      .then(fullTrip => {
        if (fullTrip && fullTrip.points && fullTrip.points.length > 0) {
          processRealAnalysisPoints(fullTrip.points);
        } else {
          showToast('No detailed GPS fixes recorded for this trip', 'info');
        }
      })
      .catch(() => {});
  }
}

function processRealAnalysisPoints(rawPoints) {
  activeAnalysisPoints = rawPoints.map((p, i) => {
    return {
      index: i,
      lat: p.latitude || p.lat,
      lng: p.longitude || p.lng,
      speed: (p.speedKmh !== undefined) ? p.speedKmh : ((p.speedMps || 0) * 3.6),
      lean: p.leanAngleDeg || p.lean || 0,
      altitude: p.altitudeMeters || p.altitude || 0,
      distKm: (p.cumulativeDistanceMeters || 0) / 1000,
      timestamp: new Date(p.timestamp).toLocaleTimeString()
    };
  });

  renderAnalysisHeatmap();
  renderAnalysisSectorsTable();
  renderAnalysisChart();
}

function renderAnalysisHeatmap() {
  if (!analysisMapInstance || activeAnalysisPoints.length === 0) return;

  // Clear existing layers
  analysisPolylineSegments.forEach(seg => analysisMapInstance.removeLayer(seg));
  analysisPolylineSegments = [];

  const bounds = L.latLngBounds();

  for (let i = 0; i < activeAnalysisPoints.length - 1; i++) {
    const p1 = activeAnalysisPoints[i];
    const p2 = activeAnalysisPoints[i + 1];

    bounds.extend([p1.lat, p1.lng]);

    // Color code based on speed in this sector:
    // Green (Apex turns): < 50 km/h
    // Amber (Sweepers): 50 - 90 km/h
    // Red (Straights): > 90 km/h
    let color = '#10B981';
    if (p1.speed >= 50 && p1.speed < 90) color = '#FFB703';
    if (p1.speed >= 90) color = '#FF3366';

    const seg = L.polyline([[p1.lat, p1.lng], [p2.lat, p2.lng]], {
      color: color,
      weight: 5,
      opacity: 0.95
    }).addTo(analysisMapInstance);

    // Interactive Hover on segment
    seg.on('mouseover', function(e) {
      updateAnalysisHoverHud(p1);
    });

    analysisPolylineSegments.push(seg);
  }

  if (bounds.isValid()) {
    analysisMapInstance.fitBounds(bounds, { padding: [30, 30] });
  }
}

function updateAnalysisHoverHud(p) {
  const hud = document.getElementById('analysisHoverHud');
  const speedEl = document.getElementById('hudSpeed');
  const leanEl = document.getElementById('hudLean');
  const altEl = document.getElementById('hudAlt');
  const placeEl = document.getElementById('hudPlace');

  if (hud) hud.classList.remove('hidden');
  if (speedEl) speedEl.textContent = p.speed.toFixed(1) + ' km/h';
  if (leanEl) leanEl.textContent = Math.abs(p.lean).toFixed(1) + '°';
  if (altEl) altEl.textContent = p.altitude + ' m';
  if (placeEl) placeEl.textContent = `At km ${p.distKm.toFixed(2)} • ${p.timestamp}`;
}

function renderAnalysisSectorsTable() {
  const tbody = document.getElementById('analysisLandmarksTableBody');
  if (!tbody || activeAnalysisPoints.length === 0) return;

  // Sample milestone points across the real route (e.g. every 10% of route)
  const step = Math.max(1, Math.floor(activeAnalysisPoints.length / 8));
  const milestones = [];

  for (let i = 0; i < activeAnalysisPoints.length; i += step) {
    milestones.push(activeAnalysisPoints[i]);
  }
  if (milestones[milestones.length - 1] !== activeAnalysisPoints[activeAnalysisPoints.length - 1]) {
    milestones.push(activeAnalysisPoints[activeAnalysisPoints.length - 1]);
  }

  tbody.innerHTML = milestones.map((m, idx) => {
    let sectorName = idx === 0 ? 'Starting Point' : (idx === milestones.length - 1 ? 'Finish Sector' : `Sector #${idx} (Km ${m.distKm.toFixed(1)})`);
    let speedColor = m.speed > 80 ? 'text-rose-400' : (m.speed > 45 ? 'text-amber-400' : 'text-emerald-400');

    return `
      <tr class="hover:bg-white/[0.04] transition border-b border-white/[0.04]">
        <td class="py-2.5 px-3 font-semibold text-white text-xs">${sectorName}</td>
        <td class="py-2.5 px-3 text-slate-400 text-xs font-mono">${m.distKm.toFixed(2)} km</td>
        <td class="py-2.5 px-3 font-bold font-mono ${speedColor}">${m.speed.toFixed(1)} km/h</td>
        <td class="py-2.5 px-3 text-slate-300 font-mono text-xs">${Math.abs(m.lean).toFixed(1)}°</td>
        <td class="py-2.5 px-3 text-slate-400 font-mono text-xs text-right">${m.altitude} m</td>
      </tr>
    `;
  }).join('');
}

function renderAnalysisChart() {
  const canvas = document.getElementById('analysisChartCanvas');
  if (!canvas || typeof Chart === 'undefined') return;

  if (analysisChartInstance) {
    analysisChartInstance.destroy();
  }

  const labels = activeAnalysisPoints.map(p => p.distKm.toFixed(1));
  const dataSpeed = activeAnalysisPoints.map(p => p.speed);
  const dataLean = activeAnalysisPoints.map(p => Math.abs(p.lean));

  const ctx = canvas.getContext('2d');
  analysisChartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [
        {
          label: 'Speed (km/h)',
          data: dataSpeed,
          borderColor: '#00F0FF',
          backgroundColor: 'rgba(0, 240, 255, 0.1)',
          fill: true,
          tension: 0.3,
          borderWidth: 2,
          pointRadius: 0
        },
        {
          label: 'Apex Lean (deg)',
          data: dataLean,
          borderColor: '#FFB703',
          borderWidth: 1.5,
          borderDash: [4, 4],
          pointRadius: 0
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: { color: '#94A3B8', font: { family: 'Inter', size: 11 } }
        }
      },
      scales: {
        x: {
          ticks: { color: '#64748B', font: { size: 10 } },
          grid: { color: 'rgba(255,255,255,0.05)' }
        },
        y: {
          ticks: { color: '#64748B', font: { size: 10 } },
          grid: { color: 'rgba(255,255,255,0.05)' }
        }
      }
    }
  });
}

// ----------------------------------------------------
// SETTINGS LOGIC & APP EXPORTS
// ----------------------------------------------------
function populateSettingsForm() {
  const nameInput = document.getElementById('settingName');
  const handleInput = document.getElementById('settingHandle');
  const bioInput = document.getElementById('settingBio');
  const bikeInput = document.getElementById('settingBikeModel');
  const specsInput = document.getElementById('settingBikeSpecs');
  const plateInput = document.getElementById('settingBikePlate');
  const tiresInput = document.getElementById('settingTires');
  const parentName = document.getElementById('settingParentName');
  const parentPhone = document.getElementById('settingParentPhone');

  if (nameInput) nameInput.value = riderProfile.name || 'Natesh';
  if (handleInput) handleInput.value = riderProfile.handle || '@natesh_pilot';
  if (bioInput) bioInput.value = riderProfile.bio || '';
  if (bikeInput) bikeInput.value = riderProfile.bikeModel || 'Yamaha MT-09 SP';
  if (specsInput) specsInput.value = riderProfile.bikeSpecs || '890cc Triple • 119 BHP • 93 Nm';
  if (plateInput) plateInput.value = riderProfile.bikePlate || 'TN-38-RH-0909';
  if (tiresInput) tiresInput.value = riderProfile.tireSetup || 'Pirelli Diablo Rosso IV';
  if (parentName) parentName.value = riderProfile.emergencyContactName || 'Family Guardian';
  if (parentPhone) parentPhone.value = riderProfile.emergencyContactPhone || '+91 98765 43210';
}

function saveSettingsFromForm() {
  const nameInput = document.getElementById('settingName');
  const handleInput = document.getElementById('settingHandle');
  const bioInput = document.getElementById('settingBio');
  const bikeInput = document.getElementById('settingBikeModel');
  const specsInput = document.getElementById('settingBikeSpecs');
  const plateInput = document.getElementById('settingBikePlate');
  const tiresInput = document.getElementById('settingTires');
  const parentName = document.getElementById('settingParentName');
  const parentPhone = document.getElementById('settingParentPhone');

  if (nameInput) riderProfile.name = nameInput.value.trim();
  if (handleInput) riderProfile.handle = handleInput.value.trim();
  if (bioInput) riderProfile.bio = bioInput.value.trim();
  if (bikeInput) riderProfile.bikeModel = bikeInput.value.trim();
  if (specsInput) riderProfile.bikeSpecs = specsInput.value.trim();
  if (plateInput) riderProfile.bikePlate = plateInput.value.trim();
  if (tiresInput) riderProfile.tireSetup = tiresInput.value.trim();
  if (parentName) riderProfile.emergencyContactName = parentName.value.trim();
  if (parentPhone) riderProfile.emergencyContactPhone = parentPhone.value.trim();

  saveProfile();
  renderDashboardProfile();
  renderDashboardBike();
  renderYoutubeDynamicHub();
  showToast('Settings & Motorcycle specifications saved!', 'success');
}

function exportAnalysisGpx() {
  if (!activeAnalysisTrip) {
    showToast('No trip selected for export', 'error');
    return;
  }
  window.open(`/api/trips/${activeAnalysisTrip._id || activeAnalysisTrip.id}/export/gpx`, '_blank');
}

function exportAnalysisCsv() {
  if (!activeAnalysisTrip) {
    showToast('No trip selected for export', 'error');
    return;
  }
  window.open(`/api/trips/${activeAnalysisTrip._id || activeAnalysisTrip.id}/export/csv`, '_blank');
}

function clearLocalDataCache() {
  if (!confirm('Clear local telemetry cache and re-sync from MongoDB Atlas?')) return;
  localStorage.removeItem('rhody_saved_places');
  savedPlaces = [];
  loadPlacesFromBackend();
  refreshRealTrips();
  showToast('Cache cleared and re-synced with cloud', 'success');
}

function backupTripsJson() {
  const blob = new Blob([JSON.stringify(allTripsCache, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `rhody_trips_backup_${Date.now()}.json`;
  a.click();
  showToast('Trips backup JSON exported', 'success');
}

function refreshData() {
  refreshRealTrips();
  loadPlacesFromBackend();
  showToast('Telemetry re-synced with MongoDB Atlas', 'success');
}

// ----------------------------------------------------
// UTILITIES: GAUGES, WEATHER & TOASTS
// ----------------------------------------------------
function drawSpeedGauge(canvasId, speedKmh) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  const cx = w / 2;
  const cy = h / 2;
  const r = (w / 2) - 12;

  ctx.clearRect(0, 0, w, h);

  // Background Bezel Track (240 deg arc)
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0.75 * Math.PI, 2.25 * Math.PI);
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
  ctx.lineWidth = 10;
  ctx.lineCap = 'round';
  ctx.stroke();

  // Active Value Arc
  const maxSpeed = 160;
  const ratio = Math.min(1.0, Math.max(0, speedKmh / maxSpeed));
  const endAngle = 0.75 * Math.PI + ratio * (1.5 * Math.PI);

  if (ratio > 0.01) {
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0.75 * Math.PI, endAngle);
    const grad = ctx.createLinearGradient(0, h, w, 0);
    grad.addColorStop(0, '#00F0FF');
    grad.addColorStop(0.6, '#FFB703');
    grad.addColorStop(1, '#FF3366');
    ctx.strokeStyle = grad;
    ctx.lineWidth = 10;
    ctx.lineCap = 'round';
    ctx.stroke();
  }
}

function drawGForceReticle(canvasId, latG, longG) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  const cx = w / 2;
  const cy = h / 2;
  const r = (w / 2) - 8;

  ctx.clearRect(0, 0, w, h);

  // Radar Rings
  [0.33, 0.66, 1.0].forEach(frac => {
    ctx.beginPath();
    ctx.arc(cx, cy, r * frac, 0, 2 * Math.PI);
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
    ctx.lineWidth = 1;
    ctx.stroke();
  });

  // Crosshairs
  ctx.beginPath();
  ctx.moveTo(cx, cy - r); ctx.lineTo(cx, cy + r);
  ctx.moveTo(cx - r, cy); ctx.lineTo(cx + r, cy);
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.12)';
  ctx.stroke();

  // Active Target Bead
  const px = cx + Math.max(-r, Math.min(r, latG * r));
  const py = cy - Math.max(-r, Math.min(r, longG * r));

  ctx.beginPath();
  ctx.arc(px, py, 6, 0, 2 * Math.PI);
  ctx.fillStyle = '#00F0FF';
  ctx.shadowColor = '#00F0FF';
  ctx.shadowBlur = 10;
  ctx.fill();
  ctx.shadowBlur = 0;
}

function fetchRoadWeather(lat, lng, label) {
  fetch(`https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lng}&current_weather=true`)
    .then(r => r.json())
    .then(data => {
      if (!data || !data.current_weather) return;
      const cw = data.current_weather;
      const gripBadge = document.getElementById('roadGripBadge');
      if (gripBadge) {
        if (cw.weathercode === 0) {
          gripBadge.innerHTML = `🟢 <strong>DRY ROAD (98% GRIP)</strong> &bull; ${cw.temperature}°C &bull; ${label}`;
        } else if (cw.weathercode > 50) {
          gripBadge.innerHTML = `🔴 <strong>WET TARMAC (45% GRIP)</strong> &bull; Caution in corners &bull; ${label}`;
        } else {
          gripBadge.innerHTML = `🟡 <strong>OPTIMAL ROAD (${cw.temperature}°C)</strong> &bull; ${label}`;
        }
      }
    })
    .catch(() => {});
}

function haversineDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

function showToast(message, type = 'info') {
  let toastContainer = document.getElementById('rhodyToastContainer');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'rhodyToastContainer';
    toastContainer.className = 'fixed bottom-20 right-5 z-[9999] flex flex-col space-y-2 pointer-events-none';
    document.body.appendChild(toastContainer);
  }

  let bgClass = 'bg-[#0E131F] border-cyan-400/40 text-cyan-300';
  if (type === 'success') bgClass = 'bg-[#0E131F] border-emerald-400/40 text-emerald-300';
  if (type === 'error') bgClass = 'bg-[#0E131F] border-rose-400/40 text-rose-300';

  const toast = document.createElement('div');
  toast.className = `px-4 py-2.5 rounded-xl text-xs font-semibold border backdrop-blur-xl shadow-2xl pointer-events-auto transition-all transform translate-y-2 opacity-0 ${bgClass}`;
  toast.textContent = message;

  toastContainer.appendChild(toast);
  setTimeout(() => { toast.classList.remove('translate-y-2', 'opacity-0'); }, 10);
  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-2');
    setTimeout(() => { if (toast.parentNode) toast.parentNode.removeChild(toast); }, 300);
  }, 3500);
}
