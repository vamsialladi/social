import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, ArrowRight, Film, Globe2 } from 'lucide-react';
import RotatingGlobe from './RotatingGlobe';
import FilmStrip3D from './FilmStrip3D';
import CraftCarousel from './CraftCarousel';
import './CinematicHero.css';

/**
 * ============================================================================
 * TrueHunt 24 Cinema Crafts Definition with Existing Popular Roles Images
 * ============================================================================
 */
export const CINEMA_24_CRAFTS = [
  {
    name: 'Acting',
    shortName: 'ACT',
    tagline: 'Live Your Character',
    desc: 'Embody captivating characters and convey powerful human emotions on screen.',
    icon: '🎭',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/Allu_Arjun_at_Pushpa_2_The_Rule_meet.jpg/250px-Allu_Arjun_at_Pushpa_2_The_Rule_meet.jpg',
    targetCraft: 'Cast (Actors)'
  },
  {
    name: 'Direction',
    shortName: 'DIR',
    tagline: 'Bring Stories to Life',
    desc: 'Orchestrate vision, guide performances, and steer cinematic narrative journeys.',
    icon: '🎬',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/SS_Rajamouli%2C_2021.jpg/250px-SS_Rajamouli%2C_2021.jpg',
    targetCraft: 'Director'
  },
  {
    name: 'Dancing',
    shortName: 'DANCE',
    tagline: 'Feel the Rhythm',
    desc: 'Express storytelling through graceful choreography and electrifying energy.',
    icon: '💃',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Farah_Khan_at_Lakme_Fashion_Week_2025.jpg/250px-Farah_Khan_at_Lakme_Fashion_Week_2025.jpg',
    targetCraft: 'Choreographer'
  },
  {
    name: 'Cinematography',
    shortName: 'DOP',
    tagline: 'Paint with Light & Lens',
    desc: 'Capture mood, composition, lighting, and camera movement in stunning 4K frames.',
    icon: '🎥',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/d/dc/Master_Class_on_Cinematography_by_K.K._Senthil_Kumar_%28Cinematographer%29%2C_at_the_46th_International_Film_Festival_of_India_%28IFFI-2015%29%2C_in_Panaji%2C_Goa_on_November_26%2C_2015.jpg/250px-thumbnail.jpg',
    targetCraft: 'Cinematographer (DOP)'
  },
  {
    name: 'Production',
    shortName: 'PROD',
    tagline: 'Build Masterpieces',
    desc: 'Assemble world-class talent, secure financing, and manifest big-screen vision.',
    icon: '🏢',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/Dil_Raju_%28cropped%29.jpg/250px-Dil_Raju_%28cropped%29.jpg',
    targetCraft: 'Producer'
  },
  {
    name: 'Editing',
    shortName: 'EDIT',
    tagline: 'Sculpt Film Rhythm',
    desc: 'Pace scenes, construct dramatic tension, and shape the final narrative cut.',
    icon: '✂️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Editor.B.Lenin_%28cropped%29.jpg/250px-Editor.B.Lenin_%28cropped%29.jpg',
    targetCraft: 'Editor'
  },
  {
    name: 'Music Direction',
    shortName: 'MUSIC',
    tagline: 'Compose Harmonies',
    desc: 'Create stirring background scores and unforgettable chartbuster melodies.',
    icon: '🎵',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ee/Devi_sri_prasad.jpg/250px-Devi_sri_prasad.jpg',
    targetCraft: 'Music Composer'
  },
  {
    name: 'Screenwriting',
    shortName: 'SCRIPT',
    tagline: 'Craft Engaging Scenes',
    desc: 'Architect scene-by-scene screenplay blueprints with dramatic beats and suspense.',
    icon: '📝',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/V._Vijayendra_Prasad_announcing_1st_Jury_Choice_Award_in_Telangana_Avatharana_Filmotsavam_Award_ceremony_in_Ravindra_Bharathi_%2805.06.2018%29_02_%28cropped%29_%28cropped%29.jpg/250px-thumbnail.jpg',
    targetCraft: 'Screenplay Writer'
  },
  {
    name: 'Story Writing',
    shortName: 'STORY',
    tagline: 'Conceive Narratives',
    desc: 'Develop original high-concept story plots, characters, and thematic conflicts.',
    icon: '📖',
    image: 'https://upload.wikimedia.org/wikipedia/en/thumb/d/d6/Jandhyala_Subramanya_Sastry.jpg/250px-Jandhyala_Subramanya_Sastry.jpg',
    targetCraft: 'Story Writer'
  },
  {
    name: 'Dialogue Writing',
    shortName: 'LINES',
    tagline: 'Voice Every Emotion',
    desc: 'Pen iconic one-liners, punch dialogues, and poignant poetic conversations.',
    icon: '💬',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Trivikram-srinivas-director.jpg/250px-Trivikram-srinivas-director.jpg',
    targetCraft: 'Dialogue Writer'
  },
  {
    name: 'Playback Singing',
    shortName: 'VOCAL',
    tagline: 'Enchant with Vocals',
    desc: 'Lend mesmerizing vocal textures and soulful playback performance to cinema.',
    icon: '🎙️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Sunitha.jpg/250px-Sunitha.jpg',
    targetCraft: 'Dubbing Artist'
  },
  {
    name: 'Choreography',
    shortName: 'CHOREO',
    tagline: 'Design Movements',
    desc: 'Direct mass dance hooks, classical routines, and grand ensemble musical numbers.',
    icon: '✨',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Farah_Khan_at_Lakme_Fashion_Week_2025.jpg/250px-Farah_Khan_at_Lakme_Fashion_Week_2025.jpg',
    targetCraft: 'Choreographer'
  },
  {
    name: 'Stunt Direction',
    shortName: 'STUNT',
    tagline: 'Execute Action',
    desc: 'Choreograph gravity-defying stunts, thrilling chases, and martial arts fights.',
    icon: '💥',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/A_token_of_appreciation_presented_to_Peter_Hein_Offl_by_the_Deputy_Director%2C_Shri_Rizwan_Ahmed%2C_during_the_47th_International_Film_Festival_of_India_%28IFFI-2016%29%2C_in_Panaji%2C_Goa_on_November_24%2C_2016_%28cropped%29.jpg/250px-thumbnail.jpg',
    targetCraft: 'Stunt Director (Action)'
  },
  {
    name: 'Art Direction',
    shortName: 'ART',
    tagline: 'Design Worlds',
    desc: 'Conceptualize set architecture, prop design, color palettes, and film aesthetics.',
    icon: '🎨',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Thota_Tharani.jpg/250px-Thota_Tharani.jpg',
    targetCraft: 'Art Director'
  },
  {
    name: 'Costume Design',
    shortName: 'WARD',
    tagline: 'Weave Identities',
    desc: 'Tailor bespoke character wardrobes, period attire, and signature silhouettes.',
    icon: '👔',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Neeta_Lulla_at_India_Resort_Fashion_Week_2012.jpg/250px-Neeta_Lulla_at_India_Resort_Fashion_Week_2012.jpg',
    targetCraft: 'Costume Designer'
  },
  {
    name: 'Makeup',
    shortName: 'MAKEUP',
    tagline: 'Transform Looks',
    desc: 'Craft character transformations, glam looks, and prosthetic special effects.',
    icon: '💄',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Pattanam_Rasheed.jpg/250px-Pattanam_Rasheed.jpg',
    targetCraft: 'Makeup & Hair Stylist'
  },
  {
    name: 'Sound Design',
    shortName: 'SOUND',
    tagline: 'Acoustic Landscapes',
    desc: 'Record Foley, synthesize atmospheric soundscapes, and mix immersive Dolby audio.',
    icon: '🎧',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/Resul_Pookutty_60th_Filmfare_Awards_%28cropped%29.jpg/250px-Resul_Pookutty_60th_Filmfare_Awards_%28cropped%29.jpg',
    targetCraft: 'Audiography (Sound Recording/Mixing)'
  },
  {
    name: 'VFX',
    shortName: 'VFX',
    tagline: 'Create Impossible',
    desc: 'Generate photorealistic CGI spectacles, fantasy creatures, and digital environments.',
    icon: '🔮',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Pc_sreeram_shooting.jpg/250px-Pc_sreeram_shooting.jpg',
    targetCraft: 'Publicity Designer'
  },
  {
    name: 'Dubbing',
    shortName: 'VOICE',
    tagline: 'Flawless Lip-Sync',
    desc: 'Voice multilingual translations and enhance emotional dialogue performances.',
    icon: '🎙️',
    image: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/S._P._Balasubrahmanyam_at_the_%27Gurkha%27_Audio_Launch.jpg/250px-S._P._Balasubrahmanyam_at_the_%27Gurkha%27_Audio_Launch.jpg',
    targetCraft: 'Dubbing Artist'
  },
  {
    name: 'Photography',
    shortName: 'PHOTO',
    tagline: 'Capture Stills',
    desc: 'Preserve on-set magic through striking publicity stills, posters, and coverage.',
    icon: '📷',
    image: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Raghu_Rai_pix_by_Vikramjit_Kakati.jpg/250px-Raghu_Rai_pix_by_Vikramjit_Kakati.jpg',
    targetCraft: 'Still Photographer'
  },
  {
    name: 'Production Management',
    shortName: 'MGMT',
    tagline: 'Flawless Logistics',
    desc: 'Oversee daily shooting schedules, crew logistics, permits, and equipment operations.',
    icon: '📋',
    image: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Karan_Johar_at_2025_Cannes_Homebound_Photocall_2_%28cropped%29.jpg/250px-Karan_Johar_at_2025_Cannes_Homebound_Photocall_2_%28cropped%29.jpg',
    targetCraft: 'Production Executive'
  },
  {
    name: 'Set Design',
    shortName: 'SET',
    tagline: 'Construct Realities',
    desc: 'Erect grandiose physical film sets, authentic interiors, and palace facades.',
    icon: '🏛️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Sabu_cyril_profile_picture.jpg/250px-Sabu_cyril_profile_picture.jpg',
    targetCraft: 'Art Director'
  },
  {
    name: 'Distribution',
    shortName: 'DIST',
    tagline: 'Reach Theatres',
    desc: 'Deliver movies across multiplexes, single screens, OTT streaming, and world territories.',
    icon: '🎟️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Daggubati_Suresh_Babu.jpg/250px-Daggubati_Suresh_Babu.jpg',
    targetCraft: 'Producer'
  },
  {
    name: 'Other Crafts',
    shortName: 'ALLIED',
    tagline: 'Power Every Unit',
    desc: 'Lighting technicians, riggers, spot teams, and skilled studio specialists.',
    icon: '⚡',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Nagarjuna_Akkineni_at_ANR_Awards.jpg/250px-Nagarjuna_Akkineni_at_ANR_Awards.jpg',
    targetCraft: 'Studio/Production Support Workers'
  }
];

