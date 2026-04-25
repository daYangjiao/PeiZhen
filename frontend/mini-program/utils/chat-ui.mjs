export const buildChatHeaderMeta = ({ role = '' } = {}) => {
  const normalizedRole = String(role || '').toLowerCase()
  if (normalizedRole === 'escort') {
    return {
      peerLabel: '用户',
      subtitle: '',
    }
  }

  return {
    peerLabel: '陪诊师',
    subtitle: '',
  }
}
