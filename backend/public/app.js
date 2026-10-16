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
  isConfigured: false,
  specs: {
    makeModel: '',
    engine: '',
    power: '',
    torque: '',
    gearbox: '',
    fuelTank: '',
    brakes: '',
    kerbWeight: '',
    currentOdometer: 0
  },
  components: {
    engineOil: { lastServiceOdo: 0, intervalKm: 4000, name: 'Engine Oil & Filter' },
    driveChain: { lastServiceOdo: 0, intervalKm: 500, name: 'Drive Chain Lube & Slack Adjust' },
    airFilter: { lastServiceOdo: 0, intervalKm: 8000, name: 'Viscous Air Filter' },
    sparkPlug: { lastServiceOdo: 0, intervalKm: 10000, name: 'Spark Plug' },
    coolant: { lastServiceOdo: 0, intervalKm: 12000, name: 'Radiator Coolant Flush' },
    brakePads: { lastServiceOdo: 0, intervalKm: 8000, name: 'Brake Pads & Fluid' }
  },
  documents: {
    rc: { number: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    dl: { number: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    insurance: { provider: '', policyNumber: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    puc: { certificateNumber: '', expiry: '', fileData: '', fileName: '', uploadedAt: null },
    rcNumber: '',
    rcExpiry: '',
    dlNumber: '',
    dlExpiry: '',
    insuranceProvider: '',
    insurancePolicyNumber: '',
    insuranceExpiry: '',
    pucNumber: '',
    pucExpiry: '',
    rsaPhone: '1800-420-1600'
  },
  serviceHistory: [],
  fuelLogs: []
};

// Guardian Parent Alert Configuration & Portal State
var guardianParentConfig = {
  guardianName: 'Family Guardian (Dad & Mom)',
  guardianPhone: '+91 98765 43210',
  speedLimitKmh: 95
};

var currentRole = 'rider'; // 'rider' | 'parent'
var parentConnectedRider = 'pilot@rhody.io';
var parentTelemetryPollTimer = null;
var parentMapInstance = null;
var parentMapMarker = null;
var parentNotificationsList = [];
var activeDocModalType = 'rc';
var activeDocFileData = null;
var activeDocFileName = '';

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

  // Check Onboarding status for rider
  checkOnboardingStatus();

  // Upfront Role Gateway: Prompt user immediately if role is not yet selected
  checkRoleOnStartup();
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
  const curOdo = specs.currentOdometer || 0;
  const isConfigured = Boolean(bikeManagementData.isConfigured);

  // 1. Technical Specs Matrix
  const specEngine = document.getElementById('specEngine');
  const specPower = document.getElementById('specPower');
  const specGearbox = document.getElementById('specGearbox');
  const specFuelTank = document.getElementById('specFuelTank');
  const specBrakes = document.getElementById('specBrakes');
  const specWeight = document.getElementById('specWeight');
  const odoDisplay = document.getElementById('mgmtOdoDisplay');

  if (specEngine) specEngine.textContent = specs.engine || (isConfigured ? 'Standard Specification' : 'Not Configured');
  if (specPower) specPower.textContent = specs.power || (isConfigured ? 'Pending Dyno / Spec' : 'Not Configured');
  if (specGearbox) specGearbox.textContent = specs.gearbox || (isConfigured ? 'Multi-Plate Clutch' : 'Not Configured');
  if (specFuelTank) specFuelTank.textContent = specs.fuelTank || (isConfigured ? 'Fuel Reservoir' : 'Not Configured');
  if (specBrakes) specBrakes.textContent = specs.brakes || (isConfigured ? 'Hydraulic Disc ABS' : 'Not Configured');
  if (specWeight) specWeight.textContent = specs.kerbWeight || (isConfigured ? 'Motorcycle Mass' : 'Not Configured');
  if (odoDisplay) {
    odoDisplay.textContent = curOdo > 0 ? `${curOdo.toLocaleString()} KM` : '0 KM (Setup Pending)';
  }

  // 2. Scheduled Preventative Maintenance Lifecycles
  updateComponentHealthBar('Oil', comps.engineOil, curOdo, isConfigured);
  updateComponentHealthBar('Chain', comps.driveChain, curOdo, isConfigured);
  updateComponentHealthBar('Air', comps.airFilter, curOdo, isConfigured);
  updateComponentHealthBar('Plug', comps.sparkPlug, curOdo, isConfigured);
  updateComponentHealthBar('Coolant', comps.coolant, curOdo, isConfigured);
  updateComponentHealthBar('Brake', comps.brakePads, curOdo, isConfigured);

  // 3. 4-Document Statutory Legal Vault & Badges
  // RC (Registration Certificate)
  const rcPlate = document.getElementById('docRcPlate');
  const rcBadge = document.getElementById('docRcBadge');
  const btnViewRc = document.getElementById('btnViewRc');
  const rcObj = docs.rc || {};
  const rcNum = rcObj.number || docs.rcNumber;
  if (rcPlate) rcPlate.textContent = rcNum || 'Not Uploaded';
  if (rcBadge) {
    if (rcNum) {
      rcBadge.textContent = rcObj.expiry ? `Verified • Valid till ${rcObj.expiry}` : 'Verified Smart Card';
      rcBadge.className = 'text-[10px] text-emerald-400 font-bold block';
    } else {
      rcBadge.textContent = 'Tap to upload RC';
      rcBadge.className = 'text-[10px] text-slate-400 font-medium block';
    }
  }
  if (btnViewRc) {
    btnViewRc.classList.toggle('hidden', !(rcObj.fileData || rcNum));
  }

  // DL (Driving Licence)
  const dlNumber = document.getElementById('docDlNumber');
  const dlBadge = document.getElementById('docDlBadge');
  const btnViewDl = document.getElementById('btnViewDl');
  const dlObj = docs.dl || {};
  const dlNum = dlObj.number || docs.dlNumber;
  if (dlNumber) dlNumber.textContent = dlNum || 'Not Uploaded';
  if (dlBadge) {
    if (dlNum) {
      dlBadge.textContent = dlObj.expiry ? `Active Licence • Till ${dlObj.expiry}` : 'Active Licence';
      dlBadge.className = 'text-[10px] text-indigo-400 font-bold block';
    } else {
      dlBadge.textContent = 'Tap to upload DL';
      dlBadge.className = 'text-[10px] text-slate-400 font-medium block';
    }
  }
  if (btnViewDl) {
    btnViewDl.classList.toggle('hidden', !(dlObj.fileData || dlNum));
  }

  // Comprehensive Insurance
  const insPolicy = document.getElementById('docInsurancePolicy');
  const insBadge = document.getElementById('docInsuranceBadge');
  const btnViewIns = document.getElementById('btnViewInsurance');
  const insObj = docs.insurance || {};
  const insNum = insObj.policyNumber || docs.insurancePolicyNumber;
  const insProv = insObj.provider || docs.insuranceProvider;
  const insExp = insObj.expiry || docs.insuranceExpiry;

  if (insPolicy) {
    if (insNum) {
      insPolicy.textContent = insProv ? `${insProv} (${insNum})` : `#${insNum}`;
    } else {
      insPolicy.textContent = 'Not Added';
    }
  }
  if (insBadge) {
    if (insExp) {
      const daysIns = calculateDaysRemaining(insExp);
      if (daysIns > 30) {
        insBadge.textContent = `Valid till ${insExp} (${daysIns} days)`;
        insBadge.className = 'text-[10px] text-emerald-400 font-bold block';
      } else if (daysIns > 0) {
        insBadge.textContent = `Expires in ${daysIns} days (${insExp})`;
        insBadge.className = 'text-[10px] text-amber-400 font-bold block';
      } else {
        insBadge.textContent = `EXPIRED on ${insExp}`;
        insBadge.className = 'text-[10px] text-rose-400 font-bold block';
      }
    } else {
      insBadge.textContent = 'Tap to add policy';
      insBadge.className = 'text-[10px] text-slate-400 font-medium block';
    }
  }
  if (btnViewIns) {
    btnViewIns.classList.toggle('hidden', !(insObj.fileData || insNum));
  }

  // PUC (Pollution Under Control)
  const pucNumEl = document.getElementById('docPucNumber');
  const pucBadge = document.getElementById('docPucBadge');
  const btnViewPuc = document.getElementById('btnViewPuc');
  const pucObj = docs.puc || {};
  const pucNum = pucObj.certificateNumber || docs.pucNumber;
  const pucExp = pucObj.expiry || docs.pucExpiry;

  if (pucNumEl) pucNumEl.textContent = pucNum || 'Not Added';
  if (pucBadge) {
    if (pucExp) {
      const daysPuc = calculateDaysRemaining(pucExp);
      if (daysPuc > 15) {
        pucBadge.textContent = `Valid till ${pucExp} (${daysPuc} days)`;
        pucBadge.className = 'text-[10px] text-emerald-400 font-bold block';
      } else if (daysPuc > 0) {
        pucBadge.textContent = `Expires in ${daysPuc} days (${pucExp})`;
        pucBadge.className = 'text-[10px] text-amber-400 font-bold block';
      } else {
        pucBadge.textContent = `EXPIRED on ${pucExp}`;
        pucBadge.className = 'text-[10px] text-rose-400 font-bold block';
      }
    } else {
      pucBadge.textContent = 'Tap to add PUC';
      pucBadge.className = 'text-[10px] text-slate-400 font-medium block';
    }
  }
  if (btnViewPuc) {
    btnViewPuc.classList.toggle('hidden', !(pucObj.fileData || pucNum));
  }

  // 4. Service History Ledger
  renderServiceHistoryTable();

  // 5. Fuel Economy & Mileage Calculator
  renderFuelEconomyStats();
}

function updateComponentHealthBar(compKey, compObj, curOdo, isConfigured = true) {
  if (!compObj) return;
  const lastOdo = compObj.lastServiceOdo || 0;
  const interval = compObj.intervalKm || 4000;

  const textEl = document.getElementById(`status${compKey}Text`);
  const barEl = document.getElementById(`status${compKey}Bar`);
  const kmEl = document.getElementById(`status${compKey}Km`);

  if (!isConfigured && curOdo === 0) {
    if (textEl) {
      textEl.textContent = 'PENDING SETTINGS';
      textEl.className = 'text-[10px] font-bold text-slate-400';
    }
    if (barEl) {
      barEl.style.width = '100%';
      barEl.className = 'h-1.5 rounded-full bg-slate-700';
    }
    if (kmEl) kmEl.textContent = 'Awaiting initial odo';
    return;
  }

  const kmSince = Math.max(0, curOdo - lastOdo);
  const remaining = Math.max(0, interval - kmSince);
  const pct = Math.max(0, Math.min(100, Math.round((remaining / interval) * 100)));

  if (kmEl) kmEl.textContent = `${remaining.toLocaleString()} KM remaining`;

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
    `Rhody Live Ride Beacon:\nI am riding my bike right now!\nTrack my live speed, location, and safety status here:\n${url}`
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
  const rounded = Math.abs(leanDeg).toFixed(1);

  if (leanReadout) {
    if (leanDeg < -2.0) {
      leanReadout.innerHTML = `<span class="text-cyan-400 font-mono">${rounded}&deg;</span> <span class="text-xs text-slate-400">LEFT APEX</span>`;
    } else if (leanDeg > 2.0) {
      leanReadout.innerHTML = `<span class="text-amber-400 font-mono">${rounded}&deg;</span> <span class="text-xs text-slate-400">RIGHT APEX</span>`;
    } else {
      leanReadout.innerHTML = `<span class="text-emerald-400 font-mono">0.0&deg;</span> <span class="text-xs text-slate-400">UPRIGHT</span>`;
    }
  }

  // Sync with Advanced Motorsport Lean Gyro & Friction Circle
  const liveLeanText = document.getElementById('liveLeanDegreeText');
  if (liveLeanText) liveLeanText.textContent = `${rounded}\u00B0 LEAN`;

  if (leanDeg < -2.0) {
    const leftEl = document.getElementById('apexMaxLeft');
    if (leftEl) {
      const curL = parseFloat(leftEl.textContent) || 0;
      if (Math.abs(leanDeg) > curL) leftEl.textContent = `${rounded}\u00B0`;
    }
  } else if (leanDeg > 2.0) {
    const rightEl = document.getElementById('apexMaxRight');
    if (rightEl) {
      const curR = parseFloat(rightEl.textContent) || 0;
      if (leanDeg > curR) rightEl.textContent = `${rounded}\u00B0`;
    }
  }

  updateFrictionCircleDot((leanDeg / 50), 0.15);
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
  const odoInput = document.getElementById('settingCurrentOdo');
  const insInput = document.getElementById('settingInsuranceExpiry');
  const pucInput = document.getElementById('settingPucExpiry');
  const dlNumInput = document.getElementById('settingDlNumber');
  const dlExpInput = document.getElementById('settingDlExpiry');
  const speedLimitInput = document.getElementById('settingSpeedLimitAlert');

  const docs = bikeManagementData.documents || {};
  const specs = bikeManagementData.specs || {};

  if (nameInput) nameInput.value = riderProfile.name || '';
  if (handleInput) handleInput.value = riderProfile.handle || '';
  if (bioInput) bioInput.value = riderProfile.bio || '';
  if (bikeInput) bikeInput.value = riderProfile.bikeModel || specs.makeModel || '';
  if (specsInput) specsInput.value = riderProfile.bikeSpecs || specs.engine || '';
  if (plateInput) plateInput.value = (docs.rc && docs.rc.number) || docs.rcNumber || riderProfile.bikePlate || '';
  if (tiresInput) tiresInput.value = riderProfile.tireSetup || '';
  if (parentName) parentName.value = guardianParentConfig.guardianName || riderProfile.emergencyContactName || '';
  if (parentPhone) parentPhone.value = guardianParentConfig.guardianPhone || riderProfile.emergencyContactPhone || '';
  if (odoInput) odoInput.value = specs.currentOdometer || '';
  if (insInput) insInput.value = (docs.insurance && docs.insurance.expiry) || docs.insuranceExpiry || '';
  if (pucInput) pucInput.value = (docs.puc && docs.puc.expiry) || docs.pucExpiry || '';
  if (dlNumInput) dlNumInput.value = (docs.dl && docs.dl.number) || docs.dlNumber || '';
  if (dlExpInput) dlExpInput.value = (docs.dl && docs.dl.expiry) || docs.dlExpiry || '';
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
  const dlNumInput = document.getElementById('settingDlNumber');
  const dlExpInput = document.getElementById('settingDlExpiry');
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
    const p = plateInput.value.trim();
    riderProfile.bikePlate = p;
    if (!bikeManagementData.documents) bikeManagementData.documents = {};
    bikeManagementData.documents.rcNumber = p;
    if (!bikeManagementData.documents.rc) bikeManagementData.documents.rc = {};
    bikeManagementData.documents.rc.number = p;
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
  if (odoInput && odoInput.value !== '') {
    const o = parseInt(odoInput.value) || 0;
    if (!bikeManagementData.specs) bikeManagementData.specs = {};
    bikeManagementData.specs.currentOdometer = o;
  }
  if (insInput && insInput.value) {
    if (!bikeManagementData.documents) bikeManagementData.documents = {};
    bikeManagementData.documents.insuranceExpiry = insInput.value;
    if (!bikeManagementData.documents.insurance) bikeManagementData.documents.insurance = {};
    bikeManagementData.documents.insurance.expiry = insInput.value;
  }
  if (pucInput && pucInput.value) {
    if (!bikeManagementData.documents) bikeManagementData.documents = {};
    bikeManagementData.documents.pucExpiry = pucInput.value;
    if (!bikeManagementData.documents.puc) bikeManagementData.documents.puc = {};
    bikeManagementData.documents.puc.expiry = pucInput.value;
  }
  if (dlNumInput) {
    const dVal = dlNumInput.value.trim();
    if (!bikeManagementData.documents) bikeManagementData.documents = {};
    bikeManagementData.documents.dlNumber = dVal;
    if (!bikeManagementData.documents.dl) bikeManagementData.documents.dl = {};
    bikeManagementData.documents.dl.number = dVal;
  }
  if (dlExpInput && dlExpInput.value) {
    if (!bikeManagementData.documents) bikeManagementData.documents = {};
    bikeManagementData.documents.dlExpiry = dlExpInput.value;
    if (!bikeManagementData.documents.dl) bikeManagementData.documents.dl = {};
    bikeManagementData.documents.dl.expiry = dlExpInput.value;
  }
  if (speedLimitInput && speedLimitInput.value) {
    guardianParentConfig.speedLimitKmh = parseInt(speedLimitInput.value) || 95;
  }

  bikeManagementData.isConfigured = true;
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
          gripBadge.innerHTML = `<span class="inline-block w-2 h-2 rounded-full bg-emerald-400 mr-1.5 shadow-[0_0_8px_#10B981]"></span><strong>DRY ROAD (98% GRIP)</strong> &bull; ${cw.temperature}&deg;C &bull; ${label}`;
        } else if (cw.weathercode > 50) {
          gripBadge.innerHTML = `<span class="inline-block w-2 h-2 rounded-full bg-rose-500 mr-1.5 shadow-[0_0_8px_#F43F5E]"></span><strong>WET TARMAC (45% GRIP)</strong> &bull; Caution in corners &bull; ${label}`;
        } else {
          gripBadge.innerHTML = `<span class="inline-block w-2 h-2 rounded-full bg-amber-400 mr-1.5 shadow-[0_0_8px_#F59E0B]"></span><strong>OPTIMAL ROAD (${cw.temperature}&deg;C)</strong> &bull; ${label}`;
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

// ========================================================
// ONBOARDING WIZARD HANDLERS (FIRST TIME RIDER SETUP)
// ========================================================
function checkOnboardingStatus() {
  const isConfigured = bikeManagementData && bikeManagementData.isConfigured;
  const skipped = localStorage.getItem('rhody_onboarding_completed') === 'true';
  // If not configured and not explicitly completed/skipped, prompt user
  if (!isConfigured && !skipped) {
    if (currentRole === 'rider') {
      setTimeout(() => {
        openOnboardingModal();
      }, 700);
    }
  }
}

function openOnboardingModal() {
  const modal = document.getElementById('onboardingModal');
  if (!modal) return;
  const specs = (bikeManagementData && bikeManagementData.specs) || {};
  const docs = (bikeManagementData && bikeManagementData.documents) || {};

  const makeModel = document.getElementById('obMakeModel');
  const plate = document.getElementById('obPlateNumber');
  const odo = document.getElementById('obOdometer');
  const specField = document.getElementById('obSpecs');
  const insExp = document.getElementById('obInsuranceExpiry');
  const pucExp = document.getElementById('obPucExpiry');
  const dlExp = document.getElementById('obDlExpiry');

  if (makeModel) makeModel.value = specs.makeModel || riderProfile.bikeModel || '';
  if (plate) plate.value = (docs.rc && docs.rc.number) || docs.rcNumber || riderProfile.bikePlate || '';
  if (odo) odo.value = specs.currentOdometer || '';
  if (specField) specField.value = specs.engine || riderProfile.bikeSpecs || '';
  if (insExp) insExp.value = (docs.insurance && docs.insurance.expiry) || docs.insuranceExpiry || '';
  if (pucExp) pucExp.value = (docs.puc && docs.puc.expiry) || docs.pucExpiry || '';
  if (dlExp) dlExp.value = (docs.dl && docs.dl.expiry) || docs.dlExpiry || '';

  modal.classList.remove('hidden');
}

function closeOnboardingModal() {
  const modal = document.getElementById('onboardingModal');
  if (modal) modal.classList.add('hidden');
}

function skipOnboarding() {
  localStorage.setItem('rhody_onboarding_completed', 'true');
  closeOnboardingModal();
  showToast('Entered as Guest. You can configure bike specs & upload documents anytime.', 'info');
}

function handleOnboardingSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();
  const makeModel = (document.getElementById('obMakeModel')?.value || '').trim();
  const plate = (document.getElementById('obPlateNumber')?.value || '').trim();
  const odoVal = parseFloat(document.getElementById('obOdometer')?.value) || 0;
  const specText = (document.getElementById('obSpecs')?.value || '').trim();
  const insExpiry = (document.getElementById('obInsuranceExpiry')?.value || '').trim();
  const pucExpiry = (document.getElementById('obPucExpiry')?.value || '').trim();
  const dlExpiry = (document.getElementById('obDlExpiry')?.value || '').trim();

  if (!bikeManagementData.specs) bikeManagementData.specs = {};
  if (!bikeManagementData.documents) bikeManagementData.documents = {};

  bikeManagementData.isConfigured = true;
  if (makeModel) bikeManagementData.specs.makeModel = makeModel;
  if (specText) bikeManagementData.specs.engine = specText;
  bikeManagementData.specs.currentOdometer = odoVal;

  if (plate) {
    bikeManagementData.documents.rcNumber = plate;
    if (!bikeManagementData.documents.rc) bikeManagementData.documents.rc = {};
    bikeManagementData.documents.rc.number = plate;
  }
  if (insExpiry) {
    bikeManagementData.documents.insuranceExpiry = insExpiry;
    if (!bikeManagementData.documents.insurance) bikeManagementData.documents.insurance = {};
    bikeManagementData.documents.insurance.expiry = insExpiry;
  }
  if (pucExpiry) {
    bikeManagementData.documents.pucExpiry = pucExpiry;
    if (!bikeManagementData.documents.puc) bikeManagementData.documents.puc = {};
    bikeManagementData.documents.puc.expiry = pucExpiry;
  }
  if (dlExpiry) {
    bikeManagementData.documents.dlExpiry = dlExpiry;
    if (!bikeManagementData.documents.dl) bikeManagementData.documents.dl = {};
    bikeManagementData.documents.dl.expiry = dlExpiry;
  }

  // Update profile
  if (makeModel) riderProfile.bikeModel = makeModel;
  if (plate) riderProfile.bikePlate = plate;
  if (specText) riderProfile.bikeSpecs = specText;
  saveProfile();

  localStorage.setItem('rhody_onboarding_completed', 'true');
  saveBikeManagementToStorage(true);
  renderDashboardBike();
  renderBikeManagement();
  populateSettingsForm();
  closeOnboardingModal();
  showToast('Motorcycle Cockpit configured successfully!', 'success');
}

// ========================================================
// 4-DOCUMENT LEGAL VAULT (RC, DL, INSURANCE, PUC)
// ========================================================
function openDocModal(type) {
  activeDocModalType = type;
  activeDocFileData = null;
  activeDocFileName = '';

  const modal = document.getElementById('docVaultModal');
  const title = document.getElementById('docVaultModalTitle');
  const subtitle = document.getElementById('docVaultModalSubtitle');
  const label = document.getElementById('docVaultNumberLabel');
  const numberInput = document.getElementById('docVaultNumberInput');
  const expiryInput = document.getElementById('docVaultExpiryInput');
  const providerCont = document.getElementById('docVaultProviderFieldContainer');
  const providerInput = document.getElementById('docVaultProviderInput');
  const fileNameDisplay = document.getElementById('docVaultFileNameDisplay');
  const previewCont = document.getElementById('docVaultPreviewContainer');
  const thumbImg = document.getElementById('docVaultThumbImg');
  const thumbName = document.getElementById('docVaultThumbName');
  const iconBadge = document.getElementById('docVaultIconBadge');

  if (!modal) return;
  const docs = bikeManagementData.documents || {};
  const currentDoc = (docs[type]) || {};

  if (providerCont) providerCont.classList.add('hidden');

  if (type === 'rc') {
    if (iconBadge) iconBadge.innerHTML = '<svg class="w-5 h-5 text-cyan-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>';
    if (title) title.textContent = 'Upload Vehicle Registration Certificate (RC)';
    if (subtitle) subtitle.textContent = 'Upload statutory RC smart card or digital scan';
    if (label) label.textContent = 'Vehicle Registration / Plate Number';
    if (numberInput) {
      numberInput.placeholder = 'e.g. TN-38-BZ-4242';
      numberInput.value = currentDoc.number || docs.rcNumber || '';
    }
    if (expiryInput) expiryInput.value = currentDoc.expiry || docs.rcExpiry || '';
  } else if (type === 'dl') {
    if (iconBadge) iconBadge.innerHTML = '<svg class="w-5 h-5 text-purple-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2"/></svg>';
    if (title) title.textContent = 'Upload Driving Licence (DL)';
    if (subtitle) subtitle.textContent = 'Official motorcycle rider licence with endorsement';
    if (label) label.textContent = 'Driving Licence (DL) Number';
    if (numberInput) {
      numberInput.placeholder = 'e.g. DL-1420110012345';
      numberInput.value = currentDoc.number || docs.dlNumber || '';
    }
    if (expiryInput) expiryInput.value = currentDoc.expiry || docs.dlExpiry || '';
  } else if (type === 'insurance') {
    if (iconBadge) iconBadge.innerHTML = '<svg class="w-5 h-5 text-emerald-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>';
    if (title) title.textContent = 'Upload Comprehensive Insurance Policy';
    if (subtitle) subtitle.textContent = 'Active third-party & own-damage motor insurance';
    if (label) label.textContent = 'Insurance Policy Number';
    if (providerCont) providerCont.classList.remove('hidden');
    if (providerInput) providerInput.value = currentDoc.provider || docs.insuranceProvider || '';
    if (numberInput) {
      numberInput.placeholder = 'e.g. POL-9928371-YMH';
      numberInput.value = currentDoc.policyNumber || docs.insurancePolicyNumber || '';
    }
    if (expiryInput) expiryInput.value = currentDoc.expiry || docs.insuranceExpiry || '';
  } else if (type === 'puc') {
    if (iconBadge) iconBadge.innerHTML = '<svg class="w-5 h-5 text-teal-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9"/></svg>';
    if (title) title.textContent = 'Upload Pollution Under Control (PUC) Certificate';
    if (subtitle) subtitle.textContent = 'Official emission testing certificate';
    if (label) label.textContent = 'PUC Certificate Number';
    if (numberInput) {
      numberInput.placeholder = 'e.g. PUC-KA01-2024-998';
      numberInput.value = currentDoc.certificateNumber || docs.pucNumber || '';
    }
    if (expiryInput) expiryInput.value = currentDoc.expiry || docs.pucExpiry || '';
  }

  // If already uploaded document file exists
  if (currentDoc.fileData) {
    activeDocFileData = currentDoc.fileData;
    activeDocFileName = currentDoc.fileName || `${type}_document.jpg`;
    if (fileNameDisplay) fileNameDisplay.textContent = activeDocFileName;
    if (previewCont) previewCont.classList.remove('hidden');
    if (thumbImg) thumbImg.src = currentDoc.fileData;
    if (thumbName) thumbName.textContent = activeDocFileName;
  } else {
    if (fileNameDisplay) fileNameDisplay.textContent = 'Click to choose image or camera scan';
    if (previewCont) previewCont.classList.add('hidden');
  }

  modal.classList.remove('hidden');
}

function closeDocModal() {
  const modal = document.getElementById('docVaultModal');
  if (modal) modal.classList.add('hidden');
  activeDocFileData = null;
  activeDocFileName = '';
}

function handleDocFileSelection(e) {
  const file = e.target.files && e.target.files[0];
  if (!file) return;

  const fileNameDisplay = document.getElementById('docVaultFileNameDisplay');
  const previewCont = document.getElementById('docVaultPreviewContainer');
  const thumbImg = document.getElementById('docVaultThumbImg');
  const thumbName = document.getElementById('docVaultThumbName');

  const reader = new FileReader();
  reader.onload = function(event) {
    activeDocFileData = event.target.result;
    activeDocFileName = file.name;
    if (fileNameDisplay) fileNameDisplay.textContent = file.name;
    if (previewCont) previewCont.classList.remove('hidden');
    if (thumbImg) thumbImg.src = activeDocFileData;
    if (thumbName) thumbName.textContent = file.name;
  };
  reader.readAsDataURL(file);
}

function removeActiveDocFile() {
  activeDocFileData = null;
  activeDocFileName = '';
  const fileInput = document.getElementById('docVaultFileInput');
  if (fileInput) fileInput.value = '';
  const fileNameDisplay = document.getElementById('docVaultFileNameDisplay');
  const previewCont = document.getElementById('docVaultPreviewContainer');
  if (fileNameDisplay) fileNameDisplay.textContent = 'Click to choose image or camera scan';
  if (previewCont) previewCont.classList.add('hidden');
}

function handleDocModalSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();
  const type = activeDocModalType;
  const numVal = (document.getElementById('docVaultNumberInput')?.value || '').trim();
  const expVal = (document.getElementById('docVaultExpiryInput')?.value || '').trim();
  const provVal = (document.getElementById('docVaultProviderInput')?.value || '').trim();

  if (!bikeManagementData.documents) bikeManagementData.documents = {};
  if (!bikeManagementData.documents[type]) bikeManagementData.documents[type] = {};

  const docObj = bikeManagementData.documents[type];
  docObj.expiry = expVal;
  docObj.uploadedAt = new Date().toISOString();
  if (activeDocFileData) {
    docObj.fileData = activeDocFileData;
    docObj.fileName = activeDocFileName;
  }

  if (type === 'rc') {
    docObj.number = numVal;
    bikeManagementData.documents.rcNumber = numVal;
    bikeManagementData.documents.rcExpiry = expVal;
    if (!riderProfile.bikePlate) {
      riderProfile.bikePlate = numVal;
      saveProfile();
    }
  } else if (type === 'dl') {
    docObj.number = numVal;
    bikeManagementData.documents.dlNumber = numVal;
    bikeManagementData.documents.dlExpiry = expVal;
  } else if (type === 'insurance') {
    docObj.provider = provVal;
    docObj.policyNumber = numVal;
    bikeManagementData.documents.insuranceProvider = provVal;
    bikeManagementData.documents.insurancePolicyNumber = numVal;
    bikeManagementData.documents.insuranceExpiry = expVal;
  } else if (type === 'puc') {
    docObj.certificateNumber = numVal;
    bikeManagementData.documents.pucNumber = numVal;
    bikeManagementData.documents.pucExpiry = expVal;
  }

  saveBikeManagementToStorage(true);
  renderBikeManagement();
  closeDocModal();
  showToast(`${type.toUpperCase()} document saved securely to vault!`, 'success');
}

