import { ArrowRight, Building2, Clapperboard, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import './AuthPageLayout.css';

function PromotionCard({ icon: Icon, eyebrow, title, description, link, linkLabel, tone }) {
  return (
    <aside className={`auth-promotion-card auth-promotion-card-${tone}`} aria-label={`Advertisement: ${title}`}>
      <span className="auth-promotion-label">Featured</span>
      <div className="auth-promotion-icon" aria-hidden="true">
        <Icon size={22} />
      </div>
      <p className="auth-promotion-eyebrow">{eyebrow}</p>
      <h2>{title}</h2>
      <p className="auth-promotion-description">{description}</p>
      <Link to={link} className="auth-promotion-link">
        {linkLabel}
        <ArrowRight size={15} aria-hidden="true" />
      </Link>
    </aside>
  );
}

export default function AuthPageLayout({ children }) {
  return (
    <div className="auth-page-layout">
      <PromotionCard
        icon={Users}
        eyebrow="For artists & creators"
        title="Make your next connection."
        description="Showcase your talent and discover opportunities across cinema and entertainment."
        link="/talents"
        linkLabel="Explore talent"
        tone="creators"
      />
      {children}
      <PromotionCard
        icon={Clapperboard}
        eyebrow="For studios & production"
        title="Bring every production together."
        description="Connect with industry professionals and keep your production moving."
        link="/protect-producer"
        linkLabel="Explore producer tools"
        tone="studios"
      />
    </div>
  );
}

export function RoleSelectionCards({ selectedRole, onSelect, label = 'I am joining TrueHunt as:' }) {
  const roles = [
    { id: 'TALENT', elementId: 'role-social', title: 'Social', description: 'Artists & Creators', icon: Users },
    { id: 'STUDIO', elementId: 'role-producer', title: 'Producer', description: 'Studios & Production', icon: Building2 },
  ];

  return (
    <section className="auth-role-selection" role="group" aria-label={label}>
      <p className="form-label">{label}</p>
      <div className="auth-role-options">
        {roles.map(({ id, elementId, title, description, icon: Icon }) => (
          <button
            key={id}
            id={elementId}
            type="button"
            className={`auth-role-option${selectedRole === id ? ' is-selected' : ''}`}
            onClick={() => onSelect(id)}
            aria-pressed={selectedRole === id}
          >
            <Icon size={22} aria-hidden="true" />
            <span>{title}</span>
            <span className="auth-role-description">{description}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
