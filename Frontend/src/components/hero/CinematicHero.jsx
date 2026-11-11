import { useState, useEffect, useLayoutEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import RotatingGlobe from './RotatingGlobe';
import FilmStrip3D from './FilmStrip3D';
import CraftCarousel from './CraftCarousel';
import './CinematicHero.css';

const CINEMA_24_CRAFTS = [
  {
    name: 'Actor',
    shortName: 'Actor',
    tagline: 'Live Your Character',
    desc: 'Embody captivating characters and convey powerful human emotions on screen.',
    icon: '🎭',
    image: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d8/Ram_Charan_2024.jpg/250px-Ram_Charan_2024.jpg?utm_source=commons.wikimedia.org&utm_campaign=index&utm_content=thumbnail',
    targetCraft: 'Acting'
  },
  {
    name: 'Director',
    shortName: 'Director',
    tagline: 'Bring Stories to Life',
    desc: 'Orchestrate vision, guide performances, and steer cinematic narrative journeys.',
    icon: '🎬',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/SS_Rajamouli%2C_2021.jpg/250px-SS_Rajamouli%2C_2021.jpg',
    targetCraft: 'Director'
  },
  {
    name: 'Dance Master',
    shortName: 'Dance Master',
    tagline: 'Feel the Rhythm',
    desc: 'Express storytelling through graceful choreography and electrifying energy.',
    icon: '💃',
    image: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5d/Prabhudeva_at_Wanted_press_meet.jpg/250px-Prabhudeva_at_Wanted_press_meet.jpg',
    targetCraft: 'Choreographer'
  },
  {
    name: 'DOP',
    shortName: 'DOP',
    tagline: 'Paint with Light & Lens',
    desc: 'Capture mood, composition, lighting, and camera movement in stunning 4K frames.',
    icon: '🎥',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/d/dc/Master_Class_on_Cinematography_by_K.K._Senthil_Kumar_%28Cinematographer%29%2C_at_the_46th_International_Film_Festival_of_India_%28IFFI-2015%29%2C_in_Panaji%2C_Goa_on_November_26%2C_2015.jpg/250px-thumbnail.jpg',
    targetCraft: 'Cinematographer (DOP)'
  },
  {
    name: 'Producer',
    shortName: 'Producer',
    tagline: 'Build Masterpieces',
    desc: 'Assemble world-class talent, secure financing, and manifest big-screen vision.',
    icon: '🏢',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/Dil_Raju_%28cropped%29.jpg/250px-Dil_Raju_%28cropped%29.jpg',
    targetCraft: 'Producer'
  },
  {
    name: 'Editor',
    shortName: 'Editor',
    tagline: 'Sculpt Film Rhythm',
    desc: 'Pace scenes, construct dramatic tension, and shape the final narrative cut.',
    icon: '✂️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Editor.B.Lenin_%28cropped%29.jpg/250px-Editor.B.Lenin_%28cropped%29.jpg',
    targetCraft: 'Editor'
  },
  {
    name: 'Music Director',
    shortName: 'Music Director',
    tagline: 'Compose Harmonies',
    desc: 'Create stirring background scores and unforgettable chartbuster melodies.',
    icon: '🎵',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ee/Devi_sri_prasad.jpg/250px-Devi_sri_prasad.jpg',
    targetCraft: 'Music Composer'
  },
  {
    name: 'Writer',
    shortName: 'Writer',
    tagline: 'Craft Engaging Scenes',
    desc: 'Architect scene-by-scene screenplay blueprints with dramatic beats and suspense.',
    icon: '📝',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/V._Vijayendra_Prasad_announcing_1st_Jury_Choice_Award_in_Telangana_Avatharana_Filmotsavam_Award_ceremony_in_Ravindra_Bharathi_%2805.06.2018%29_02_%28cropped%29_%28cropped%29.jpg/250px-thumbnail.jpg',
    targetCraft: 'Screenplay Writer'
  },
  {
    name: 'Story Writer',
    shortName: 'Story Writer',
    tagline: 'Conceive Narratives',
    desc: 'Develop original high-concept story plots, characters, and thematic conflicts.',
    icon: '📖',
    image: 'https://upload.wikimedia.org/wikipedia/en/thumb/d/d6/Jandhyala_Subramanya_Sastry.jpg/250px-Jandhyala_Subramanya_Sastry.jpg',
    targetCraft: 'Story Writer'
  },
  {
    name: 'Dialogue Writer',
    shortName: 'Dialogue Writer',
    tagline: 'Voice Every Emotion',
    desc: 'Pen iconic one-liners, punch dialogues, and poignant poetic conversations.',
    icon: '💬',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Trivikram-srinivas-director.jpg/250px-Trivikram-srinivas-director.jpg',
    targetCraft: 'Dialogue Writer'
  },
  {
    name: 'Playback Singer',
    shortName: 'Playback Singer',
    tagline: 'Enchant with Vocals',
    desc: 'Lend mesmerizing vocal textures and soulful playback performance to cinema.',
    icon: '🎙️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Sunitha.jpg/250px-Sunitha.jpg',
    targetCraft: 'Dubbing Artist'
  },
  {
    name: 'Choreography',
    shortName: 'Choreography',
    tagline: 'Design Movements',
    desc: 'Direct mass dance hooks, classical routines, and grand ensemble musical numbers.',
    icon: '✨',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Farah_Khan_at_Lakme_Fashion_Week_2025.jpg/250px-Farah_Khan_at_Lakme_Fashion_Week_2025.jpg',
    targetCraft: 'Choreographer'
  },
  {
    name: 'Stunt Director',
    shortName: 'Stunt Director',
    tagline: 'Execute Action',
    desc: 'Choreograph gravity-defying stunts, thrilling chases, and martial arts fights.',
    icon: '💥',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/A_token_of_appreciation_presented_to_Peter_Hein_Offl_by_the_Deputy_Director%2C_Shri_Rizwan_Ahmed%2C_during_the_47th_International_Film_Festival_of_India_%28IFFI-2016%29%2C_in_Panaji%2C_Goa_on_November_24%2C_2016_%28cropped%29.jpg/250px-thumbnail.jpg',
    targetCraft: 'Stunt Director (Action)'
  },
  {
    name: 'Art Director',
    shortName: 'Art Director',
    tagline: 'Design Worlds',
    desc: 'Conceptualize set architecture, prop design, color palettes, and film aesthetics.',
    icon: '🎨',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Thota_Tharani.jpg/250px-Thota_Tharani.jpg',
    targetCraft: 'Art Director'
  },
  {
    name: 'Costume Designer',
    shortName: 'Costume Designer',
    tagline: 'Weave Identities',
    desc: 'Tailor bespoke character wardrobes, period attire, and signature silhouettes.',
    icon: '👔',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Neeta_Lulla_at_India_Resort_Fashion_Week_2012.jpg/250px-Neeta_Lulla_at_India_Resort_Fashion_Week_2012.jpg',
    targetCraft: 'Costume Designer'
  },
  {
    name: 'Makeup',
    shortName: 'Makeup',
    tagline: 'Transform Looks',
    desc: 'Craft character transformations, glam looks, and prosthetic special effects.',
    icon: '💄',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Pattanam_Rasheed.jpg/250px-Pattanam_Rasheed.jpg',
    targetCraft: 'Makeup & Hair Stylist'
  },
  {
    name: 'Sound Design',
    shortName: 'Sound Design ',
    tagline: 'Acoustic Landscapes',
    desc: 'Record Foley, synthesize atmospheric soundscapes, and mix immersive Dolby audio.',
    icon: '🎧',
    image: 'https://images.openai.com/static-rsc-4/fTPQNsiXbaMjqmXq20G7TDLoRv8G9pzVYmOMoCaI1vfNANSOVBPv3C2Ru2FaE6ZGOkW9sGstAkffhX7SLPyqkpMVnWsjDCE8euDBjf96Er5rzSoQZQifysrO7MAXOJdO8yUSKBnGNBf1SOrnHi-Gof0MBmZas1cFWGPvL51BDzv0urh9aSFOLA7o1xZ9kzav?purpose=fullsize',
    targetCraft: 'Audiography (Sound Recording/Mixing)'
  },
  {
    name: 'VFX',
    shortName: 'VFX',
    tagline: 'Create Impossible',
    desc: 'Generate photorealistic CGI spectacles, fantasy creatures, and digital environments.',
    icon: '🔮',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Pc_sreeram_shooting.jpg/250px-Pc_sreeram_shooting.jpg',
    targetCraft: 'VFX & Animation'
  },
  {
    name: 'Dubbing',
    shortName: 'DUBBING',
    tagline: 'Flawless Lip-Sync',
    desc: 'Voice multilingual translations and enhance emotional dialogue performances.',
    icon: '🎙️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/7/77/Sai_Kumar_at_IFFI-2010%2C_in_Panjim%2C_Goa_on_November_27%2C_2010_%28cropped%29.jpg ',
    targetCraft: 'Dubbing Artist'
  },
  {
    name: 'STILL Photography',
    shortName: 'STILL PHOTOGRAPHY',
    tagline: 'Capture Stills',
    desc: 'Preserve on-set magic through striking publicity stills, posters, and coverage.',
    icon: '📷',
    image: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Raghu_Rai_pix_by_Vikramjit_Kakati.jpg/250px-Raghu_Rai_pix_by_Vikramjit_Kakati.jpg',
    targetCraft: 'Still Photographer'
  },
  {
    name: 'PRODUCTION Manager',
    shortName: 'PRODUCTION Manager',
    tagline: 'Flawless Logistics',
    desc: 'Oversee daily shooting schedules, crew logistics, permits, and equipment operations.',
    icon: '📋',
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRX5NrljI2o2zIbxQ_0PAMS6xYC49eptRuJBUAVXMuYsyVQV8EiXuNpjxqkBLla4Yj2I86qCg5DwykJ1_hm_eUUVD2tpnIYpZsawCCgQw&s=10',
    targetCraft: 'Production Executive'
  },
  {
    name: 'SET Designer',
    shortName: 'SET Designer',
    tagline: 'Construct Realities',
    desc: 'Erect grandiose physical film sets, authentic interiors, and palace facades.',
    icon: '🏛️',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Sabu_cyril_profile_picture.jpg/250px-Sabu_cyril_profile_picture.jpg',
    targetCraft: 'Art Director'
  },
  {
    name: 'DISTRIBUTION',
    shortName: 'DISTRIBUTION',
    tagline: 'Reach Theatres',
    desc: 'Deliver movies across multiplexes, single screens, OTT streaming, and world territories.',
    icon: '🎟️',
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ5Y5uXKnOJm7I98srVqmi7YX5dmBOkL4AWmz1Ynxu7amXHxZDk30Hb5aYG2GF0yXPql9Lyss6QaqLGDgDpjezKK9ZtC0FqUns6soG6ipXC1A&s=10',
    targetCraft: 'Producer'
  },
  {
    name: 'production Support',
    shortName: 'production Support',
    tagline: 'Power Every Unit',
    desc: 'Lighting technicians, riggers, spot teams, and skilled studio specialists.',
    icon: '⚡',
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSERyScUAqNB4Y1hqfdAUfpkRvvIAafINLaw-PIziZOutcPglAsnkkl3BSPGfBiYAoWD4L2l4B2dgyWFoCIXHRmcwgV0nLoboYNezyNyw&s=10',
    targetCraft: 'Studio/Production Support Workers'
  },
  {
    name:'MOdel',
    shortName:'Model',
    tagline:'Model for the Camera',
    desc:'Look wherever you want, but look forward and we shall succeed',
    icon:'🎯',
    image:'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRRajBCqF5pQqpqep2ADaM4dhU-tksZkKi3xxqxtRm9Ybn67frvK2uX-Ds&s=10',
    targetCraft:'Modal'
  }
];
export default function CinematicHero() {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [photoSlideIndex, setPhotoSlideIndex] = useState(0);
  const [isCardVisible, setIsCardVisible] = useState(false);
  const initialRotationOffset = Math.PI / 2;
  const [rotationOffset, setRotationOffset] = useState(initialRotationOffset);
  const [stageSize, setStageSize] = useState({ width: 1000, height: 520 });
  const stageRef = useRef(null);
  const navigate = useNavigate();
  const N = CINEMA_24_CRAFTS.length;

  const animRef = useRef({
    currentAngle: initialRotationOffset,
    targetAngle: initialRotationOffset,
    animating: false,
    startTime: null,
    startAngle: initialRotationOffset
  });
  const animFrameId = useRef(null);
  const cardHideTimeoutRef = useRef(null);

  useEffect(() => {
    const slideshowTimer = window.setInterval(() => {
      setPhotoSlideIndex((index) => index + 1);
    }, 3000);

    return () => window.clearInterval(slideshowTimer);
  }, []);

  const revealCard = useCallback(() => {
    setIsCardVisible(true);
    window.clearTimeout(cardHideTimeoutRef.current);
    cardHideTimeoutRef.current = window.setTimeout(() => {
      setIsCardVisible(false);
    }, 5000);
  }, []);

  // Smooth ease-out animation step for user navigation
  const animateToAngle = useCallback((targetAngle) => {
    animRef.current.targetAngle = targetAngle;
    animRef.current.startAngle = animRef.current.currentAngle;
    animRef.current.startTime = null;

    if (!animRef.current.animating) {
      animRef.current.animating = true;

      const step = (timestamp) => {
        if (!animRef.current.startTime) animRef.current.startTime = timestamp;
        const progress = Math.min(1, (timestamp - animRef.current.startTime) / 420);
        // Cubic ease-out
        const ease = 1 - Math.pow(1 - progress, 3);
        
        const nextAngle = animRef.current.startAngle + (animRef.current.targetAngle - animRef.current.startAngle) * ease;
        animRef.current.currentAngle = nextAngle;
        setRotationOffset(nextAngle);

        if (progress < 1) {
          animFrameId.current = requestAnimationFrame(step);
        } else {
          animRef.current.animating = false;
          animRef.current.currentAngle = animRef.current.targetAngle;
          setRotationOffset(animRef.current.targetAngle);
        }
      };

      animFrameId.current = requestAnimationFrame(step);
    }
  }, []);

  useEffect(() => {
    return () => {
      if (animFrameId.current) cancelAnimationFrame(animFrameId.current);
      window.clearTimeout(cardHideTimeoutRef.current);
    };
  }, []);

  useLayoutEffect(() => {
    const stage = stageRef.current;
    if (!stage) return undefined;

    const updateStageSize = (width, height) => {
      const nextSize = {
        width: Math.max(1, Math.round(width / 8) * 8),
        height: Math.max(1, Math.round(height / 8) * 8),
      };
      setStageSize((currentSize) => (
        currentSize.width === nextSize.width && currentSize.height === nextSize.height
          ? currentSize
          : nextSize
      ));
    };

    const observer = new ResizeObserver(([entry]) => {
      updateStageSize(entry.contentRect.width, entry.contentRect.height);
    });
    observer.observe(stage);
    updateStageSize(stage.clientWidth, stage.clientHeight);

    return () => observer.disconnect();
  }, []);

  // ONLY move when the user clicks the Left button
  const handlePrev = useCallback(() => {
    revealCard();
    const nextIdx = (currentIndex - 1 + N) % N;
    setCurrentIndex(nextIdx);
    const targetAngle = animRef.current.targetAngle + (2 * Math.PI) / N;
    animateToAngle(targetAngle);
  }, [currentIndex, N, animateToAngle, revealCard]);

  // ONLY move when the user clicks the Right button
  const handleNext = useCallback(() => {
    revealCard();
    const nextIdx = (currentIndex + 1) % N;
    setCurrentIndex(nextIdx);
    const targetAngle = animRef.current.targetAngle - (2 * Math.PI) / N;
    animateToAngle(targetAngle);
  }, [currentIndex, N, animateToAngle, revealCard]);

  // Move when user clicks a specific craft indicator or miniature frame
  const handleSelectCraft = useCallback((targetIndex) => {
    revealCard();
    let diff = targetIndex - currentIndex;
    if (diff > N / 2) diff -= N;
    if (diff < -N / 2) diff += N;
    
    setCurrentIndex(targetIndex);
    const targetAngle = animRef.current.targetAngle - diff * ((2 * Math.PI) / N);
    animateToAngle(targetAngle);
  }, [currentIndex, N, animateToAngle, revealCard]);

  // Open the talent directory filtered to the selected craft.
  const handleCraftClick = useCallback((craft) => {
    const craftParam = craft.targetCraft || craft.name;
    navigate(`/talents?craft=${encodeURIComponent(craftParam)}`);
  }, [navigate]);

  return (
    <section 
      className="cinematic-hero-banner"
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
        
        <header className="cinematic-hero-header">
          
          <div className="cinematic-brand-pill">
            <Sparkles size={14} className="sparkle-icon" />
            <span>The Global Cinema & Film Crafts Network</span>
          </div>

          <h1 className="cinematic-main-heading">
            Your <span className="gold-text">Talent.</span> Your <span className="gold-text">Craft.</span> Your <span className="gold-text-glow">Cinema Journey.</span>
          </h1>
        </header>

        {/* Center Stage: 3D Earth Globe + Orbiting Golden Film Strip with Circular Craft Images */}
        <div className="cinematic-stage-arena" ref={stageRef}>
          
          {/* Central 3D Earth Globe (Rotates continuously on vertical axis) */}
          <div className="globe-center-pod">
            <RotatingGlobe
              size={Math.floor(Math.min(
                600,
                stageSize.width * (stageSize.width <= 640 ? 0.74 : 0.66),
                stageSize.height * 0.99,
              ))}
              isRotating={true}
            />
          </div>

          {/* Golden 3D Revolving Cinema Film Reel with Circular Miniatures */}
          <FilmStrip3D
            crafts={CINEMA_24_CRAFTS}
            rotationOffset={rotationOffset}
            photoSlideIndex={photoSlideIndex}
            onSelectCraft={handleSelectCraft}
            onCraftClick={handleCraftClick}
            width={stageSize.width}
            height={stageSize.height}
          />

          {/* Front 3 Craft Showcase Cards with Circular Images (Moves ONLY on Left/Right Click) */}
          <CraftCarousel
            crafts={CINEMA_24_CRAFTS}
            currentIndex={currentIndex}
            photoSlideIndex={photoSlideIndex}
            isCardVisible={isCardVisible}
            onPrev={handlePrev}
            onNext={handleNext}
            onSelectCraft={handleSelectCraft}
            onCraftClick={handleCraftClick}
          />

        </div>
      </div>
    </section>
  );
}
