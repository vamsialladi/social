import React from 'react';
import { 
  ChevronLeft, 
  ChevronRight, 
  Pause, 
  Play, 
  ArrowUpRight, 
  Sparkles,
  Clapperboard
} from 'lucide-react';

/**
 * ============================================================================
 * CraftCarousel – Front 3 Craft Showcase with Full Carousel Controls
 * ============================================================================
 * Features:
 * - Displays 3 prominent craft cards in the front (Left Prev, Center Active, Right Next)
 * - Clickable cards navigating to `/join-now?craft=...`
 * - Previous / Next navigation controls
 * - Pause / Play auto-rotation toggle
 * - 24 Craft pagination indicator dots
 * - Responsive layout for desktop, tablet, and mobile
 */
export default function CraftCarousel({
  crafts = [],
  currentIndex = 0,
  isPaused = false,
  onPrev,
  onNext,
  onTogglePause,
  onSelectCraft,
  onCraftClick
}) {
  const N = crafts.length || 24;

  // Retrieve previous, current, and next crafts
  const prevIndex = (currentIndex - 1 + N) % N;
  const nextIndex = (currentIndex + 1) % N;

  const currentCraft = crafts[currentIndex] || crafts[0];
  const prevCraft = crafts[prevIndex] || crafts[N - 1];
  const nextCraft = crafts[nextIndex] || crafts[1];

  const handleCardClick = (craft) => {
    if (onCraftClick) {
      onCraftClick(craft);
    }
  };

  return (
    <div className="craft-carousel-root">
      
      {/* 3 Prominent Craft Cards in Front of the Globe */}
      <div className="craft-cards-stage">
        
        {/* Left Craft Card (Slot -1) */}
        <div
          className="craft-card-slot slot-left"
          onClick={() => {
            onPrev();
            handleCardClick(prevCraft);
          }}
          title={`Previous Craft: ${prevCraft.name}`}
          role="button"
          tabIndex={0}
        >
          <div className="craft-card-inner">
            <div className="craft-card-media">
              <img
                src={prevCraft.image}
                alt={prevCraft.name}
                className="craft-card-img"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
              <div className="craft-card-badge">
                <span className="craft-badge-icon">{prevCraft.icon || '🎬'}</span>
                <span>{prevCraft.name}</span>
              </div>
            </div>
            <div className="craft-card-content">
              <span className="craft-tagline">{prevCraft.tagline}</span>
            </div>
          </div>
        </div>

        {/* Center Craft Card (Slot 0 - Featured Active) */}
        <div
          className="craft-card-slot slot-center"
          onClick={() => handleCardClick(currentCraft)}
          title={`Click to Join TrueHunt as ${currentCraft.name}`}
          role="button"
          tabIndex={0}
        >
          <div className="craft-card-inner card-featured">
            <div className="craft-card-gold-glow" />
            
            <div className="craft-card-media">
              <img
                src={currentCraft.image}
                alt={currentCraft.name}
                className="craft-card-img"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
              <div className="craft-card-overlay-grad" />
              
              {/* Category Pill */}
              <div className="craft-card-badge badge-gold">
                <span className="craft-badge-icon">{currentCraft.icon || '🎬'}</span>
                <span className="craft-badge-name">{currentCraft.name}</span>
                <span className="craft-badge-counter">{currentIndex + 1}/24</span>
              </div>

              {/* Instant Join Click Pill */}
              <div className="craft-join-cta-pill">
                <span>Join as {currentCraft.name}</span>
                <ArrowUpRight size={14} />
              </div>
            </div>

            <div className="craft-card-content content-featured">
              <h3 className="craft-headline">
                <span className="craft-headline-name">{currentCraft.name}</span>
                <span className="craft-headline-sep">—</span>
                <span className="craft-headline-tagline">{currentCraft.tagline}</span>
              </h3>
              <p className="craft-desc">{currentCraft.desc}</p>
            </div>
          </div>
        </div>

        {/* Right Craft Card (Slot +1) */}
        <div
          className="craft-card-slot slot-right"
          onClick={() => {
            onNext();
            handleCardClick(nextCraft);
          }}
          title={`Next Craft: ${nextCraft.name}`}
          role="button"
          tabIndex={0}
        >
          <div className="craft-card-inner">
            <div className="craft-card-media">
              <img
                src={nextCraft.image}
                alt={nextCraft.name}
                className="craft-card-img"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
              <div className="craft-card-badge">
                <span className="craft-badge-icon">{nextCraft.icon || '🎬'}</span>
                <span>{nextCraft.name}</span>
              </div>
            </div>
            <div className="craft-card-content">
              <span className="craft-tagline">{nextCraft.tagline}</span>
            </div>
          </div>
        </div>

      </div>

      {/* Carousel Navigation & Playback Controls Bar */}
      <div className="craft-controls-bar">
        
        {/* Previous Button */}
        <button
          type="button"
          onClick={onPrev}
          className="craft-ctrl-arrow-btn"
          aria-label="Previous Craft"
          title="Previous Craft (Arrow Left)"
        >
          <ChevronLeft size={20} />
        </button>

        {/* Center Indicators and Play/Pause */}
        <div className="craft-center-playback">
          <button
            type="button"
            onClick={onTogglePause}
            className={`craft-play-pause-btn ${isPaused ? 'btn-paused' : 'btn-playing'}`}
            aria-label={isPaused ? 'Resume auto-rotation' : 'Pause auto-rotation'}
            title={isPaused ? 'Resume Carousel' : 'Pause Carousel'}
          >
            {isPaused ? <Play size={14} /> : <Pause size={14} />}
            <span>{isPaused ? 'Play' : 'Pause'}</span>
          </button>

          <span className="craft-active-label">
            <span style={{ color: '#F59E0B', fontWeight: 800 }}>{currentCraft.name}</span>
            <span style={{ color: '#94A3B8', margin: '0 6px' }}>•</span>
            <span style={{ color: '#E2E8F0' }}>Craft {currentIndex + 1} of {N}</span>
          </span>
        </div>

        {/* Next Button */}
        <button
          type="button"
          onClick={onNext}
          className="craft-ctrl-arrow-btn"
          aria-label="Next Craft"
          title="Next Craft (Arrow Right)"
        >
          <ChevronRight size={20} />
        </button>

      </div>

      {/* 24 Craft Mini Progress Dots */}
      <div className="craft-dots-indicator" role="tablist" aria-label="Craft indicators">
        {crafts.map((c, i) => (
          <button
            key={c.name}
            type="button"
            role="tab"
            aria-selected={i === currentIndex}
            aria-label={`Jump to ${c.name}`}
            onClick={() => onSelectCraft && onSelectCraft(i)}
            className={`craft-dot ${i === currentIndex ? 'active-dot' : ''}`}
            title={`${i + 1}. ${c.name}`}
          />
        ))}
      </div>

    </div>
  );
}
