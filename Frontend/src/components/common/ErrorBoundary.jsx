import React from 'react';

/**
 * Global React Error Boundary
 * Catches JavaScript errors anywhere in the child component tree,
 * logs them, and displays a graceful fallback UI instead of crashing
 * to a blank white screen.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo });
    console.error('TrueHunt Application Error Caught by Boundary:', error, errorInfo);
  }

  handleReload = () => {
    try {
      sessionStorage.removeItem('truehunt_intro_seen');
    } catch {}
    window.location.reload();
  };

  handleReset = () => {
    try {
      sessionStorage.clear();
      localStorage.removeItem('truehunt_token');
      localStorage.removeItem('truehunt_user');
    } catch {}
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      const errorMessage = this.state.error?.message || 'An unexpected rendering error occurred.';
      return (
        <div
          style={{
            minHeight: '100vh',
            width: '100vw',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: '#030712',
            color: '#FFFFFF',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            padding: '24px',
            boxSizing: 'border-box',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              maxWidth: '560px',
              width: '100%',
              backgroundColor: '#0F172A',
              border: '1px solid rgba(245, 158, 11, 0.3)',
              borderRadius: '16px',
              padding: '36px 28px',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.6)',
            }}
          >
            <div
              style={{
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                backgroundColor: 'rgba(128, 24, 35, 0.3)',
                color: '#F59E0B',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '28px',
                margin: '0 auto 18px',
                border: '1px solid rgba(245, 158, 11, 0.4)',
              }}
            >
              🎬
            </div>

            <h1
              style={{
                fontSize: '22px',
                fontWeight: 700,
                color: '#FFFFFF',
                marginBottom: '10px',
                letterSpacing: '-0.3px',
              }}
            >
              TrueHunt Cinema Network
            </h1>

            <p
              style={{
                fontSize: '14px',
                color: '#94A3B8',
                marginBottom: '20px',
                lineHeight: 1.6,
              }}
            >
              The application encountered a display issue while rendering this view.
              Your data is safe.
            </p>

            <div
              style={{
                backgroundColor: '#030712',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: '8px',
                padding: '12px 16px',
                marginBottom: '24px',
                fontSize: '12px',
                color: '#F87171',
                fontFamily: 'monospace',
                textAlign: 'left',
                overflowX: 'auto',
                maxHeight: '120px',
              }}
            >
              {errorMessage}
            </div>

            <div
              style={{
                display: 'flex',
                gap: '12px',
                justifyContent: 'center',
                flexWrap: 'wrap',
              }}
            >
              <button
                type="button"
                onClick={this.handleReload}
                style={{
                  padding: '10px 22px',
                  backgroundColor: '#801823',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '8px',
                  fontWeight: 600,
                  fontSize: '14px',
                  cursor: 'pointer',
                  transition: 'background-color 0.2s ease',
                }}
              >
                Reload Application
              </button>

              <button
                type="button"
                onClick={this.handleReset}
                style={{
                  padding: '10px 22px',
                  backgroundColor: 'transparent',
                  color: '#CBD5E1',
                  border: '1px solid rgba(255, 255, 255, 0.2)',
                  borderRadius: '8px',
                  fontWeight: 500,
                  fontSize: '14px',
                  cursor: 'pointer',
                }}
              >
                Reset & Return Home
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

