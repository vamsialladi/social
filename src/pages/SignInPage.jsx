import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react';
import TrueHuntLogo from '../components/common/TrueHuntLogo';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';

export default function SignInPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const { login } = useAuth();
  const { showNotification } = useNotification();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setLoading(true);

    try {
      const res = await login(email, password);
      if (res.success) {
        showNotification(`Welcome back, ${res.user.name}!`, 'success');
        navigate('/talents');
      } else {
        setErrorMsg(res.message || 'Invalid credentials');
      }
    } catch (err) {
      setErrorMsg('Login failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };

  const fillDemoAccount = (role) => {
    if (role === 'talent') {
      setEmail('aarav.varma@truehunt.in');
      setPassword('TrueHunt2026!');
    } else {
      setEmail('mythri.studios@truehunt.in');
      setPassword('TrueHunt2026!');
    }
  };

  return (
    <div
      style={{
        minHeight: '88vh',
        backgroundColor: 'var(--color-bg-page)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '40px 20px'
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '460px',
          padding: '40px 36px',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-card)',
          backgroundColor: 'var(--color-white)',
          border: '1px solid var(--color-border)'
        }}
      >
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={{ display: 'inline-block', marginBottom: '16px' }}>
            <TrueHuntLogo size="lg" />
          </div>
          <h1 style={{ fontSize: '24px', color: 'var(--color-navy)', marginBottom: '6px' }}>
            Sign In to TrueHunt
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>
            Access film industry opportunities, portfolios & call sheets.
          </p>
        </div>

        {errorMsg && (
          <div
            style={{
              padding: '12px 14px',
              backgroundColor: '#FEF2F2',
              border: '1px solid #FCA5A5',
              borderRadius: 'var(--radius-sm)',
              color: '#991B1B',
              fontSize: '13px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              marginBottom: '20px'
            }}
          >
            <AlertCircle size={16} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Professional Email</label>
            <div style={{ position: 'relative' }}>
              <input
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@cinema.com"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label className="form-label">Password</label>
              <span style={{ fontSize: '12px', color: 'var(--color-maroon)', cursor: 'pointer' }}>
                Forgot Password?
              </span>
            </div>
            <input
              type="password"
              className="form-control"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-maroon btn-lg"
            style={{ width: '100%', marginTop: '10px' }}
          >
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        {/* Demo Fast Access Buttons */}
        <div style={{ marginTop: '24px', paddingTop: '20px', borderTop: '1px solid var(--color-border)' }}>
          <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--color-text-muted)', fontWeight: 700, display: 'block', textAlign: 'center', marginBottom: '10px' }}>
            Quick Demo Auto-Fill:
          </span>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            <button
              type="button"
              onClick={() => fillDemoAccount('talent')}
              className="btn btn-sm btn-outline-subtle"
              style={{ fontSize: '12px' }}
            >
              Demo Talent
            </button>
            <button
              type="button"
              onClick={() => fillDemoAccount('producer')}
              className="btn btn-sm btn-outline-subtle"
              style={{ fontSize: '12px' }}
            >
              Demo Producer
            </button>
          </div>
        </div>

        {/* Footer Link */}
        <div style={{ marginTop: '24px', textAlign: 'center', fontSize: '14px', color: 'var(--color-text-secondary)' }}>
          New to TrueHunt?{' '}
          <Link to="/join-now" style={{ color: 'var(--color-maroon)', fontWeight: 700 }}>
            Join Now
          </Link>
        </div>

      </div>
    </div>
  );
}
