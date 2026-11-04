import React, { useState, useEffect, useRef } from 'react';
import { NavLink, Link, useNavigate, useLocation } from 'react-router-dom';
import {
  Film,
  Users,
  Briefcase,
  TrendingUp,
  Shield,
  Smartphone,
  HelpCircle,
  Menu,
  X,
  User,
  LogOut,
  Phone,
  Languages
} from 'lucide-react';
import TrueHuntLogo from '../common/TrueHuntLogo';
import { useAuth } from '../../context/AuthContext';
import './Navbar.css';

function LanguageSelector({ selectedLanguage, onChange }) {
  const [isOpen, setIsOpen] = useState(false);
  const selectorRef = useRef(null);
  const triggerRef = useRef(null);
  const optionRefs = useRef([]);
  const languages = ['Telugu', 'Hindi', 'English', 'Kannada', 'Tamil'];

  useEffect(() => {
    if (!isOpen) return undefined;

    function dismissOnOutsideClick(event) {
      if (!selectorRef.current?.contains(event.target)) setIsOpen(false);
    }

    document.addEventListener('pointerdown', dismissOnOutsideClick);
    return () => document.removeEventListener('pointerdown', dismissOnOutsideClick);
  }, [isOpen]);

  function openMenu() {
    setIsOpen(true);
    requestAnimationFrame(() => {
      const selectedIndex = languages.indexOf(selectedLanguage);
      optionRefs.current[selectedIndex]?.focus();
    });
  }

  function chooseLanguage(language) {
    onChange(language);
    setIsOpen(false);
    triggerRef.current?.focus();
  }

  function handleOptionKeyDown(event, index) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const direction = event.key === 'ArrowDown' ? 1 : -1;
      const nextIndex = (index + direction + languages.length) % languages.length;
      optionRefs.current[nextIndex]?.focus();
    } else if (event.key === 'Escape') {
      event.preventDefault();
      setIsOpen(false);
      triggerRef.current?.focus();
    }
  }

  return (
    <div className={`navbar-language-selector${isOpen ? ' is-open' : ''}`} ref={selectorRef}>
      <button
        className="navbar-language-trigger"
        type="button"
        ref={triggerRef}
        aria-label={`Language: ${selectedLanguage}`}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        onClick={() => (isOpen ? setIsOpen(false) : openMenu())}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            openMenu();
          }
        }}
      >
        <Languages size={17} aria-hidden="true" />
        <span className="navbar-language-label">Language</span>
        <span className="navbar-language-current">{selectedLanguage}</span>
        <span className="navbar-language-chevron" aria-hidden="true" />
      </button>
      {isOpen && (
        <div className="navbar-language-menu" role="listbox" aria-label="Choose a language">
          {languages.map((language, index) => (
            <button
              className="navbar-language-option"
              type="button"
              role="option"
              aria-selected={selectedLanguage === language}
              key={language}
              ref={(element) => {
                optionRefs.current[index] = element;
              }}
              onClick={() => chooseLanguage(language)}
              onKeyDown={(event) => handleOptionKeyDown(event, index)}
            >
              <span>{language}</span>
              {selectedLanguage === language && <span className="navbar-language-check" aria-hidden="true">✓</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState(() => {
    const savedLanguage = localStorage.getItem('truehunt_language');
    return ['Telugu', 'Hindi', 'English', 'Kannada', 'Tamil'].includes(savedLanguage)
      ? savedLanguage
      : 'English';
  });
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Subtle border shadow when scrolled
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 15);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { name: 'Entertainments', path: '/entertainments', icon: Film },
    { name: 'Talents', path: '/talents', icon: Users },
    { name: 'Jobs', path: '/jobs', icon: Briefcase },
    { name: 'Investors Connect', path: '/investors-connect', icon: TrendingUp },
    { name: 'Protect Producer', path: '/protect-producer', icon: Shield },
    { name: 'LCM App', path: '/lcm-app', icon: Smartphone },
    { name: 'Get Support', path: '/get-support', icon: HelpCircle },
  ];

  function updateLanguage(language) {
    setSelectedLanguage(language);
    localStorage.setItem('truehunt_language', language);
  }

  return (
    <header
      style={{
        position: 'sticky',
        top: 0,
        zIndex: 100,
        backgroundColor: 'var(--color-white)',
        borderBottom: `1px solid ${scrolled ? 'var(--color-border)' : 'var(--color-border-subtle)'}`,
        boxShadow: scrolled ? 'var(--shadow-subtle)' : 'none',
        transition: 'all var(--transition-fast)'
      }}
    >
      <div
        className="navbar-inner-container"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          height: '70px',
          width: '100%',
          maxWidth: '1440px',
          margin: '0 auto',
          padding: '0 24px',
          boxSizing: 'border-box'
        }}
      >

        {/* Left: Brand Logo */}
        <div style={{ display: 'flex', alignItems: 'center', flexShrink: 0 }}>
          <TrueHuntLogo size="md" />
        </div>

        {/* Center: Desktop Navigation Links */}
        <nav
          style={{
            display: 'none',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '4px',
            flexShrink: 0
          }}
          className="desktop-nav"
        >
          {navLinks.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className="nav-link-desktop"
                onClick={(e) => {
                  if (item.path === '#') {
                    e.preventDefault();
                  }
                }}
                style={({ isActive }) => {
                  const isCurrentActive = item.path !== '#' && (isActive || (item.path === '/protect-producer' && location.pathname.startsWith('/protect-producer')));
                  return {
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '8px 11px',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '14px',
                    fontWeight: isCurrentActive ? 600 : 500,
                    color: isCurrentActive ? 'var(--color-maroon)' : 'var(--color-navy)',
                    backgroundColor: isCurrentActive ? 'var(--color-maroon-light)' : 'transparent',
                    borderBottom: isCurrentActive ? '2px solid var(--color-maroon)' : '2px solid transparent',
                    whiteSpace: 'nowrap',
                    lineHeight: 1,
                    boxSizing: 'border-box',
                    transition: 'all var(--transition-fast)'
                  };
                }}
              >
                <Icon size={16} strokeWidth={2} style={{ flexShrink: 0 }} />
                <span style={{ whiteSpace: 'nowrap' }}>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* Right: Auth CTA Buttons */}
        <div
          style={{ display: 'flex', alignItems: 'center', gap: '12px', flexShrink: 0 }}
          className="desktop-auth-actions"
        >
          <LanguageSelector selectedLanguage={selectedLanguage} onChange={updateLanguage} />
          {isAuthenticated && user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Link
                to={user.role === 'STUDIO' ? '/protect-producer' : '/social'}
                className="btn btn-sm btn-outline-navy"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontWeight: 600,
                  fontSize: '14px',
                  padding: '7px 14px',
                  textDecoration: 'none'
                }}
              >
                <User size={15} />
                <span>{user.name || 'Dashboard'}</span>
              </Link>
              <button
                type="button"
                onClick={logout}
                className="btn btn-sm btn-ghost"
                title="Sign Out"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  padding: '7px 10px',
                  color: 'var(--color-text-muted)',
                  fontSize: '13px'
                }}
              >
                <LogOut size={15} />
                <span>Sign Out</span>
              </button>
            </div>
          ) : (
            <>
              <Link
                to="/sign-in"
                className="btn btn-sm btn-ghost"
                onClick={() => {
                  if (location.pathname === '/sign-in') {
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }
                }}
                style={{
                  fontWeight: 600,
                  color: 'var(--color-navy)',
                  fontSize: '15px',
                  padding: '8px 12px',
                  textDecoration: 'none'
                }}
              >
                Sign In
              </Link>
              <Link
                to="/join-now"
                className="btn btn-sm btn-maroon"
                onClick={() => {
                  if (location.pathname === '/join-now') {
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }
                }}
                style={{
                  borderRadius: 'var(--radius-full)',
                  padding: '9px 24px',
                  fontWeight: 600,
                  fontSize: '14.5px',
                  backgroundColor: '#801823',
                  color: '#ffffff',
                  boxShadow: '0 2px 8px rgba(128, 24, 35, 0.25)',
                  textDecoration: 'none',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  letterSpacing: '0.2px'
                }}
              >
                Join Now
              </Link>
            </>
          )}
        </div>

        {/* Mobile Hamburger Toggle Button */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="mobile-toggle-btn"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '8px',
            borderRadius: 'var(--radius-sm)',
            color: 'var(--color-navy)',
            backgroundColor: 'transparent',
            flexShrink: 0
          }}
          aria-label={mobileMenuOpen ? 'Close Navigation Menu' : 'Open Navigation Menu'}
        >
          {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>

      </div>

      {/* Mobile Menu Drawer */}
      {mobileMenuOpen && (
        <div
          style={{
            backgroundColor: 'var(--color-white)',
            borderTop: '1px solid var(--color-border)',
            padding: '10px 16px 14px',
            display: 'flex',
            flexDirection: 'column',
            gap: '4px',
            boxShadow: 'var(--shadow-dropdown)'
          }}
          className="mobile-nav-drawer"
        >
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: '4px',
            }}
          >
            {navLinks.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  onClick={(e) => {
                    if (item.path === '#') {
                      e.preventDefault();
                    } else {
                      setMobileMenuOpen(false);
                    }
                  }}
                  style={({ isActive }) => {
                    const isCurrentActive = item.path !== '#' && (isActive || (item.path === '/protect-producer' && location.pathname.startsWith('/protect-producer')));
                    return {
                      display: 'flex',
                      minWidth: 0,
                      minHeight: '36px',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '6px 8px',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '12px',
                      fontWeight: isCurrentActive ? 600 : 500,
                      color: isCurrentActive ? 'var(--color-maroon)' : 'var(--color-navy)',
                      backgroundColor: isCurrentActive ? 'var(--color-maroon-light)' : 'transparent',
                    };
                  }}
                >
                  <Icon size={15} style={{ flexShrink: 0 }} />
                  <span>{item.name}</span>
                </NavLink>
              );
            })}

            <NavLink
              to="/contact-us"
              onClick={() => setMobileMenuOpen(false)}
              style={({ isActive }) => ({
                display: 'flex',
                minWidth: 0,
                minHeight: '36px',
                alignItems: 'center',
                gap: '8px',
                padding: '6px 8px',
                borderRadius: 'var(--radius-sm)',
                fontSize: '12px',
                fontWeight: isActive ? 600 : 500,
                color: isActive ? 'var(--color-maroon)' : 'var(--color-navy)',
                backgroundColor: isActive ? 'var(--color-maroon-light)' : 'transparent',
                textDecoration: 'none'
              })}
            >
              <Phone size={15} style={{ flexShrink: 0 }} />
              <span>Contact Us</span>
            </NavLink>
          </div>

          <div style={{ height: '1px', backgroundColor: 'var(--color-border)', margin: '6px 0' }} />

          <LanguageSelector selectedLanguage={selectedLanguage} onChange={updateLanguage} />

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {isAuthenticated && user ? (
              <>
                <Link
                  to={user.role === 'STUDIO' ? '/protect-producer' : '/social'}
                  onClick={() => setMobileMenuOpen(false)}
                  className="btn btn-outline-navy"
                  style={{ width: '100%', minHeight: '38px', padding: '8px 12px', textAlign: 'center', textDecoration: 'none' }}
                >
                  My Dashboard ({user.name})
                </Link>
                <button
                  type="button"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    logout();
                  }}
                  className="btn btn-ghost"
                  style={{ width: '100%', minHeight: '38px', padding: '8px 12px', textAlign: 'center' }}
                >
                  Sign Out
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/sign-in"
                  onClick={() => setMobileMenuOpen(false)}
                  className="btn btn-outline-navy"
                  style={{ width: '100%', minHeight: '38px', padding: '8px 12px', textAlign: 'center', textDecoration: 'none' }}
                >
                  Sign In
                </Link>
                <Link 
                  to="/join-now" 
                  className="btn btn-maroon" 
                  onClick={() => {
                    setMobileMenuOpen(false);
                    if (location.pathname === '/join-now') {
                      window.scrollTo({ top: 0, behavior: 'smooth' });
                    }
                  }}
                  style={{ width: '100%', minHeight: '38px', padding: '8px 12px', textAlign: 'center', textDecoration: 'none', backgroundColor: '#801823', color: '#ffffff' }}
                >
                  Join Now
                </Link>
              </>
            )}
          </div>
        </div>
      )}

      {/* Embedded CSS for responsive navbar queries */}
      <style>{`
        @media (max-width: 991px) {
          .desktop-auth-actions {
            display: none !important;
          }
        }
        @media (min-width: 992px) {
          .desktop-nav {
            display: flex !important;
          }
          .desktop-auth-actions {
            display: flex !important;
          }
          .mobile-toggle-btn {
            display: none !important;
          }
        }
        @media (min-width: 992px) and (max-width: 1260px) {
          .nav-link-desktop {
            padding: 7px 6px !important;
            font-size: 13px !important;
            gap: 4px !important;
          }
          .desktop-nav {
            gap: 2px !important;
          }
        }
        .nav-link-desktop:hover {
          color: var(--color-maroon) !important;
          background-color: var(--color-maroon-light) !important;
        }
      `}</style>
    </header>
  );
}
