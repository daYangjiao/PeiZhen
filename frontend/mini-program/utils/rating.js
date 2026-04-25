const MAX_RATING = 5
const MIN_RATING = 0

const normalizeRating = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return 0
  }
  return Math.min(MAX_RATING, Math.max(MIN_RATING, numeric))
}

const hasEvaluationCount = (evaluationCount) => evaluationCount !== undefined && evaluationCount !== null

const hasEvaluations = (evaluationCount) => !hasEvaluationCount(evaluationCount) || Number(evaluationCount || 0) > 0

export const formatRatingScore = (value, evaluationCount) => {
  if (!hasEvaluations(evaluationCount)) {
    return '暂无评分'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '暂无评分'
  }
  return normalizeRating(numeric).toFixed(1)
}

export const formatPraiseRate = (value, evaluationCount) => {
  if (!hasEvaluations(evaluationCount)) {
    return '暂无评价'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '0%'
  }
  return `${Math.max(0, Math.min(100, Math.round(numeric)))}%`
}

export const getRatingStarStates = (value, evaluationCount) => {
  if (!hasEvaluations(evaluationCount)) {
    return Array.from({ length: MAX_RATING }, () => 'empty')
  }
  const roundedToHalf = Math.round(normalizeRating(value) * 2) / 2
  return Array.from({ length: MAX_RATING }, (_, index) => {
    const starValue = index + 1
    if (roundedToHalf >= starValue) return 'full'
    if (roundedToHalf >= starValue - 0.5) return 'half'
    return 'empty'
  })
}

export const getRatingStarCount = (value) => Math.round(normalizeRating(value))
