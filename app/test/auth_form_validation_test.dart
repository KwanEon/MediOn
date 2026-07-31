import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:medion_app/pages/login_page.dart';
import 'package:medion_app/pages/register_page.dart';
import 'package:medion_app/services/auth_api_client.dart';
import 'package:medion_app/widgets/common_widgets.dart';

void main() {
  late AuthApiClient authApi;

  setUp(() {
    authApi = AuthApiClient(
      baseUrl: 'http://localhost',
      client: MockClient((_) async => http.Response('{}', 500)),
    );
  });

  testWidgets('로그인은 제출 후 실패한 항목만 입력 중 다시 검사한다', (tester) async {
    await tester.pumpWidget(MaterialApp(home: LoginPage(authApi: authApi)));

    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(1), 'password');
    await tester.pump();

    expect(find.text('아이디를 입력해 주세요.'), findsNothing);

    await tester.ensureVisible(find.byType(PrimaryButton));
    await tester.tap(find.byType(PrimaryButton));
    await tester.pump();

    expect(find.text('아이디를 입력해 주세요.'), findsOneWidget);
    expect(find.text('비밀번호를 입력해 주세요.'), findsNothing);

    await tester.enterText(fields.at(0), 'admin');
    await tester.pump();

    expect(find.text('아이디를 입력해 주세요.'), findsNothing);
  });

  testWidgets('회원가입은 제출 전에는 검사하지 않고 수정 즉시 오류를 지운다', (tester) async {
    await tester.pumpWidget(MaterialApp(home: RegisterPage(authApi: authApi)));

    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), 'ab');
    await tester.pump();

    expect(find.text('아이디는 4자 이상 30자 이하여야 합니다.'), findsNothing);

    await tester.ensureVisible(find.byType(PrimaryButton));
    await tester.tap(find.byType(PrimaryButton));
    await tester.pump();

    expect(find.text('아이디는 4자 이상 30자 이하여야 합니다.'), findsOneWidget);
    expect(find.text('비밀번호를 입력해 주세요.'), findsOneWidget);
    expect(find.text('필수 약관에 동의해 주세요.'), findsOneWidget);

    await tester.enterText(fields.at(0), 'valid_user');
    await tester.pump();

    expect(find.text('아이디는 4자 이상 30자 이하여야 합니다.'), findsNothing);
    expect(find.text('비밀번호를 입력해 주세요.'), findsOneWidget);

    await tester.ensureVisible(find.byType(Checkbox));
    await tester.tap(find.byType(Checkbox));
    await tester.pump();

    expect(find.text('필수 약관에 동의해 주세요.'), findsNothing);
  });
}