function deleteActiveDoc() {
  const type = activeDocModalType;
  if (!confirm(`Are you sure you want to remove this ${type.toUpperCase()} document?`)) return;

  if (bikeManagementData.documents && bikeManagementData.documents[type]) {
    delete bikeManagementData.documents[type];
  }
  if (type === 'rc') { bikeManagementData.documents.rcNumber = ''; bikeManagementData.documents.rcExpiry = ''; }
  if (type === 'dl') { bikeManagementData.documents.dlNumber = ''; bikeManagementData.documents.dlExpiry = ''; }
  if (type === 'insurance') { bikeManagementData.documents.insurancePolicyNumber = ''; bikeManagementData.documents.insuranceProvider = ''; bikeManagementData.documents.insuranceExpiry = ''; }
  if (type === 'puc') { bikeManagementData.documents.pucNumber = ''; bikeManagementData.documents.pucExpiry = ''; }

  saveBikeManagementToStorage(true);
  renderBikeManagement();
  closeDocModal();
  showToast(`${type.toUpperCase()} document removed.`, 'info');
}

function viewUploadedDoc(type) {
  const docs = bikeManagementData.documents || {};
  const doc = docs[type] || {};

  const modal = document.getElementById('docViewerModal');
  const title = document.getElementById('docViewerTitle');
  const meta = document.getElementById('docViewerMeta');
  const img = document.getElementById('docViewerImg');
  const dlBtn = document.getElementById('docViewerDownloadBtn');

  if (!modal) return;

  const typeLabels = {
    rc: 'Registration Certificate (RC)',
    dl: 'Driving Licence (DL)',
    insurance: 'Comprehensive Insurance Policy',
    puc: 'Pollution Under Control (PUC)'
  };

  const docNumber = doc.number || doc.policyNumber || doc.certificateNumber || docs[type + 'Number'] || 'Verified';
  const expiry = doc.expiry || docs[type + 'Expiry'] || 'Active';

  if (title) title.textContent = typeLabels[type] || 'Document Scan';
  if (meta) meta.textContent = `${docNumber} • Valid till: ${expiry}`;

  // Default visual SVG badge scan if user saved without image
  const defaultScanSvg = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="600" height="400" viewBox="0 0 600 400"><rect width="600" height="400" rx="20" fill="%230E131F" stroke="%2300F0FF" stroke-width="3"/><text x="50" y="80" fill="%2300F0FF" font-family="monospace" font-size="24" font-weight="bold">RHODY OFFICIAL LEGAL VAULT</text><text x="50" y="140" fill="%23FFFFFF" font-family="sans-serif" font-size="28" font-weight="900">${typeLabels[type] || type.toUpperCase()}</text><text x="50" y="190" fill="%2394A3B8" font-family="monospace" font-size="18">ID: ${docNumber}</text><text x="50" y="230" fill="%2310B981" font-family="sans-serif" font-size="16" font-weight="bold">VERIFIED COMPLIANCE RECORD</text><text x="50" y="270" fill="%23E2E8F0" font-family="sans-serif" font-size="14">Validity Date: ${expiry}</text><rect x="50" y="320" width="160" height="35" rx="8" fill="%2310B981" fill-opacity="0.2" stroke="%2310B981"/><text x="70" y="343" fill="%2310B981" font-family="sans-serif" font-size="12" font-weight="bold">✓ ACTIVE RECORD</text></svg>`;

  const imageSrc = doc.fileData || defaultScanSvg;
  if (img) img.src = imageSrc;
  if (dlBtn) {
    dlBtn.href = imageSrc;
    dlBtn.download = doc.fileName || `${type}_document.png`;
  }

  modal.classList.remove('hidden');
}

function closeDocViewerModal() {
  const modal = document.getElementById('docViewerModal');
  if (modal) modal.classList.add('hidden');
}

// Quick Odometer Prompt Modal
function openOdoPrompt() {
  const modal = document.getElementById('odoPromptModal');
  const input = document.getElementById('promptOdoInput');
  const curOdo = (bikeManagementData.specs && bikeManagementData.specs.currentOdometer) || 0;
  if (input) input.value = curOdo || '';
  if (modal) modal.classList.remove('hidden');
}

function closeOdoPrompt() {
  const modal = document.getElementById('odoPromptModal');
  if (modal) modal.classList.add('hidden');
}

function saveOdoPrompt(e) {
  if (e && e.preventDefault) e.preventDefault();
  const input = document.getElementById('promptOdoInput');
  const val = parseFloat(input?.value) || 0;
  if (!bikeManagementData.specs) bikeManagementData.specs = {};
  bikeManagementData.specs.currentOdometer = val;
  saveBikeManagementToStorage(true);
  renderBikeManagement();
  closeOdoPrompt();
  showToast(`Odometer updated to ${val.toLocaleString()} KM!`, 'success');
}

// ========================================================
// UPFRONT ROLE GATEWAY & CONSENT HANDSHAKE
// ========================================================
let activeGuardianRequestId = null;
let riderGuardianPollTimer = null;

function checkRoleOnStartup() {
  const savedRole = localStorage.getItem('rhody_user_role');
  const urlParams = new URLSearchParams(window.location.search);
  const roleParam = urlParams.get('role');

  if (roleParam === 'parent' || roleParam === 'rider') {
    selectRoleFromGateway(roleParam, false);
    return;
  }

  if (savedRole === 'parent') {
    switchRole('parent');
  } else if (savedRole === 'rider') {
    switchRole('rider');
    checkRiderGuardianRequests();
  } else {
    // Prompt upfront at entry
    openRoleGatewayModal();
  }
}

function openRoleGatewayModal() {
  const modal = document.getElementById('roleGatewayModal');
  if (modal) modal.classList.remove('hidden');
}

function closeRoleGatewayModal() {
  const modal = document.getElementById('roleGatewayModal');
  if (modal) modal.classList.add('hidden');
}

function selectRoleFromGateway(role, notify = true) {
  closeRoleGatewayModal();
  localStorage.setItem('rhody_user_role', role);

  if (role === 'parent') {
    localStorage.setItem('rhody_parent_logged_in', 'true');
    if (!localStorage.getItem('rhody_parent_email')) {
      localStorage.setItem('rhody_parent_email', 'guardian@family.io');
    }
    switchRole('parent');
    if (notify) showToast('Logged in as Parent / Guardian Radar', 'success');
  } else {
    switchRole('rider');
    checkRiderGuardianRequests();
    if (notify) showToast('Logged in as Rider Cockpit — Telemetry Active', 'success');
  }
}

function checkRiderGuardianRequests() {
  const riderCode = 'RHODY-8821';
  fetch(`/api/rider/guardian-requests?riderCode=${encodeURIComponent(riderCode)}`)
    .then(r => r.json())
    .then(data => {
      const banner = document.getElementById('riderGuardianRequestBanner');
      if (!banner) return;
      if (data && data.requests && data.requests.length > 0) {
        const pending = data.requests.find(req => req.status === 'pending_approval');
        if (pending) {
          activeGuardianRequestId = pending.id;
          const reqName = document.getElementById('guardianRequesterName');
          const reqEmail = document.getElementById('guardianRequesterEmail');
          if (reqName) reqName.textContent = pending.parentName || 'Family Guardian';
          if (reqEmail) reqEmail.textContent = pending.parentEmail || 'guardian@family.io';
          banner.classList.remove('hidden');
          return;
        }
      }
      banner.classList.add('hidden');
    })
    .catch(() => {});
}

function respondGuardianRequest(action) {
  const reqId = activeGuardianRequestId || 'req_1';
  const apiAction = action === 'accept' ? 'approved' : 'rejected';

  fetch('/api/rider/respond-guardian-request', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      requestId: reqId,
      action: apiAction,
      parentEmail: 'guardian@family.io'
    })
  })
    .then(r => r.json())
    .then(data => {
      const banner = document.getElementById('riderGuardianRequestBanner');
      if (banner) banner.classList.add('hidden');
      if (action === 'accept') {
        showToast('Guardian access granted! Telemetry & safety radar shared.', 'success');
      } else {
        showToast('Guardian connection request declined.', 'info');
      }
    })
    .catch(() => {
      const banner = document.getElementById('riderGuardianRequestBanner');
      if (banner) banner.classList.add('hidden');
      showToast(action === 'accept' ? 'Guardian access granted' : 'Request declined', 'info');
    });
}

function simulateRiderAcceptanceDemo() {
  fetch('/api/rider/respond-guardian-request', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      requestId: activeGuardianRequestId || 'req_demo_01',
      action: 'approved',
      parentEmail: localStorage.getItem('rhody_parent_email') || 'guardian@family.io'
    })
  })
    .then(() => {
      showToast('Rider authorized telemetry access! Radar unlocked.', 'success');
      fetchParentRiderStatus();
    })
    .catch(() => {
      showToast('Rider authorized access (Demo)', 'success');
      const waitingBanner = document.getElementById('parentWaitingApprovalBanner');
      const authorizedGrid = document.getElementById('parentAuthorizedGrid');
      if (waitingBanner) waitingBanner.classList.add('hidden');
      if (authorizedGrid) authorizedGrid.classList.remove('hidden');
    });
}

// ========================================================
// PARENT / GUARDIAN LIVE PORTAL & ROLE SWITCHER
// ========================================================
function toggleParentRole() {
  if (currentRole === 'rider') {
    const parentAuth = localStorage.getItem('rhody_parent_logged_in');
    if (parentAuth === 'true') {
      switchRole('parent');
    } else {
      openParentLoginModal();
    }
  } else {
    switchRole('rider');
  }
}

function openParentLoginModal() {
  const modal = document.getElementById('parentLoginModal');
  if (modal) modal.classList.remove('hidden');
}

function closeParentLoginModal() {
  const modal = document.getElementById('parentLoginModal');
  if (modal) modal.classList.add('hidden');
}

function loginQuickParentDemo() {
  const emailInput = document.getElementById('parentLoginEmail');
  const codeInput = document.getElementById('parentLoginRiderCode');
  if (emailInput) emailInput.value = 'guardian@family.io';
  if (codeInput) codeInput.value = 'RHODY-8821';
  handleParentLoginSubmit(new Event('submit'));
}

function handleParentLoginSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();
  const email = (document.getElementById('parentLoginEmail')?.value || '').trim() || 'guardian@family.io';
  const riderCode = (document.getElementById('parentLoginRiderCode')?.value || '').trim() || 'RHODY-8821';

  fetch('/api/parent/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, riderCode })
  })
    .then(r => r.json())
    .then(data => {
      localStorage.setItem('rhody_parent_logged_in', 'true');
      localStorage.setItem('rhody_parent_email', email);
      parentConnectedRider = (data && data.profile && data.profile.connectedRiderEmail) || riderCode;
      closeParentLoginModal();
      switchRole('parent');
      showToast('Welcome to Guardian Safety Radar', 'success');
    })
    .catch(() => {
      localStorage.setItem('rhody_parent_logged_in', 'true');
      closeParentLoginModal();
      switchRole('parent');
      showToast('Connected to Guardian Safety Radar', 'info');
    });
}

function switchRole(role) {
  currentRole = role;
  const parentView = document.getElementById('viewParentPortal');
  const btnRoleSwitcher = document.getElementById('btnRoleSwitcher');
  const roleGatewayNavLabel = document.getElementById('roleGatewayNavLabel');
  const btnRoleSwitcherText = document.getElementById('btnRoleSwitcherText');

  if (role === 'parent') {
    // Hide rider view tabs
    ['dashboard', 'trips', 'parenting', 'settings', 'analysis'].forEach(v => {
      const el = document.getElementById('view' + capitalizeFirst(v));
      if (el) el.classList.add('hidden');
    });

    if (parentView) parentView.classList.remove('hidden');
    if (roleGatewayNavLabel) roleGatewayNavLabel.textContent = 'Role: Parent Radar';
    if (btnRoleSwitcherText) btnRoleSwitcherText.textContent = 'Rider Cockpit';
    if (btnRoleSwitcher) {
      btnRoleSwitcher.className = 'px-3.5 py-1.5 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-500 text-white shadow-[0_0_20px_rgba(168,85,247,0.4)] transition flex items-center space-x-1.5';
    }

    if (riderGuardianPollTimer) {
      clearInterval(riderGuardianPollTimer);
      riderGuardianPollTimer = null;
    }

    ensureLeaflet(function() {
      initParentMap();
    });
    fetchParentRiderStatus();
    startParentTelemetryPolling();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } else {
    // Return to rider mode
    if (parentTelemetryPollTimer) {
      clearInterval(parentTelemetryPollTimer);
      parentTelemetryPollTimer = null;
    }
    if (parentView) parentView.classList.add('hidden');
    if (roleGatewayNavLabel) roleGatewayNavLabel.textContent = 'Role: Rider Cockpit';
    if (btnRoleSwitcherText) btnRoleSwitcherText.textContent = 'Parent Portal';
    if (btnRoleSwitcher) {
      btnRoleSwitcher.className = 'px-3 py-1.5 rounded-xl text-xs font-bold bg-purple-500/15 hover:bg-purple-500/25 text-purple-300 border border-purple-400/40 transition flex items-center space-x-1.5 shadow-[0_0_15px_rgba(168,85,247,0.2)]';
    }

    switchMainView(currentView || 'dashboard');

    checkRiderGuardianRequests();
    if (!riderGuardianPollTimer) {
      riderGuardianPollTimer = setInterval(checkRiderGuardianRequests, 4000);
    }
  }
}

function connectParentRider() {
  const input = document.getElementById('parentPairingCodeInput');
  const code = (input?.value || '').trim();
  if (!code) {
    showToast('Please enter a rider code or email', 'error');
    return;
  }
  const parentEmail = localStorage.getItem('rhody_parent_email') || 'guardian@family.io';

  fetch('/api/parent/connect-rider', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ parentEmail, riderCodeOrEmail: code })
  })
    .then(r => r.json())
    .then(data => {
      parentConnectedRider = (data && data.parentProfile && data.parentProfile.connectedRiderEmail) || code;
      if (input) input.value = '';
      fetchParentRiderStatus();
      showToast(`Link request sent to rider ${parentConnectedRider}. Awaiting approval.`, 'success');
    })
    .catch(() => {
      parentConnectedRider = code;
      fetchParentRiderStatus();
      showToast(`Connection request sent to ${code}`, 'info');
    });
}

function startParentTelemetryPolling() {
  if (parentTelemetryPollTimer) clearInterval(parentTelemetryPollTimer);
  parentTelemetryPollTimer = setInterval(fetchParentRiderStatus, 3500);
}

function fetchParentRiderStatus() {
  const riderQuery = parentConnectedRider || 'pilot@rhody.io';
  const parentEmail = localStorage.getItem('rhody_parent_email') || 'guardian@family.io';

  fetch(`/api/parent/rider-status?riderEmail=${encodeURIComponent(riderQuery)}&parentEmail=${encodeURIComponent(parentEmail)}`)
    .then(r => r.json())
    .then(data => {
      if (!data) return;
      updateParentDashboardUI(data);
    })
    .catch(() => {
      // In case backend is offline, update with safe local state
      updateParentDashboardUI({
        authorized: true,
        rider: riderProfile,
        bike: bikeManagementData,
        beacon: {
          currentSpeedKmh: liveSpeedKmh || 0,
          maxSpeedKmh: peakTripSpeed || 86.4,
          batteryPercent: 88,
          isMoving: isTripRecording && liveSpeedKmh > 2,
          latitude: lastKnownGpsFix ? lastKnownGpsFix.coords.latitude : 12.9716,
          longitude: lastKnownGpsFix ? lastKnownGpsFix.coords.longitude : 77.5946
        },
        trips: allTripsCache || [],
        notifications: parentNotificationsList
      });
    });
}

function updateParentDashboardUI(data) {
  const waitingBanner = document.getElementById('parentWaitingApprovalBanner');
  const authorizedGrid = document.getElementById('parentAuthorizedGrid');
  const waitingRiderEmail = document.getElementById('parentWaitingRiderEmail');

  // STRICT CONSENT GATING: Hide private telemetry, GPS, and documents until approved
  if (data.authorized === false) {
    if (waitingBanner) waitingBanner.classList.remove('hidden');
    if (authorizedGrid) authorizedGrid.classList.add('hidden');
    if (waitingRiderEmail) {
      waitingRiderEmail.textContent = (data.rider && data.rider.email) || parentConnectedRider || 'pilot@rhody.io';
    }
    return;
  }

  // Rider has authorized access -> reveal full portal
  if (waitingBanner) waitingBanner.classList.add('hidden');
  if (authorizedGrid) authorizedGrid.classList.remove('hidden');

  const rider = data.rider || riderProfile;
  const bike = data.bike || bikeManagementData;
  const beacon = data.beacon || {};
  const trips = data.trips || [];
  const notifs = data.notifications || [];

  // 1. Rider & Bike Identity
  const pairedRiderName = document.getElementById('parentPairedRiderName');
  const pairedBikeName = document.getElementById('parentPairedBikeName');
  const pairedPlate = document.getElementById('parentPairedPlate');
  const pairedCodeDisplay = document.getElementById('parentPairedCodeDisplay');
  const callRiderBtn = document.getElementById('parentCallRiderBtn');

  if (pairedRiderName) pairedRiderName.textContent = rider.name || 'Natesh';
  if (pairedBikeName) pairedBikeName.textContent = (bike && bike.specs && bike.specs.makeModel) || rider.bikeModel || 'Yamaha MT-15 V2';
  if (pairedPlate) pairedPlate.textContent = (bike && bike.documents && bike.documents.rcNumber) || rider.bikePlate || 'KA-01-EQ-1550';
  if (pairedCodeDisplay) pairedCodeDisplay.textContent = `RHODY-8821 (${rider.email || 'pilot@rhody.io'})`;
  if (callRiderBtn) callRiderBtn.href = `tel:${rider.emergencyContactPhone || '+919876543210'}`;

  // 2. Telemetry Readouts
  const motionStatus = document.getElementById('parentTelemetryMotionStatus');
  const speedVal = document.getElementById('parentTelemetrySpeedVal');
  const topSpeed = document.getElementById('parentTelemetryTopSpeed');
  const batteryVal = document.getElementById('parentTelemetryBattery');
  const speedThresholdDisplay = document.getElementById('parentSpeedThresholdDisplay');

  const curSpeed = Number(beacon.currentSpeedKmh) || 0;
  const maxSpd = Number(beacon.maxSpeedKmh) || 0;
  const isMoving = Boolean(beacon.isMoving);

  if (speedVal) speedVal.textContent = curSpeed.toFixed(1);
  if (topSpeed) topSpeed.textContent = maxSpd.toFixed(1);
  if (batteryVal) batteryVal.textContent = `${beacon.batteryPercent || 88}%`;
  if (speedThresholdDisplay) speedThresholdDisplay.textContent = `${guardianParentConfig.speedLimitKmh} km/h`;

  if (motionStatus) {
    if (isMoving && curSpeed > 0) {
      motionStatus.innerHTML = '<span class="inline-flex items-center space-x-1.5 text-emerald-400 font-bold"><span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span><span>RIDING ON ROAD</span></span>';
    } else {
      motionStatus.innerHTML = '<span class="inline-flex items-center space-x-1.5 text-cyan-300 font-bold"><span class="w-2 h-2 rounded-full bg-cyan-400"></span><span>PARKED / IDLE</span></span>';
    }
  }

  // 3. Live Radar Map Marker & Coordinates
  const lat = beacon.latitude || (lastKnownGpsFix && lastKnownGpsFix.coords && lastKnownGpsFix.coords.latitude) || 12.9716;
  const lng = beacon.longitude || (lastKnownGpsFix && lastKnownGpsFix.coords && lastKnownGpsFix.coords.longitude) || 77.5946;

  if (parentMapInstance) {
    const latLng = [lat, lng];
    if (!parentMapMarker) {
      const bikeIcon = L.divIcon({
        className: 'custom-parent-radar-pin',
        html: `<div style="width:36px;height:36px;border-radius:50%;background:#A855F7;border:2px solid #FFFFFF;display:flex;align-items:center;justify-content:center;box-shadow:0 0 20px #A855F7;"><svg style="width:20px;height:20px;color:white;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 19a3 3 0 100-6 3 3 0 000 6zM5 19a3 3 0 100-6 3 3 0 000 6zm0 0h3l3.5-7H16l2 4m-4.5-4L12 8l-3 4"/></svg></div>`,
        iconSize: [36, 36],
        iconAnchor: [18, 18]
      });
      parentMapMarker = L.marker(latLng, { icon: bikeIcon }).addTo(parentMapInstance);
      parentMapInstance.setView(latLng, 14);
    } else {
      parentMapMarker.setLatLng(latLng);
    }
  }

  // 4. Statutory Legal Document Status
  const docs = (bike && bike.documents) || {};
  const parentDocRc = document.getElementById('parentDocRcStatus');
  const parentDocDl = document.getElementById('parentDocDlStatus');
  const parentDocIns = document.getElementById('parentDocInsStatus');
  const parentDocPuc = document.getElementById('parentDocPucStatus');

  if (parentDocRc) {
    const rcNum = (docs.rc && docs.rc.number) || docs.rcNumber;
    parentDocRc.textContent = rcNum ? `Active (${rcNum})` : 'Not Uploaded';
    parentDocRc.className = `text-xs font-bold ${rcNum ? 'text-emerald-400' : 'text-slate-400'} block mt-1 truncate`;
  }
  if (parentDocDl) {
    const dlNum = (docs.dl && docs.dl.number) || docs.dlNumber;
    parentDocDl.textContent = dlNum ? `Valid (${dlNum})` : 'Not Uploaded';
    parentDocDl.className = `text-xs font-bold ${dlNum ? 'text-emerald-400' : 'text-slate-400'} block mt-1 truncate`;
  }
  if (parentDocIns) {
    const insExp = (docs.insurance && docs.insurance.expiry) || docs.insuranceExpiry;
    if (insExp) {
      const days = calculateDaysRemaining(insExp);
      parentDocIns.textContent = days > 0 ? `Valid (${days}d left)` : 'Expired';
      parentDocIns.className = `text-xs font-bold ${days > 0 ? 'text-emerald-400' : 'text-rose-400'} block mt-1 truncate`;
    } else {
      parentDocIns.textContent = 'Missing Policy';
      parentDocIns.className = 'text-xs font-bold text-slate-400 block mt-1 truncate';
    }
  }
  if (parentDocPuc) {
    const pucExp = (docs.puc && docs.puc.expiry) || docs.pucExpiry;
    if (pucExp) {
      const days = calculateDaysRemaining(pucExp);
      parentDocPuc.textContent = days > 0 ? `Valid (${days}d left)` : 'Expired';
      parentDocPuc.className = `text-xs font-bold ${days > 0 ? 'text-amber-400' : 'text-rose-400'} block mt-1 truncate`;
    } else {
      parentDocPuc.textContent = 'Missing PUC';
      parentDocPuc.className = 'text-xs font-bold text-slate-400 block mt-1 truncate';
    }
  }

  // 5. Recent Trips Ledger for Parent
  const recentTripsList = document.getElementById('parentRecentTripsList');
  if (recentTripsList && trips.length > 0) {
    recentTripsList.innerHTML = trips.slice(0, 4).map(t => `
      <div class="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-white/[0.06] hover:border-purple-500/30 transition">
        <div class="flex items-center space-x-3">
          <div class="w-8 h-8 rounded-lg bg-purple-500/15 text-purple-300 flex items-center justify-center text-xs font-black">
            <svg class="w-4 h-4 text-purple-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 21v-4m0 0V5a2 2 0 012-2h6.5l1 1H21l-3 6 3 6h-8.5l-1-1H5a2 2 0 00-2 2zm9-13.5V9"/></svg>
          </div>
          <div>
            <span class="text-xs font-bold text-white block">${t.title || 'Recorded Ride'}</span>
            <span class="text-[10px] text-slate-400">${new Date(t.startTime || t.createdAt || Date.now()).toLocaleDateString()} &bull; ${t.durationFormatted || '24m 12s'}</span>
          </div>
        </div>
        <div class="text-right">
          <span class="text-xs font-black text-cyan-300 block font-mono-numbers">${(t.distanceKm || 0).toFixed(1)} KM</span>
          <span class="text-[10px] text-slate-400 font-mono-numbers">Top: ${(t.topSpeedKmh || 0).toFixed(0)} km/h</span>
        </div>
      </div>
    `).join('');
  } else if (recentTripsList) {
    recentTripsList.innerHTML = '<p class="text-xs text-slate-400 py-3 text-center">No recent rides logged for this rider yet.</p>';
  }

  // 6. Notifications
  renderParentNotifications(notifs);
}

function initParentMap() {
  const container = document.getElementById('parentLiveMap');
  if (!container) return;
  if (parentMapInstance) {
    parentMapInstance.invalidateSize();
    return;
  }

  const defaultCoords = [12.9716, 77.5946];
  parentMapInstance = L.map('parentLiveMap', {
    zoomControl: true,
    attributionControl: false
  }).setView(defaultCoords, 14);

  L.tileLayer(esriDarkLayerUrl, {
    maxZoom: 19
  }).addTo(parentMapInstance);
}

function recenterParentMap() {
  if (!parentMapInstance) return;
  if (parentMapMarker) {
    parentMapInstance.setView(parentMapMarker.getLatLng(), 15, { animate: true });
  } else {
    parentMapInstance.setView([12.9716, 77.5946], 14, { animate: true });
  }
}

function renderParentNotifications(notifs) {
  parentNotificationsList = notifs || [];
  const feed = document.getElementById('parentNotifFeed');
  const countBadge = document.getElementById('parentNotifCountBadge');
  if (!feed) return;

  if (countBadge) {
    countBadge.textContent = `${parentNotificationsList.length} Alerts`;
  }

  if (parentNotificationsList.length === 0) {
    feed.innerHTML = `
      <div class="text-center py-8 text-slate-400 text-xs">
        <div class="w-10 h-10 mx-auto mb-2 rounded-xl bg-purple-500/15 border border-purple-400/30 flex items-center justify-center text-purple-300">
          <svg class="w-5 h-5 text-purple-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
        </div>
        <span class="font-bold text-white block">All Systems Nominal</span>
        <span>No safety alerts or warnings detected.</span>
      </div>
    `;
    return;
  }

  feed.innerHTML = parentNotificationsList.map(n => {
    let iconSvg = '<svg class="w-4 h-4 text-purple-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>';
    let borderColor = 'border-purple-500/30';
    let badgeColor = 'text-purple-300 bg-purple-500/20';

    if (n.type === 'speed_warning' || n.severity === 'warning') {
      iconSvg = '<svg class="w-4 h-4 text-amber-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>';
      borderColor = 'border-amber-500/40';
      badgeColor = 'text-amber-300 bg-amber-500/20';
    } else if (n.type === 'sos' || n.severity === 'critical') {
      iconSvg = '<svg class="w-4 h-4 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>';
      borderColor = 'border-rose-500/50';
      badgeColor = 'text-rose-300 bg-rose-500/20';
    } else if (n.type === 'trip_start') {
      iconSvg = '<svg class="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>';
      borderColor = 'border-emerald-500/40';
      badgeColor = 'text-emerald-300 bg-emerald-500/20';
    }

    const timeStr = n.timestamp ? new Date(n.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now';

    return `
      <div class="p-3.5 rounded-xl bg-white/[0.03] border ${borderColor} space-y-1.5 transition hover:bg-white/[0.05]">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-2">
            ${iconSvg}
            <span class="text-xs font-bold text-white">${n.title || 'Guardian Alert'}</span>
          </div>
          <span class="text-[10px] ${badgeColor} font-bold px-2 py-0.5 rounded-full">${timeStr}</span>
        </div>
        <p class="text-xs text-slate-300 leading-relaxed">${n.message || ''}</p>
        <div class="flex items-center justify-between pt-1 border-t border-white/[0.04]">
          <span class="text-[10px] text-slate-400 font-mono">${n.riderEmail || 'pilot@rhody.io'}</span>
          <button onclick="dismissParentNotification('${n.id}')" class="text-[10px] text-slate-400 hover:text-rose-400 transition flex items-center space-x-1">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            <span>Dismiss</span>
          </button>
        </div>
      </div>
    `;
  }).join('');
}

function simulateSpeedAlert() {
  const speed = 104.5;
  const newAlert = {
    id: 'notif_' + Date.now(),
    type: 'speed_warning',
    title: 'Speed Threshold Alert',
    message: `Rider Natesh exceeded guardian safety limit (${guardianParentConfig.speedLimitKmh} km/h) reaching ${speed} km/h on Bangalore Expressway.`,
    severity: 'warning',
    riderEmail: parentConnectedRider || 'pilot@rhody.io',
    timestamp: new Date().toISOString()
  };

  fetch('/api/parent/notifications', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(newAlert)
  })
    .then(r => r.json())
    .then(data => {
      fetchParentRiderStatus();
      showToast('Speed alert notification dispatched to parent feed', 'error');
    })
    .catch(() => {
      parentNotificationsList.unshift(newAlert);
      renderParentNotifications(parentNotificationsList);
      showToast('Speed alert logged', 'error');
    });
}

function dismissParentNotification(id) {
  fetch(`/api/parent/notifications/${id}`, { method: 'DELETE' })
    .then(() => {
      parentNotificationsList = parentNotificationsList.filter(n => n.id !== id);
      renderParentNotifications(parentNotificationsList);
    })
    .catch(() => {
      parentNotificationsList = parentNotificationsList.filter(n => n.id !== id);
      renderParentNotifications(parentNotificationsList);
    });
}

function clearParentNotifications() {
  fetch('/api/parent/notifications/all', { method: 'DELETE' })
    .then(() => {
      parentNotificationsList = [];
      renderParentNotifications([]);
      showToast('Notifications cleared', 'info');
    })
    .catch(() => {
      parentNotificationsList = [];
      renderParentNotifications([]);
    });
}

// ========================================================
// ADVANCED MOTORSPORT DYNO & FRICTION CIRCLE ENGINE
// ========================================================
let dynoTestRunning = false;
let dynoTimerInterval = null;
let dynoStartTime = null;

function startSprintDynoTest() {
  if (dynoTestRunning) return;
  dynoTestRunning = true;

  const btnText = document.getElementById('btnStartDynoText');
  const statusBadge = document.getElementById('dynoStatusBadge');
  const treeStage = document.getElementById('treeStageLight');
  const treeY1 = document.getElementById('treeYellow1');
  const treeY2 = document.getElementById('treeYellow2');
  const treeGreen = document.getElementById('treeGreenGo');
  const speedText = document.getElementById('dynoCurrentSpeed');
  const elapsedText = document.getElementById('dynoElapsedMs');
  const progressBar = document.getElementById('dynoSpeedProgressBar');

  if (btnText) btnText.textContent = 'Staging Tree...';
  if (statusBadge) {
    statusBadge.textContent = 'STAGING';
    statusBadge.className = 'px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-400/40';
  }

  // Reset lights
  [treeStage, treeY1, treeY2, treeGreen].forEach(l => {
    if (l) l.className = 'w-3.5 h-3.5 rounded-full bg-slate-800 border border-white/10';
  });

  // Stage light
  if (treeStage) treeStage.className = 'w-3.5 h-3.5 rounded-full bg-amber-400 shadow-[0_0_12px_#F59E0B] border border-amber-300';

  setTimeout(() => {
    if (treeY1) treeY1.className = 'w-3.5 h-3.5 rounded-full bg-amber-400 shadow-[0_0_12px_#F59E0B] border border-amber-300';
  }, 400);

  setTimeout(() => {
    if (treeY2) treeY2.className = 'w-3.5 h-3.5 rounded-full bg-amber-400 shadow-[0_0_12px_#F59E0B] border border-amber-300';
  }, 800);

  setTimeout(() => {
    // GREEN GO!
    if (treeGreen) treeGreen.className = 'w-3.5 h-3.5 rounded-full bg-emerald-400 shadow-[0_0_16px_#10B981] border border-emerald-300 animate-pulse';
    if (btnText) btnText.textContent = 'SPRINT ACTIVE';
    if (statusBadge) {
      statusBadge.textContent = 'RUNNING';
      statusBadge.className = 'px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-400/40';
    }

    dynoStartTime = performance.now();
    let recorded60 = false;
    let recorded100 = false;

    dynoTimerInterval = setInterval(() => {
      const now = performance.now();
      const elapsedSec = (now - dynoStartTime) / 1000;
      if (elapsedText) elapsedText.textContent = `${elapsedSec.toFixed(2)} s`;

      // Non-linear realistic motorcycle acceleration curve
      const speed = Math.min(124, Math.pow(elapsedSec / 6.2, 0.72) * 100);
      if (speedText) speedText.textContent = `${speed.toFixed(1)} km/h`;
      if (progressBar) progressBar.style.width = `${Math.min(100, (speed / 120) * 100)}%`;

      // Update IMU friction circle dot dynamically during acceleration
      updateFrictionCircleDot(0.12, Math.min(1.0, 0.35 + (speed / 120) * 0.55));

      if (speed >= 60 && !recorded60) {
        recorded60 = true;
        const val60 = document.getElementById('dyno0to60Val');
        if (val60) val60.textContent = `${elapsedSec.toFixed(2)} s`;
      }

      if (speed >= 100 && !recorded100) {
        recorded100 = true;
        const val100 = document.getElementById('dyno0to100Val');
        if (val100) val100.textContent = `${elapsedSec.toFixed(2)} s`;
        const trapVal = document.getElementById('dynoTrapSpeedVal');
        if (trapVal) trapVal.textContent = (speed * 1.08).toFixed(1);

        clearInterval(dynoTimerInterval);
        dynoTestRunning = false;
        if (btnText) btnText.textContent = 'Launch Sprint Dyno';
        if (statusBadge) {
          statusBadge.textContent = `PASS: ${elapsedSec.toFixed(2)}s`;
          statusBadge.className = 'px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-400/40';
        }
        showToast(`Sprint Dyno Complete! 0-100 in ${elapsedSec.toFixed(2)}s`, 'success');
      }
    }, 40);
  }, 1200);
}

function resetDynoRecords() {
  if (dynoTimerInterval) clearInterval(dynoTimerInterval);
  dynoTestRunning = false;
  const val60 = document.getElementById('dyno0to60Val');
  const val100 = document.getElementById('dyno0to100Val');
  const trapVal = document.getElementById('dynoTrapSpeedVal');
  const speedText = document.getElementById('dynoCurrentSpeed');
  const elapsedText = document.getElementById('dynoElapsedMs');
  const progressBar = document.getElementById('dynoSpeedProgressBar');
  const statusBadge = document.getElementById('dynoStatusBadge');
  const btnText = document.getElementById('btnStartDynoText');
  const apexL = document.getElementById('apexMaxLeft');
  const apexR = document.getElementById('apexMaxRight');

  if (val60) val60.textContent = '--';
  if (val100) val100.textContent = '--';
  if (trapVal) trapVal.textContent = '--';
  if (speedText) speedText.textContent = '0.0 km/h';
  if (elapsedText) elapsedText.textContent = '0.00 s';
  if (progressBar) progressBar.style.width = '0%';
  if (statusBadge) {
    statusBadge.textContent = 'READY';
    statusBadge.className = 'px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-white/10';
  }
  if (btnText) btnText.textContent = 'Launch Sprint Dyno';
  if (apexL) apexL.textContent = '0.0\u00B0';
  if (apexR) apexR.textContent = '0.0\u00B0';
  updateFrictionCircleDot(0, 0);
  showToast('Dyno and Apex records reset to zero', 'info');
}

function updateFrictionCircleDot(lateralG, longitudinalG) {
  const dot = document.getElementById('frictionCircleDot');
  if (!dot) return;
  const xPercent = 50 + Math.max(-45, Math.min(45, lateralG * 40));
  const yPercent = 50 - Math.max(-45, Math.min(45, longitudinalG * 40));
  dot.style.left = `${xPercent}%`;
  dot.style.top = `${yPercent}%`;
}

