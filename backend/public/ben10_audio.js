/**
 * Rhody Cyber HUD Audio Synthesizer (Zero External Audio Files)
 * Uses Web Audio API oscillator synthesis inspired by Galvan / Omnitrix tactile clicks & sweeps.
 */
(function() {
  const RhodyAudio = {
    ctx: null,
    muted: false,

    init() {
      if (!this.ctx && typeof window !== 'undefined') {
        const AudioContextClass = window.AudioContext || window.webkitAudioContext;
        if (AudioContextClass) {
          this.ctx = new AudioContextClass();
        }
      }
      if (this.ctx && this.ctx.state === 'suspended') {
        this.ctx.resume();
      }
    },

    toggleMute() {
      this.muted = !this.muted;
      localStorage.setItem('rhody_sound_muted', this.muted ? 'true' : 'false');
      this.updateUI();
      if (!this.muted) {
        this.playClick();
      }
      return this.muted;
    },

    updateUI() {
      const btn = document.getElementById('btnSoundToggle');
      if (btn) {
        if (this.muted) {
          btn.classList.add('sound-muted');
          btn.setAttribute('title', 'Sound: MUTED (Click to Enable)');
          const lbl = document.getElementById('soundToggleLabel');
          if (lbl) lbl.textContent = 'MUTE';
        } else {
          btn.classList.remove('sound-muted');
          btn.setAttribute('title', 'Sound: ACTIVE (Click to Mute)');
          const lbl = document.getElementById('soundToggleLabel');
          if (lbl) lbl.textContent = 'FX ON';
        }
      }
    },

    // 1. Mechanical Sci-Fi Click (for button taps, tab navigation)
    playClick() {
      if (this.muted) return;
      this.init();
      if (!this.ctx) return;

      try {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(880, this.ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(440, this.ctx.currentTime + 0.05);

        gain.gain.setValueAtTime(0.08, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.05);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start();
        osc.stop(this.ctx.currentTime + 0.05);
      } catch (e) {}
    },

    // 2. High-Tech Holo Scan / Tab Switch Beep
    playTabSwitch() {
      if (this.muted) return;
      this.init();
      if (!this.ctx) return;

      try {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(520, this.ctx.currentTime);
        osc.frequency.linearRampToValueAtTime(1040, this.ctx.currentTime + 0.07);

        gain.gain.setValueAtTime(0.06, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.07);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start();
        osc.stop(this.ctx.currentTime + 0.07);
      } catch (e) {}
    },

    // 3. Omnitrix / Sprint Dyno Power Surge Sweep
    playLaunchSweep() {
      if (this.muted) return;
      this.init();
      if (!this.ctx) return;

      try {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(120, this.ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(1200, this.ctx.currentTime + 0.35);

        gain.gain.setValueAtTime(0.07, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.35);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start();
        osc.stop(this.ctx.currentTime + 0.35);
      } catch (e) {}
    },

    // 4. GPS Calibration / Apex Chime
    playLockChime() {
      if (this.muted) return;
      this.init();
      if (!this.ctx) return;

      try {
        const notes = [660, 880, 1320];
        notes.forEach((freq, i) => {
          const osc = this.ctx.createOscillator();
          const gain = this.ctx.createGain();
          const startTime = this.ctx.currentTime + i * 0.06;

          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, startTime);

          gain.gain.setValueAtTime(0.06, startTime);
          gain.gain.exponentialRampToValueAtTime(0.001, startTime + 0.12);

          osc.connect(gain);
          gain.connect(this.ctx.destination);

          osc.start(startTime);
          osc.stop(startTime + 0.12);
        });
      } catch (e) {}
    },

    // 5. Guardian Alert / Crash Siren
    playAlertSiren() {
      if (this.muted) return;
      this.init();
      if (!this.ctx) return;

      try {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'square';
        osc.frequency.setValueAtTime(800, this.ctx.currentTime);
        osc.frequency.setValueAtTime(600, this.ctx.currentTime + 0.1);
        osc.frequency.setValueAtTime(800, this.ctx.currentTime + 0.2);

        gain.gain.setValueAtTime(0.12, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.3);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start();
        osc.stop(this.ctx.currentTime + 0.3);
      } catch (e) {}
    }
  };

  // Restore mute state from localStorage
  if (typeof localStorage !== 'undefined') {
    RhodyAudio.muted = localStorage.getItem('rhody_sound_muted') === 'true';
  }

  // Initialize on first user interaction
  const unlockAudio = () => {
    RhodyAudio.init();
    window.removeEventListener('click', unlockAudio);
    window.removeEventListener('keydown', unlockAudio);
    window.removeEventListener('touchstart', unlockAudio);
  };
  window.addEventListener('click', unlockAudio);
  window.addEventListener('keydown', unlockAudio);
  window.addEventListener('touchstart', unlockAudio);

  window.RhodyAudio = RhodyAudio;
})();
