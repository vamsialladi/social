import { useEffect, useState } from 'react'
import api from '../../api'
import {
  ArrowRight,
  Bell,
  LockKeyhole,
  Shield,
  UserRound,
} from 'lucide-react'
import './Settings.css'

const settingsSections = [
  {
    id: 'preferences',
    label: 'Account preferences',
    icon: UserRound,
    description: 'Manage your profile details and contact information.',
  },
  {
    id: 'security',
    label: 'Sign in & security',
    icon: LockKeyhole,
    description: 'Review sign-in and security information for your account.',
  },
  {
    id: 'privacy',
    label: 'Data privacy',
    icon: Shield,
    description: 'Learn about the profile data currently associated with your account.',
  },
  {
    id: 'notifications',
    label: 'Notifications',
    icon: Bell,
    description: 'Choose which notifications you receive.',
  },
]

const notificationCategories = [
  {
    id: 'connections',
    label: 'Connecting with others',
    settingLabel: 'Allow connections related notifications',
  },
  {
    id: 'network-catch-up',
    label: 'Network catch-up updates',
    settingLabel: 'Allow network catch-up update notifications',
  },
  {
    id: 'posting',
    label: 'Posting and commenting',
    settingLabel: 'Allow posting and commenting notifications',
  },
  {
    id: 'messaging',
    label: 'Messaging',
    settingLabel: 'Allow messaging notifications',
  },
  {
    id: 'news',
    label: 'News and reports',
    settingLabel: 'Allow news and reports notifications',
  },
  {
    id: 'profile',
    label: 'Updating your profile',
    settingLabel: 'Allow profile update notifications',
  },
  {
    id: 'verifications',
    label: 'Verifications',
    settingLabel: 'Allow verification notifications',
  },
]

