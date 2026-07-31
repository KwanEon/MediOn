import 'package:flutter/services.dart';

const int _maxDomesticPhoneDigits = 11;

String _domesticDigits(String value) {
  final digits = value.replaceAll(RegExp(r'\D'), '');
  final normalized = digits.startsWith('82') && digits.length > 10
      ? '0${digits.substring(2)}'
      : digits;

  if (normalized.length <= _maxDomesticPhoneDigits) {
    return normalized;
  }
  return normalized.substring(0, _maxDomesticPhoneDigits);
}

String _joinPhoneGroups(String prefix, String remainder) {
  if (remainder.isEmpty) {
    return prefix;
  }
  if (remainder.length <= 4) {
    return '$prefix-$remainder';
  }

  final lastGroupStart = remainder.length - 4;
  return '$prefix-${remainder.substring(0, lastGroupStart)}'
      '-${remainder.substring(lastGroupStart)}';
}

String formatKoreanPhoneNumber(String value) {
  final digits = _domesticDigits(value);
  if (digits.isEmpty) {
    return '';
  }

  if (digits.startsWith('02')) {
    return _joinPhoneGroups(digits.substring(0, 2), digits.substring(2));
  }
  if (digits.startsWith('0505')) {
    return _joinPhoneGroups(digits.substring(0, 4), digits.substring(4));
  }
  if (digits.length <= 3) {
    return digits;
  }

  return _joinPhoneGroups(digits.substring(0, 3), digits.substring(3));
}

int _digitCountBeforeSelection(TextEditingValue value) {
  if (!value.selection.isValid) {
    return value.text.replaceAll(RegExp(r'\D'), '').length;
  }

  final selectionOffset = value.selection.baseOffset.clamp(
    0,
    value.text.length,
  );
  return value.text
      .substring(0, selectionOffset)
      .replaceAll(RegExp(r'\D'), '')
      .length;
}

int _selectionOffsetAfterDigits(String value, int digitCount) {
  if (digitCount <= 0) {
    return 0;
  }

  var seenDigits = 0;
  for (var index = 0; index < value.length; index += 1) {
    if (RegExp(r'\d').hasMatch(value[index])) {
      seenDigits += 1;
      if (seenDigits == digitCount) {
        return index + 1;
      }
    }
  }
  return value.length;
}

class KoreanPhoneNumberInputFormatter extends TextInputFormatter {
  const KoreanPhoneNumberInputFormatter();

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final formatted = formatKoreanPhoneNumber(newValue.text);
    final digitCount = _digitCountBeforeSelection(newValue);
    final selectionOffset = _selectionOffsetAfterDigits(formatted, digitCount);

    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: selectionOffset),
    );
  }
}
