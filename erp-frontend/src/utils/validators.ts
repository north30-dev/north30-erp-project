/**
 * 表单校验辅助（与后端规则对齐的前置校验，后端校验为第二层保险）。
 */

/** 口令复杂度：≥8 位且大写字母/小写字母/数字/特殊字符 4 类中至少含 3 类（对齐后端 PasswordStrategy / 18009） */
export function isPasswordComplexityValid(password: string): boolean {
  if (password.length < 8) {
    return false
  }
  let categories = 0
  if (/[a-z]/.test(password)) categories += 1
  if (/[A-Z]/.test(password)) categories += 1
  if (/\d/.test(password)) categories += 1
  if (/[^a-zA-Z0-9]/.test(password)) categories += 1
  return categories >= 3
}

/** 口令复杂度校验文案 */
export const PASSWORD_COMPLEXITY_MESSAGE = '不少于 8 位，且须包含大写字母、小写字母、数字、特殊字符中的至少 3 类'
