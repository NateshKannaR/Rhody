/**
 * Rhody Cyber HUD Visualizer & Machine Archive
 * Provides 3D Holographic Wireframe, Segmented Telemetry Cells, and Omnitrix Machine Fleet Selector
 */
(function() {
  // Machine Registry (Omnitrix-style Fleet)
  const RHODY_FLEET = [
    {
      id: 'pulsar150',
      name: 'BAJAJ PULSAR 150',
      edition: 'Twin Disc Edition',
      dnaCode: 'MOTO-DNA #8821',
      archetype: 'COMMUTER MISSILE',
      powerHp: '14.0 PS @ 8,500 RPM',
      torqueNm: '13.25 Nm @ 6,500 RPM',
      topSpeed: 115,
      accel0100: 14.8,
      maxLean: 42,
      brakingG: 0.85,
      shieldCoverage: 92,
      glowColor: '#39ff14', // Neon Green
      wireframeStyle: 'commuter',
      desc: 'DTS-i dual spark engine calibrated for dependable city agility and everyday telemetry logging.'
    },
    {
      id: 'rc390',
      name: 'KTM RC 390 GP',
      edition: 'Ready to Race Factory',
      dnaCode: 'MOTO-DNA #3900',
      archetype: 'APEX PREDATOR',
      powerHp: '43.5 PS @ 9,000 RPM',
      torqueNm: '37.0 Nm @ 7,000 RPM',
      topSpeed: 172,
      accel0100: 5.4,
      maxLean: 54,
      brakingG: 1.15,
      shieldCoverage: 98,
      glowColor: '#ff9900', // Amber-Orange
      wireframeStyle: 'supersport',
      desc: 'Ultra-lightweight trellis chassis with WP Apex suspension, cornering ABS, and MotoGP telemetry geometry.'
    },
    {
      id: 'r15v4',
      name: 'YAMAHA YZF-R15 V4',
      edition: 'Racing Blue VVA',
      dnaCode: 'MOTO-DNA #1550',
      archetype: 'TRACK SCALPEL',
      powerHp: '18.4 PS @ 10,000 RPM',
      torqueNm: '14.2 Nm @ 7,500 RPM',
      topSpeed: 144,
      accel0100: 10.1,
      maxLean: 50,
      brakingG: 0.98,
      shieldCoverage: 95,
      glowColor: '#00e5ff', // Galvan Cyan
      wireframeStyle: 'supersport',
      desc: 'Variable Valve Actuation with Deltabox chassis, quickshifter, and aggressive supersport riding ergonomics.'
    },
    {
      id: 'panigalev4',
      name: 'DUCATI PANIGALE V4 S',
      edition: 'Corse Telemetry Spec',
      dnaCode: 'MOTO-DNA #1103',
      archetype: 'HYPER TITAN',
      powerHp: '215.5 HP @ 13,000 RPM',
      torqueNm: '123.6 Nm @ 9,500 RPM',
      topSpeed: 299,
      accel0100: 3.1,
      maxLean: 61,
      brakingG: 1.45,
      shieldCoverage: 99,
      glowColor: '#ff3344', // Threat Red
      wireframeStyle: 'hyperbike',
      desc: 'Desmosedici Stradale 90° V4 with Öhlins Smart EC 2.0 electronic suspension and multi-axis IMU slide control.'
    },
    {
      id: 'zx10r',
      name: 'KAWASAKI NINJA ZX-10R',
      edition: 'KRT WorldSBK Edition',
      dnaCode: 'MOTO-DNA #9980',
      archetype: 'WSBK CHAMPION',
      powerHp: '203.0 PS @ 13,200 RPM',
      torqueNm: '114.9 Nm @ 11,400 RPM',
      topSpeed: 299,
      accel0100: 3.2,
      maxLean: 58,
      brakingG: 1.38,
      shieldCoverage: 99,
      glowColor: '#39ff14', // Lime Green
      wireframeStyle: 'hyperbike',
      desc: 'WorldSBK proven aerodynamics with integrated winglets, air-cooled oil cooler, and titanium valves.'
    }
  ];

  let activeMachineIndex = 0;
  let canvas3D = null;
  let ctx3D = null;
  let animFrameId = null;
  let tiltX = 0;
  let tiltY = 0;
  let targetTiltX = 0;
  let targetTiltY = 0;

  // Initialize Cyber Components
  function initCyberComponents() {
    setupSoundToggle();
    setupFloatingTrigger();
    setupMachineArchive();
    setup3DHoloCanvas();
    enhanceSegmentedBars();
  }

  // 1. Sound Toggle in Header
  function setupSoundToggle() {
    const btn = document.getElementById('btnSoundToggle');
    if (btn) {
      btn.addEventListener('click', function(e) {
        e.preventDefault();
        if (window.RhodyAudio) {
          window.RhodyAudio.toggleMute();
        }
      });
      if (window.RhodyAudio) window.RhodyAudio.updateUI();
    }
  }

  // 2. Floating Omnitrix / Cockpit Trigger
  function setupFloatingTrigger() {
    const trigger = document.getElementById('rhodyFloatingTrigger');
    if (trigger) {
      trigger.addEventListener('click', function() {
        if (window.RhodyAudio) window.RhodyAudio.playTabSwitch();
        if (typeof openHudMirrorModal === 'function') {
          openHudMirrorModal();
        }
      });
    }
  }

  // 3. Machine Archive (Omnitrix Selector)
  function setupMachineArchive() {
    const container = document.getElementById('machineArchiveContainer');
    if (!container) return;

    renderMachineTabs();
    selectMachine(0, false);
  }

  function renderMachineTabs() {
    const tabsContainer = document.getElementById('machineTabsList');
    if (!tabsContainer) return;

    tabsContainer.innerHTML = '';
    RHODY_FLEET.forEach((machine, idx) => {
      const btn = document.createElement('button');
      btn.className = `machine-tab ${idx === activeMachineIndex ? 'machine-tab--active' : ''}`;
      btn.innerHTML = `
        <span class="w-2 h-2 rounded-full" style="background:${machine.glowColor}; box-shadow:0 0 8px ${machine.glowColor}"></span>
        <span>${machine.name}</span>
      `;
      btn.addEventListener('click', () => {
        if (window.RhodyAudio) window.RhodyAudio.playClick();
        selectMachine(idx, true);
      });
      tabsContainer.appendChild(btn);
    });
  }

  function selectMachine(index, triggerSound) {
    if (index < 0 || index >= RHODY_FLEET.length) return;
    activeMachineIndex = index;
    const m = RHODY_FLEET[index];

    // Re-render tab states
    const tabs = document.querySelectorAll('.machine-tab');
    tabs.forEach((tab, i) => {
      if (i === index) {
        tab.classList.add('machine-tab--active');
      } else {
        tab.classList.remove('machine-tab--active');
      }
    });

    // Populate Details
    const nameEl = document.getElementById('activeMachineName');
    const editionEl = document.getElementById('activeMachineEdition');
    const dnaCodeEl = document.getElementById('activeMachineDnaCode');
    const archetypeEl = document.getElementById('activeMachineArchetype');
    const powerEl = document.getElementById('activeMachinePower');
    const torqueEl = document.getElementById('activeMachineTorque');
    const descEl = document.getElementById('activeMachineDesc');

    if (nameEl) nameEl.textContent = m.name;
    if (editionEl) editionEl.textContent = m.edition;
    if (dnaCodeEl) {
      dnaCodeEl.textContent = m.dnaCode;
      dnaCodeEl.style.borderColor = m.glowColor;
      dnaCodeEl.style.color = m.glowColor;
      dnaCodeEl.style.boxShadow = `0 0 12px ${m.glowColor}40`;
    }
    if (archetypeEl) archetypeEl.textContent = m.archetype;
    if (powerEl) powerEl.textContent = m.powerHp;
    if (torqueEl) torqueEl.textContent = m.torqueNm;
    if (descEl) descEl.textContent = m.desc;

    // Render Segmented Stat Bars
    updateStatCells('cellsTopSpeed', Math.min(10, Math.round((m.topSpeed / 300) * 10)), m.glowColor);
    updateStatCells('cellsAccel', Math.min(10, Math.round(((16 - m.accel0100) / 13) * 10)), m.glowColor);
    updateStatCells('cellsLean', Math.min(10, Math.round((m.maxLean / 65) * 10)), m.glowColor);
    updateStatCells('cellsBraking', Math.min(10, Math.round((m.brakingG / 1.5) * 10)), m.glowColor);
    updateStatCells('cellsShield', Math.min(10, Math.round((m.shieldCoverage / 100) * 10)), m.glowColor);

    // Update raw value labels
    const vSpeed = document.getElementById('valTopSpeed');
    const vAccel = document.getElementById('valAccel');
    const vLean = document.getElementById('valLean');
    const vBraking = document.getElementById('valBraking');
    const vShield = document.getElementById('valShield');

    if (vSpeed) vSpeed.textContent = `${m.topSpeed} km/h`;
    if (vAccel) vAccel.textContent = `${m.accel0100}s`;
    if (vLean) vLean.textContent = `${m.maxLean}°`;
    if (vBraking) vBraking.textContent = `${m.brakingG} G`;
    if (vShield) vShield.textContent = `${m.shieldCoverage}%`;

    if (triggerSound && window.RhodyAudio) {
      window.RhodyAudio.playTabSwitch();
    }
  }

  function updateStatCells(containerId, activeCount, color) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '';
    for (let i = 0; i < 10; i++) {
      const cell = document.createElement('i');
      if (i < activeCount) {
        cell.classList.add('on');
        cell.style.background = color;
        cell.style.boxShadow = `0 0 8px ${color}80`;
      }
      container.appendChild(cell);
    }
  }

  // 4. Interactive 3D Holographic Wireframe Canvas
  function setup3DHoloCanvas() {
    canvas3D = document.getElementById('holoWireframeCanvas');
    if (!canvas3D) return;

    ctx3D = canvas3D.getContext('2d');
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);

    // Mouse tilt tracking
    canvas3D.addEventListener('mousemove', (e) => {
      const rect = canvas3D.getBoundingClientRect();
      const x = (e.clientX - rect.left) / rect.width - 0.5;
      const y = (e.clientY - rect.top) / rect.height - 0.5;
      targetTiltX = x * 2.2;
      targetTiltY = y * 1.8;
    });

    canvas3D.addEventListener('mouseleave', () => {
      targetTiltX = 0;
      targetTiltY = 0;
    });

    // Gyro tilt for mobile devices
    if (window.DeviceOrientationEvent) {
      window.addEventListener('deviceorientation', (e) => {
        if (e.gamma !== null && e.beta !== null) {
          targetTiltX = Math.max(-1, Math.min(1, e.gamma / 30));
          targetTiltY = Math.max(-1, Math.min(1, (e.beta - 45) / 30));
        }
      });
    }

    render3DLoop();
  }

  function resizeCanvas() {
    if (!canvas3D) return;
    const rect = canvas3D.parentElement.getBoundingClientRect();
    canvas3D.width = rect.width * (window.devicePixelRatio || 1);
    canvas3D.height = rect.height * (window.devicePixelRatio || 1);
    if (ctx3D) {
      ctx3D.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
    }
  }

  function render3DLoop() {
    if (!ctx3D || !canvas3D) return;

    // Smooth lerp tilt
    tiltX += (targetTiltX - tiltX) * 0.08;
    tiltY += (targetTiltY - tiltY) * 0.08;

    const w = canvas3D.width / (window.devicePixelRatio || 1);
    const h = canvas3D.height / (window.devicePixelRatio || 1);
    const cx = w / 2;
    const cy = h / 2;
    const time = Date.now() * 0.0015;

    ctx3D.clearRect(0, 0, w, h);

    const machine = RHODY_FLEET[activeMachineIndex] || RHODY_FLEET[0];
    const color = machine.glowColor;

    // Draw Holographic Circular Pitch Rings
    ctx3D.save();
    ctx3D.translate(cx + tiltX * 30, cy + tiltY * 20);

    // Outer rotating dashed ring
    ctx3D.beginPath();
    ctx3D.arc(0, 0, 75, 0, Math.PI * 2);
    ctx3D.strokeStyle = `${color}33`;
    ctx3D.lineWidth = 1.5;
    ctx3D.setLineDash([8, 12]);
    ctx3D.stroke();

    // Inner counter-rotating solid reticle
    ctx3D.beginPath();
    ctx3D.arc(0, 0, 55, time, time + Math.PI * 1.5);
    ctx3D.strokeStyle = `${color}88`;
    ctx3D.lineWidth = 2;
    ctx3D.setLineDash([]);
    ctx3D.stroke();

    // Crosshairs
    ctx3D.strokeStyle = `${color}44`;
    ctx3D.beginPath();
    ctx3D.moveTo(-85, 0); ctx3D.lineTo(-60, 0);
    ctx3D.moveTo(60, 0); ctx3D.lineTo(85, 0);
    ctx3D.moveTo(0, -85); ctx3D.lineTo(0, -60);
    ctx3D.moveTo(0, 60); ctx3D.lineTo(0, 85);
    ctx3D.stroke();

    // 3D Perspective Motorcycle Wireframe Geometry
    ctx3D.save();
    ctx3D.rotate(tiltX * 0.35); // Lean angle rotation

    // Wheels
    const frontWheelX = 45 + tiltX * 8;
    const rearWheelX = -45 - tiltX * 8;
    const wheelY = 22;

    // Rear Wheel
    ctx3D.strokeStyle = color;
    ctx3D.lineWidth = 2.5;
    ctx3D.beginPath();
    ctx3D.arc(rearWheelX, wheelY, 18, 0, Math.PI * 2);
    ctx3D.stroke();

    // Front Wheel
    ctx3D.beginPath();
    ctx3D.arc(frontWheelX, wheelY, 18, 0, Math.PI * 2);
    ctx3D.stroke();

    // Chassis lines (Trellis / Deltabox)
    ctx3D.lineWidth = 2;
    ctx3D.beginPath();
    ctx3D.moveTo(rearWheelX, wheelY);
    ctx3D.lineTo(-15, 0); // Swingarm to pivot
    ctx3D.lineTo(15, -12); // Tank to headstock
    ctx3D.lineTo(frontWheelX, wheelY); // Front fork
    ctx3D.moveTo(-15, 0);
    ctx3D.lineTo(5, 12); // Engine cradle
    ctx3D.lineTo(frontWheelX, wheelY);
    ctx3D.moveTo(-15, 0);
    ctx3D.lineTo(-30, -18); // Subframe / tail
    ctx3D.stroke();

    // Handlebar & Cockpit Apex
    ctx3D.beginPath();
    ctx3D.moveTo(15, -12);
    ctx3D.lineTo(12, -26);
    ctx3D.lineTo(24, -26);
    ctx3D.stroke();

    // Glowing Apex Dot
    ctx3D.fillStyle = color;
    ctx3D.beginPath();
    ctx3D.arc(0, -4, 4, 0, Math.PI * 2);
    ctx3D.fill();

    ctx3D.restore();

    // Artificial Horizon Indicator
    ctx3D.strokeStyle = `${color}aa`;
    ctx3D.lineWidth = 1.5;
    ctx3D.beginPath();
    const horizonTilt = tiltX * 45;
    ctx3D.moveTo(-40, horizonTilt);
    ctx3D.lineTo(-12, horizonTilt);
    ctx3D.moveTo(12, -horizonTilt);
    ctx3D.lineTo(40, -horizonTilt);
    ctx3D.stroke();

    ctx3D.restore();

    animFrameId = requestAnimationFrame(render3DLoop);
  }

  // 5. Enhance Segmented Bars on the whole page
  function enhanceSegmentedBars() {
    document.querySelectorAll('.stat__cells').forEach(container => {
      const activeAttr = container.getAttribute('data-value');
      if (activeAttr) {
        const val = parseInt(activeAttr, 10) || 5;
        container.querySelectorAll('i').forEach((cell, idx) => {
          if (idx < val) cell.classList.add('on');
        });
      }
    });
  }

  // Expose to window
  window.RhodyVisualizer = {
    init: initCyberComponents,
    selectMachine: selectMachine,
    getFleet: () => RHODY_FLEET
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initCyberComponents);
  } else {
    initCyberComponents();
  }
})();
