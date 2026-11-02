import React, { useState, useEffect, useRef } from 'react';
import { Volume2, VolumeX, FastForward, Film, Video, Radio, Sparkles } from 'lucide-react';
import cinemaSetImg from '../../assets/cinema-hero.jpg';
import clapperboardImg from '../../assets/clapperboard.png';
import logoImg from '../../assets/truehunt-logo.png';
import emblemImg from '../../assets/truehunt-emblem.png';

/**
 * ============================================================================
 * TrueHunt – Cinematic Movie Production Intro Screen
 * ============================================================================
 * Realistic film production soundstage experience:
 * - Professional movie set background (cinema-hero.jpg) with camera dolly push-in
 * - 2.39:1 Anamorphic cinema scope letterbox bars & 24 FPS SMPTE timecode HUD
 * - Film crew member stepping into scene wearing production headset & holding slate
 * - Movie clapperboard modeled directly after TrueHunt reference:
 *   - PROD. TRUEHUNT, SCENE 48B, TAKE 3, DIR: J. REED, CAM: L. CHEN, DATE: 14 OCT
 *   - Multi-colored chevron stripes (Green, Red, Yellow, Blue, White, Black)
 *   - Hinged top-arm lifting and slamming shut with realistic recoil physics
 * - Built-in Web Audio API synthesized wooden slate clap sound effect
 * - TrueHunt cinematic logo reveal with anamorphic lens flare sweep
 * - Skip Intro & Sound toggle buttons with responsive mobile/tablet layout
 * - Smooth unmount transition to TrueHunt homepage
 */
