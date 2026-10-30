import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, AlertCircle, RefreshCw, Eye, EyeOff, Copy } from 'lucide-react';
import TrueHuntLogo from '../components/common/TrueHuntLogo';
import AuthPageLayout, { RoleSelectionCards } from '../components/common/AuthPageLayout';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { EXPERIENCE_OPTIONS, REGISTRATION_CRAFTS, SUB_CRAFTS_BY_PRIMARY_CRAFT } from '../services/mockData';
import { api } from '../services/api';

const SORTED_REGISTRATION_CRAFTS = [...REGISTRATION_CRAFTS].sort((first, second) => first.localeCompare(second));

const generateCaptchaCode = (previousCode = '') => {
  const characters = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code;
  do {
    code = Array.from({ length: 5 }, () => characters[Math.floor(Math.random() * characters.length)]).join('');
  } while (code === previousCode);
  return code;
};

export default function JoinNowPage() {
  const navigate = useNavigate();
  const location = useLocation();
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
  const [craft, setCraft] = useState(() => {
    const requestedCraft = new URLSearchParams(location.search).get('craft');
    return REGISTRATION_CRAFTS.includes(requestedCraft) ? requestedCraft : 'Cast (Actors)';
  });
  const [subCraft, setSubCraft] = useState('');
  const [secondaryCrafts, setSecondaryCrafts] = useState([]);
  const [isSecondaryCraftDropdownOpen, setIsSecondaryCraftDropdownOpen] = useState(false);
  const [secondarySubCrafts, setSecondarySubCrafts] = useState([]);
  const [secondarySubCraftSearch, setSecondarySubCraftSearch] = useState('');
  const [isSecondarySubCraftDropdownOpen, setIsSecondarySubCraftDropdownOpen] = useState(false);
  const [experienceYears, setExperienceYears] = useState('3 - 5 Years');
  const [companyName, setCompanyName] = useState('');
  const [headquarters, setHeadquarters] = useState('');
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [generatedId, setGeneratedId] = useState('');

  const { register } = useAuth();
  const { showNotification } = useNotification();
  const fullName = [firstName.trim(), lastName.trim()].filter(Boolean).join(' ');
  const secondarySubCraftOptions = [...new Set(
    secondaryCrafts.flatMap((secondaryCraft) => SUB_CRAFTS_BY_PRIMARY_CRAFT[secondaryCraft] || [])
  )].sort((first, second) => first.localeCompare(second));

  const handleOtpRequest = async () => {
    try {
      const res = await api.auth.sendOtp(email || 'user@cinema.com');
      const otpCode = res?.otp || '123456';
      setEmailOtp(otpCode);
      showNotification(`Dummy OTP: ${otpCode} (auto-filled)`, 'success');
    } catch {
      setEmailOtp('123456');
      showNotification('Dummy OTP: 123456 (auto-filled)', 'success');
    }
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
      const finalOtp = emailOtp.trim() || '123456';
      const payload = {
        fullName,
        email,
        phoneNumber,
        otp: finalOtp,
        emailOtp: finalOtp,
        password,
        role: selectedRole,
        craft: selectedRole === 'TALENT' ? craft : null,
        subCraft: selectedRole === 'TALENT' ? subCraft : null,
        secondaryCrafts: selectedRole === 'TALENT' ? secondaryCrafts : null,
        secondarySubCrafts: selectedRole === 'TALENT' ? secondarySubCrafts : null,
        experienceYears: selectedRole === 'TALENT' ? experienceYears : null,
        companyName: selectedRole === 'STUDIO' ? companyName : null,
        headquarters: selectedRole === 'STUDIO' ? headquarters : null,
      };

      const res = await register(payload);
      if (res.success) {
        setGeneratedId(res.user.userId || '');
        if (selectedRole === 'TALENT') {
          // Construct Social Profile Data for existing frontend
          const socialProfileData = {
            userId: res.user.userId,
            name: fullName,
            phone: phoneNumber,
            email: email,
            experience: experienceYears,
            address: 'Hyderabad, India',
            languages: ['English', 'Telugu'],
            otherLanguage: '',
            primaryCraft: craft,
            subCraft,
            secondaryCrafts,
            secondarySubCrafts,
            talents: [craft, ...secondaryCrafts],
            workTypes: ['Cinema'],
            videos: [],
            videoUrls: [],
            experiences: [
              {
                id: `exp-${Date.now()}`,
                projectName: `${craft} Showcase`,
                movieName: `${craft} Production`,
                workedAs: `${craft} Artist`,
                organizationName: 'Film Industry',
                location: 'Hyderabad, India',
                startDate: '2023-01-01',
                endDate: 'Present',
                highlights: `Professional ${craft} artist with ${experienceYears} experience in cinema and entertainment.`,
                title: `${craft} Artist`,
                company: 'Film Industry',
                duration: experienceYears,
                description: `Professional ${craft} artist with ${experienceYears} experience in cinema and entertainment.`
              }
            ]
          };

          // Cache locally for instant availability in existing frontend
          localStorage.setItem('truehunt_social_profile', JSON.stringify(socialProfileData));
          localStorage.setItem('truehunt_profile_details', JSON.stringify(socialProfileData));

          // Also update backend database profile endpoint
          try {
            await fetch('/api/profiles/me', {
              method: 'PATCH',
              headers: {
                'Content-Type': 'application/json',
                'X-User-Email': email,
              },
              body: JSON.stringify(socialProfileData)
            });
          } catch (syncErr) {
            console.warn('Backend profile sync notice:', syncErr);
          }

          showNotification(`Welcome to TrueHunt, ${fullName}! Directing to your social feed...`, 'success');

          // Immediately navigate to the existing social frontend
          navigate('/social');
        } else {
          const producerProfileData = {
            companyName,
            headquarters,
            fullName,
            email,
            phoneNumber,
            role: 'Production House',
            createdAt: new Date().toISOString()
          };
          localStorage.setItem('truehunt_producer_profile', JSON.stringify(producerProfileData));
          showNotification(`Welcome to TrueHunt, ${companyName || fullName}! Directing to Protect Producer...`, 'success');
          navigate('/protect-producer');
        }
      } else {
        setErrorMsg(res.message || 'Registration failed');
      }
    } catch (err) {
      setErrorMsg(err.message || 'An error occurred during registration.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthPageLayout>
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
              padding: '28px 24px',
              marginBottom: '24px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-page)',
              textAlign: 'center'
            }}
          >
            <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: '52px', height: '52px', borderRadius: '50%', backgroundColor: '#DCFCE7', color: '#15803D', marginBottom: '14px' }}>
              <CheckCircle2 size={32} />
            </div>

            <h2 style={{ fontSize: '22px', color: 'var(--color-navy)', marginBottom: '8px', fontWeight: 700 }}>
              {selectedRole === 'TALENT' ? 'Social Account Created!' : 'Production Team Account Created!'}
            </h2>
            <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '18px' }}>
              Your account has been created. Save your TrueHunt ID for your records.
            </p>
            
            <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '10px', padding: '10px 18px', backgroundColor: 'var(--color-white)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)', marginBottom: '20px' }}>
              <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>TrueHunt ID:</span>
              <strong style={{ fontSize: '18px', color: 'var(--color-maroon)', letterSpacing: '1px' }}>
                {generatedId}
              </strong>
              <button
                type="button"
                onClick={handleCopyId}
                aria-label="Copy TrueHunt ID"
                className="btn"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '4px 8px', fontSize: '12px' }}
              >
                <Copy size={14} />
                Copy
              </button>
            </div>

            {/* Submitted Information Summary */}
            <div style={{ backgroundColor: 'var(--color-white)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)', padding: '16px', textAlign: 'left', marginBottom: '22px', fontSize: '13px' }}>
              <div style={{ fontWeight: 700, color: 'var(--color-navy)', marginBottom: '10px', borderBottom: '1px solid var(--color-border-subtle)', paddingBottom: '6px' }}>
                Registered Profile Information:
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <div><span style={{ color: 'var(--color-text-muted)' }}>Name:</span> <strong>{fullName}</strong></div>
                <div><span style={{ color: 'var(--color-text-muted)' }}>Account:</span> <strong style={{ color: 'var(--color-maroon)' }}>{selectedRole === 'TALENT' ? 'Social' : 'Production Team'}</strong></div>
                {selectedRole === 'TALENT' ? (
                  <>
                    <div><span style={{ color: 'var(--color-text-muted)' }}>Primary Craft:</span> <strong>{craft}</strong></div>
                    <div><span style={{ color: 'var(--color-text-muted)' }}>Sub Craft :</span> <strong>{subCraft || 'None selected'}</strong></div>
                    <div><span style={{ color: 'var(--color-text-muted)' }}>Secondary Crafts *:</span> <strong>{secondaryCrafts.length ? secondaryCrafts.join(', ') : 'None selected'}</strong></div>
                    <div><span style={{ color: 'var(--color-text-muted)' }}>Secondary Sub Craft  :</span> <strong>{secondarySubCrafts.length ? [...secondarySubCrafts].sort((first, second) => first.localeCompare(second)).join(', ') : 'None selected'}</strong></div>
                    <div><span style={{ color: 'var(--color-text-muted)' }}>Experience:</span> <strong>{experienceYears}</strong></div>
                  </>
                ) : (
                  <>
                    <div><span style={{ color: 'var(--color-text-muted)' }}>Banner:</span> <strong>{companyName}</strong></div>
                    <div><span style={{ color: 'var(--color-text-muted)' }}>Hub:</span> <strong>{headquarters}</strong></div>
                  </>
                )}
                <div><span style={{ color: 'var(--color-text-muted)' }}>Email:</span> <strong>{email}</strong></div>
                <div><span style={{ color: 'var(--color-text-muted)' }}>Phone:</span> <strong>{phoneNumber}</strong></div>
              </div>
            </div>

            {/* Role-Specific Navigation Buttons */}
            {selectedRole === 'TALENT' ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <Link
                  to="/social#profile"
                  className="btn btn-maroon btn-lg"
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', textDecoration: 'none' }}
                >
                  View My Profile in Social Platform →
                </Link>
                <Link
                  to="/social#home"
                  className="btn btn-lg"
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-white)', textDecoration: 'none', color: 'var(--color-navy)' }}
                >
                  Open Social Feed
                </Link>
                <Link
                  to="/"
                  style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginTop: '8px', textDecoration: 'underline' }}
                >
                  Return to Main Dashboard
                </Link>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <Link
                  to="/protect-producer"
                  className="btn btn-maroon btn-lg"
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', textDecoration: 'none' }}
                >
                  Go to Protect Producer Dashboard →
                </Link>
                <Link
                  to="/"
                  className="btn btn-lg"
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-white)', textDecoration: 'none', color: 'var(--color-navy)' }}
                >
                  Return to Main Dashboard
                </Link>
              </div>
            )}
          </div>
        )}

        {/* Role Selection Tabs */}
        <div hidden={Boolean(generatedId)}>
          <RoleSelectionCards selectedRole={selectedRole} onSelect={setSelectedRole} />
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
                onClick={handleOtpRequest}
                style={{ flexShrink: 0 }}
                title="Click to generate and auto-fill dummy OTP"
              >
                Send OTP
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="email-otp">
              Email OTP <span style={{ fontSize: '12px', color: 'var(--color-text-muted)', fontWeight: 400 }}>(Default dummy OTP: 123456)</span>
            </label>
            <input
              id="email-otp"
              type="text"
              className="form-control"
              value={emailOtp}
              onChange={(e) => setEmailOtp(e.target.value)}
              placeholder="Enter dummy OTP (e.g. 123456)"
              inputMode="numeric"
              autoComplete="off"
              maxLength={6}
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
            <div className="join-talent-fields-grid">
              <div className="form-group">
                <label className="form-label" htmlFor="primary-craft">Primary Craft *</label>
                <select 
                  id="primary-craft"
                  className="form-control"
                  value={craft}
                  onChange={(e) => {
                    const nextCraft = e.target.value;
                    setCraft(nextCraft);
                    setSubCraft('');
                    setSecondaryCrafts((selected) => selected.filter((item) => item !== nextCraft));
                  }}
                  required
                >
                  {SORTED_REGISTRATION_CRAFTS.map((craftOption) => (
                    <option key={craftOption} value={craftOption}>{craftOption}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="sub-craft">Sub Craft * </label>
                <select
                  id="sub-craft"
                  className="form-control"
                  value={subCraft}
                  onChange={(e) => setSubCraft(e.target.value)}
                >
                  <option value="">Select a sub craft * </option>
                  {[...(SUB_CRAFTS_BY_PRIMARY_CRAFT[craft] || [])]
                    .sort((first, second) => first.localeCompare(second))
                    .map((subCraftOption) => (
                    <option key={subCraftOption} value={subCraftOption}>{subCraftOption}</option>
                  ))}
                </select>
              </div>

              <div className="form-group join-experience-field">
                <label className="form-label" htmlFor="experience-years">Experience *</label>
                <select
                  id="experience-years"
                  className="form-control"
                  value={experienceYears}
                  onChange={(e) => setExperienceYears(e.target.value)}
                >
                  {EXPERIENCE_OPTIONS.map((experienceOption) => (
                    <option key={experienceOption} value={experienceOption}>{experienceOption}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <span className="form-label" id="secondary-crafts-label">Secondary Crafts * </span>
                <div className="secondary-craft-dropdown">
                  <button
                    type="button"
                    className="form-control secondary-craft-dropdown-trigger"
                    aria-labelledby="secondary-crafts-label"
                    aria-describedby="secondary-crafts-help"
                    aria-expanded={isSecondaryCraftDropdownOpen}
                    aria-controls="secondary-craft-options"
                    onClick={() => setIsSecondaryCraftDropdownOpen((open) => !open)}
                  >
                    {secondaryCrafts.length
                      ? `${secondaryCrafts.length} selected`
                      : 'Select secondary crafts'}
                  </button>
                  {isSecondaryCraftDropdownOpen && (
                  <div className="secondary-craft-dropdown-options" id="secondary-craft-options">
                    {SORTED_REGISTRATION_CRAFTS.filter((craftOption) => craftOption !== craft).map((craftOption) => (
                      <label className="secondary-craft-dropdown-option" key={craftOption}>
                        <input
                          type="checkbox"
                          checked={secondaryCrafts.includes(craftOption)}
                          onChange={() => {
                            const nextSecondaryCrafts = secondaryCrafts.includes(craftOption)
                              ? secondaryCrafts.filter((item) => item !== craftOption)
                              : [...secondaryCrafts, craftOption];
                            const nextSubCraftOptions = new Set(
                              nextSecondaryCrafts.flatMap((selectedCraft) => SUB_CRAFTS_BY_PRIMARY_CRAFT[selectedCraft] || [])
                            );
                            setSecondaryCrafts(nextSecondaryCrafts);
                            setSecondarySubCrafts((selected) => selected.filter((item) => nextSubCraftOptions.has(item)));
                          }}
                        />
                        <span>{craftOption}</span>
                      </label>
                    ))}
                  </div>
                  )}
                </div>
                <small id="secondary-crafts-help" style={{ color: 'var(--color-text-muted)' }}>
                  Choose one or more additional crafts.
                </small>
              </div>

              <div className="form-group">
                <span className="form-label" id="secondary-sub-crafts-label">Secondary Sub Craft * </span>
                <div className="secondary-craft-dropdown">
                  <button
                    type="button"
                    className="form-control secondary-craft-dropdown-trigger"
                    aria-labelledby="secondary-sub-crafts-label"
                    aria-describedby="secondary-sub-crafts-help"
                    aria-expanded={isSecondarySubCraftDropdownOpen}
                    aria-controls="secondary-sub-craft-options"
                    onClick={() => setIsSecondarySubCraftDropdownOpen((open) => !open)}
                  >
                    {secondarySubCrafts.length
                      ? `${secondarySubCrafts.length} selected`
                      : 'Select secondary sub crafts'}
                  </button>
                  {isSecondarySubCraftDropdownOpen && (
                    <div className="secondary-craft-dropdown-options" id="secondary-sub-craft-options">
                      <input
                        type="search"
                        className="form-control secondary-sub-craft-search"
                        aria-label="Search secondary sub-crafts"
                        placeholder="Search sub crafts"
                        value={secondarySubCraftSearch}
                        onChange={(event) => setSecondarySubCraftSearch(event.target.value)}
                        disabled={secondaryCrafts.length === 0}
                      />
                      {secondarySubCraftOptions
                        .filter((option) => option.toLowerCase().startsWith(secondarySubCraftSearch.trim().toLowerCase()))
                        .map((option) => (
                          <label className="secondary-craft-dropdown-option" key={option}>
                            <input
                              type="checkbox"
                              checked={secondarySubCrafts.includes(option)}
                              onChange={() => setSecondarySubCrafts((selected) => (
                                selected.includes(option)
                                  ? selected.filter((item) => item !== option)
                                  : [...selected, option]
                              ))}
                            />
                            <span>{option}</span>
                          </label>
                        ))}
                      {secondaryCrafts.length === 0 && (
                        <p className="secondary-sub-craft-empty">Select a secondary craft first.</p>
                      )}
                      {secondaryCrafts.length > 0 && secondarySubCraftOptions.every((option) => !option.toLowerCase().startsWith(secondarySubCraftSearch.trim().toLowerCase())) && (
                        <p className="secondary-sub-craft-empty">No sub crafts match your search.</p>
                      )}
                    </div>
                  )}
                </div>
                <small id="secondary-sub-crafts-help" style={{ color: 'var(--color-text-muted)' }}>
                  Search and choose one or more sub crafts.
                </small>
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
    </AuthPageLayout>
  );
}
