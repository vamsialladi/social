import React, { useState, useEffect, useRef } from 'react';
import { Volume2, VolumeX, FastForward, Film, Clapperboard, CheckCircle2 } from 'lucide-react';
import logoImg from '../../assets/truehunt-logo.png';

/**
 * ============================================================================
 * TrueHunt – All-in-One Cinematic Movie Production Intro Experience
 * ============================================================================
 * Features:
 * - 100% self-contained single-file React component (Zero external CSS needed)
 * - 2.39:1 Anamorphic cinema scope letterbox bars with live 24 FPS timecode HUD
 * - Atmospheric film soundstage with volumetric spotlight beam, haze & gear silhouettes
 * - Realistic film crew member figure entering the set with production headset
 * - Authentic movie slate clapperboard with realistic hinged top-arm clap animation
 * - Built-in Web Audio API synthesizer for realistic wooden slate impact clap sound
 * - TrueHunt cinematic brand reveal with golden anamorphic lens flare sweep
 * - Skip Intro button, Mute/Sound toggle, browser autoplay safety & smooth unmount
 */
export default function CinematicIntro({ onComplete }) {
  // Phase sequence: INITIALIZING -> CREW_ENTERING -> ARM_OPENING -> SLATE_CLAP -> CREW_EXIT -> TRUEHUNT_REVEAL -> FADING_OUT
  const [phase, setPhase] = useState('INITIALIZING');
  const [isMuted, setIsMuted] = useState(false);
  const [calloutText, setCalloutText] = useState('SCENE 1, TAKE 1');
  const [timecode, setTimecode] = useState('00:00:01:04');

  const audioCtxRef = useRef(null);
  const timeoutsRef = useRef([]);

  // Timecode simulation (24 FPS SMPTE)
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

  // Web Audio API Synthesis for realistic wooden slate clapper sound
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

      // 1. Sharp wooden slap transient (bandpassed noise burst)
      const bufferSize = ctx.sampleRate * 0.04;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (ctx.sampleRate * 0.008));
      }
      const noise = ctx.createBufferSource();
      noise.buffer = buffer;

      const noiseFilter = ctx.createBiquadFilter();
      noiseFilter.type = 'bandpass';
      noiseFilter.frequency.setValueAtTime(2400, now);
      noiseFilter.Q.setValueAtTime(2.5, now);

      const noiseGain = ctx.createGain();
      noiseGain.gain.setValueAtTime(0.95, now);
      noiseGain.gain.exponentialRampToValueAtTime(0.001, now + 0.045);

      noise.connect(noiseFilter);
      noiseFilter.connect(noiseGain);
      noiseGain.connect(ctx.destination);
      noise.start(now);

      // 2. Solid wooden slate board body resonance
      const osc = ctx.createOscillator();
      const oscGain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(380, now);
      osc.frequency.exponentialRampToValueAtTime(110, now + 0.06);

      oscGain.gain.setValueAtTime(0.85, now);
      oscGain.gain.exponentialRampToValueAtTime(0.001, now + 0.07);

      osc.connect(oscGain);
      oscGain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.08);

      // 3. Crisp mechanical strike click
      const clickOsc = ctx.createOscillator();
      const clickGain = ctx.createGain();
      clickOsc.type = 'sine';
      clickOsc.frequency.setValueAtTime(1200, now);
      clickOsc.frequency.exponentialRampToValueAtTime(300, now + 0.02);

      clickGain.gain.setValueAtTime(0.6, now);
      clickGain.gain.exponentialRampToValueAtTime(0.001, now + 0.025);

      clickOsc.connect(clickGain);
      clickGain.connect(ctx.destination);
      clickOsc.start(now);
      clickOsc.stop(now + 0.03);
    } catch {
      // Audio autoplay policy fallback
    }
  };

  const schedule = (fn, delayMs) => {
    const id = setTimeout(fn, delayMs);
    timeoutsRef.current.push(id);
    return id;
  };

  // Choreographed movie production sequence
  useEffect(() => {
    // Stage 1: Crew member steps into the spotlight (1.0s)
    schedule(() => {
      setPhase('CREW_ENTERING');
      setCalloutText('SCENE 1, TAKE 1... MARKER!');
    }, 1000);

    // Stage 2: Top clapper arm lifts up (2.5s)
    schedule(() => {
      setPhase('ARM_OPENING');
    }, 2500);

    // Stage 3: SLAM! Clapper snaps shut with sound effect (3.8s)
    schedule(() => {
      setPhase('SLATE_CLAP');
      playClapperSound();
      setCalloutText('CLAP! 🎬');
    }, 3800);

    // Stage 4: Crew member clears the camera frame (4.6s)
    schedule(() => {
      setPhase('CREW_EXIT');
      setCalloutText('ACTION!');
    }, 4600);

    // Stage 5: TrueHunt cinematic branding reveal (5.2s)
    schedule(() => {
      setPhase('TRUEHUNT_REVEAL');
    }, 5200);

    // Stage 6: Crossfade to homepage (7.4s)
    schedule(() => {
      setPhase('FADING_OUT');
    }, 7400);

    // Stage 7: Transition complete & unmount (8.1s)
    schedule(() => {
      if (onComplete) onComplete();
    }, 8100);

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
      aria-label="TrueHunt Cinema Production Intro"
    >
      {/* 2.39:1 Anamorphic Scope Letterbox Bars */}
      <div className="cinema-letterbox-bar cinema-letterbox-top" />
      <div className="cinema-letterbox-bar cinema-letterbox-bottom" />

      {/* Production Camera HUD Header */}
      <header className="cinema-hud-header">
        <div className="cinema-rec-indicator">
          <div className="cinema-rec-dot" />
          <span>REC</span>
          <span style={{ color: '#F3F4F6', marginLeft: '6px' }}>{timecode}</span>
        </div>
        <div className="cinema-camera-data">
          <span>4K RAW</span>
          <span>24 FPS</span>
          <span>SHUTTER 1/48</span>
          <span>ISO 800</span>
          <span>5600K</span>
        </div>
      </header>

      {/* Action Controls: Skip & Audio Toggle */}
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
          className="cinema-ctrl-btn"
          aria-label="Skip Cinematic Intro"
          title="Skip Intro to Homepage"
          style={{ backgroundColor: 'rgba(128, 24, 35, 0.85)' }}
        >
          <span>Skip Intro</span>
          <FastForward size={14} />
        </button>
      </div>

      {/* Soundstage Environment */}
      <div className="cinema-stage-bg">
        <div className="cinema-spotlight-beam" />
        <div className="cinema-haze-layer" />
        <div className="cinema-stage-floor" />

        {/* Studio Silhouette Equipment */}
        <div className="cinema-silhouette-equipment">
          <svg width="120" height="180" viewBox="0 0 120 180" fill="none">
            <circle cx="60" cy="30" r="22" fill="#1E293B" />
            <rect x="42" y="14" width="36" height="32" rx="4" fill="#0F172A" />
            <line x1="60" y1="52" x2="60" y2="180" stroke="#334155" strokeWidth="4" />
            <line x1="60" y1="120" x2="20" y2="180" stroke="#334155" strokeWidth="3" />
            <line x1="60" y1="120" x2="100" y2="180" stroke="#334155" strokeWidth="3" />
          </svg>
          <svg width="140" height="180" viewBox="0 0 140 180" fill="none">
            <rect x="35" y="30" width="70" height="35" rx="5" fill="#0F172A" />
            <rect x="18" y="34" width="20" height="26" rx="2" fill="#1E293B" />
            <circle cx="70" cy="46" r="12" fill="#334155" />
            <rect x="85" y="20" width="16" height="18" fill="#1E293B" />
            <line x1="70" y1="65" x2="70" y2="180" stroke="#334155" strokeWidth="5" />
            <line x1="70" y1="100" x2="25" y2="180" stroke="#334155" strokeWidth="4" />
            <line x1="70" y1="100" x2="115" y2="180" stroke="#334155" strokeWidth="4" />
          </svg>
        </div>
      </div>

      {/* Center Stage Presentation */}
      <main className="cinema-center-stage">
        
        {/* ACT 1 & ACT 2: Film Crew Member with Movie Clapperboard */}
        {(phase === 'INITIALIZING' || phase === 'CREW_ENTERING' || phase === 'ARM_OPENING' || phase === 'SLATE_CLAP' || phase === 'CREW_EXIT') && (
          <div className="crew-clapper-scene">
            
            {/* Film Crew Member Figure */}
            <div className={`crew-member-figure ${phase === 'CREW_EXIT' ? 'crew-exiting' : ''}`}>
              
              {/* Clapperboard Apparatus */}
              <div className={`clapperboard-container ${phase === 'SLATE_CLAP' ? 'just-clapped' : ''}`}>
                
                {/* Director Voice Callout */}
                <div className="cinema-callout-bubble">
                  {calloutText}
                </div>

                {/* Top Hinged Clapper Stick (Opens & Slams) */}
                <div 
                  className={`clapper-top-arm ${
                    phase === 'ARM_OPENING' ? 'arm-opened' : 
                    phase === 'SLATE_CLAP' || phase === 'CREW_EXIT' ? 'arm-slammed' : ''
                  }`}
                >
                  <div className="clapper-stripes" />
                  <div className="clapper-hinge-pin" />
                </div>

                {/* Slate Board Body */}
                <div className="clapper-board-body">
                  <div className="slate-header-title">
                    <span className="slate-prod-name">PROD. TRUEHUNT</span>
                    <span style={{ fontSize: '11px', color: '#9CA3AF', fontFamily: 'monospace' }}>FPS 24</span>
                  </div>

                  <div className="slate-grid-row">
                    <div className="slate-cell">
                      <span className="slate-cell-label">SCENE</span>
                      <span className="slate-cell-value">01</span>
                    </div>
                    <div className="slate-cell">
                      <span className="slate-cell-label">TAKE</span>
                      <span className="slate-cell-value" style={{ color: '#F87171' }}>01</span>
                    </div>
                    <div className="slate-cell">
                      <span className="slate-cell-label">ROLL</span>
                      <span className="slate-cell-value">A1</span>
                    </div>
                  </div>

                  <div className="slate-meta-row">
                    <div>
                      <span style={{ color: '#9CA3AF', fontSize: '9px' }}>DIRECTOR: </span>
                      <strong>CINEMA CREATORS</strong>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span style={{ color: '#9CA3AF', fontSize: '9px' }}>DATE: </span>
                      <strong>2026</strong>
                    </div>
                  </div>
                </div>

              </div>

              {/* Realistic Film Crew Member Silhouette with Production Headset */}
              <svg width="240" height="160" viewBox="0 0 240 160" fill="none" style={{ filter: 'drop-shadow(0 10px 20px rgba(0,0,0,0.8))' }}>
                <circle cx="120" cy="38" r="26" fill="#1E293B" />
                <path d="M96 34 C96 16, 144 16, 144 34" stroke="#475569" strokeWidth="5" fill="none" />
                <rect x="92" y="28" width="9" height="18" rx="4" fill="#64748B" />
                <rect x="139" y="28" width="9" height="18" rx="4" fill="#64748B" />
                <path d="M96 38 Q90 54, 108 52" stroke="#64748B" strokeWidth="3" fill="none" />
                <path d="M60 160 C60 85, 95 72, 120 72 C145 72, 180 85, 180 160 Z" fill="#0F172A" />
                <path d="M45 40 Q70 50, 85 70" stroke="#1E293B" strokeWidth="14" strokeLinecap="round" />
                <path d="M195 40 Q170 50, 155 70" stroke="#1E293B" strokeWidth="14" strokeLinecap="round" />
              </svg>

            </div>

          </div>
        )}

        {/* ACT 3: TrueHunt Brand Reveal */}
        <div className={`truehunt-reveal-stage ${phase === 'TRUEHUNT_REVEAL' || phase === 'FADING_OUT' ? 'stage-active' : ''}`}>
          <div className="cinema-lens-flare" />

          <div className="truehunt-logo-hero">
            <img 
              src={logoImg} 
              alt="TrueHunt" 
              className="truehunt-brand-image" 
            />

            <div className="cinema-tagline-badge">
              <Film size={14} />
              <span>The Film & Entertainment Industry Network</span>
            </div>

            <p className="cinema-sub-heading">
              Connect. Grow. Create the Next Story.
            </p>

            <div className="cinema-action-indicator">
              <span>● Production Rolling</span>
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

        .cinema-letterbox-bar {
          position: absolute;
          left: 0;
          right: 0;
          height: clamp(32px, 6vh, 64px);
          background: #000000;
          z-index: 50;
          box-shadow: 0 0 30px rgba(0, 0, 0, 0.9);
        }

        .cinema-letterbox-top {
          top: 0;
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        }

        .cinema-letterbox-bottom {
          bottom: 0;
          border-top: 1px solid rgba(255, 255, 255, 0.08);
        }

        .cinema-hud-header {
          position: absolute;
          top: clamp(8px, 1.5vh, 16px);
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
          color: rgba(255, 255, 255, 0.7);
          pointer-events: none;
        }

        .cinema-rec-indicator {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #EF4444;
          font-family: monospace;
          font-size: 13px;
          font-weight: 700;
        }

        .cinema-rec-dot {
          width: 10px;
          height: 10px;
          background-color: #EF4444;
          border-radius: 50%;
          box-shadow: 0 0 12px #EF4444;
          animation: cinemaPulseRec 1s ease-in-out infinite;
        }

        @keyframes cinemaPulseRec {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.3; transform: scale(0.85); }
        }

        .cinema-camera-data {
          display: flex;
          align-items: center;
          gap: 16px;
          font-family: monospace;
          font-size: 12px;
          color: #9CA3AF;
        }

        .cinema-action-controls {
          position: absolute;
          top: clamp(40px, 8vh, 76px);
          right: clamp(16px, 3vw, 36px);
          z-index: 70;
          display: flex;
          align-items: center;
          gap: 12px;
          pointer-events: auto;
        }

        .cinema-ctrl-btn {
          background: rgba(15, 23, 42, 0.75);
          border: 1px solid rgba(255, 255, 255, 0.2);
          color: #FFFFFF;
          padding: 8px 16px;
          border-radius: 9999px;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 8px;
          backdrop-filter: blur(12px);
          transition: all 0.2s ease;
        }

        .cinema-ctrl-btn:hover {
          background: rgba(128, 24, 35, 0.9);
          border-color: #F87171;
          transform: translateY(-1px);
          box-shadow: 0 4px 16px rgba(128, 24, 35, 0.4);
        }

        .cinema-stage-bg {
          position: absolute;
          inset: 0;
          background: radial-gradient(circle at 50% 45%, #0B132B 0%, #050811 60%, #020307 100%);
          overflow: hidden;
        }

        .cinema-spotlight-beam {
          position: absolute;
          top: -120px;
          left: 50%;
          transform: translateX(-50%);
          width: 580px;
          height: 800px;
          background: radial-gradient(ellipse at 50% 0%, rgba(224, 231, 255, 0.22) 0%, rgba(147, 197, 253, 0.08) 40%, transparent 75%);
          pointer-events: none;
          filter: blur(28px);
          animation: cinemaSpotlightSway 8s ease-in-out infinite alternate;
        }

        @keyframes cinemaSpotlightSway {
          0% { transform: translateX(-52%) rotate(-2deg); }
          100% { transform: translateX(-48%) rotate(2deg); }
        }

        .cinema-haze-layer {
          position: absolute;
          inset: 0;
          background: radial-gradient(circle at 50% 60%, rgba(30, 41, 59, 0.4) 0%, transparent 70%);
          pointer-events: none;
          mix-blend-mode: screen;
        }

        .cinema-stage-floor {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 260px;
          background: linear-gradient(to top, rgba(15, 23, 42, 0.95), transparent);
          border-top: 1px solid rgba(255, 255, 255, 0.05);
        }

        .cinema-silhouette-equipment {
          position: absolute;
          bottom: 40px;
          width: 100%;
          display: flex;
          justify-content: space-between;
          padding: 0 4vw;
          opacity: 0.35;
          pointer-events: none;
          filter: blur(1.5px);
        }

        .cinema-center-stage {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-direction: column;
          z-index: 20;
        }

        .crew-clapper-scene {
          position: relative;
          width: min(640px, 92vw);
          height: min(480px, 68vh);
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .crew-member-figure {
          position: absolute;
          bottom: -40px;
          left: 50%;
          transform: translateX(-50%) translateY(40px) scale(0.95);
          width: 100%;
          height: 100%;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: flex-end;
          pointer-events: none;
          animation: crewWalkIn 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }

        @keyframes crewWalkIn {
          0% {
            opacity: 0;
            transform: translateX(-62%) translateY(140px) scale(0.85);
          }
          60% {
            opacity: 1;
          }
          100% {
            opacity: 1;
            transform: translateX(-50%) translateY(0px) scale(1);
          }
        }

        .crew-member-figure.crew-exiting {
          animation: crewWalkOut 0.8s cubic-bezier(0.4, 0, 1, 1) forwards;
        }

        @keyframes crewWalkOut {
          0% {
            opacity: 1;
            transform: translateX(-50%) translateY(0px) scale(1);
          }
          100% {
            opacity: 0;
            transform: translateX(-50%) translateY(180px) scale(0.9);
          }
        }

        .clapperboard-container {
          position: relative;
          width: min(380px, 85vw);
          height: min(280px, 50vh);
          perspective: 1200px;
          margin-bottom: 20px;
          filter: drop-shadow(0 25px 45px rgba(0, 0, 0, 0.85));
        }

        .clapperboard-container.just-clapped {
          animation: slateRecoilImpact 0.35s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
        }

        @keyframes slateRecoilImpact {
          0% { transform: translateY(0) scale(1); }
          25% { transform: translateY(8px) rotate(-1.5deg) scale(0.99); }
          50% { transform: translateY(-3px) rotate(1deg) scale(1.01); }
          75% { transform: translateY(2px) scale(1); }
          100% { transform: translateY(0) scale(1); }
        }

        .clapper-top-arm {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: clamp(48px, 9vh, 60px);
          transform-origin: 18px 46px;
          z-index: 10;
          transition: transform 0.15s cubic-bezier(0.2, 0.9, 0.2, 1);
          transform: rotate(0deg);
        }

        .clapper-top-arm.arm-opened {
          transform: rotate(-32deg);
        }

        .clapper-top-arm.arm-slammed {
          transform: rotate(0deg);
          animation: armBounceSnap 0.22s ease-out;
        }

        @keyframes armBounceSnap {
          0% { transform: rotate(0deg); }
          40% { transform: rotate(-5deg); }
          70% { transform: rotate(1deg); }
          100% { transform: rotate(0deg); }
        }

        .clapper-stripes {
          width: 100%;
          height: 100%;
          border-radius: 8px 8px 0 0;
          background: repeating-linear-gradient(
            -45deg,
            #111827 0,
            #111827 22px,
            #F3F4F6 22px,
            #F3F4F6 44px
          );
          box-shadow: inset 0 2px 4px rgba(255, 255, 255, 0.2), 0 4px 10px rgba(0, 0, 0, 0.6);
          border: 2px solid #1F2937;
          position: relative;
        }

        .clapper-hinge-pin {
          position: absolute;
          left: 14px;
          bottom: 8px;
          width: 14px;
          height: 14px;
          background: radial-gradient(circle at 35% 35%, #9CA3AF 0%, #4B5563 60%, #1F2937 100%);
          border-radius: 50%;
          border: 1px solid #111827;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.6);
          z-index: 20;
        }

        .clapper-board-body {
          position: absolute;
          top: clamp(48px, 9vh, 60px);
          left: 0;
          width: 100%;
          height: calc(100% - clamp(48px, 9vh, 60px));
          background-color: #111827;
          border-radius: 0 0 10px 10px;
          border: 3px solid #1F2937;
          border-top: 3px solid #000000;
          padding: 14px;
          box-sizing: border-box;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          box-shadow: inset 0 0 40px rgba(0, 0, 0, 0.8), 0 15px 35px rgba(0, 0, 0, 0.7);
        }

        .slate-header-title {
          display: flex;
          justify-content: space-between;
          align-items: center;
          border-bottom: 2px solid rgba(255, 255, 255, 0.2);
          padding-bottom: 6px;
          margin-bottom: 8px;
        }

        .slate-prod-name {
          font-size: clamp(14px, 2.5vw, 18px);
          font-weight: 800;
          color: #FFFFFF;
          letter-spacing: 0.08em;
          font-family: 'Courier New', monospace;
          text-transform: uppercase;
        }

        .slate-grid-row {
          display: grid;
          grid-template-columns: 1fr 1fr 1fr;
          gap: 6px;
          border-bottom: 1.5px solid rgba(255, 255, 255, 0.15);
          padding-bottom: 8px;
          margin-bottom: 8px;
        }

        .slate-cell {
          background: rgba(255, 255, 255, 0.04);
          border: 1px solid rgba(255, 255, 255, 0.1);
          padding: 6px 8px;
          border-radius: 4px;
          text-align: center;
        }

        .slate-cell-label {
          display: block;
          font-size: 9px;
          letter-spacing: 0.08em;
          color: #9CA3AF;
          font-weight: 700;
          text-transform: uppercase;
          margin-bottom: 2px;
        }

        .slate-cell-value {
          font-family: 'Courier New', monospace;
          font-size: clamp(15px, 2.8vw, 20px);
          font-weight: 800;
          color: #FFFFFF;
        }

        .slate-meta-row {
          display: grid;
          grid-template-columns: 1.4fr 1fr;
          gap: 8px;
          font-size: 11px;
          color: #D1D5DB;
          font-family: 'Courier New', monospace;
        }

        .cinema-callout-bubble {
          position: absolute;
          top: -46px;
          left: 50%;
          transform: translateX(-50%);
          background: rgba(128, 24, 35, 0.95);
          border: 1px solid #F87171;
          padding: 6px 18px;
          border-radius: 9999px;
          font-size: 13px;
          font-weight: 700;
          letter-spacing: 0.06em;
          color: #FFFFFF;
          box-shadow: 0 4px 20px rgba(128, 24, 35, 0.6);
          white-space: nowrap;
          animation: calloutPop 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
        }

        @keyframes calloutPop {
          0% { opacity: 0; transform: translateX(-50%) translateY(8px) scale(0.9); }
          100% { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }
        }

        .truehunt-reveal-stage {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-direction: column;
          z-index: 30;
          opacity: 0;
          transform: scale(0.92);
          transition: all 0.9s cubic-bezier(0.16, 1, 0.3, 1);
          pointer-events: none;
        }

        .truehunt-reveal-stage.stage-active {
          opacity: 1;
          transform: scale(1);
        }

        .cinema-lens-flare {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          width: min(850px, 98vw);
          height: 6px;
          background: linear-gradient(90deg, transparent 0%, rgba(251, 191, 36, 0.2) 20%, #F59E0B 50%, rgba(251, 191, 36, 0.2) 80%, transparent 100%);
          filter: blur(2px);
          box-shadow: 0 0 35px #F59E0B, 0 0 80px rgba(245, 158, 11, 0.8);
          animation: flareExpand 1.2s ease-out forwards;
        }

        @keyframes flareExpand {
          0% { width: 0; opacity: 0; }
          50% { opacity: 1; }
          100% { width: min(900px, 98vw); opacity: 0.85; }
        }

        .truehunt-logo-hero {
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          gap: 16px;
          z-index: 40;
        }

        .truehunt-brand-image {
          max-width: min(360px, 82vw);
          height: auto;
          filter: drop-shadow(0 12px 30px rgba(0, 0, 0, 0.9)) drop-shadow(0 0 25px rgba(128, 24, 35, 0.6));
          animation: brandImageZoom 1.2s cubic-bezier(0.16, 1, 0.3, 1) both;
        }

        @keyframes brandImageZoom {
          0% { transform: scale(0.85); opacity: 0; filter: blur(8px); }
          100% { transform: scale(1); opacity: 1; filter: blur(0); }
        }

        .cinema-tagline-badge {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          padding: 6px 18px;
          background: rgba(128, 24, 35, 0.85);
          border: 1px solid rgba(248, 113, 113, 0.6);
          border-radius: 9999px;
          color: #FFFFFF;
          font-size: 13px;
          font-weight: 700;
          letter-spacing: 0.08em;
          text-transform: uppercase;
          box-shadow: 0 6px 25px rgba(128, 24, 35, 0.5);
          animation: badgeSlideUp 0.8s 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
        }

        @keyframes badgeSlideUp {
          0% { opacity: 0; transform: translateY(16px); }
          100% { opacity: 1; transform: translateY(0); }
        }

        .cinema-sub-heading {
          font-size: clamp(16px, 2.5vw, 22px);
          color: #E2E8F0;
          font-weight: 500;
          letter-spacing: -0.01em;
          max-width: 540px;
          line-height: 1.5;
          margin: 0;
          animation: badgeSlideUp 0.8s 0.45s cubic-bezier(0.16, 1, 0.3, 1) both;
        }

        .cinema-action-indicator {
          margin-top: 14px;
          font-size: 14px;
          font-weight: 700;
          color: #F87171;
          letter-spacing: 0.15em;
          text-transform: uppercase;
          animation: actionPulse 1.2s infinite alternate;
        }

        @keyframes actionPulse {
          0% { opacity: 0.6; transform: scale(0.98); }
          100% { opacity: 1; transform: scale(1.02); }
        }
      `}</style>
    </div>
  );
}
