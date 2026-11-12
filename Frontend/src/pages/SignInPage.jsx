import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';
import TrueHuntLogo from '../components/common/TrueHuntLogo';
import AuthPageLayout, { RoleSelectionCards } from '../components/common/AuthPageLayout';
import PasswordField from '../components/common/PasswordField';
import { getPasswordValidationMessage, isPasswordValid } from '../components/common/passwordValidation';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';

export default function SignInPage() {
  const [selectedRole, setSelectedRole] = useState('TALENT');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const { login } = useAuth();
  const { showNotification } = useNotification();
  const location = useLocation();
  const navigate = useNavigate();

  const isFormValid = email.trim().length > 0 && isPasswordValid(password);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    const passwordError = getPasswordValidationMessage(password);
    if (passwordError) {
      setErrorMsg(passwordError);
      return;
    }

    setLoading(true);

    try {
      const res = await login(email, password);
      if (res.success) {
        showNotification(`Welcome back, ${res.user.name}!`, 'success');
        const connectTalent = location.state?.connectTalent;
        if (connectTalent) {
          navigate('/social/profile', { state: { connectTalent } });
        } else {
          navigate(selectedRole === 'STUDIO' ? '/protect-producer' : '/social/home');
        }
      } else {
        setErrorMsg(res.message || 'Invalid credentials');
      }
    } catch (err) {
      setErrorMsg(err.message || 'Login failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };

  const fillDemoAccount = (role) => {
    if (role === 'talent') {
      setSelectedRole('TALENT');
      setEmail('aarav.varma@truehunt.in');
      setPassword('TrueHunt2026!');
    } else {
      setSelectedRole('STUDIO');
      setEmail('mythri.studios@truehunt.in');
      setPassword('TrueHunt2026!');
    }
  };

  return (
    <AuthPageLayout>
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

        <RoleSelectionCards
          selectedRole={selectedRole}
          onSelect={setSelectedRole}
          label="I am signing in as:"
        />

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="sign-in-email">Professional Email</label>
            <div style={{ position: 'relative' }}>
              <input
                id="sign-in-email"
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setErrorMsg('');
                }}
                placeholder="name@cinema.com"
                autoComplete="username"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <PasswordField
              id="sign-in-password"
              name="password"
              label="Password"
              headerAction={(
                <Link
                  to="/forgot-password"
                  style={{ fontSize: '12px', color: 'var(--color-maroon)', fontWeight: 600 }}
                >
                  Forgot Password?
                </Link>
              )}
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setErrorMsg('');
              }}
              placeholder="Enter your password"
              autoComplete="current-password"
              showValidation
            />
            {errorMsg && (
              <div
                role="alert"
                style={{
                  padding: '10px 12px',
                  marginTop: '8px',
                  backgroundColor: '#FEF2F2',
                  border: '1px solid #FCA5A5',
                  borderRadius: 'var(--radius-sm)',
                  color: '#991B1B',
                  fontSize: '13px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}
              >
                <AlertCircle size={16} />
                <span>{errorMsg}</span>
              </div>
            )}
          </div>

          <button
            type="submit"
            id="sign-in-submit-btn"
            disabled={loading || !isFormValid}
            className="btn btn-maroon btn-lg"
            style={{ width: '100%', marginTop: '10px', fontSize: '16px', fontWeight: 700 }}
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
    </AuthPageLayout>
  );
}
