import 'package:flutter_test/flutter_test.dart';
import 'package:medion_app/app.dart';

void main() {
  testWidgets('메디온 기본 화면을 표시한다', (tester) async {
    await tester.pumpWidget(const MediOnApp());

    expect(find.text('메디온'), findsOneWidget);
    expect(find.text('내 주변 의료기관을\n찾아보세요'), findsOneWidget);
    expect(find.text('홈'), findsOneWidget);
    expect(find.text('진료과'), findsOneWidget);
    expect(find.text('건강정보'), findsOneWidget);
    expect(find.text('공지'), findsOneWidget);
    expect(find.text('이용안내'), findsOneWidget);
    expect(find.text('문의하기'), findsOneWidget);
  });
}
