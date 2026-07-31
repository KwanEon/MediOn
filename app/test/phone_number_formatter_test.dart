import 'package:flutter_test/flutter_test.dart';
import 'package:medion_app/utils/phone_number_formatter.dart';

void main() {
  group('formatKoreanPhoneNumber', () {
    test('휴대전화 번호를 입력 길이에 맞춰 포맷한다', () {
      expect(formatKoreanPhoneNumber('010'), '010');
      expect(formatKoreanPhoneNumber('0101'), '010-1');
      expect(formatKoreanPhoneNumber('0101234'), '010-1234');
      expect(formatKoreanPhoneNumber('01012345'), '010-1-2345');
      expect(formatKoreanPhoneNumber('0101234567'), '010-123-4567');
      expect(formatKoreanPhoneNumber('01012345678'), '010-1234-5678');
    });

    test('기존 구분자와 문자를 제거해 다시 포맷한다', () {
      expect(formatKoreanPhoneNumber('010 1234-5678'), '010-1234-5678');
      expect(formatKoreanPhoneNumber('+82 10 1234 5678'), '010-1234-5678');
    });

    test('서울 및 0505 번호 형식을 지원한다', () {
      expect(formatKoreanPhoneNumber('021234567'), '02-123-4567');
      expect(formatKoreanPhoneNumber('0212345678'), '02-1234-5678');
      expect(formatKoreanPhoneNumber('05051234567'), '0505-123-4567');
    });
  });
}
