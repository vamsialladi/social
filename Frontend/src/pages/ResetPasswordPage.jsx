import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2, ArrowLeft } from 'lucide-react';
import TrueHuntLogo from '../components/common/TrueHuntLogo';
import AuthPageLayout from '../components/common/AuthPageLayout';
import PasswordField from '../components/common/PasswordField';
import { isPasswordValid } from '../components/common/passwordValidation';
import { api } from '../services/api';
import { useNotification } from '../context/NotificationContext';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState(() => searchParams.get('token') || '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const { showNotification } = useNotification();

  const isFormValid =
    token.trim().length > 0 &&
    isPasswordValid(newPassword) &&
    newPassword === confirmPassword;

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!isFormValid) {
      return;
    }

    setErrorMsg('');
    setSuccessMsg('');
    setLoading(true);
    try {
      const res = await api.auth.resetPassword({
        token: token.trim(),
        newPassword,
        confirmPassword,
      });
      setSuccessMsg(res.data?.message || 'Password has been reset successfully.');
      showNotification('Password reset successful. Please sign in with your new password.', 'success');
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
            Reset Password
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>
            Create a strong new password that meets all security requirements.
          </p>
        </div>

        {successMsg && (
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
            <span>{successMsg}</span>
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

        {!successMsg && (
          <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '16px' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" htmlFor="reset-token">Reset Token</label>
              <input
                id="reset-token"
                type="text"
                className="form-control"
                value={token}
                onChange={(e) => {
                  setToken(e.target.value);
                  setErrorMsg('');
                }}
                placeholder="Paste your password reset token"
                autoComplete="one-time-code"
                required
              />
            </div>

            <PasswordField
              id="reset-new-password"
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
              id="reset-confirm-password"
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
              id="reset-password-submit-btn"
              disabled={loading || !isFormValid}
              className="btn btn-maroon btn-lg"
              style={{ width: '100%', fontSize: '15px', fontWeight: 700 }}
            >
              {loading ? 'Resetting Password...' : 'Reset Password'}
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
