export const HOSPITAL_OPTIONS = [
  '都江堰医院',
  '成都市医院',
  '都江堰市人民医院',
  '都江堰市中医医院',
  '成都市第一人民医院',
  '成都市第二人民医院',
  '成都市第三人民医院'
]

export const normalizeHospitalOption = (value) => {
  const trimmed = String(value || '').trim()
  return HOSPITAL_OPTIONS.includes(trimmed) ? trimmed : ''
}

export const findHospitalOptionIndex = (value) => HOSPITAL_OPTIONS.indexOf(normalizeHospitalOption(value))