/**
 * ============================================================================
 * CinematicHero – Top Homepage Banner with 3D Earth & Rotating Golden Film Strip
 * ============================================================================
 */
export default function CinematicHero() {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [rotationOffset, setRotationOffset] = useState(0);
  const navigate = useNavigate();
  const N = CINEMA_24_CRAFTS.length;

  const timerRef = useRef(null);
  const animFrameRef = useRef(null);
  const lastTimeRef = useRef(null);

  // Sync 3D Film Strip Continuous Rotation
  useEffect(() => {
    const loop = (timestamp) => {
      if (lastTimeRef.current === null) lastTimeRef.current = timestamp;
      const delta = (timestamp - lastTimeRef.current) / 1000;
      lastTimeRef.current = timestamp;

      // Continuously rotate ribbon subtly
      if (!isPaused) {
        setRotationOffset((prev) => prev - delta * 0.14);
      }

      animFrameRef.current = requestAnimationFrame(loop);
    };

    animFrameRef.current = requestAnimationFrame(loop);
    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [isPaused]);

  // Automatic Craft Carousel Rotation every 4 seconds
  useEffect(() => {
    if (isPaused) return;

    timerRef.current = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % N);
    }, 4000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPaused, N]);

  // Navigate to previous craft
  const handlePrev = useCallback(() => {
    setCurrentIndex((prev) => (prev - 1 + N) % N);
    // Align ribbon angle to active craft
    setRotationOffset((prev) => prev + (2 * Math.PI) / N);
  }, [N]);

  // Navigate to next craft
  const handleNext = useCallback(() => {
    setCurrentIndex((prev) => (prev + 1) % N);
    setRotationOffset((prev) => prev - (2 * Math.PI) / N);
  }, [N]);

  // Jump to specific craft
  const handleSelectCraft = useCallback((index) => {
    setCurrentIndex(index);
    // Calculate targeted angle offset
    setRotationOffset(Math.PI / 2 - (index / N) * 2 * Math.PI);
  }, [N]);

  // Click on craft card -> Navigate to Join Now page with preselected craft
  const handleCraftClick = useCallback((craft) => {
    const craftParam = craft.targetCraft || craft.name;
    navigate(`/join-now?craft=${encodeURIComponent(craftParam)}`);
  }, [navigate]);

  // Explore All Crafts button -> Smooth scroll to crafts/popular roles section
  const handleExploreAllCrafts = () => {
    const target = document.getElementById('popular-roles') || document.querySelector('.hero-roles-section') || document.querySelector('.landing-crafts-grid');
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      navigate('/talents');
    }
  };

  return (
    <section 
      className="cinematic-hero-banner"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      aria-label="TrueHunt 3D Cinematic Globe & Film Strip Showcase"
    >
      {/* Deep Space Background with Stars & Golden Atmospheric Nebula */}
      <div className="cinematic-space-bg">
        <div className="space-star-layer space-stars-1" />
        <div className="space-star-layer space-stars-2" />
        <div className="space-nebula-golden" />
        <div className="space-nebula-blue" />
        <div className="space-vignette-overlay" />
      </div>

      <div className="cinematic-hero-container">
        
        {/* Top Header & Tagline */}
        <header className="cinematic-hero-header">
          
          <div className="cinematic-brand-pill">
            <Sparkles size={14} className="sparkle-icon" />
            <span>The Global Cinema & Film Crafts Network</span>
          </div>

          <h1 className="cinematic-main-heading">
            Your <span className="gold-text">Talent.</span> Your <span className="gold-text">Craft.</span> Your <span className="gold-text-glow">Cinema Journey.</span>
          </h1>

          <p className="cinematic-sub-heading">
            Discover opportunities, showcase your talent, and connect with the film industry through TrueHunt.
          </p>

        </header>

        {/* Center Stage: Rotating 3D Globe + Orbiting Golden Film Strip */}
        <div className="cinematic-stage-arena">
          
          {/* Central 3D Earth Globe with Atmospheric Rim Lighting */}
          <div className="globe-center-pod">
            <RotatingGlobe size={360} isRotating={!isPaused} />
          </div>

          {/* Golden 3D Revolving Cinema Film Reel */}
          <FilmStrip3D
            crafts={CINEMA_24_CRAFTS}
            currentIndex={currentIndex}
            rotationOffset={rotationOffset}
            onSelectCraft={handleSelectCraft}
            isPaused={isPaused}
            width={980}
            height={520}
          />

          {/* Front 3 Craft Showcase Cards with Interactive Controls */}
          <CraftCarousel
            crafts={CINEMA_24_CRAFTS}
            currentIndex={currentIndex}
            isPaused={isPaused}
            onPrev={handlePrev}
            onNext={handleNext}
            onTogglePause={() => setIsPaused((p) => !p)}
            onSelectCraft={handleSelectCraft}
            onCraftClick={handleCraftClick}
          />

        </div>

        {/* Bottom CTA: Explore All Crafts Button */}
        <footer className="cinematic-hero-footer">
          <button
            type="button"
            onClick={handleExploreAllCrafts}
            className="explore-all-crafts-btn"
            title="Explore all 24 cinema crafts on TrueHunt"
          >
            <span>Explore All 24 Crafts</span>
            <ArrowRight size={17} className="explore-arrow-icon" />
          </button>
        </footer>

      </div>
    </section>
  );
}
