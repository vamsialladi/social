import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, CheckCircle2, ArrowLeft } from 'lucide-react';
import TrueHuntLogo from '../components/common/TrueHuntLogo';
import AuthPageLayout from '../components/common/AuthPageLayout';
import PasswordField from '../components/common/PasswordField';
import { isPasswordValid } from '../components/common/passwordValidation';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [requestSent, setRequestSent] = useState(false);
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [statusMsg, setStatusMsg] = useState('');
  const [resetComplete, setResetComplete] = useState(false);
  const { showNotification } = useNotification();

  const handleRequestReset = async (event) => {
    event.preventDefault();
    setErrorMsg('');
    setStatusMsg('');
    setLoading(true);
    try {
      const res = await api.auth.forgotPassword(email.trim());
      setRequestSent(true);
      setStatusMsg(res.data?.message || 'If an account exists for that email, password reset instructions have been sent.');
      showNotification('Password reset instructions have been sent if the account exists.', 'success');
    } catch (err) {
      setErrorMsg(err.message || 'Unable to process password reset request.');
    } finally {
      setLoading(false);
    }
  };

  const isResetFormValid =
    token.trim().length > 0 &&
    isPasswordValid(newPassword) &&
    newPassword === confirmPassword;

  const handleResetSubmit = async (event) => {
    event.preventDefault();
    if (!isResetFormValid) {
      return;
    }
    setErrorMsg('');
    setStatusMsg('');
    setLoading(true);
    try {
      const res = await api.auth.resetPassword({
        token: token.trim(),
        newPassword,
        confirmPassword,
      });
      setResetComplete(true);
      setStatusMsg(res.data?.message || 'Password has been reset successfully.');
      showNotification('Your password has been reset. You can now sign in.', 'success');
    } catch (err) {
      setErrorMsg(err.message || 'Invalid or expired password reset token.');
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
          maxWidth: '480px',
          padding: '36px 32px',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-card)',
          backgroundColor: 'var(--color-white)',
          border: '1px solid var(--color-border)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{ display: 'inline-block', marginBottom: '14px' }}>
            <TrueHuntLogo size="lg" />
          </div>
          <h1 style={{ fontSize: '24px', color: 'var(--color-navy)', marginBottom: '6px' }}>
            {requestSent ? 'Reset Your Password' : 'Forgot Password'}
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>
            {requestSent
              ? 'Enter the reset token from your email and choose a strong new password.'
              : 'Enter your registered email to receive password reset instructions.'}
          </p>
        </div>

        {statusMsg && (
          <div
            role="status"
            style={{
              padding: '10px 12px',
              marginBottom: '16px',
              backgroundColor: '#F0FDF4',
              border: '1px solid #86EFAC',
              borderRadius: 'var(--radius-sm)',
              color: '#166534',
              fontSize: '13px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <CheckCircle2 size={16} />
            <span>{statusMsg}</span>
          </div>
        )}

        {errorMsg && (
          <div
            role="alert"
            style={{
              padding: '10px 12px',
              marginBottom: '16px',
              backgroundColor: '#FEF2F2',
              border: '1px solid #FCA5A5',
              borderRadius: 'var(--radius-sm)',
              color: '#991B1B',
              fontSize: '13px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <AlertCircle size={16} />
            <span>{errorMsg}</span>
          </div>
        )}

        {!requestSent && (
          <form onSubmit={handleRequestReset}>
            <div className="form-group">
              <label className="form-label" htmlFor="forgot-email">Professional Email</label>
              <input
                id="forgot-email"
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setErrorMsg('');
                }}
                placeholder="name@cinema.com"
                autoComplete="email"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading || !email.trim()}
              className="btn btn-maroon btn-lg"
              style={{ width: '100%', fontSize: '15px', fontWeight: 700 }}
            >
              {loading ? 'Sending Instructions...' : 'Send Reset Instructions'}
            </button>

            <div style={{ marginTop: '14px', textAlign: 'center' }}>
              <button
                type="button"
                onClick={() => {
                  setRequestSent(true);
                  setErrorMsg('');
                }}
                style={{
                  border: 'none',
                  background: 'none',
                  color: 'var(--color-maroon)',
                  fontSize: '13px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Already have a reset token? Enter it here
              </button>
            </div>
          </form>
        )}

        {requestSent && !resetComplete && (
          <form onSubmit={handleResetSubmit} style={{ display: 'grid', gap: '16px' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" htmlFor="forgot-reset-token">Reset Token</label>
              <input
                id="forgot-reset-token"
                type="text"
                className="form-control"
                value={token}
                onChange={(e) => {
                  setToken(e.target.value);
                  setErrorMsg('');
                }}
                placeholder="Paste your reset token"
                autoComplete="one-time-code"
                required
              />
            </div>

            <PasswordField
              id="forgot-new-password"
              name="newPassword"
              label="New Password"
              value={newPassword}
              onChange={(e) => {
                setNewPassword(e.target.value);
                setErrorMsg('');
              }}
              placeholder="Create a strong password"
              autoComplete="new-password"
              showValidation
            />

            <PasswordField
              id="forgot-confirm-password"
              name="confirmPassword"
              label="Confirm New Password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                setErrorMsg('');
              }}
              placeholder="Re-enter your new password"
              autoComplete="new-password"
              isConfirm
              compareValue={newPassword}
            />

            <button
              type="submit"
              disabled={loading || !isResetFormValid}
              className="btn btn-maroon btn-lg"
              style={{ width: '100%', fontSize: '15px', fontWeight: 700 }}
            >
              {loading ? 'Updating Password...' : 'Reset Password'}
            </button>
          </form>
        )}

        <div style={{ marginTop: '22px', textAlign: 'center', fontSize: '14px' }}>
          <Link
            to="/sign-in"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              color: 'var(--color-maroon)',
              fontWeight: 700,
            }}
          >
            <ArrowLeft size={16} />
            <span>Back to Sign In</span>
          </Link>
        </div>
      </div>
    </AuthPageLayout>
  );
}
