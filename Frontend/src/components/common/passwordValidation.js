export const PASSWORD_REQUIREMENTS = [
  {
    id: 'minLength',
    label: 'Minimum 8 characters',
    test: (pw) => pw.length >= 8,
  },
  {
    id: 'maxLength',
    label: 'Maximum 20 characters',
    test: (pw) => pw.length > 0 && pw.length <= 20,
  },
  {
    id: 'uppercase',
    label: 'At least one uppercase letter (A-Z)',
    test: (pw) => /[A-Z]/.test(pw),
  },
  {
    id: 'lowercase',
    label: 'At least one lowercase letter (a-z)',
    test: (pw) => /[a-z]/.test(pw),
  },
  {
    id: 'number',
    label: 'At least one number (0-9)',
    test: (pw) => /[0-9]/.test(pw),
  },
  {
    id: 'special',
    label: 'At least one special character (@, #, $, %, !, &, *)',
    test: (pw) => /[@#$%!&*]/.test(pw),
  },
  {
    id: 'noSpaces',
    label: 'No spaces allowed',
    test: (pw) => pw.length > 0 && !/\s/.test(pw),
  },
];

export function evaluatePasswordRules(password = '') {
  const pw = typeof password === 'string' ? password : '';
  return PASSWORD_REQUIREMENTS.map((rule) => ({
    ...rule,
    met: rule.test(pw),
  }));
}

export function getPasswordValidationMessage(password = '') {
  const pw = typeof password === 'string' ? password : '';
  if (!pw.trim()) {
    return 'Password is required';
  }
  if (/\s/.test(pw)) {
    return 'Password must not contain spaces';
  }
  if (pw.length < 8) {
    return 'Password must be at least 8 characters';
  }
  if (pw.length > 20) {
    return 'Password must not exceed 20 characters';
  }
  if (!/[A-Z]/.test(pw)) {
    return 'Password must contain at least one uppercase letter';
  }
  if (!/[a-z]/.test(pw)) {
    return 'Password must contain at least one lowercase letter';
  }
  if (!/[0-9]/.test(pw)) {
    return 'Password must contain at least one number';
  }
  if (!/[@#$%!&*]/.test(pw)) {
    return 'Password must contain at least one special character (@, #, $, %, !, &, *)';
  }
  return '';
}

export function isPasswordValid(password = '') {
  return Boolean(password) && getPasswordValidationMessage(password) === '';
}

export function getPasswordStrength(password = '') {
  const pw = typeof password === 'string' ? password : '';
  if (!pw) {
    return { label: 'Weak', level: 0, color: '#cbd5e1' };
  }

  const classChecks = [
    /[A-Z]/.test(pw),
    /[a-z]/.test(pw),
    /[0-9]/.test(pw),
    /[@#$%!&*]/.test(pw),
  ].filter(Boolean).length;

  const hasValidLength = pw.length >= 8 && pw.length <= 20;
  const hasNoSpaces = !/\s/.test(pw);

  if (!hasNoSpaces || pw.length < 8 || pw.length > 20 || classChecks <= 2) {
    return { label: 'Weak', level: 1, color: '#dc2626' };
  }
  if (classChecks === 3 || !hasValidLength) {
    return { label: 'Medium', level: 2, color: '#d97706' };
  }

  const specialCount = (pw.match(/[@#$%!&*]/g) || []).length;
  const digitCount = (pw.match(/[0-9]/g) || []).length;
  if (pw.length >= 12 && (specialCount >= 2 || digitCount >= 2 || pw.length >= 14)) {
    return { label: 'Very Strong', level: 4, color: '#15803d' };
  }

  return { label: 'Strong', level: 3, color: '#0284c7' };
}

