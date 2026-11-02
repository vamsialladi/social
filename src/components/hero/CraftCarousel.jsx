import React from 'react';
import { 
  ChevronLeft, 
  ChevronRight, 
  ArrowUpRight, 
  Sparkles,
  Film
} from 'lucide-react';

/**
 * ============================================================================
 * CraftCarousel – Circular Craft Images with User-Driven Left/Right Navigation
 * ============================================================================
 * Features:
 * - 100% Circular shaped images for all crafts with golden film-reel rings
 * - Three visible front craft cards (Previous, Center Active, Next)
 * - Moves ONLY when the user clicks the Left and Right navigation buttons
 * - Clickable to join/register as that craft on TrueHunt
 * - Interactive pagination dots allowing direct jump to any of the 24 crafts
 */
export default function CraftCarousel({
  crafts = [],
  currentIndex = 0,
  onPrev,
  onNext,
  onSelectCraft,
  onCraftClick
}) {
  const N = crafts.length || 24;

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
      
      {/* 3 Prominent Craft Cards with Circular Images */}
      <div className="craft-cards-stage">
        
        {/* Left Craft Card (Slot -1) */}
        <div
          className="craft-card-slot slot-left"
          onClick={() => {
            onPrev();
          }}
          title={`Click to view Previous: ${prevCraft.name}`}
          role="button"
          tabIndex={0}
        >
          <div className="craft-card-inner card-circular">
            {/* Circular Image Container */}
            <div className="craft-circle-media circle-side">
              <div className="craft-circle-ring" />
              <img
                src={prevCraft.image}
                alt={prevCraft.name}
                className="craft-circle-img"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
              <span className="craft-circle-icon">{prevCraft.icon || '🎬'}</span>
            </div>
            
            <div className="craft-card-content">
              <span className="craft-name-side">{prevCraft.name}</span>
              <span className="craft-tagline-side">{prevCraft.tagline}</span>
            </div>
          </div>
        </div>

        {/* Center Featured Craft Card (Slot 0 - Active) */}
        <div
          className="craft-card-slot slot-center"
          onClick={() => handleCardClick(currentCraft)}
          title={`Click to Join TrueHunt as ${currentCraft.name}`}
          role="button"
          tabIndex={0}
        >
          <div className="craft-card-inner card-featured card-circular-featured">
            <div className="craft-card-gold-glow" />
            
            {/* Top Counter Badge */}
            <div className="craft-top-badge">
              <span className="craft-top-badge-icon">{currentCraft.icon || '🎬'}</span>
              <span className="craft-top-badge-name">{currentCraft.name}</span>
              <span className="craft-top-badge-num">{currentIndex + 1} / {N}</span>
            </div>

            {/* Large Glowing Circular Image with Film Reel Border */}
            <div className="craft-circle-media circle-center">
              <div className="craft-film-sprocket-ring" />
              <div className="craft-circle-gold-halo" />
              <img
                src={currentCraft.image}
                alt={currentCraft.name}
                className="craft-circle-img"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
              <div className="craft-circle-overlay-shimmer" />
            </div>

            {/* Craft Title & Tagline */}
            <div className="craft-card-content content-featured">
              <h3 className="craft-headline">
                <span className="craft-headline-name">{currentCraft.name}</span>
                <span className="craft-headline-sep">—</span>
                <span className="craft-headline-tagline">{currentCraft.tagline}</span>
              </h3>
              <p className="craft-desc">{currentCraft.desc}</p>
              
              {/* Click to Join CTA */}
              <div className="craft-join-cta-row">
                <span className="craft-join-cta-btn">
                  <span>Join as {currentCraft.name}</span>
                  <ArrowUpRight size={15} />
                </span>
                <span className="craft-click-hint">Click card to start journey</span>
              </div>
            </div>

          </div>
        </div>

        {/* Right Craft Card (Slot +1) */}
        <div
          className="craft-card-slot slot-right"
          onClick={() => {
            onNext();
          }}
          title={`Click to view Next: ${nextCraft.name}`}
          role="button"
          tabIndex={0}
        >
          <div className="craft-card-inner card-circular">
            {/* Circular Image Container */}
            <div className="craft-circle-media circle-side">
              <div className="craft-circle-ring" />
              <img
                src={nextCraft.image}
                alt={nextCraft.name}
                className="craft-circle-img"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
              <span className="craft-circle-icon">{nextCraft.icon || '🎬'}</span>
            </div>
            
            <div className="craft-card-content">
              <span className="craft-name-side">{nextCraft.name}</span>
              <span className="craft-tagline-side">{nextCraft.tagline}</span>
            </div>
          </div>
        </div>

      </div>

      {/* Manual Left & Right Controls Bar (Moves only on click) */}
      <div className="craft-controls-bar">
        
        {/* Left Arrow Button */}
        <button
          type="button"
          onClick={onPrev}
          className="craft-ctrl-arrow-btn arrow-prev"
          aria-label="Previous Craft"
          title="Previous Craft (Click to move left)"
        >
          <ChevronLeft size={22} />
        </button>

        {/* Center Current Craft Name Badge */}
        <div className="craft-center-playback">
          <span className="craft-active-label">
            <span style={{ color: '#F59E0B', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              {currentCraft.name}
            </span>
            <span style={{ color: '#64748B', margin: '0 8px' }}>•</span>
            <span style={{ color: '#E2E8F0', fontWeight: 600 }}>
              {currentIndex + 1} of {N} Crafts
            </span>
          </span>
        </div>

        {/* Right Arrow Button */}
        <button
          type="button"
          onClick={onNext}
          className="craft-ctrl-arrow-btn arrow-next"
          aria-label="Next Craft"
          title="Next Craft (Click to move right)"
        >
          <ChevronRight size={22} />
        </button>

      </div>

      {/* 24 Craft Mini Progress Dots (Clickable) */}
      <div className="craft-dots-indicator" role="tablist" aria-label="Craft selection dots">
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
