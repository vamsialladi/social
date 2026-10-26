import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Users, Building2, CheckCircle2, XCircle, AlertCircle, RefreshCw, Eye, EyeOff, Copy } from 'lucide-react';
import TrueHuntLogo from '../components/common/TrueHuntLogo';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { FILM_CRAFTS } from '../services/mockData';

const generateCaptchaCode = (previousCode = '') => {
  const characters = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code;
  do {
    code = Array.from({ length: 5 }, () => characters[Math.floor(Math.random() * characters.length)]).join('');
  } while (code === previousCode);
  return code;
};

const generateUserId = (name) => {
  const namePrefix = name.replace(/[^a-z\d]/gi, '').slice(0, 2).toUpperCase().padEnd(2, 'X');
  const number = Math.floor(1000000000 + Math.random() * 9000000000);
  return `TH${namePrefix}${number}`;
};

export default function JoinNowPage() {
  const [selectedRole, setSelectedRole] = useState('TALENT');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [emailOtp, setEmailOtp] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [captchaCode, setCaptchaCode] = useState(generateCaptchaCode);
  const [captchaInput, setCaptchaInput] = useState('');
  const [craft, setCraft] = useState('Acting');
  const [experienceYears, setExperienceYears] = useState('3-5 Years');
  const [companyName, setCompanyName] = useState('');
  const [headquarters, setHeadquarters] = useState('');
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [generatedId, setGeneratedId] = useState('');

  const { register } = useAuth();
  const { showNotification } = useNotification();
  const fullName = [firstName.trim(), lastName.trim()].filter(Boolean).join(' ');

  const handleOtpRequest = (destination) => {
    showNotification(`OTP delivery for ${destination} is not configured. Please contact support.`, 'error');
  };

  const handleCopyId = async () => {
    try {
      await navigator.clipboard.writeText(generatedId);
      showNotification('TrueHunt ID copied to clipboard.', 'success');
    } catch {
      showNotification('Unable to copy the ID. Please copy it manually.', 'error');
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!agreeTerms) {
      setErrorMsg('Please agree to the Terms of Service & Privacy Policy.');
      return;
    }

    if (password !== confirmPassword) {
      setErrorMsg('Passwords do not match.');
      return;
    }

    if (!/^\d{10}$/.test(phoneNumber)) {
      setErrorMsg('Phone number must contain exactly 10 digits.');
      return;
    }

    if (captchaInput.trim().toUpperCase() !== captchaCode) {
      setErrorMsg('Please enter the CAPTCHA code shown.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        fullName,
        email,
        phoneNumber,
        otp: emailOtp,
        emailOtp,
        password,
        role: selectedRole,
        craft: selectedRole === 'TALENT' ? craft : null,
        experienceYears: selectedRole === 'TALENT' ? experienceYears : null,
        companyName: selectedRole === 'STUDIO' ? companyName : null,
        headquarters: selectedRole === 'STUDIO' ? headquarters : null,
      };

      const res = await register(payload);
      if (res.success) {
        setGeneratedId(generateUserId(fullName));
        showNotification(`Welcome to TrueHunt, ${fullName}! Your portfolio has been established.`, 'success');
      } else {
        setErrorMsg(res.message || 'Registration failed');
      }
    } catch (err) {
      setErrorMsg('An error occurred during registration.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div 
      style={{
        minHeight: '90vh',
        backgroundColor: 'var(--color-bg-page)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 20px'
      }}
    >
      <div 
        className="card"
        style={{
          width: '100%',
          maxWidth: '560px',
          padding: '40px',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-card)',
          backgroundColor: 'var(--color-white)',
          border: '1px solid var(--color-border)'
        }}
      >
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{ display: 'inline-block', marginBottom: '14px' }}>
            <TrueHuntLogo size="lg" />
          </div>
          <h1 style={{ fontSize: '24px', color: 'var(--color-navy)', marginBottom: '4px' }}>
            Join TrueHunt
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>
            The professional network built exclusively for cinema & entertainment.
          </p>
        </div>

        {generatedId && (
          <div
            role="status"
            aria-live="polite"
            style={{
              padding: '24px',
              marginBottom: '24px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: 'var(--color-bg-page)',
              textAlign: 'center'
            }}
          >
            <h2 style={{ fontSize: '20px', color: 'var(--color-navy)', marginBottom: '8px' }}>
              Your TrueHunt ID
            </h2>
            <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '16px' }}>
              Your account has been created. Save this ID for your records.
            </p>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <strong style={{ fontSize: '20px', color: 'var(--color-maroon)', letterSpacing: '1px' }}>
                {generatedId}
              </strong>
              <button
                type="button"
                onClick={handleCopyId}
                aria-label="Copy TrueHunt ID"
                className="btn"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
              >
                <Copy size={16} />
                Copy
              </button>
            </div>
            <Link
              to="/talents"
              className="btn btn-maroon"
              style={{ display: 'inline-block', marginTop: '20px' }}
            >
              Continue to TrueHunt
            </Link>
          </div>
        )}

        {/* Role Selection Tabs */}
        <div hidden={Boolean(generatedId)} style={{ marginBottom: '24px' }}>
          <label className="form-label" style={{ marginBottom: '8px', display: 'block' }}>
            I am joining TrueHunt as:
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            
            <button
              type="button"
              onClick={() => setSelectedRole('TALENT')}
              style={{
                padding: '12px 8px',
                borderRadius: 'var(--radius-sm)',
                border: selectedRole === 'TALENT' ? '2px solid var(--color-maroon)' : '1px solid var(--color-border)',
                backgroundColor: selectedRole === 'TALENT' ? 'var(--color-maroon-light)' : 'var(--color-white)',
                color: selectedRole === 'TALENT' ? 'var(--color-maroon)' : 'var(--color-navy)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '6px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              <Users size={20} />
              <span>Social</span>
            </button>

            <button
              type="button"
              onClick={() => setSelectedRole('STUDIO')}
              style={{
                padding: '12px 8px',
                borderRadius: 'var(--radius-sm)',
                border: selectedRole === 'STUDIO' ? '2px solid var(--color-maroon)' : '1px solid var(--color-border)',
                backgroundColor: selectedRole === 'STUDIO' ? 'var(--color-maroon-light)' : 'var(--color-white)',
                color: selectedRole === 'STUDIO' ? 'var(--color-maroon)' : 'var(--color-navy)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '6px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              <Building2 size={20} />
              <span>Production Team</span>
            </button>

          </div>
        </div>

        {errorMsg && !generatedId && (
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

        <form onSubmit={handleRegister} hidden={Boolean(generatedId)}>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
            <div className="form-group">
              <label className="form-label" htmlFor="first-name">First Name *</label>
              <input
                id="first-name"
                type="text"
                className="form-control"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="First name"
                autoComplete="given-name"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="last-name">Last Name *</label>
              <input
                id="last-name"
                type="text"
                className="form-control"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="Last name"
                autoComplete="family-name"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Professional Email *</label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setEmailOtp('');
                }}
                placeholder="name@cinema.com"
                autoComplete="email"
                required
              />
              <button
                type="button"
                className="btn btn-maroon"
                onClick={() => handleOtpRequest('email')}
                style={{ flexShrink: 0 }}
              >
                Send OTP
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="email-otp">Email OTP *</label>
            <input
              id="email-otp"
              type="text"
              className="form-control"
              value={emailOtp}
              onChange={(e) => setEmailOtp(e.target.value)}
              placeholder="Enter email OTP"
              inputMode="numeric"
              autoComplete="off"
              maxLength={6}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number *</label>
            <input
              type="tel"
              className="form-control"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value.replace(/\D/g, '').slice(0, 10))}
              placeholder="Enter your phone number"
              autoComplete="tel"
              inputMode="numeric"
              pattern="\d{10}"
              maxLength={10}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="create-password">Create Password *</label>
            <div style={{ position: 'relative' }}>
              <input
                id="create-password"
                type={showPassword ? 'text' : 'password'}
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Minimum 8 characters"
                autoComplete="new-password"
                required
                minLength={6}
                style={{ paddingRight: '42px' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                title={showPassword ? 'Hide password' : 'Show password'}
                style={{
                  position: 'absolute',
                  right: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: 0,
                  border: 'none',
                  background: 'none',
                  color: 'var(--color-text-muted)',
                  cursor: 'pointer'
                }}
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="confirm-password">Confirm Password *</label>
            <div style={{ position: 'relative' }}>
              <input
                id="confirm-password"
                type={showConfirmPassword ? 'text' : 'password'}
                className="form-control"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter your password"
                autoComplete="new-password"
                required
                aria-describedby="password-match-status"
                style={{ paddingRight: '76px' }}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword((visible) => !visible)}
                aria-label={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
                title={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
                style={{
                  position: 'absolute',
                  right: '40px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: 0,
                  border: 'none',
                  background: 'none',
                  color: 'var(--color-text-muted)',
                  cursor: 'pointer'
                }}
              >
                {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
              {confirmPassword && (
                <span
                  id="password-match-status"
                  role="status"
                  aria-label={password === confirmPassword ? 'Passwords match' : 'Passwords do not match'}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    display: 'flex',
                    pointerEvents: 'none'
                  }}
                >
                  {password === confirmPassword
                    ? <CheckCircle2 size={20} color="#15803D" />
                    : <XCircle size={20} color="#B91C1C" />}
                </span>
              )}
            </div>
          </div>

          {/* Role-Specific Fields */}
          {selectedRole === 'TALENT' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label">Primary Craft *</label>
                <select 
                  className="form-control"
                  value={craft}
                  onChange={(e) => setCraft(e.target.value)}
                >
                  {FILM_CRAFTS.map(c => (
                    <option key={c.id} value={c.title}>{c.title}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Experience *</label>
                <select 
                  className="form-control"
                  value={experienceYears}
                  onChange={(e) => setExperienceYears(e.target.value)}
                >
                  <option value="1-2 Years">1 - 2 Years</option>
                  <option value="3-5 Years">3 - 5 Years</option>
                  <option value="6-9 Years">6 - 9 Years</option>
                  <option value="10+ Years">10+ Years</option>
                </select>
              </div>
            </div>
          )}

          {selectedRole === 'STUDIO' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label">Production Banner Name *</label>
                <input 
                  type="text"
                  className="form-control"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  placeholder="e.g. Navarasa Pictures"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Headquarters / Hub *</label>
                <input 
                  type="text"
                  className="form-control"
                  value={headquarters}
                  onChange={(e) => setHeadquarters(e.target.value)}
                  placeholder="e.g. Hyderabad"
                  required
                />
              </div>
            </div>
          )}

          {/* Demo CAPTCHA */}
          <div className="form-group" style={{ marginTop: '10px' }}>
            <label className="form-label">CAPTCHA *</label>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
              <svg
                role="img"
                aria-label="CAPTCHA image"
                viewBox="0 0 180 56"
                style={{
                  display: 'block',
                  width: '180px',
                  height: '56px',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: '#F8FAFC'
                }}
              >
                <path d="M8 43 L170 12 M10 15 L168 42 M35 4 L110 52 M145 3 L70 53" stroke="#CBD5E1" strokeWidth="1.5" />
                <text
                  x="90"
                  y="38"
                  textAnchor="middle"
                  fill="var(--color-navy)"
                  fontSize="28"
                  fontWeight="700"
                  letterSpacing="7"
                  fontFamily="monospace"
                >
                  {captchaCode}
                </text>
              </svg>
              <button
                type="button"
                onClick={() => {
                  setCaptchaCode((currentCode) => generateCaptchaCode(currentCode));
                  setCaptchaInput('');
                }}
                aria-label="Refresh CAPTCHA"
                title="Refresh CAPTCHA"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: '40px',
                  height: '40px',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: 'var(--color-white)',
                  color: 'var(--color-navy)',
                  cursor: 'pointer'
                }}
              >
                <RefreshCw size={18} />
              </button>
            </div>
            <label className="form-label" htmlFor="captcha-input">Enter the code shown above</label>
            <input
              id="captcha-input"
              type="text"
              className="form-control"
              value={captchaInput}
              onChange={(e) => setCaptchaInput(e.target.value)}
              placeholder="Enter CAPTCHA"
              autoComplete="off"
              required
            />
          </div>

          {/* Terms checkbox */}
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', marginTop: '10px', marginBottom: '20px' }}>
            <input 
              type="checkbox"
              id="terms"
              checked={agreeTerms}
              onChange={(e) => setAgreeTerms(e.target.checked)}
              style={{ marginTop: '3px', width: '16px', height: '16px', accentColor: 'var(--color-maroon)' }}
            />
            <label htmlFor="terms" style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: '1.4' }}>
              I agree to the <Link to="/terms-and-conditions" style={{ color: 'var(--color-maroon)', fontWeight: 600 }}>Terms of Service</Link> and acknowledge the <Link to="/privacy-policy" style={{ color: 'var(--color-maroon)', fontWeight: 600 }}>Privacy Policy</Link>.
            </label>
          </div>

          <button 
            type="submit"
            disabled={loading}
            className="btn btn-maroon btn-lg"
            style={{ width: '100%' }}
          >
            {loading ? 'Creating Account...' : 'Agree & Join TrueHunt'}
          </button>

        </form>

        <div hidden={Boolean(generatedId)} style={{ marginTop: '24px', textAlign: 'center', fontSize: '14px', color: 'var(--color-text-secondary)' }}>
          Already have an account?{' '}
          <Link to="/sign-in" style={{ color: 'var(--color-maroon)', fontWeight: 700 }}>
            Sign In
          </Link>
        </div>

      </div>
    </div>
  );
}
