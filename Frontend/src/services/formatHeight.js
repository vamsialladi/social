export function formatHeightInFeet(heightCm) {
  const centimeters = Number(heightCm)
  if (!Number.isFinite(centimeters) || centimeters <= 0) return ''

  const totalInches = Math.round(centimeters / 2.54)
  const feet = Math.floor(totalInches / 12)
  const inches = totalInches % 12

  if (!feet) return `${inches} in`
  return inches ? `${feet} ft ${inches} in` : `${feet} ft`
}
