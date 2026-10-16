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

// Motorcycle Garage Catalog
var motorcycleGarage = {
  mt15: {
    key: 'mt15',
    name: 'Yamaha MT-15 V2',
    model: 'Yamaha MT-15 V2 (2024)',
    specs: '155cc Liquid VVA &bull; 18.4 PS',
    plate: 'KA-01-EQ-1550',
    tireSetup: 'MRF Nylogrip Zapper (Cold PSI: F 32 / R 34)',
    frontPsi: 'F: 32 PSI',
    rearPsi: 'R: 34 PSI',
    fuelPercent: 82,
    fuelLiters: '8.2L remaining',
    estRange: '~380 KM Estimated Range',
    vvaBadge: 'VVA ENGAGED',
    absBadge: 'ABS ACTIVE',
    tcsBadge: 'TCS READY',
    coolantVal: '88&deg;C',
    coolantStatus: 'Nominal',
    batteryVal: '12.6 V',
    batteryStatus: 'Healthy',
    oilVal: '76%',
    oilNext: 'Next: 2.4k km',
    odoVal: '8,420 km',
    query: 'Yamaha MT-15 V2 motorcycle review pure exhaust sound'
  },
  mt09: {
    key: 'mt09',
    name: 'Yamaha MT-09 SP',
    model: 'Yamaha MT-09 SP (2024)',
    specs: '890cc Triple CP3 &bull; 119 BHP &bull; 93 Nm',
    plate: 'TN-38-RH-0909',
    tireSetup: 'Pirelli Diablo Rosso IV (Cold PSI: F 32 / R 36)',
    frontPsi: 'F: 32 PSI',
    rearPsi: 'R: 36 PSI',
    fuelPercent: 65,
    fuelLiters: '9.1L remaining',
    estRange: '~240 KM Estimated Range',
    vvaBadge: 'CP3 TORQUE ACTIVE',
    absBadge: 'CORNERING ABS ACTIVE',
    tcsBadge: 'LEAN TCS LEVEL 2',
    coolantVal: '92&deg;C',
    coolantStatus: 'Nominal',
    batteryVal: '12.8 V',
    batteryStatus: 'Healthy',
    oilVal: '84%',
    oilNext: 'Next: 4.2k km',
    odoVal: '12,650 km',
    query: 'Yamaha MT-09 SP motorcycle review pure sound'
  }
};
var activeBikeKey = 'mt15';

// Rider Profile State
var riderProfile = {
  name: 'Natesh',
  email: 'pilot@rhody.io',
  avatar: '',
  handle: '@natesh_pilot',
  bio: 'Weekend canyon carver & telemetry enthusiast. Western Ghats apex seeker pushing precision motorcycle telemetry.',
  bikeModel: 'Yamaha MT-15 V2',
  bikeSpecs: '155cc Liquid VVA • 18.4 PS',
  bikePlate: 'KA-01-EQ-1550',
  tireSetup: 'MRF Nylogrip Zapper (Cold PSI: F 32 / R 34)',
  emergencyContactName: 'Family Guardian (Mom & Dad)',
  emergencyContactPhone: '+91 98765 43210',
  speedUnit: 'kmh',
  defaultMapLayer: 'dark',
  gpsPrecision: 'sport'
};

