const MAX_RATING = 5
const MIN_RATING = 0

const normalizeRating = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return 0
  }
  return Math.min(MAX_RATING, Math.max(MIN_RATING, numeric))
}

export const formatRatingScore = (value) => normalizeRating(value).toFixed(1)

export const getRatingStarCount = (value) => Math.round(normalizeRating(value))
