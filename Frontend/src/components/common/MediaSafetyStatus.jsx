import './MediaSafetyStatus.css'

function MediaSafetyStatus({
  safety,
  blockedMessage,
  safeMessage = 'Safe content verified.',
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
    </section>
  )
}

export default MediaSafetyStatus