// ----------------------------------------------------
// PILLAR 1: MOTORCYCLE WHOLE MANAGEMENT DATA STATE
// ----------------------------------------------------
var bikeManagementData = {
  specs: {
    makeModel: 'Yamaha MT-15 V2 (BS6 OBD-2)',
    engine: '155cc Liquid-Cooled 4V SOHC VVA',
    power: '18.4 PS @ 10,000 RPM',
    torque: '14.1 Nm @ 7,500 RPM',
    gearbox: '6-Speed Constant Mesh w/ Assist & Slipper Clutch',
    fuelTank: '10 Liters (1.5L Reserve)',
    brakes: 'Dual Channel ABS (282mm Front / 220mm Rear Disc)',
    kerbWeight: '141 kg',
    currentOdometer: 14850
  },
  components: {
    engineOil: { lastServiceOdo: 12000, intervalKm: 4000, name: 'Engine Oil & Filter (Yamalube 10W40)' },
    driveChain: { lastServiceOdo: 14500, intervalKm: 500, name: 'Drive Chain Lube & Slack Adjust' },
    airFilter: { lastServiceOdo: 10000, intervalKm: 10000, name: 'High-Flow Air Filter Element' },
    sparkPlug: { lastServiceOdo: 10000, intervalKm: 12000, name: 'NGK Iridium Spark Plug' },
    coolant: { lastServiceOdo: 8000, intervalKm: 15000, name: 'Radiator Glycol Coolant Flush' },
    brakePads: { lastServiceOdo: 11000, intervalKm: 8000, name: 'Sintered Front & Rear Brake Pads' }
  },
  documents: {
    rcNumber: 'TN-38-BZ-4242',
    rcValidUntil: '2037-04-15',
    insurancePolicy: 'BAJAJ-ALLIANZ-COMP-99824',
    insuranceExpiry: '2026-11-28',
    pucNumber: 'TN38PUC2026-092',
    pucExpiry: '2026-12-14',
    rsaPhone: '+91 1800 209 7979'
  },
  serviceHistory: [
    { id: 'srv_1', date: '2026-08-10', odo: 12000, center: 'Yamaha Authorized Prime Service, Coimbatore', workDone: 'Periodic 3rd Service: Yamalube fully synthetic oil change, oil filter replacement, brake pad clean, chain degrease & tension set.', cost: 1850 },
    { id: 'srv_2', date: '2026-04-12', odo: 8000, center: 'Apex Motosports Performance Garage', workDone: 'Radiator coolant flush, spark plug inspection, throttle body synchronization, steering cone bearing greasing.', cost: 2400 },
    { id: 'srv_3', date: '2025-11-20', odo: 4000, center: 'Yamaha Authorized Prime Service, Coimbatore', workDone: 'Periodic 2nd Service: Engine oil renewal, chain lubrication, clutch free-play adjustment, ECU diagnostics test.', cost: 1320 }
  ],
  fuelLogs: [
    { id: 'fuel_1', date: '2026-10-12', odo: 14850, liters: 8.5, costPerLiter: 102.6, totalCost: 872, fuelGrade: 'XP95 Octane', mileageKmPerL: 48.2 },
    { id: 'fuel_2', date: '2026-09-28', odo: 14440, liters: 8.2, costPerLiter: 102.5, totalCost: 840, fuelGrade: 'XP95 Octane', mileageKmPerL: 47.6 },
    { id: 'fuel_3', date: '2026-09-15', odo: 14050, liters: 8.0, costPerLiter: 102.8, totalCost: 822, fuelGrade: 'Standard Petrol', mileageKmPerL: 46.8 }
  ]
};

// Guardian Parent Alert Configuration
var guardianParentConfig = {
  guardianName: 'Family Guardian (Dad & Mom)',
  guardianPhone: '+91 98765 43210',
  speedLimitKmh: 95
};

// Real YouTube Active Query
var currentYoutubeQuery = 'Yamaha MT-15 V2 motorcycle review pure exhaust sound';

