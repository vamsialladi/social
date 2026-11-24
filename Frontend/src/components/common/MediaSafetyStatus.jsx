import './MediaSafetyStatus.css'

function MediaSafetyStatus({
  safety,
  blockedMessage,
  safeMessage = 'Safe content verified.',
  showBreakdown = true,
}) {
  if (!safety?.isSupported) return null

  const isFailed = Boolean(safety.error)
  const statusClass = safety.isRejected || isFailed ? ' is-rejected' : ''
  return (
    <section
      className={`media-safety${statusClass}`}
      aria-label="Content safety analysis"
      aria-live="polite"
    >
      {safety.isAnalyzing ? (
        <p role="status">Scanning {safety.kind} for adult content…</p>
      ) : safety.isRejected ? (
        <p role="alert">
          {blockedMessage || `This ${safety.kind} has high adult content, so we can’t post this ${safety.kind}.`}
        </p>
      ) : safety.error ? (
        <p role="alert">{safety.error}</p>
      ) : safety.predictions.length > 0 ? (
        <p>{safeMessage}</p>
      ) : null}
      {showBreakdown && safety.predictions.length > 0 && (
        <div className="media-safety-breakdown">
          <strong>
            {safety.kind === 'video' ? 'Average video frame scores' : 'Image content scores'}
          </strong>
          {safety.predictions.map((item) => {
            const percentage = Math.min(100, item.probability * 100)
            const isFlaggedExplicit = safety.isRejected
              && ['Porn', 'Hentai'].includes(item.className)
            return (
              <div className="media-safety-score" key={item.className}>
                <div>
                  <span>{item.className}{isFlaggedExplicit ? ' (18+)' : ''}</span>
                  <span>{percentage.toFixed(2)}%</span>
                </div>
                <div className="media-safety-track">
                  <div
                    className={isFlaggedExplicit ? 'is-explicit' : ''}
                    style={{ width: `${percentage}%` }}
                  />
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}

export default MediaSafetyStatus
