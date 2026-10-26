import React, { useState, useEffect } from 'react';
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
  Phone
} from 'lucide-react';
import TrueHuntLogo from '../common/TrueHuntLogo';
import { useAuth } from '../../context/AuthContext';

export default function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
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
          style={{ display: 'none', alignItems: 'center', gap: '10px', flexShrink: 0 }}
          className="desktop-auth-actions"
        >
          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Link 
                to="/profile"
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '8px',
                  backgroundColor: location.pathname.startsWith('/profile') ? 'var(--color-maroon-light)' : 'var(--color-bg-subtle)',
                  border: `1px solid ${location.pathname.startsWith('/profile') ? 'var(--color-maroon-border)' : 'transparent'}`,
                  padding: '6px 14px',
                  borderRadius: 'var(--radius-full)',
                  textDecoration: 'none',
                  transition: 'all var(--transition-fast)'
                }}
                title="View and edit your professional profile"
              >
                <div
                  style={{
                    width: '28px',
                    height: '28px',
                    borderRadius: '50%',
                    backgroundColor: 'var(--color-navy)',
                    color: 'white',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '12px',
                    fontWeight: 700
                  }}
                >
                  {user?.name ? user.name[0].toUpperCase() : 'U'}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--color-navy)', lineHeight: 1.2 }}>
                    {user?.name || 'Professional'}
                  </span>
                  <span style={{ fontSize: '10px', color: 'var(--color-maroon)', fontWeight: 700, textTransform: 'uppercase' }}>
                    My Profile
                  </span>
                </div>
              </Link>
              <button 
                onClick={logout} 
                className="btn btn-sm btn-ghost" 
                title="Sign Out"
                style={{ color: 'var(--color-maroon)', gap: '4px' }}
              >
                <LogOut size={16} />
                <span>Exit</span>
              </button>
            </div>
          ) : (
            <>
              <Link to="/sign-in" className="btn btn-sm btn-ghost" style={{ fontWeight: 600, color: 'var(--color-navy)' }}>
                Sign In
              </Link>
              <Link
                to="/join-now"
                className="btn btn-sm btn-maroon"
                style={{
                  borderRadius: 'var(--radius-full)',
                  padding: '8px 18px',
                  fontWeight: 600,
                  boxShadow: '0 2px 8px rgba(128, 24, 35, 0.2)'
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
            padding: '16px 20px 24px 20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
            boxShadow: 'var(--shadow-dropdown)'
          }}
          className="mobile-nav-drawer"
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
                    alignItems: 'center',
                    gap: '12px',
                    padding: '12px 14px',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '15px',
                    fontWeight: isCurrentActive ? 600 : 500,
                    color: isCurrentActive ? 'var(--color-maroon)' : 'var(--color-navy)',
                    backgroundColor: isCurrentActive ? 'var(--color-maroon-light)' : 'transparent',
                  };
                }}
              >
                <Icon size={18} style={{ flexShrink: 0 }} />
                <span>{item.name}</span>
              </NavLink>
            );
          })}

          <NavLink
            to="/contact-us"
            onClick={() => setMobileMenuOpen(false)}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              padding: '12px 14px',
              borderRadius: 'var(--radius-sm)',
              fontSize: '15px',
              fontWeight: isActive ? 600 : 500,
              color: isActive ? 'var(--color-maroon)' : 'var(--color-navy)',
              backgroundColor: isActive ? 'var(--color-maroon-light)' : 'transparent',
              textDecoration: 'none'
            })}
          >
            <Phone size={18} style={{ flexShrink: 0 }} />
            <span>Contact Us</span>
          </NavLink>

          <div style={{ height: '1px', backgroundColor: 'var(--color-border)', margin: '12px 0' }} />

          {isAuthenticated ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <NavLink 
                to="/profile"
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '12px 14px',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '15px',
                  fontWeight: 600,
                  color: isActive ? 'var(--color-maroon)' : 'var(--color-navy)',
                  backgroundColor: isActive ? 'var(--color-maroon-light)' : 'var(--color-bg-subtle)',
                })}
              >
                <User size={18} />
                <span>My Profile ({user?.name || 'Professional'})</span>
              </NavLink>
              <button 
                onClick={logout} 
                className="btn btn-outline-maroon" 
                style={{ width: '100%' }}
              >
                <LogOut size={16} /> Sign Out
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <Link to="/sign-in" className="btn btn-outline-navy" style={{ width: '100%' }}>
                Sign In
              </Link>
              <Link to="/join-now" className="btn btn-maroon" style={{ width: '100%' }}>
                Join Now
              </Link>
            </div>
          )}
        </div>
      )}

      {/* Embedded CSS for responsive navbar queries */}
      <style>{`
        @media (min-width: 1140px) {
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
        @media (min-width: 1140px) and (max-width: 1260px) {
          .nav-link-desktop {
            padding: 7px 8px !important;
            font-size: 13.5px !important;
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
