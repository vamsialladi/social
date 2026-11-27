import { useId, useState } from 'react'
import { MoreVertical } from 'lucide-react'

export default function PromotionCard({ ariaLabel, children, className = 'home-promotion-card' }) {
  const [isHidden, setIsHidden] = useState(false)
  const [isHidePromptOpen, setIsHidePromptOpen] = useState(false)
  const promptTitleId = useId()

  if (isHidden) return null

  return (
    <section className={className} aria-label={ariaLabel}>
      {children}
      <button
        className="home-promotion-menu-icon"
        type="button"
        aria-label="Promotion options"
        aria-haspopup="dialog"
        aria-expanded={isHidePromptOpen}
        onClick={() => setIsHidePromptOpen((isOpen) => !isOpen)}
      >
        <MoreVertical size={18} aria-hidden="true" />
      </button>
      {isHidePromptOpen && (
        <div className="home-promotion-hide-prompt" role="dialog" aria-labelledby={promptTitleId}>
          <h2 id={promptTitleId}>Hide this promotion?</h2>
          <p>This promotion will be removed from this page.</p>
          <div className="home-promotion-hide-actions">
            <button type="button" onClick={() => setIsHidePromptOpen(false)}>Cancel</button>
            <button
              type="button"
              onClick={() => {
                setIsHidden(true)
                setIsHidePromptOpen(false)
              }}
            >
              Hide
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