export default function CinematicIntro({ onComplete }) {
  // Phases: INITIALIZING -> CREW_ENTERING -> ARM_OPENING -> SLATE_CLAP -> CREW_EXIT -> TRUEHUNT_REVEAL -> FADING_OUT
  const [phase, setPhase] = useState('INITIALIZING');
  const [isMuted, setIsMuted] = useState(false);
  const [calloutText, setCalloutText] = useState('SCENE 48B, TAKE 3');
  const [timecode, setTimecode] = useState('00:00:01:04');

  const audioCtxRef = useRef(null);
  const timeoutsRef = useRef([]);

  // Timecode generator simulating 24 FPS SMPTE cinema recorder
  useEffect(() => {
    let frame = 4;
    let sec = 1;
    const interval = setInterval(() => {
      frame += 1;
      if (frame >= 24) {
        frame = 0;
        sec += 1;
      }
      const pad = (n) => String(n).padStart(2, '0');
      setTimecode(`00:00:${pad(sec)}:${pad(frame)}`);
    }, 1000 / 24);

    return () => clearInterval(interval);
  }, []);

  // Web Audio API Synthesis for authentic wooden clapperboard impact clap
  const playClapperSound = () => {
    if (isMuted) return;
    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return;

      if (!audioCtxRef.current) {
        audioCtxRef.current = new AudioContext();
      }
      const ctx = audioCtxRef.current;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }

      const now = ctx.currentTime;

      // 1. Sharp wooden slap transient (bandpassed high-frequency noise burst)
      const bufferSize = ctx.sampleRate * 0.045;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (ctx.sampleRate * 0.007));
      }
      const noise = ctx.createBufferSource();
      noise.buffer = buffer;

      const noiseFilter = ctx.createBiquadFilter();
      noiseFilter.type = 'bandpass';
      noiseFilter.frequency.setValueAtTime(2200, now);
      noiseFilter.Q.setValueAtTime(3.0, now);

      const noiseGain = ctx.createGain();
      noiseGain.gain.setValueAtTime(1.0, now);
      noiseGain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);

      noise.connect(noiseFilter);
      noiseFilter.connect(noiseGain);
      noiseGain.connect(ctx.destination);
      noise.start(now);

      // 2. Heavy wooden slate body resonance (low triangle decay)
      const osc = ctx.createOscillator();
      const oscGain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(390, now);
      osc.frequency.exponentialRampToValueAtTime(105, now + 0.065);

      oscGain.gain.setValueAtTime(0.9, now);
      oscGain.gain.exponentialRampToValueAtTime(0.001, now + 0.075);

      osc.connect(oscGain);
      oscGain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.08);

      // 3. Crisp mechanical hinge click
      const clickOsc = ctx.createOscillator();
      const clickGain = ctx.createGain();
      clickOsc.type = 'sine';
      clickOsc.frequency.setValueAtTime(1400, now);
      clickOsc.frequency.exponentialRampToValueAtTime(320, now + 0.022);

      clickGain.gain.setValueAtTime(0.65, now);
      clickGain.gain.exponentialRampToValueAtTime(0.001, now + 0.028);

      clickOsc.connect(clickGain);
      clickGain.connect(ctx.destination);
      clickOsc.start(now);
      clickOsc.stop(now + 0.03);
    } catch {
      // Browser autoplay restriction fallback
    }
  };

  const schedule = (fn, delayMs) => {
    const id = setTimeout(fn, delayMs);
    timeoutsRef.current.push(id);
    return id;
  };

  // Movie set production choreography
  useEffect(() => {
    // Act 1: Crew member walks in holding slate (1.0s)
    schedule(() => {
      setPhase('CREW_ENTERING');
      setCalloutText('SCENE 48B, TAKE 3... MARKER!');
    }, 1000);

    // Act 2: Clapper top arm raises smoothly (2.3s)
    schedule(() => {
      setPhase('ARM_OPENING');
      setCalloutText('MARKER READY...');
    }, 2300);

    // Act 3: SNAP! Clapper snaps shut with authentic sound (3.6s)
    schedule(() => {
      setPhase('SLATE_CLAP');
      playClapperSound();
      setCalloutText('CLAP! 🎬');
    }, 3600);

    // Act 4: Crew member clears the frame (4.4s)
    schedule(() => {
      setPhase('CREW_EXIT');
      setCalloutText('ACTION! 🎥');
    }, 4400);

    // Act 5: TrueHunt cinematic branding reveal (5.2s)
    schedule(() => {
      setPhase('TRUEHUNT_REVEAL');
    }, 5200);

    // Act 6: Crossfade to TrueHunt main homepage (7.5s)
    schedule(() => {
      setPhase('FADING_OUT');
    }, 7500);

    // Act 7: Complete and unmount (8.2s)
    schedule(() => {
      if (onComplete) onComplete();
    }, 8200);

    return () => {
      timeoutsRef.current.forEach(clearTimeout);
      if (audioCtxRef.current && audioCtxRef.current.state !== 'closed') {
        try {
          audioCtxRef.current.close();
        } catch {}
      }
    };
  }, []);

  const handleSkip = () => {
    setPhase('FADING_OUT');
    schedule(() => {
      if (onComplete) onComplete();
    }, 350);
  };

  return (
    <div
      className={`cinema-intro-wrapper ${phase === 'FADING_OUT' ? 'fading-out' : ''}`}
      role="region"
      aria-label="TrueHunt Movie Production Intro"
    >
      {/* 2.39:1 Anamorphic Cinema Scope Letterbox Bars */}
      <div className="cinema-letterbox-bar cinema-letterbox-top" />
      <div className="cinema-letterbox-bar cinema-letterbox-bottom" />

      {/* Production Camera Live HUD */}
      <header className="cinema-hud-header">
        <div className="cinema-rec-indicator">
          <div className="cinema-rec-dot" />
          <span>REC</span>
          <span className="cinema-timecode-display">{timecode}</span>
        </div>
        <div className="cinema-camera-data">
          <span>ARRI RAW 4K</span>
          <span>24 FPS</span>
          <span>SHUTTER 180°</span>
          <span>ISO 800</span>
          <span>5600K</span>
        </div>
      </header>

      {/* Quick Action Controls: Skip & Audio Toggle */}
      <div className="cinema-action-controls">
        <button
          type="button"
          onClick={() => setIsMuted((m) => !m)}
          className="cinema-ctrl-btn"
          aria-label={isMuted ? 'Unmute Audio' : 'Mute Audio'}
          title={isMuted ? 'Unmute Sound' : 'Mute Sound'}
        >
          {isMuted ? <VolumeX size={15} /> : <Volume2 size={15} />}
          <span>{isMuted ? 'Muted' : 'Sound ON'}</span>
        </button>

        <button
          type="button"
          onClick={handleSkip}
          className="cinema-ctrl-btn cinema-skip-btn"
          aria-label="Skip Cinematic Intro"
          title="Skip directly to TrueHunt Homepage"
        >
          <span>Skip Intro</span>
          <FastForward size={14} />
        </button>
      </div>

      {/* Professional Movie Soundstage Environment Background */}
      <div className={`cinema-stage-bg ${phase === 'SLATE_CLAP' ? 'camera-shake' : ''}`}>
        {/* Real Movie Production Set Backdrop Image */}
        <img
          src={cinemaSetImg}
          alt="TrueHunt Movie Production Soundstage"
          className={`cinema-set-backdrop ${phase === 'TRUEHUNT_REVEAL' || phase === 'FADING_OUT' ? 'backdrop-blurred' : ''}`}
        />
        
        {/* Cinematic Lighting Vignette & Depth Layers */}
        <div className="cinema-backdrop-overlay" />
        <div className="cinema-spotlight-beam" />
        <div className="cinema-haze-layer" />
        <div className="cinema-stage-floor-reflection" />

        {/* Ambient Production Strobe Flash on Clap */}
        {phase === 'SLATE_CLAP' && <div className="cinema-strobe-flash" />}
      </div>

      {/* Center Stage Presentation */}
      <main className="cinema-center-stage">
        {/* ACT 1 & 2: Crew Member entering and operating Movie Clapperboard */}
        {(phase === 'INITIALIZING' || phase === 'CREW_ENTERING' || phase === 'ARM_OPENING' || phase === 'SLATE_CLAP' || phase === 'CREW_EXIT') && (
          <div className="crew-clapper-scene">
            
            {/* Film Crew Member Figure & Hands */}
            <div className={`crew-member-figure ${phase === 'CREW_EXIT' ? 'crew-exiting' : ''}`}>
              
              {/* Clapperboard Apparatus */}
              <div className={`clapperboard-container ${phase === 'SLATE_CLAP' ? 'just-clapped' : ''}`}>
                
                {/* Director Voice Callout Bubble */}
                <div className="cinema-callout-bubble">
                  {calloutText}
                </div>

                {/* Top Hinged Clapper Stick (Opens & Slams with Chevrons) */}
                <div
                  className={`clapper-top-arm ${
                    phase === 'ARM_OPENING' ? 'arm-opened' :
                    phase === 'SLATE_CLAP' || phase === 'CREW_EXIT' ? 'arm-slammed' : ''
                  }`}
                >
                  {/* Multi-Colored Chevron Stripes matching TrueHunt Reference */}
                  <div className="clapper-chevrons-bar">
                    <span className="ch-stripe ch-green" />
                    <span className="ch-stripe ch-red" />
                    <span className="ch-stripe ch-yellow" />
                    <span className="ch-stripe ch-blue" />
                    <span className="ch-stripe ch-white" />
                    <span className="ch-stripe ch-black" />
                    <span className="ch-stripe ch-green" />
                    <span className="ch-stripe ch-red" />
                    <span className="ch-stripe ch-yellow" />
                    <span className="ch-stripe ch-blue" />
                  </div>
                  {/* Heavy Steel Hinge Bolt */}
                  <div className="clapper-hinge-pin" />
                </div>

                {/* Stationary Slate Top Bar */}
                <div className="clapper-fixed-bar">
                  <div className="clapper-chevrons-bar fixed-stripes">
                    <span className="ch-stripe ch-green" />
                    <span className="ch-stripe ch-red" />
                    <span className="ch-stripe ch-yellow" />
                    <span className="ch-stripe ch-blue" />
                    <span className="ch-stripe ch-white" />
                    <span className="ch-stripe ch-black" />
                    <span className="ch-stripe ch-green" />
                    <span className="ch-stripe ch-red" />
                    <span className="ch-stripe ch-yellow" />
                    <span className="ch-stripe ch-blue" />
                  </div>
                </div>

                {/* White Acrylic Slate Body with TrueHunt Production Grid */}
                <div className="clapper-board-body">
                  {/* Header Title */}
                  <div className="slate-header-title">
                    <span className="slate-prod-label">PROD.</span>
                    <span className="slate-prod-name">TRUEHUNT</span>
                    <span className="slate-fps-badge">24 FPS</span>
                  </div>

                  {/* Scene, Take, Roll Grid */}
                  <div className="slate-grid-row">
                    <div className="slate-cell">
                      <span className="slate-cell-label">SCENE</span>
                      <span className="slate-cell-value">48B</span>
                    </div>
                    <div className="slate-cell slate-cell-take">
                      <span className="slate-cell-label">TAKE</span>
                      <span className="slate-cell-value">3</span>
                    </div>
                    <div className="slate-cell">
                      <span className="slate-cell-label">ROLL</span>
                      <span className="slate-cell-value">A1</span>
                    </div>
                  </div>

                  {/* Director & Camera Credits */}
                  <div className="slate-meta-grid">
                    <div className="slate-meta-item">
                      <span className="slate-meta-k">DIR:</span>
                      <span className="slate-meta-v">J. REED</span>
                    </div>
                    <div className="slate-meta-item">
                      <span className="slate-meta-k">CAM:</span>
                      <span className="slate-meta-v">L. CHEN</span>
                    </div>
                    <div className="slate-meta-item slate-meta-full">
                      <span className="slate-meta-k">DATE:</span>
                      <span className="slate-meta-v">14 OCT 2026</span>
                      <span className="slate-sync-pill">SYNC SOUND</span>
                    </div>
                  </div>
                </div>

                {/* Realistic Hands Gripping the Slate */}
                <div className="crew-hand-left" title="Crew Hand Left" />
                <div className="crew-hand-right" title="Crew Hand Right" />

              </div>

              {/* Realistic Film Crew Member Silhouette with Headset */}
              <div className="crew-silhouette-wrapper">
                <svg width="280" height="200" viewBox="0 0 280 200" fill="none">
                  {/* Head */}
                  <circle cx="140" cy="42" r="28" fill="#0F172A" />
                  {/* Production Headset Band & Earcups */}
                  <path d="M112 38 C112 18, 168 18, 168 38" stroke="#64748B" strokeWidth="5" fill="none" />
                  <rect x="108" y="32" width="10" height="20" rx="4" fill="#94A3B8" />
                  <rect x="162" y="32" width="10" height="20" rx="4" fill="#94A3B8" />
                  {/* Boom Mic */}
                  <path d="M114 44 Q108 58, 126 56" stroke="#94A3B8" strokeWidth="3" fill="none" />
                  <circle cx="127" cy="56" r="3" fill="#EF4444" />
                  {/* Crew Torso with Tactical Jacket */}
                  <path d="M70 200 C70 100, 110 82, 140 82 C170 82, 210 100, 210 200 Z" fill="#0B1120" />
                  {/* Crew Arms reaching forward to hold slate */}
                  <path d="M50 48 Q80 65, 100 95" stroke="#0F172A" strokeWidth="18" strokeLinecap="round" />
                  <path d="M230 48 Q200 65, 180 95" stroke="#0F172A" strokeWidth="18" strokeLinecap="round" />
                </svg>
              </div>

            </div>

          </div>
        )}

        {/* ACT 3: TrueHunt Brand Reveal with Golden Anamorphic Flares */}
        <div className={`truehunt-reveal-stage ${phase === 'TRUEHUNT_REVEAL' || phase === 'FADING_OUT' ? 'stage-active' : ''}`}>
          <div className="cinema-lens-flare" />
          <div className="cinema-light-burst" />

          <div className="truehunt-logo-hero">
            {/* TrueHunt Emblem Badge */}
            <div className="truehunt-emblem-container">
              <img
                src={emblemImg}
                alt="TrueHunt Emblem"
                className="truehunt-emblem-img"
              />
            </div>

            {/* TrueHunt Official Logo */}
            <div className="truehunt-brand-image-wrapper">
              <img
                src={logoImg}
                alt="TrueHunt"
                className="truehunt-brand-image"
              />
              <div className="truehunt-logo-shimmer" />
            </div>

            {/* Tagline Badge */}
            <div className="cinema-tagline-badge">
              <Film size={15} color="#D97706" />
              <span>THE FILM & ENTERTAINMENT NETWORK</span>
            </div>

            <p className="cinema-sub-heading">
              Connect. Collaborate. Create the Next Story.
            </p>

            <div className="cinema-action-indicator">
              <span className="live-dot" />
              <span>PRODUCTION ROLLING • ENTERING STUDIO</span>
            </div>
          </div>
        </div>
      </main>

      {/* Embedded Cinema Styles (All-in-One Self Contained) */}
      <style>{`
        .cinema-intro-wrapper {
          position: fixed;
          inset: 0;
          z-index: 999999;
          background-color: #030712;
          color: #FFFFFF;
          overflow: hidden;
          user-select: none;
          font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
          transition: opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1), transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
          opacity: 1;
          transform: scale(1);
          pointer-events: auto;
        }

        .cinema-intro-wrapper.fading-out {
          opacity: 0;
          transform: scale(1.05);
          pointer-events: none;
        }

        /* 2.39:1 Anamorphic Cinema Scope Letterbox Bars */
        .cinema-letterbox-bar {
          position: absolute;
          left: 0;
          right: 0;
          height: clamp(34px, 6.5vh, 70px);
          background: #000000;
          z-index: 50;
          box-shadow: 0 0 35px rgba(0, 0, 0, 0.95);
        }

        .cinema-letterbox-top {
          top: 0;
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        }

        .cinema-letterbox-bottom {
          bottom: 0;
          border-top: 1px solid rgba(255, 255, 255, 0.08);
        }

        /* Top Film Production Status HUD */
        .cinema-hud-header {
          position: absolute;
          top: clamp(8px, 1.8vh, 18px);
          left: 24px;
          right: 24px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          z-index: 60;
          font-size: 11px;
          letter-spacing: 0.12em;
          font-weight: 600;
          text-transform: uppercase;
          color: rgba(255, 255, 255, 0.75);
          pointer-events: none;
        }

        .cinema-rec-indicator {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #EF4444;
          font-family: 'Courier New', Courier, monospace;
          font-size: 13px;
          font-weight: 700;
        }

        .cinema-rec-dot {
          width: 10px;
          height: 10px;
          background-color: #EF4444;
          border-radius: 50%;
          box-shadow: 0 0 14px #EF4444;
          animation: cinemaPulseRec 1s ease-in-out infinite;
        }

        @keyframes cinemaPulseRec {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.3; transform: scale(0.85); }
        }

        .cinema-timecode-display {
          color: #F3F4F6;
          margin-left: 6px;
          letter-spacing: 0.08em;
        }

        .cinema-camera-data {
          display: flex;
          align-items: center;
          gap: 16px;
          font-family: 'Courier New', Courier, monospace;
          font-size: 11px;
          color: #9CA3AF;
        }

        @media (max-width: 768px) {
          .cinema-camera-data {
            display: none;
          }
        }

        /* User Controls (Skip & Audio Toggle) */
        .cinema-action-controls {
          position: absolute;
          bottom: clamp(14px, 2.5vh, 26px);
          right: 24px;
          display: flex;
          align-items: center;
          gap: 12px;
          z-index: 70;
        }

        .cinema-ctrl-btn {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          padding: 8px 16px;
          border-radius: 9999px;
          background-color: rgba(15, 23, 42, 0.85);
          border: 1px solid rgba(255, 255, 255, 0.18);
          color: #FFFFFF;
          font-size: 12px;
          font-weight: 600;
          letter-spacing: 0.05em;
          cursor: pointer;
          backdrop-filter: blur(8px);
          transition: all 0.2s ease;
          box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
        }

        .cinema-ctrl-btn:hover {
          background-color: rgba(30, 41, 59, 0.95);
          border-color: rgba(255, 255, 255, 0.35);
          transform: translateY(-2px);
        }

        .cinema-skip-btn {
          background-color: #801823;
          border-color: #A01B2B;
        }

        .cinema-skip-btn:hover {
          background-color: #991B1B;
          border-color: #DC2626;
        }

        /* Soundstage Environment Background */
        .cinema-stage-bg {
          position: absolute;
          inset: 0;
          background-color: #03060A;
          overflow: hidden;
        }

        .cinema-stage-bg.camera-shake {
          animation: cameraShakeImpact 0.22s ease-out;
        }

        @keyframes cameraShakeImpact {
          0% { transform: translate(0, 0) scale(1); }
          25% { transform: translate(-3px, 4px) scale(1.002); }
          50% { transform: translate(3px, -2px) scale(0.999); }
          75% { transform: translate(-1px, 1px) scale(1.001); }
          100% { transform: translate(0, 0) scale(1); }
        }

        /* Real Movie Set Backdrop Image */
        .cinema-set-backdrop {
          position: absolute;
          inset: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
          object-position: center;
          filter: brightness(0.45) contrast(1.15) saturate(0.95);
          transform: scale(1.0);
          animation: cinemaDollyPush 10s ease-out forwards;
          transition: filter 1.2s ease, transform 1.2s ease;
        }

        @keyframes cinemaDollyPush {
          0% { transform: scale(1.0); }
          100% { transform: scale(1.07); }
        }

        .cinema-set-backdrop.backdrop-blurred {
          filter: brightness(0.25) contrast(1.2) blur(8px);
          transform: scale(1.08);
        }

        .cinema-backdrop-overlay {
          position: absolute;
          inset: 0;
          background: radial-gradient(circle at 50% 50%, rgba(3, 7, 18, 0.3) 0%, rgba(3, 7, 18, 0.75) 75%, #030712 100%);
          pointer-events: none;
        }

        .cinema-spotlight-beam {
          position: absolute;
          top: -20%;
          left: 50%;
          transform: translateX(-50%);
          width: clamp(350px, 60vw, 900px);
          height: 120%;
          background: radial-gradient(ellipse at 50% 0%, rgba(245, 158, 11, 0.16) 0%, rgba(217, 119, 6, 0.05) 45%, transparent 75%);
          pointer-events: none;
          mix-blend-mode: screen;
        }

        .cinema-haze-layer {
          position: absolute;
          inset: 0;
          background: linear-gradient(180deg, transparent 0%, rgba(15, 23, 42, 0.35) 65%, rgba(3, 7, 18, 0.8) 100%);
          pointer-events: none;
        }

        .cinema-strobe-flash {
          position: absolute;
          inset: 0;
          background: rgba(255, 255, 255, 0.18);
          pointer-events: none;
          animation: flashFade 0.25s ease-out forwards;
        }

        @keyframes flashFade {
          0% { opacity: 1; }
          100% { opacity: 0; }
        }

        /* Center Stage Layout */
        .cinema-center-stage {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 40;
          padding: 20px;
        }

        /* Act 1 & 2: Crew Member & Clapper Scene */
        .crew-clapper-scene {
          display: flex;
          flex-direction: column;
          align-items: center;
          position: relative;
        }

        .crew-member-figure {
          position: relative;
          display: flex;
          flex-direction: column;
          align-items: center;
          animation: crewStepIn 1.2s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }

        @keyframes crewStepIn {
          0% {
            opacity: 0;
            transform: translateY(80px) scale(0.92);
          }
          100% {
            opacity: 1;
            transform: translateY(0) scale(1);
          }
        }

        .crew-member-figure.crew-exiting {
          animation: crewClearFrame 0.75s cubic-bezier(0.7, 0, 0.84, 0) forwards;
        }

        @keyframes crewClearFrame {
          0% {
            opacity: 1;
            transform: translateY(0) scale(1);
          }
          100% {
            opacity: 0;
            transform: translateY(120px) translateX(60px) scale(0.85);
          }
        }

        /* Clapperboard Container */
        .clapperboard-container {
          position: relative;
          width: clamp(280px, 38vw, 440px);
          display: flex;
          flex-direction: column;
          box-shadow: 0 20px 45px rgba(0, 0, 0, 0.85), 0 0 30px rgba(245, 158, 11, 0.15);
          border-radius: 8px;
          margin-bottom: -50px;
          z-index: 25;
        }

        .clapperboard-container.just-clapped {
          animation: clapperRecoilShock 0.25s ease-out;
        }

        @keyframes clapperRecoilShock {
          0% { transform: scale(1); }
          30% { transform: scale(0.97) rotate(-0.5deg); }
          60% { transform: scale(1.02) rotate(0.4deg); }
          100% { transform: scale(1) rotate(0); }
        }

        /* Director Callout Speech Bubble */
        .cinema-callout-bubble {
          position: absolute;
          top: -55px;
          left: 50%;
          transform: translateX(-50%);
          background-color: #801823;
          color: #FFFFFF;
          padding: 6px 18px;
          border-radius: 20px;
          font-size: 13px;
          font-weight: 700;
          letter-spacing: 0.08em;
          box-shadow: 0 4px 15px rgba(128, 24, 35, 0.6);
          white-space: nowrap;
          border: 1px solid rgba(255, 255, 255, 0.2);
          animation: bubbleBounce 0.4s ease-out;
        }

        @keyframes bubbleBounce {
          0% { opacity: 0; transform: translateX(-50%) translateY(-10px) scale(0.8); }
          100% { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }
        }

        /* Top Hinged Clapper Stick */
        .clapper-top-arm {
          position: relative;
          height: 38px;
          background: #0B0F19;
          border-top-left-radius: 8px;
          border-top-right-radius: 8px;
          transform-origin: 18px 36px;
          transition: transform 0.2s cubic-bezier(0.2, 0.8, 0.2, 1);
          z-index: 20;
          box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.5);
          border: 2px solid #1E293B;
          border-bottom: none;
        }

        .clapper-top-arm.arm-opened {
          transform: rotate(-35deg);
        }

        .clapper-top-arm.arm-slammed {
          animation: clapperSnapDown 0.16s cubic-bezier(0.8, 0, 0.2, 1) forwards;
        }

        @keyframes clapperSnapDown {
          0% { transform: rotate(-35deg); }
          70% { transform: rotate(1.5deg); }
          88% { transform: rotate(-1deg); }
          100% { transform: rotate(0deg); }
        }

        /* Multi-Colored Chevron Stripes */
        .clapper-chevrons-bar {
          display: flex;
          height: 100%;
          width: 100%;
          overflow: hidden;
          border-radius: 6px 6px 0 0;
        }

        .ch-stripe {
          flex: 1;
          height: 100%;
          transform: skewX(-26deg);
        }

        .ch-green { background-color: #10B981; }
        .ch-red { background-color: #EF4444; }
        .ch-yellow { background-color: #F59E0B; }
        .ch-blue { background-color: #3B82F6; }
        .ch-white { background-color: #F3F4F6; }
        .ch-black { background-color: #111827; }

        .clapper-fixed-bar {
          height: 24px;
          background: #0B0F19;
          border-left: 2px solid #1E293B;
          border-right: 2px solid #1E293B;
          border-bottom: 2px solid #1E293B;
        }

        .clapper-hinge-pin {
          position: absolute;
          left: 12px;
          bottom: 4px;
          width: 14px;
          height: 14px;
          border-radius: 50%;
          background: radial-gradient(circle, #E2E8F0 30%, #475569 80%);
          border: 1px solid #1E293B;
          box-shadow: 0 2px 5px rgba(0,0,0,0.6);
          z-index: 30;
        }

        /* White Acrylic Slate Body */
        .clapper-board-body {
          background-color: #FFFFFF;
          color: #0F172A;
          padding: 16px 18px;
          border-radius: 0 0 8px 8px;
          border: 2px solid #1E293B;
          border-top: none;
          box-shadow: inset 0 2px 6px rgba(0,0,0,0.1);
        }

        .slate-header-title {
          display: flex;
          align-items: center;
          justify-content: space-between;
          border-bottom: 2px solid #0F172A;
          padding-bottom: 8px;
          margin-bottom: 10px;
        }

        .slate-prod-label {
          font-size: 11px;
          font-weight: 800;
          letter-spacing: 0.1em;
          color: #475569;
        }

        .slate-prod-name {
          font-size: 18px;
          font-weight: 900;
          letter-spacing: 0.14em;
          color: #801823;
        }

        .slate-fps-badge {
          font-family: 'Courier New', Courier, monospace;
          font-size: 11px;
          font-weight: 700;
          background: #0F172A;
          color: #FFFFFF;
          padding: 2px 6px;
          border-radius: 3px;
        }

        /* Scene Take Roll Grid */
        .slate-grid-row {
          display: grid;
          grid-template-columns: 1fr 1fr 1fr;
          gap: 8px;
          border-bottom: 2px solid #0F172A;
          padding-bottom: 10px;
          margin-bottom: 10px;
        }

        .slate-cell {
          display: flex;
          flex-direction: column;
          align-items: center;
          background-color: #F8FAFC;
          border: 1.5px solid #CBD5E1;
          border-radius: 4px;
          padding: 6px 4px;
        }

        .slate-cell-take {
          border-color: #801823;
          background-color: #FFF1F2;
        }

        .slate-cell-label {
          font-size: 9px;
          font-weight: 800;
          letter-spacing: 0.12em;
          color: #64748B;
        }

        .slate-cell-take .slate-cell-label {
          color: #801823;
        }

        .slate-cell-value {
          font-size: 24px;
          font-weight: 900;
          font-family: 'Courier New', Courier, monospace;
          color: #0F172A;
          margin-top: 2px;
        }

        .slate-cell-take .slate-cell-value {
          color: #801823;
        }

        /* Metadata Grid */
        .slate-meta-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 6px 12px;
          font-size: 10px;
          font-weight: 700;
          color: #1E293B;
        }

        .slate-meta-k {
          color: #64748B;
          margin-right: 4px;
        }

        .slate-meta-v {
          color: #0F172A;
        }

        .slate-meta-full {
          grid-column: 1 / -1;
          display: flex;
          align-items: center;
          justify-content: space-between;
          border-top: 1px dashed #CBD5E1;
          padding-top: 6px;
          margin-top: 2px;
        }

        .slate-sync-pill {
          background-color: #059669;
          color: #FFFFFF;
          padding: 1px 6px;
          border-radius: 3px;
          font-size: 8px;
          font-weight: 800;
          letter-spacing: 0.05em;
        }

        /* Realistic Hands holding slate sides */
        .crew-hand-left {
          position: absolute;
          left: -18px;
          top: 45%;
          width: 28px;
          height: 60px;
          background: #B45309;
          border-radius: 12px 4px 4px 12px;
          box-shadow: 2px 4px 10px rgba(0, 0, 0, 0.4);
          border: 2px solid #78350F;
        }

        .crew-hand-right {
          position: absolute;
          right: -18px;
          top: 45%;
          width: 28px;
          height: 60px;
          background: #B45309;
          border-radius: 4px 12px 12px 4px;
          box-shadow: -2px 4px 10px rgba(0, 0, 0, 0.4);
          border: 2px solid #78350F;
        }

        .crew-silhouette-wrapper {
          filter: drop-shadow(0 15px 30px rgba(0, 0, 0, 0.9));
        }

        /* Act 3: TrueHunt Brand Reveal Stage */
        .truehunt-reveal-stage {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          opacity: 0;
          pointer-events: none;
          transform: scale(0.92);
          transition: all 0.9s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .truehunt-reveal-stage.stage-active {
          opacity: 1;
          pointer-events: auto;
          transform: scale(1);
        }

        .cinema-lens-flare {
          position: absolute;
          width: 140%;
          height: 4px;
          background: linear-gradient(90deg, transparent 0%, rgba(245, 158, 11, 0.2) 20%, rgba(255, 255, 255, 0.9) 50%, rgba(245, 158, 11, 0.2) 80%, transparent 100%);
          box-shadow: 0 0 40px rgba(245, 158, 11, 0.7);
          animation: lensFlareSweep 2.2s ease-in-out infinite alternate;
        }

        @keyframes lensFlareSweep {
          0% { transform: translateY(-40px) scaleX(0.7); opacity: 0.6; }
          100% { transform: translateY(40px) scaleX(1.1); opacity: 1; }
        }

        .cinema-light-burst {
          position: absolute;
          width: 500px;
          height: 500px;
          border-radius: 50%;
          background: radial-gradient(circle, rgba(245, 158, 11, 0.2) 0%, rgba(128, 24, 35, 0.1) 40%, transparent 70%);
          animation: burstPulse 3s ease-in-out infinite alternate;
        }

        @keyframes burstPulse {
          0% { transform: scale(0.9); opacity: 0.6; }
          100% { transform: scale(1.15); opacity: 1; }
        }

        .truehunt-logo-hero {
          position: relative;
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          z-index: 30;
          max-width: 680px;
          padding: 24px;
        }

        .truehunt-emblem-container {
          width: 86px;
          height: 86px;
          margin-bottom: 16px;
          border-radius: 20px;
          background: rgba(15, 23, 42, 0.75);
          border: 1.5px solid rgba(245, 158, 11, 0.5);
          box-shadow: 0 10px 25px rgba(0, 0, 0, 0.6), 0 0 25px rgba(245, 158, 11, 0.3);
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 8px;
          animation: emblemFloat 3s ease-in-out infinite alternate;
        }

        @keyframes emblemFloat {
          0% { transform: translateY(0); }
          100% { transform: translateY(-6px); }
        }

        .truehunt-emblem-img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }

        .truehunt-brand-image-wrapper {
          position: relative;
          overflow: hidden;
          padding: 4px 12px;
        }

        .truehunt-brand-image {
          height: clamp(56px, 10vw, 84px);
          width: auto;
          object-fit: contain;
          filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.75));
        }

        .truehunt-logo-shimmer {
          position: absolute;
          inset: 0;
          background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.4) 50%, transparent 100%);
          transform: translateX(-100%) skewX(-20deg);
          animation: logoShimmerPass 3.5s ease-in-out infinite;
        }

        @keyframes logoShimmerPass {
          0% { transform: translateX(-100%) skewX(-20deg); }
          40%, 100% { transform: translateX(200%) skewX(-20deg); }
        }

        .cinema-tagline-badge {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          margin-top: 18px;
          padding: 6px 16px;
          border-radius: 9999px;
          background: rgba(30, 41, 59, 0.7);
          border: 1px solid rgba(245, 158, 11, 0.4);
          color: #F3F4F6;
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.14em;
          text-transform: uppercase;
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
        }

        .cinema-sub-heading {
          font-size: 16px;
          color: #E2E8F0;
          margin-top: 12px;
          font-weight: 500;
          letter-spacing: 0.04em;
          text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
        }

        .cinema-action-indicator {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-top: 16px;
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.12em;
          color: #D97706;
          font-family: 'Courier New', Courier, monospace;
        }

        .live-dot {
          width: 8px;
          height: 8px;
          background-color: #D97706;
          border-radius: 50%;
          animation: pulseDot 1s infinite alternate;
        }

        @keyframes pulseDot {
          0% { opacity: 0.4; }
          100% { opacity: 1; transform: scale(1.2); }
        }

        /* Mobile & Small Device Optimization */
        @media (max-width: 640px) {
          .clapperboard-container {
            width: 90vw;
            margin-bottom: -30px;
          }

          .slate-header-title {
            padding-bottom: 4px;
            margin-bottom: 6px;
          }

          .slate-prod-name {
            font-size: 15px;
          }

          .slate-cell-value {
            font-size: 18px;
          }

          .slate-meta-grid {
            font-size: 9px;
          }

          .crew-silhouette-wrapper svg {
            width: 220px;
            height: 150px;
          }

          .truehunt-sub-heading {
            font-size: 13px;
          }
        }
      `}</style>
    </div>
  );
}
