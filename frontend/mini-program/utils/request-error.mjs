export const isUnauthorizedResponse = (error = {}) => {
  return Number(error.statusCode) === 401
    || Number(error.code) === 401
    || Number(error.data?.code) === 401
}
