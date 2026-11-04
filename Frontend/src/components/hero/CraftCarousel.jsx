import { 
  ChevronLeft, 
  ChevronRight
} from 'lucide-react';

/**
 * ============================================================================
 * CraftCarousel – Featured craft card with user-driven navigation
 * ============================================================================
 * Features:
 * - 100% Circular shaped images for all crafts with golden film-reel rings
 * - One featured front craft card
 * - Moves ONLY when the user clicks the Left and Right navigation buttons
 * - Clickable to join/register as that craft on TrueHunt
 * - Interactive pagination dots allowing direct jump to any of the 24 crafts
 */
export default function CraftCarousel({
  crafts = [],
  currentIndex = 0,
  isCardVisible = false,
  onPrev,
  onNext,
  onSelectCraft,
  onCraftClick
}) {
  const N = crafts.length || 24;

  const currentCraft = crafts[currentIndex] || crafts[0];

  const handleCardClick = (craft) => {
    if (onCraftClick) {
      onCraftClick(craft);
    }
  };

  return (
    <div className="craft-carousel-root">
      
      {/* Featured craft card */}
      <div className="craft-cards-stage">
        {/* Center Featured Craft Card (Slot 0 - Active) */}
        <div
          className={`craft-card-slot slot-center${isCardVisible ? ' is-visible' : ''}`}
          onClick={() => handleCardClick(currentCraft)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault();
              handleCardClick(currentCraft);
            }
          }}
          title={`View ${currentCraft.name} talents`}
          aria-label={`View talents for ${currentCraft.name}`}
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
                loading="eager"
                decoding="async"
                fetchPriority="high"
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
            </div>

          </div>
        </div>

      </div>

      {/* Manual Left & Right Controls Bar (Moves only on click) */}
      <div className="craft-controls-bar">
        
        <button
          type="button"
          onClick={onPrev}
          className="craft-ctrl-arrow-btn arrow-prev"
          aria-label="Previous Craft"
          title="Previous Craft (Click to move left)"
        >
          <ChevronLeft size={30} />
        </button>

        {/* Right Arrow Button */}
        <button
          type="button"
          onClick={onNext}
          className="craft-ctrl-arrow-btn arrow-next"
          aria-label="Next Craft"
          title="Next Craft (Click to move right)"
        >
          <ChevronRight size={30} />
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
