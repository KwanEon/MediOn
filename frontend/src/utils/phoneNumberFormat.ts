const NON_DIGIT_PATTERN = /\D/g;
const MAX_DOMESTIC_PHONE_DIGITS = 11;

function domesticDigits(value: string): string {
  const digits = value.replace(NON_DIGIT_PATTERN, '');
  const normalized = digits.startsWith('82') && digits.length > 10
    ? `0${digits.slice(2)}`
    : digits;

  return normalized.slice(0, MAX_DOMESTIC_PHONE_DIGITS);
}

function joinPhoneGroups(prefix: string, remainder: string): string {
  if (!remainder) {
    return prefix;
  }

  if (remainder.length <= 4) {
    return `${prefix}-${remainder}`;
  }

  return `${prefix}-${remainder.slice(0, -4)}-${remainder.slice(-4)}`;
}

export function formatKoreanPhoneNumber(value: string): string {
  const digits = domesticDigits(value);

  if (!digits) {
    return '';
  }

  if (digits.startsWith('02')) {
    return joinPhoneGroups(digits.slice(0, 2), digits.slice(2));
  }

  if (digits.startsWith('0505')) {
    return joinPhoneGroups(digits.slice(0, 4), digits.slice(4));
  }

  if (digits.length <= 3) {
    return digits;
  }

  return joinPhoneGroups(digits.slice(0, 3), digits.slice(3));
}
