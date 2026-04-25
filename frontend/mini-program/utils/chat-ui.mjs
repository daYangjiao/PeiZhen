export const buildChatHeaderMeta = ({ role = '' } = {}) => {
  const normalizedRole = String(role || '').toLowerCase()
  if (normalizedRole === 'escort') {
    return {
      peerLabel: '用户',
      subtitle: '服务沟通',
    }
  }

  return {
    peerLabel: '陪诊师',
    subtitle: '陪诊沟通',
  }
}