function Settings({ profileDetails, profilePreview, onEditProfile, theme, onThemeChange }) {
  const [activeSection, setActiveSection] = useState('preferences')
  const [isPasswordFormOpen, setIsPasswordFormOpen] = useState(false)
  const [isThemeOptionsOpen, setIsThemeOptionsOpen] = useState(false)
  const [expandedNotificationCategories, setExpandedNotificationCategories] = useState({})
  const [notificationPreferences, setNotificationPreferences] = useState(() =>
    Object.fromEntries(notificationCategories.map(({ id }) => [id, true])),
  )
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(false)
  const [securityMessage, setSecurityMessage] = useState('')
  const [passwordMessage, setPasswordMessage] = useState('')
  const [accountManagementMessage, setAccountManagementMessage] = useState('')
  const name = profileDetails.name || 'Your name'
  const email = profileDetails.email || 'Not added yet'
  const phone = profileDetails.phone || 'Not added yet'
  const address = profileDetails.address || 'Not added yet'
  const selectedSection = settingsSections.find((section) => section.id === activeSection)

  useEffect(() => {
    async function loadSettings() {
      try {
        const data = await api.getSettings()
        if (data?.notifications) {
          setNotificationPreferences((prev) => ({ ...prev, ...data.notifications }))
        }
      } catch (err) {
        console.warn('Backend settings load note:', err)
      }
    }
    loadSettings()
  }, [])

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    const form = event.currentTarget
    const formData = new FormData(form)

    if (formData.get('new-password') !== formData.get('confirm-password')) {
      setPasswordMessage('The new passwords do not match.')
      return
    }

    try {
      await api.changePassword({
        'current-password': formData.get('current-password')?.toString() || '',
        'new-password': formData.get('new-password')?.toString() || '',
        'confirm-password': formData.get('confirm-password')?.toString() || '',
      })
      setPasswordMessage('Password updated successfully!')
      form.reset()
      setTimeout(() => setIsPasswordFormOpen(false), 2000)
    } catch (err) {
      setPasswordMessage(err.message || 'Failed to update password.')
    }
  }

  async function handleToggleNotification(id) {
    const nextVal = !notificationPreferences[id]
    const nextPrefs = {
      ...notificationPreferences,
      [id]: nextVal,
    }
    setNotificationPreferences(nextPrefs)
    try {
      await api.updateNotificationPreferences({ [id]: nextVal })
    } catch (err) {
      console.warn('Notification preference API note:', err)
    }
  }

  async function handleThemeChange(mode) {
    onThemeChange(mode)
    try {
      await api.updateTheme(mode)
    } catch (err) {
      console.warn('Theme update API note:', err)
    }
  }

  async function handleDeleteAccount() {
    if (!window.confirm("Are you sure you want to close and delete your TrueHunt account? This action cannot be undone.")) {
      return
    }
    try {
      const res = await api.deleteAccount()
      setAccountManagementMessage(res.message || 'Account successfully scheduled for deletion.')
    } catch (err) {
      setAccountManagementMessage(err.message || 'Failed to schedule account deletion.')
    }
  }

  return (
    <section className="settings-page" aria-labelledby="settings-title">
      <header className="settings-page-header">
        <p className="settings-eyebrow">YOUR ACCOUNT</p>
        <h1 id="settings-title">Settings</h1>
        <p>Manage your account preferences, privacy, and notifications.</p>
      </header>

      <div className="settings-layout">
        <aside className="settings-sidebar" aria-label="Settings navigation">
          <section className="settings-sidebar-profile" aria-label="Your profile">
            <span className="settings-avatar" aria-hidden="true">
              {profilePreview
                ? <img src={profilePreview} alt="" />
                : name.charAt(0).toUpperCase()}
            </span>
            <strong>{name}</strong>
            <p>{profileDetails.talents.length > 0 ? profileDetails.talents.join(' · ') : 'Add your professional headline'}</p>
          </section>
          <nav className="settings-sidebar-nav" aria-label="Settings sections">
            {settingsSections.map(({ id, label, icon: Icon }) => (
              <button
                className={`settings-nav-item${activeSection === id ? ' is-active' : ''}`}
                key={id}
                type="button"
                aria-current={activeSection === id ? 'page' : undefined}
                onClick={() => setActiveSection(id)}
              >
                <Icon size={19} strokeWidth={2.3} aria-hidden="true" />
                <span>{label}</span>
              </button>
            ))}
          </nav>
        </aside>

        <div className="settings-content" aria-live="polite">
          <section className="settings-card" aria-labelledby="settings-section-title">
            <div className="settings-section-heading">
              <div>
                <h2 id="settings-section-title">
                  {activeSection === 'notifications' ? 'Notifications you receive' : selectedSection.label}
                </h2>
                {activeSection !== 'notifications' && <p>{selectedSection.description}</p>}
              </div>
              {activeSection === 'preferences' && (
                <button className="settings-edit-button" type="button" onClick={onEditProfile}>
                  Edit profile
                </button>
              )}
            </div>
            {activeSection === 'preferences' && (
              <dl className="settings-details-list">
                <div className="settings-detail-row">
                  <dt>Name</dt>
                  <dd>{name}</dd>
                </div>
                <div className="settings-detail-row">
                  <dt>Email</dt>
                  <dd>{email}</dd>
                </div>
                <div className="settings-detail-row">
                  <dt>Phone</dt>
                  <dd>{phone}</dd>
                </div>
                <div className="settings-detail-row">
                  <dt>Location</dt>
                  <dd>{address}</dd>
                </div>
              </dl>
            )}
            {activeSection === 'security' && (
              <div className="settings-access-list">
                {securityMessage && (
                  <p style={{ padding: '8px 12px', background: 'var(--color-surface, #f0f4f8)', borderRadius: '6px', fontSize: '13px', color: '#1a73e8', marginBottom: '8px' }}>
                    {securityMessage}
                  </p>
                )}
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSecurityMessage(`Verified primary email: ${email}`)}
                >
                  <span>Email addresses</span>
                  <span className="settings-access-value">
                    {email}
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSecurityMessage(`Verified primary phone: ${phone}`)}
                >
                  <span>Phone numbers</span>
                  <span className="settings-access-value">
                    {phone}
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setSecurityMessage('Passkeys active: 1 device key (Windows Hello / FIDO2).')}
                >
                  <span>Passkeys</span>
                  <span className="settings-access-value">
                    1 registered
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
                <div className="settings-access-row">
                  <button
                    className="settings-access-action"
                    type="button"
                    aria-expanded={isPasswordFormOpen}
                    aria-controls="settings-password-form"
                    onClick={() => {
                      setIsPasswordFormOpen((open) => !open)
                      setPasswordMessage('')
                      setSecurityMessage('')
                    }}
                  >
                    <span>Change password</span>
                    <ArrowRight size={18} aria-hidden="true" />
                  </button>
                </div>
                <div
                  className="settings-access-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => {
                    const next = !twoFactorEnabled
                    setTwoFactorEnabled(next)
                    setSecurityMessage(`Two-factor authentication is now ${next ? 'enabled' : 'disabled'}.`)
                  }}
                >
                  <span>Two-factor authentication</span>
                  <span className="settings-access-value">
                    {twoFactorEnabled ? 'On (SMS/App)' : 'Off'}
                    <ArrowRight size={18} aria-hidden="true" />
                  </span>
                </div>
              </div>
            )}
            {activeSection === 'security' && isPasswordFormOpen && (
              <form
                className="settings-password-form"
                id="settings-password-form"
                onSubmit={handlePasswordSubmit}
              >
                <h3>Change password</h3>
                <label>
                  Current password
                  <input name="current-password" type="password" autoComplete="current-password" required />
                </label>
                <label>
                  New password
                  <input name="new-password" type="password" autoComplete="new-password" minLength={8} required />
                  <small>Use at least 8 characters.</small>
                </label>
                <label>
                  Confirm new password
                  <input name="confirm-password" type="password" autoComplete="new-password" minLength={8} required />
                </label>
                {passwordMessage && (
                  <p
                    className={passwordMessage.startsWith('The new passwords') ? 'settings-password-error' : 'settings-password-note'}
                    role="status"
                  >
                    {passwordMessage}
                  </p>
                )}
                <button className="settings-edit-button" type="submit">
                  Update password
                </button>
              </form>
            )}
            {activeSection === 'privacy' && (
              <dl className="settings-details-list">
                <div className="settings-detail-row">
                  <dt>Profile information</dt>
                  <dd>Managed in your profile</dd>
                </div>
                <div className="settings-detail-row">
                  <dt>Contact information</dt>
                  <dd>Managed in your profile</dd>
                </div>
              </dl>
            )}
            {activeSection === 'notifications' && (
              <ul className="settings-notification-list" aria-label="Notification categories">
                {notificationCategories.map(({ id, label, settingLabel }) => {
                  const isExpanded = Boolean(expandedNotificationCategories[id])
                  const settingId = `notification-setting-${id}`

                  return (
                    <li className="settings-notification-item" key={id}>
                      <button
                        className="settings-notification-row"
                        type="button"
                        aria-expanded={isExpanded}
                        aria-controls={settingId}
                        onClick={() => {
                          setExpandedNotificationCategories((expanded) => ({
                            ...expanded,
                            [id]: !expanded[id],
                          }))
                        }}
                      >
                        <span>{label}</span>
                        <ArrowRight
                          className={isExpanded ? 'is-expanded' : ''}
                          size={18}
                          aria-hidden="true"
                        />
                      </button>
                      <div
                        className="settings-notification-setting"
                        id={settingId}
                        hidden={!isExpanded}
                      >
                        <span>{settingLabel}</span>
                        <button
                          className={`settings-notification-switch${notificationPreferences[id] ? ' is-on' : ''}`}
                          type="button"
                          role="switch"
                          aria-checked={notificationPreferences[id]}
                          aria-label={settingLabel}
                          onClick={() => handleToggleNotification(id)}
                        >
                          <span>{notificationPreferences[id] ? 'On' : 'Off'}</span>
                          <span className="settings-notification-switch-track" aria-hidden="true">
                            <span />
                          </span>
                        </button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>
          {activeSection === 'preferences' && (
            <section className="settings-card settings-display-card" aria-labelledby="settings-display-title">
              <button
                className="settings-display-toggle"
                type="button"
                aria-expanded={isThemeOptionsOpen}
                aria-controls="settings-theme-options"
                onClick={() => setIsThemeOptionsOpen((open) => !open)}
              >
                <span>
                  <strong id="settings-display-title">Display</strong>
                  <small>Choose how TrueHunt looks on this device.</small>
                </span>
                <span className="settings-current-theme">
                  {theme === 'light' ? 'Light mode' : 'Dark mode'}
                  <span aria-hidden="true">{isThemeOptionsOpen ? '⌃' : '⌄'}</span>
                </span>
              </button>
              {isThemeOptionsOpen && (
                <fieldset className="settings-theme-options" id="settings-theme-options">
                  <legend>Choose a theme</legend>
                  {['light', 'dark'].map((mode) => (
                    <label className={`settings-theme-option${theme === mode ? ' is-selected' : ''}`} key={mode}>
                      <input
                        type="radio"
                        name="theme"
                        value={mode}
                        checked={theme === mode}
                        onChange={() => handleThemeChange(mode)}
                      />
                      <span>{mode === 'light' ? 'Light mode' : 'Dark mode'}</span>
                    </label>
                  ))}
                </fieldset>
              )}
            </section>
          )}
          {activeSection === 'preferences' && (
            <section className="settings-card settings-management-card" aria-labelledby="settings-management-title">
              <h2 id="settings-management-title">Account management</h2>
              <div className="settings-management-list">
                <button
                  className="settings-management-action"
                  type="button"
                  onClick={handleDeleteAccount}
                >
                  <span>Close and delete account</span>
                  <ArrowRight size={18} aria-hidden="true" />
                </button>
              </div>
              {accountManagementMessage && (
                <p className="settings-management-message" role="status">
                  {accountManagementMessage}
                </p>
              )}
            </section>
          )}
        </div>
      </div>
    </section>
  )
}

export default Settings