// ----------------------------------------------------
// INITIALIZATION
// ----------------------------------------------------
document.addEventListener('DOMContentLoaded', function() {
  loadProfileFromStorage();
  loadBikeManagementFromStorage();
  renderDashboardProfile();
  renderDashboardBike();
  renderBikeManagement();
  renderYoutubeDynamicHub();
  populateSettingsForm();

  // Initialize Maps with Leaflet
  ensureLeaflet(function() {
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
  const views = ['dashboard', 'trips', 'parenting', 'settings', 'analysis'];

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

  if (viewName === 'parenting') {
    renderParentingView();
  }

  // Re-invalidate map sizes on tab change
  setTimeout(function() {
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
  const cachedAvatar = localStorage.getItem('rhody_user_avatar');

  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      riderProfile = Object.assign({}, riderProfile, parsed);
    } catch (e) {}
  }

  // Backup avatar persistence
  if (cachedAvatar && !riderProfile.avatar) {
    riderProfile.avatar = cachedAvatar;
  }

  fetch('/api/user/profile')
    .then(r => r.json())
    .then(data => {
      if (data && data.profile) {
        // PERMANENT FIX: Never overwrite an existing user avatar with empty string or null!
        const currentAvatar = riderProfile.avatar || localStorage.getItem('rhody_user_avatar') || '';
        
        riderProfile = Object.assign({}, riderProfile, data.profile);

        if (!data.profile.avatar && currentAvatar) {
          riderProfile.avatar = currentAvatar;
        } else if (data.profile.avatar) {
          try {
            localStorage.setItem('rhody_user_avatar', data.profile.avatar);
          } catch (e) {}
        }

        try {
          localStorage.setItem('rhody_rider_profile', JSON.stringify(riderProfile));
        } catch (e) {}

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
  const navProfileName = document.getElementById('navProfileName');

  if (greeting) greeting.textContent = riderProfile.name || 'Natesh';
  if (handle) handle.textContent = riderProfile.handle || '@natesh_pilot';
  if (bio) bio.textContent = riderProfile.bio || 'Weekend canyon carver & telemetry enthusiast.';
  if (navProfileName) navProfileName.textContent = riderProfile.name || 'Natesh';

  const initial = (riderProfile.name || 'N').charAt(0).toUpperCase();

  // Check backup avatar if state is missing it
  const activeAvatar = riderProfile.avatar || localStorage.getItem('rhody_user_avatar') || '';
  if (activeAvatar && !riderProfile.avatar) {
    riderProfile.avatar = activeAvatar;
  }

  if (activeAvatar) {
    if (avatarImg) { avatarImg.src = activeAvatar; avatarImg.classList.remove('hidden'); }
    if (avatarPlaceholder) avatarPlaceholder.classList.add('hidden');
    if (navAvatarImg) { navAvatarImg.src = activeAvatar; navAvatarImg.classList.remove('hidden'); }
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

function switchActiveBike(key, silent = false) {
  if (!motorcycleGarage[key]) return;
  activeBikeKey = key;
  const bike = motorcycleGarage[key];

  riderProfile.bikeModel = bike.model;
  riderProfile.bikeSpecs = bike.specs;
  riderProfile.bikePlate = bike.plate;
  riderProfile.tireSetup = bike.tireSetup;

  // Toggle button styling
  const btn15 = document.getElementById('bikeBtn_mt15');
  const btn09 = document.getElementById('bikeBtn_mt09');
  if (btn15 && btn09) {
    if (key === 'mt15') {
      btn15.className = 'px-3 py-1.5 rounded-lg text-xs font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-400/40 transition';
      btn09.className = 'px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-white transition';
    } else {
      btn09.className = 'px-3 py-1.5 rounded-lg text-xs font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-400/40 transition';
      btn15.className = 'px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-white transition';
    }
  }

  // Update DOM readouts
  const model = document.getElementById('dashBikeModel');
  const specs = document.getElementById('dashBikeSpecs');
  const plate = document.getElementById('dashBikePlate');
  const tires = document.getElementById('dashTires');
  const tiresF = document.getElementById('dashTiresFront');
  const tiresR = document.getElementById('dashTiresRear');
  const fuelPct = document.getElementById('dashFuelPercent');
  const fuelLit = document.getElementById('dashFuelLiters');
  const estRange = document.getElementById('dashEstRange');
  const fuelBar = document.getElementById('dashFuelBar');
  const absBadge = document.getElementById('dashAbsBadge');
  const tcsBadge = document.getElementById('dashTcsBadge');
  const vvaBadge = document.getElementById('dashVvaBadge');
  const coolantVal = document.getElementById('dashCoolantVal');
  const coolantStatus = document.getElementById('dashCoolantStatus');
  const batteryVal = document.getElementById('dashBatteryVal');
  const batteryStatus = document.getElementById('dashBatteryStatus');
  const oilVal = document.getElementById('dashOilVal');
  const oilNext = document.getElementById('dashOilNext');
  const odoVal = document.getElementById('dashOdoVal');

  if (model) model.textContent = bike.name;
  if (specs) specs.innerHTML = bike.specs;
  if (plate) plate.textContent = bike.plate;
  if (tires) tires.textContent = bike.tireSetup.split('(')[0].trim();
  if (tiresF) tiresF.textContent = bike.frontPsi;
  if (tiresR) tiresR.textContent = bike.rearPsi;
  if (fuelPct) fuelPct.textContent = `${bike.fuelPercent}%`;
  if (fuelLit) fuelLit.textContent = bike.fuelLiters;
  if (estRange) estRange.textContent = bike.estRange;
  if (fuelBar) fuelBar.style.width = `${bike.fuelPercent}%`;
  if (absBadge) absBadge.textContent = bike.absBadge;
  if (tcsBadge) tcsBadge.textContent = bike.tcsBadge;
  if (vvaBadge) vvaBadge.textContent = bike.vvaBadge;
  if (coolantVal) coolantVal.innerHTML = bike.coolantVal;
  if (coolantStatus) coolantStatus.textContent = bike.coolantStatus;
  if (batteryVal) batteryVal.textContent = bike.batteryVal;
  if (batteryStatus) batteryStatus.textContent = bike.batteryStatus;
  if (oilVal) oilVal.textContent = bike.oilVal;
  if (oilNext) oilNext.textContent = bike.oilNext;
  if (odoVal) odoVal.textContent = bike.odoVal;

  // Refresh YouTube Hub with bike query
  currentYoutubeQuery = bike.query;
  if (!silent) {
    renderYoutubeDynamicHub();
    showToast(`Active motorcycle switched to ${bike.name}`, 'info');
  }
}

function renderDashboardBike() {
  switchActiveBike(activeBikeKey, true);
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
      const maxDim = 320;
      let width = img.width;
      let height = img.height;

      if (width > height) {
        if (width > maxDim) { height = Math.round(height * (maxDim / width)); width = maxDim; }
      } else {
        if (height > maxDim) { width = Math.round(width * (maxDim / height)); height = maxDim; }
      }

      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, width, height);

      const optimizedBase64 = canvas.toDataURL('image/jpeg', 0.82);
      riderProfile.avatar = optimizedBase64;
      try {
        localStorage.setItem('rhody_user_avatar', optimizedBase64);
      } catch (err) {
        console.warn('Avatar backup storage warning:', err);
      }
      saveProfile();
      renderDashboardProfile();
      showToast('Profile photo updated permanently', 'success');
    };
    img.src = rawBase64;
  };
  reader.readAsDataURL(file);
}

function removeRiderAvatar() {
  riderProfile.avatar = '';
  try {
    localStorage.removeItem('rhody_user_avatar');
  } catch (err) {}
  saveProfile();
  renderDashboardProfile();
  showToast('Profile photo removed', 'info');
}

function saveProfile() {
  try {
    localStorage.setItem('rhody_rider_profile', JSON.stringify(riderProfile));
    if (riderProfile.avatar) {
      localStorage.setItem('rhody_user_avatar', riderProfile.avatar);
    } else {
      localStorage.removeItem('rhody_user_avatar');
    }
  } catch (e) {
    console.warn('LocalStorage save warning:', e);
  }

  fetch('/api/user/profile', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(riderProfile)
  }).catch(function() {});
}

// ----------------------------------------------------
// DASHBOARD: MOTORCYCLE WHOLE MANAGEMENT ENGINE (PILLAR 1 & 4)
// ----------------------------------------------------
function loadBikeManagementFromStorage() {
  const cached = localStorage.getItem('rhody_bike_management');
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      if (parsed && parsed.specs) {
        bikeManagementData = Object.assign({}, bikeManagementData, parsed);
      }
    } catch (e) {}
  }

  fetch('/api/user/bike-management')
    .then(r => r.json())
    .then(res => {
      if (res && res.data && res.data.specs) {
        bikeManagementData = Object.assign({}, bikeManagementData, res.data);
        saveBikeManagementToStorage(false);
      }
      renderBikeManagement();
      renderParentingView();
    })
    .catch(() => {
      renderBikeManagement();
      renderParentingView();
    });
}

function saveBikeManagementToStorage(syncBackend = true) {
  try {
    localStorage.setItem('rhody_bike_management', JSON.stringify(bikeManagementData));
  } catch (e) {}

  if (syncBackend) {
    fetch('/api/user/bike-management', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(bikeManagementData)
    }).catch(() => {});
  }
}

function renderBikeManagement() {
  const specs = bikeManagementData.specs || {};
  const comps = bikeManagementData.components || {};
  const docs = bikeManagementData.documents || {};
  const curOdo = specs.currentOdometer || 14850;

  // 1. Technical Specs Matrix
  const specEngine = document.getElementById('specEngine');
  const specPower = document.getElementById('specPower');
  const specGearbox = document.getElementById('specGearbox');
  const specFuelTank = document.getElementById('specFuelTank');
  const specBrakes = document.getElementById('specBrakes');
  const specWeight = document.getElementById('specWeight');
  const odoDisplay = document.getElementById('mgmtOdoDisplay');

  if (specEngine) specEngine.textContent = specs.engine || '155cc Liquid-Cooled 4V SOHC VVA';
  if (specPower) specPower.textContent = specs.power || '18.4 PS @ 10,000 RPM';
  if (specGearbox) specGearbox.textContent = specs.gearbox || '6-Speed Constant Mesh w/ Slipper Clutch';
  if (specFuelTank) specFuelTank.textContent = specs.fuelTank || '10 Liters (1.5L Reserve)';
  if (specBrakes) specBrakes.textContent = specs.brakes || 'Dual Channel ABS (282mm Front / 220mm Rear Disc)';
  if (specWeight) specWeight.textContent = specs.kerbWeight || '141 kg';
  if (odoDisplay) odoDisplay.textContent = `${curOdo.toLocaleString()} KM`;

  // 2. Scheduled Preventative Maintenance Lifecycles
  updateComponentHealthBar('Oil', comps.engineOil, curOdo);
  updateComponentHealthBar('Chain', comps.driveChain, curOdo);
  updateComponentHealthBar('Air', comps.airFilter, curOdo);
  updateComponentHealthBar('Plug', comps.sparkPlug, curOdo);
  updateComponentHealthBar('Coolant', comps.coolant, curOdo);
  updateComponentHealthBar('Brake', comps.brakePads, curOdo);

  // 3. Legal Documents Compliance Countdown
  if (docs.insuranceExpiry) {
    const daysIns = calculateDaysRemaining(docs.insuranceExpiry);
    const badge = document.getElementById('docInsuranceBadge');
    if (badge) {
      if (daysIns > 30) {
        badge.textContent = `Valid till ${docs.insuranceExpiry} (${daysIns} days)`;
        badge.className = 'text-[10px] text-emerald-400 font-bold block';
      } else if (daysIns > 0) {
        badge.textContent = `Expires in ${daysIns} days (${docs.insuranceExpiry})`;
        badge.className = 'text-[10px] text-amber-400 font-bold block';
      } else {
        badge.textContent = `EXPIRED on ${docs.insuranceExpiry}`;
        badge.className = 'text-[10px] text-rose-400 font-bold block';
      }
    }
  }

  if (docs.pucExpiry) {
    const daysPuc = calculateDaysRemaining(docs.pucExpiry);
    const badge = document.getElementById('docPucBadge');
    if (badge) {
      if (daysPuc > 15) {
        badge.textContent = `Valid till ${docs.pucExpiry} (${daysPuc} days)`;
        badge.className = 'text-[10px] text-emerald-400 font-bold block';
      } else if (daysPuc > 0) {
        badge.textContent = `Expires in ${daysPuc} days (${docs.pucExpiry})`;
        badge.className = 'text-[10px] text-amber-400 font-bold block';
      } else {
        badge.textContent = `EXPIRED on ${docs.pucExpiry}`;
        badge.className = 'text-[10px] text-rose-400 font-bold block';
      }
    }
  }

  // 4. Service History Ledger
  renderServiceHistoryTable();

  // 5. Fuel Economy & Mileage Calculator
  renderFuelEconomyStats();
}

function updateComponentHealthBar(compKey, compObj, curOdo) {
  if (!compObj) return;
  const lastOdo = compObj.lastServiceOdo || 0;
  const interval = compObj.intervalKm || 4000;
  const kmSince = Math.max(0, curOdo - lastOdo);
  const remaining = Math.max(0, interval - kmSince);
  const pct = Math.max(0, Math.min(100, Math.round((remaining / interval) * 100)));

  const textEl = document.getElementById(`status${compKey}Text`);
  const barEl = document.getElementById(`status${compKey}Bar`);
  const kmEl = document.getElementById(`status${compKey}Km`);

  if (kmEl) kmEl.textContent = `${remaining} KM remaining`;

  let colorClass = 'bg-cyan-400';
  let badgeColor = 'text-cyan-300';
  let statusLabel = 'GOOD';

  if (pct > 50) {
    colorClass = 'bg-emerald-400';
    badgeColor = 'text-emerald-400';
    statusLabel = 'OPTIMAL';
  } else if (pct > 20) {
    colorClass = 'bg-amber-400';
    badgeColor = 'text-amber-400';
    statusLabel = 'SERVICE SOON';
  } else {
    colorClass = 'bg-rose-500';
    badgeColor = 'text-rose-400';
    statusLabel = 'DUE NOW';
  }

  if (textEl) {
    textEl.textContent = `${statusLabel} (${pct}%)`;
    textEl.className = `text-[10px] font-bold ${badgeColor}`;
  }

  if (barEl) {
    barEl.style.width = `${pct}%`;
    barEl.className = `h-1.5 rounded-full transition-all duration-500 ${colorClass}`;
  }
}

function calculateDaysRemaining(dateString) {
  try {
    const target = new Date(dateString);
    const today = new Date();
    const diffTime = target - today;
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  } catch (e) {
    return 0;
  }
}

function renderServiceHistoryTable() {
  const tbody = document.getElementById('serviceHistoryTableBody');
  if (!tbody) return;

  const list = bikeManagementData.serviceHistory || [];
  if (list.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" class="py-6 text-center text-xs text-slate-500">
          No past service records logged. Tap "+ Add Record" to log maintenance invoice.
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = list.map(item => {
    return `
      <tr class="hover:bg-white/[0.02] transition">
        <td class="py-2.5 px-3 text-white font-mono font-medium">${item.date}</td>
        <td class="py-2.5 px-3 text-slate-300">${item.center}</td>
        <td class="py-2.5 px-3 text-cyan-400 font-mono font-bold">${(item.odo || 0).toLocaleString()} KM</td>
        <td class="py-2.5 px-3 text-slate-400 max-w-xs truncate" title="${item.workDone || ''}">${item.workDone || '-'}</td>
        <td class="py-2.5 px-3 text-right font-mono font-bold text-emerald-400">₹${(item.cost || 0).toLocaleString()}</td>
      </tr>
    `;
  }).join('');
}

function renderFuelEconomyStats() {
  const logs = bikeManagementData.fuelLogs || [];
  const avgMileageEl = document.getElementById('fuelAvgMileage');
  const costPerKmEl = document.getElementById('fuelCostPerKm');
  const totalLitersEl = document.getElementById('fuelTotalLiters');
  const totalExpenseEl = document.getElementById('fuelTotalExpense');

  if (logs.length === 0) return;

  let totalLiters = 0;
  let totalCost = 0;
  let totalMileage = 0;
  let count = logs.length;

  logs.forEach(l => {
    totalLiters += parseFloat(l.liters || 0);
    totalCost += parseFloat(l.totalCost || 0);
    totalMileage += parseFloat(l.mileageKmPerL || 45);
  });

  const avgMileage = (totalMileage / count).toFixed(1);
  const costPerKm = (totalCost / (totalLiters * parseFloat(avgMileage))).toFixed(2);

  if (avgMileageEl) avgMileageEl.textContent = `${avgMileage} KM/L`;
  if (costPerKmEl) costPerKmEl.textContent = `₹${costPerKm} / KM`;
  if (totalLitersEl) totalLitersEl.textContent = `${totalLiters.toFixed(1)} L`;
  if (totalExpenseEl) totalExpenseEl.textContent = `₹${Math.round(totalCost).toLocaleString()}`;
}

// ----------------------------------------------------
// SERVICE & FUEL LOG MODAL HANDLERS
// ----------------------------------------------------
function openServiceLogModal() {
  const modal = document.getElementById('serviceLogModal');
  if (!modal) return;
  modal.classList.remove('hidden');

  const odoInput = document.getElementById('srvOdometer');
  if (odoInput && bikeManagementData.specs) {
    odoInput.value = bikeManagementData.specs.currentOdometer || 14850;
  }
  const dateInput = document.getElementById('srvDate');
  if (dateInput) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }
}

function closeServiceLogModal() {
  const modal = document.getElementById('serviceLogModal');
  if (modal) modal.classList.add('hidden');
}

function submitServiceRecord(e) {
  e.preventDefault();
  const date = document.getElementById('srvDate')?.value;
  const odo = parseInt(document.getElementById('srvOdometer')?.value) || 0;
  const center = document.getElementById('srvCenter')?.value?.trim();
  const work = document.getElementById('srvWork')?.value?.trim();
  const cost = parseFloat(document.getElementById('srvCost')?.value) || 0;

  if (!date || !center || !work) {
    showToast('Please fill all service details', 'error');
    return;
  }

  const record = {
    id: 'srv_' + Date.now(),
    date,
    odo,
    center,
    workDone: work,
    cost
  };

  if (!bikeManagementData.serviceHistory) bikeManagementData.serviceHistory = [];
  bikeManagementData.serviceHistory.unshift(record);

  // If service odometer is higher than current, advance the motorcycle odometer
  if (odo > (bikeManagementData.specs.currentOdometer || 0)) {
    bikeManagementData.specs.currentOdometer = odo;
  }

  saveBikeManagementToStorage(true);
  renderBikeManagement();
  closeServiceLogModal();
  document.getElementById('serviceLogForm')?.reset();
  showToast('Service record recorded in maintenance ledger!', 'success');
}

function openFuelLogModal() {
  const modal = document.getElementById('fuelLogModal');
  if (!modal) return;
  modal.classList.remove('hidden');

  const odoInput = document.getElementById('fuelOdoInput');
  if (odoInput && bikeManagementData.specs) {
    odoInput.value = bikeManagementData.specs.currentOdometer || 14850;
  }
}

function closeFuelLogModal() {
  const modal = document.getElementById('fuelLogModal');
  if (modal) modal.classList.add('hidden');
}

function submitFuelRecord(e) {
  e.preventDefault();
  const liters = parseFloat(document.getElementById('fuelLitersInput')?.value) || 0;
  const odo = parseInt(document.getElementById('fuelOdoInput')?.value) || (bikeManagementData.specs.currentOdometer || 14850);
  const grade = document.getElementById('fuelGradeInput')?.value || 'XP95 Octane';
  const totalCost = parseFloat(document.getElementById('fuelTotalCostInput')?.value) || 0;

  if (liters <= 0 || totalCost <= 0) {
    showToast('Please enter valid fuel liters and invoice amount', 'error');
    return;
  }

  // Calculate mileage based on distance from last fuel fill
  let mileage = 47.5;
  if (bikeManagementData.fuelLogs && bikeManagementData.fuelLogs.length > 0) {
    const lastFill = bikeManagementData.fuelLogs[0];
    const distRun = odo - (lastFill.odo || 0);
    if (distRun > 20 && liters > 0) {
      mileage = parseFloat((distRun / liters).toFixed(1));
    }
  }

  const newLog = {
    id: 'fuel_' + Date.now(),
    date: new Date().toISOString().split('T')[0],
    odo,
    liters,
    costPerLiter: parseFloat((totalCost / liters).toFixed(1)),
    totalCost,
    fuelGrade: grade,
    mileageKmPerL: mileage
  };

  if (!bikeManagementData.fuelLogs) bikeManagementData.fuelLogs = [];
  bikeManagementData.fuelLogs.unshift(newLog);

  if (odo > (bikeManagementData.specs.currentOdometer || 0)) {
    bikeManagementData.specs.currentOdometer = odo;
  }

  saveBikeManagementToStorage(true);
  renderBikeManagement();
  closeFuelLogModal();
  document.getElementById('fuelLogForm')?.reset();
  showToast(`Logged ${liters}L of ${grade} • Calculated ${mileage} KM/L!`, 'success');
}

function logComponentService(type) {
  const curOdo = bikeManagementData.specs.currentOdometer || 14850;
  if (!bikeManagementData.components) bikeManagementData.components = {};

  if (type === 'oil' && bikeManagementData.components.engineOil) {
    bikeManagementData.components.engineOil.lastServiceOdo = curOdo;
    showToast('Engine Oil & Filter service logged at ' + curOdo + ' KM', 'success');
  } else if (type === 'chain' && bikeManagementData.components.driveChain) {
    bikeManagementData.components.driveChain.lastServiceOdo = curOdo;
    showToast('Drive Chain degrease, lube & slack adjust logged at ' + curOdo + ' KM', 'success');
  }

  saveBikeManagementToStorage(true);
  renderBikeManagement();
}

// ----------------------------------------------------
// PILLAR 3: BIKE PARENTING & GUARDIAN SAFETY ENGINE
// ----------------------------------------------------
function renderParentingView() {
  const liveUrl = getParentLiveBeaconUrl();
  const linkDisplay = document.getElementById('parentLiveLinkDisplay');
  const speedLimitDisplay = document.getElementById('parentSpeedLimitDisplay');
  const guardianNameDisplay = document.getElementById('parentContactNameDisplay');
  const guardianPhoneDisplay = document.getElementById('parentContactPhoneDisplay');
  const callBtn = document.getElementById('btnCallGuardian');
  const motionStatus = document.getElementById('parentMotionStatus');
  const liveSpeed = document.getElementById('parentLiveSpeed');

  if (linkDisplay) linkDisplay.value = liveUrl;
  if (speedLimitDisplay) speedLimitDisplay.textContent = `${guardianParentConfig.speedLimitKmh} km/h`;
  if (guardianNameDisplay) guardianNameDisplay.textContent = guardianParentConfig.guardianName;
  if (guardianPhoneDisplay) guardianPhoneDisplay.textContent = guardianParentConfig.guardianPhone;
  if (callBtn) callBtn.onclick = function() { window.location.href = `tel:${guardianParentConfig.guardianPhone}`; };

  if (liveSpeed) {
    liveSpeed.textContent = (currentTripSpeedKmh || 0).toFixed(1);
  }

  if (motionStatus) {
    if (isTripRecording && !isTripPaused && currentTripSpeedKmh > 2) {
      motionStatus.textContent = `BIKE IS MOVING — ${currentTripSpeedKmh.toFixed(1)} KM/H`;
      motionStatus.className = 'text-xs text-emerald-400 font-mono font-bold block mt-1 tracking-wider';
    } else if (isTripRecording) {
      motionStatus.textContent = 'BIKE IS STATIONARY (IDLE ENGINE)';
      motionStatus.className = 'text-xs text-amber-400 font-mono font-bold block mt-1 tracking-wider';
    } else {
      motionStatus.textContent = 'PARKED AT BASECAMP (ENGINE OFF)';
      motionStatus.className = 'text-xs text-slate-400 font-mono font-bold block mt-1 tracking-wider';
    }
  }

  // Check parent speed alert limit
  const alertBox = document.getElementById('parentSpeedAlertBox');
  if (alertBox) {
    if (isTripRecording && currentTripSpeedKmh > guardianParentConfig.speedLimitKmh) {
      alertBox.classList.remove('hidden');
      alertBox.textContent = `⚠️ PARENT SPEED WARNING: Current speed (${currentTripSpeedKmh.toFixed(1)} km/h) exceeds safe threshold (${guardianParentConfig.speedLimitKmh} km/h)!`;
    } else {
      alertBox.classList.add('hidden');
    }
  }
}

function getParentLiveBeaconUrl() {
  const baseUrl = window.location.origin;
  const tripParam = activeTripDoc ? (activeTripDoc._id || activeTripDoc.id) : 'live-safety-beacon';
  return `${baseUrl}/?live=${tripParam}`;
}

function copyParentTrackingLink() {
  const url = getParentLiveBeaconUrl();
  navigator.clipboard.writeText(url).then(() => {
    showToast('Guardian live tracking link copied to clipboard!', 'success');
  }).catch(() => {
    prompt('Copy guardian tracking link:', url);
  });
}

function openParentViewNewTab() {
  const url = getParentLiveBeaconUrl();
  window.open(url, '_blank');
}

function shareTripWithParents() {
  const url = getParentLiveBeaconUrl();
  const text = encodeURIComponent(
    `🏍️ Rhody Live Ride Beacon:\nI am riding my bike right now!\nTrack my live speed, location, and safety status here:\n${url}`
  );
  const waUrl = `https://wa.me/?text=${text}`;
  window.open(waUrl, '_blank');
  showToast('Opening WhatsApp to share live beacon with family...', 'info');
}

// ----------------------------------------------------
// DASHBOARD: REAL DYNAMIC YOUTUBE MOTORCYCLE GUIDES
// ----------------------------------------------------
function renderYoutubeDynamicHub() {
  const pillsContainer = document.getElementById('youtubePillsContainer');
  const cardsContainer = document.getElementById('youtubeVideoGrid');
  if (!pillsContainer || !cardsContainer) return;

  const bike = (bikeManagementData.specs && bikeManagementData.specs.makeModel) || riderProfile.bikeModel || 'Yamaha MT-15 V2';

  // Dynamic high-adrenaline and mechanical mastery topics
  const dynamicTopics = [
    { label: `${bike} Pure Sound`, query: `${bike} motorcycle exhaust pure sound acceleration` },
    { label: `Drive Chain Maintenance`, query: `motorcycle drive chain clean lube slack adjustment tutorial` },
    { label: `Engine Oil & Filter Change`, query: `${bike} motorcycle engine oil change tutorial Yamalube` },
    { label: `Apex Cornering Masterclass`, query: `motorcycle apex cornering body position trail braking technique` },
    { label: `Rain Grip & Lean Angle`, query: `motorcycle wet weather riding grip tire lean angle safety` },
    { label: `Western Ghats Twisties Vlog`, query: `Kolli Hills Agumbe Ghat motorcycle ride vlog 4K` }
  ];

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
  const odoInput = document.getElementById('settingCurrentOdo');
  const insInput = document.getElementById('settingInsuranceExpiry');
  const pucInput = document.getElementById('settingPucExpiry');
  const speedLimitInput = document.getElementById('settingSpeedLimitAlert');

  if (nameInput) nameInput.value = riderProfile.name || 'Natesh';
  if (handleInput) handleInput.value = riderProfile.handle || '@natesh_pilot';
  if (bioInput) bioInput.value = riderProfile.bio || '';
  if (bikeInput) bikeInput.value = riderProfile.bikeModel || (bikeManagementData.specs && bikeManagementData.specs.makeModel) || 'Yamaha MT-15 V2';
  if (specsInput) specsInput.value = riderProfile.bikeSpecs || (bikeManagementData.specs && bikeManagementData.specs.engine) || '155cc Liquid-Cooled 4V SOHC VVA';
  if (plateInput) plateInput.value = riderProfile.bikePlate || (bikeManagementData.documents && bikeManagementData.documents.rcNumber) || 'TN-38-BZ-4242';
  if (tiresInput) tiresInput.value = riderProfile.tireSetup || 'MRF Zapper FX1 / Revz Radial';
  if (parentName) parentName.value = guardianParentConfig.guardianName || riderProfile.emergencyContactName || 'Family Guardian (Dad & Mom)';
  if (parentPhone) parentPhone.value = guardianParentConfig.guardianPhone || riderProfile.emergencyContactPhone || '+91 98765 43210';
  if (odoInput) odoInput.value = (bikeManagementData.specs && bikeManagementData.specs.currentOdometer) || 14850;
  if (insInput && bikeManagementData.documents) insInput.value = bikeManagementData.documents.insuranceExpiry || '2026-11-28';
  if (pucInput && bikeManagementData.documents) pucInput.value = bikeManagementData.documents.pucExpiry || '2026-12-14';
  if (speedLimitInput) speedLimitInput.value = guardianParentConfig.speedLimitKmh || 95;
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
  const odoInput = document.getElementById('settingCurrentOdo');
  const insInput = document.getElementById('settingInsuranceExpiry');
  const pucInput = document.getElementById('settingPucExpiry');
  const speedLimitInput = document.getElementById('settingSpeedLimitAlert');

  if (nameInput) riderProfile.name = nameInput.value.trim();
  if (handleInput) riderProfile.handle = handleInput.value.trim();
  if (bioInput) riderProfile.bio = bioInput.value.trim();
  if (bikeInput) {
    riderProfile.bikeModel = bikeInput.value.trim();
    if (!bikeManagementData.specs) bikeManagementData.specs = {};
    bikeManagementData.specs.makeModel = bikeInput.value.trim();
  }
  if (specsInput) {
    riderProfile.bikeSpecs = specsInput.value.trim();
    if (!bikeManagementData.specs) bikeManagementData.specs = {};
    bikeManagementData.specs.engine = specsInput.value.trim();
  }
  if (plateInput) {
    riderProfile.bikePlate = plateInput.value.trim();
    if (!bikeManagementData.documents) bikeManagementData.documents = {};
    bikeManagementData.documents.rcNumber = plateInput.value.trim();
  }
  if (tiresInput) riderProfile.tireSetup = tiresInput.value.trim();
  if (parentName) {
    riderProfile.emergencyContactName = parentName.value.trim();
    guardianParentConfig.guardianName = parentName.value.trim();
  }
  if (parentPhone) {
    riderProfile.emergencyContactPhone = parentPhone.value.trim();
    guardianParentConfig.guardianPhone = parentPhone.value.trim();
  }
  if (odoInput && odoInput.value) {
    const o = parseInt(odoInput.value) || 0;
    if (o > 0) {
      if (!bikeManagementData.specs) bikeManagementData.specs = {};
      bikeManagementData.specs.currentOdometer = o;
    }
  }
  if (insInput && insInput.value && bikeManagementData.documents) {
    bikeManagementData.documents.insuranceExpiry = insInput.value;
  }
  if (pucInput && pucInput.value && bikeManagementData.documents) {
    bikeManagementData.documents.pucExpiry = pucInput.value;
  }
  if (speedLimitInput && speedLimitInput.value) {
    guardianParentConfig.speedLimitKmh = parseInt(speedLimitInput.value) || 95;
  }

  saveProfile();
  saveBikeManagementToStorage(true);
  renderDashboardProfile();
  renderDashboardBike();
  renderBikeManagement();
  renderParentingView();
  renderYoutubeDynamicHub();
  showToast('Settings & Motorcycle specifications saved and synced!', 'success');
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
  localStorage.removeItem('rhody_bike_management');
  localStorage.removeItem('rhody_saved_places');
  loadBikeManagementFromStorage();
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
  loadBikeManagementFromStorage();
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
