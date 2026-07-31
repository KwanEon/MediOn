import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:medion_app/data/auth_models.dart';
import 'package:medion_app/pages/my_page.dart';
import 'package:medion_app/services/auth_api_client.dart';

void main() {
  testWidgets('마이페이지에서 개인정보 및 약관과 앱 정보를 확인한다', (tester) async {
    final authApi = AuthApiClient(
      baseUrl: 'http://localhost',
      client: MockClient((_) async => http.Response('{}', 500)),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: MyPage(
          user: const AuthUser(
            id: 1,
            username: 'tester',
            name: '테스터',
            email: 'tester@medion.com',
            phoneNumber: '010-1234-5678',
            address: '서울특별시 마포구',
            latitude: 37.5665,
            longitude: 126.978,
          ),
          authApi: authApi,
          onUserChanged: (_) {},
          favoriteInstitutions: const [],
          onFavoriteToggle: (_) async {},
          healthNoticeEnabled: false,
          onHealthNoticeChanged: (enabled) async => enabled,
          locationSearchEnabled: true,
          onLocationSearchChanged: (_) async {},
        ),
      ),
    );

    await tester.drag(
      find.byType(CustomScrollView),
      const Offset(0, -400),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('개인정보 및 약관'));
    await tester.pumpAndSettle();

    expect(find.text('개인정보처리방침'), findsWidgets);
    expect(find.text('이용약관'), findsOneWidget);
    expect(find.text('수집하는 개인정보'), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.tap(find.text('앱 정보'));
    await tester.pumpAndSettle();

    expect(find.text('현재 버전'), findsOneWidget);
    expect(find.text('1.0.0'), findsOneWidget);
    expect(find.text('주요 기능'), findsOneWidget);
  });
}
