// The arc of a 0–100 progress ring. The circle is drawn with pathLength="100", so the dash
// is exactly the value: 0.6 % is a sliver, 100 % the full circle — whatever the radius and
// however the browser measures the circle. Below 3 % the ends are square: a round cap
// (half the stroke width on each side) would make a tiny value look like a dot or more.
export function ringArc(value) {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null
  const v = Math.max(0, Math.min(100, value))
  return {
    value: v,
    dasharray: `${v} ${100 - v}`,
    empty: '0 100',
    cap: v < 3 ? 'butt' : 'round'
  }
}
