import 'dart:async';

import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../data/institution_models.dart';
import '../pages/departments_page.dart';
import '../pages/guide_page.dart';
import '../pages/health_page.dart';
import '../pages/home_page.dart';
import '../pages/institution_detail_page.dart';
import '../pages/inquiry_page.dart';
import '../pages/login_page.dart';
import '../pages/my_page.dart';
import '../pages/notices_feed_page.dart';
import '../services/app_preferences.dart';
import '../services/auth_api_client.dart';
import '../services/health_notification_service.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  final AuthApiClient _authApi = AuthApiClient();
  final AppPreferences _preferences = AppPreferences();
  final HealthNotificationService _healthNotifications =
      HealthNotificationService();
  int _currentIndex = 0;
  AuthUser? _currentUser;
  int _homeRevision = 0;
  String _requestedHomeCategory = '전체';
  String _savedAddress = '서울특별시 마포구 양화로 123';
  bool _healthNoticeEnabled = false;
  bool _locationSearchEnabled = true;
  Set<int> _favoriteIds = {};

  bool get _isLoggedIn => _currentUser != null;

  @override
  void initState() {
    super.initState();
    unawaited(_initializeAppState());
  }

  @override
  void dispose() {
    _authApi.close();
    super.dispose();
  }

  Future<void> _initializeAppState() async {
    try {
      await _healthNotifications.initialize();
    } catch (_) {
      // 알림 초기화 실패는 다른 앱 기능의 시작을 막지 않습니다.
    }

    try {
      final settings = await Future.wait<bool>([
        _preferences.healthNoticeEnabled(),
        _preferences.locationSearchEnabled(),
      ]);
      if (mounted) {
        setState(() {
          _healthNoticeEnabled = settings[0];
          _locationSearchEnabled = settings[1];
        });
      }
    } catch (_) {
      // 저장된 설정을 읽지 못하면 안전한 기본값을 사용합니다.
    }

    await _restoreSession();
    if (_healthNoticeEnabled) {
      await _checkForHealthNotices();
    }
  }

  Future<void> _checkForHealthNotices({bool announceEnabled = false}) async {
    try {
      final notices = await _authApi.getNotices();
      final healthNotices =
          notices
              .where(
                (notice) =>
                    notice.category == 'GUIDE' ||
                    notice.category == 'IMPORTANT',
              )
              .toList()
            ..sort((left, right) => right.updatedAt.compareTo(left.updatedAt));
      final latest = healthNotices.isEmpty ? null : healthNotices.first;
      if (latest == null) {
        if (announceEnabled) {
          await _healthNotifications.show(
            title: '건강 정보 알림이 켜졌어요',
            body: '새 건강 가이드와 중요 공지가 등록되면 알려드릴게요.',
          );
        }
        return;
      }

      final lastNoticeId = await _preferences.lastHealthNoticeId();
      if (announceEnabled) {
        await _healthNotifications.show(
          title: '건강 정보 알림이 켜졌어요',
          body: latest.title,
        );
        await _preferences.setLastHealthNoticeId(latest.id);
        return;
      }

      if (lastNoticeId == null) {
        await _preferences.setLastHealthNoticeId(latest.id);
      } else if (latest.id > lastNoticeId) {
        await _healthNotifications.show(
          title: '새로운 건강 정보가 도착했어요',
          body: latest.title,
        );
        await _preferences.setLastHealthNoticeId(latest.id);
      }
    } catch (_) {
      if (announceEnabled) {
        await _healthNotifications.show(
          title: '건강 정보 알림이 켜졌어요',
          body: '새 건강 가이드와 중요 공지가 등록되면 알려드릴게요.',
        );
      }
    }
  }

  Future<bool> _setHealthNoticeEnabled(bool enabled) async {
    if (enabled) {
      try {
        final permitted = await _healthNotifications.requestPermission();
        if (!permitted) return false;
      } catch (_) {
        return false;
      }
    }

    if (mounted) {
      setState(() => _healthNoticeEnabled = enabled);
    }
    try {
      await _preferences.setHealthNoticeEnabled(enabled);
    } catch (_) {
      // 현재 실행 중인 앱에는 변경된 설정을 유지합니다.
    }

    if (enabled) {
      await _checkForHealthNotices(announceEnabled: true);
    } else {
      await _healthNotifications.cancel();
    }
    return enabled;
  }

  Future<void> _setLocationSearchEnabled(bool enabled) async {
    if (mounted) {
      setState(() => _locationSearchEnabled = enabled);
    }
    try {
      await _preferences.setLocationSearchEnabled(enabled);
    } catch (_) {
      // 현재 실행 중인 앱에는 변경된 설정을 유지합니다.
    }
  }

  Future<void> _restoreSession() async {
    try {
      final user = await _authApi.currentUser();
      if (!mounted || user == null) return;
      setState(() {
        _currentUser = user;
        _savedAddress = user.address;
      });
      await _loadFavorites();
    } on AuthApiException {
      // 로그인된 세션이 없거나 백엔드에 연결할 수 없으면 비로그인 상태를 유지합니다.
    }
  }

  Future<void> _loadFavorites() async {
    if (!_isLoggedIn) return;
    try {
      final favoriteIds = await _authApi.getFavoriteInstitutionIds();
      if (!mounted) return;
      setState(() => _favoriteIds = favoriteIds);
    } on AuthApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('즐겨찾기를 불러오지 못했습니다: ${error.message}')),
      );
    }
  }

  Future<void> _toggleFavorite(Institution institution) async {
    if (!_isLoggedIn) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('로그인 후 즐겨찾기를 사용할 수 있습니다.')));
      return;
    }

    final wasFavorite = _favoriteIds.contains(institution.id);
    setState(() {
      if (wasFavorite) {
        _favoriteIds.remove(institution.id);
      } else {
        _favoriteIds.add(institution.id);
      }
    });

    try {
      if (wasFavorite) {
        await _authApi.removeFavoriteInstitution(institution.id);
      } else {
        await _authApi.addFavoriteInstitution(institution.id);
      }
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() {
        if (wasFavorite) {
          _favoriteIds.add(institution.id);
        } else {
          _favoriteIds.remove(institution.id);
        }
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  void _openHome({String category = '전체'}) {
    setState(() {
      _requestedHomeCategory = category;
      _homeRevision += 1;
      _currentIndex = 0;
    });
  }

  Future<void> _openInstitution(Institution institution) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => InstitutionDetailPage(
          institution: institution,
          initiallyFavorite: _favoriteIds.contains(institution.id),
          onFavoriteChanged: (_) => _toggleFavorite(institution),
        ),
      ),
    );
  }

  Future<void> _openAccount() async {
    if (!_isLoggedIn) {
      final user = await Navigator.of(context).push<AuthUser>(
        MaterialPageRoute(builder: (_) => LoginPage(authApi: _authApi)),
      );
      if (!mounted || user == null) return;
      setState(() {
        _currentUser = user;
        _savedAddress = user.address;
      });
      await _loadFavorites();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('메디온에 로그인했습니다.')));
      return;
    }

    final signedOut = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => MyPage(
          user: _currentUser!,
          authApi: _authApi,
          onUserChanged: (user) {
            setState(() {
              _currentUser = user;
              _savedAddress = user.address;
            });
          },
          healthNoticeEnabled: _healthNoticeEnabled,
          onHealthNoticeChanged: _setHealthNoticeEnabled,
          locationSearchEnabled: _locationSearchEnabled,
          onLocationSearchChanged: _setLocationSearchEnabled,
        ),
      ),
    );
    if (!mounted || signedOut != true) return;
    String message = '로그아웃했습니다.';
    try {
      await _authApi.logout();
    } on AuthApiException catch (error) {
      message = '앱에서는 로그아웃했지만 서버 응답을 확인하지 못했습니다: ${error.message}';
    }
    if (!mounted) return;
    setState(() {
      _currentUser = null;
      _favoriteIds = {};
    });
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      HomePage(
        key: ValueKey(_homeRevision),
        authApi: _authApi,
        user: _currentUser,
        initialCategory: _requestedHomeCategory,
        favoriteIds: _favoriteIds,
        onFavoriteToggle: _toggleFavorite,
        onOpenInstitution: _openInstitution,
        savedAddress: _savedAddress,
        locationSearchEnabled: _locationSearchEnabled,
      ),
      const DepartmentsPage(),
      HealthPage(
        onOpenDepartments: () => setState(() => _currentIndex = 1),
        onOpenEmergency: () => _openHome(category: '응급실'),
      ),
      NoticesFeedPage(api: _authApi),
      GuidePage(onFindInstitution: () => _openHome()),
      InquiryPage(
        api: _authApi,
        user: _currentUser,
        onLoginRequested: () => unawaited(_openAccount()),
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 70,
        titleSpacing: 20,
        title: const MediOnBrand(),
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: IconButton(
              tooltip: _isLoggedIn ? '마이페이지' : '로그인',
              onPressed: _openAccount,
              style: IconButton.styleFrom(
                minimumSize: const Size(42, 42),
                backgroundColor: _isLoggedIn
                    ? AppColors.primarySoft
                    : Colors.white,
                side: BorderSide(
                  color: _isLoggedIn ? AppColors.primary : AppColors.line,
                ),
              ),
              icon: Icon(
                _isLoggedIn
                    ? Icons.person_rounded
                    : Icons.person_outline_rounded,
                size: 22,
                color: AppColors.primary,
              ),
            ),
          ),
        ],
      ),
      body: IndexedStack(index: _currentIndex, children: pages),
      bottomNavigationBar: DecoratedBox(
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: AppColors.line)),
        ),
        child: SafeArea(
          top: false,
          child: BottomNavigationBar(
            currentIndex: _currentIndex,
            onTap: (index) => setState(() => _currentIndex = index),
            type: BottomNavigationBarType.fixed,
            elevation: 0,
            backgroundColor: Colors.white,
            selectedItemColor: AppColors.primary,
            unselectedItemColor: const Color(0xFF526174),
            selectedFontSize: 11,
            unselectedFontSize: 11,
            selectedLabelStyle: const TextStyle(
              fontWeight: FontWeight.w700,
              height: 1.8,
            ),
            unselectedLabelStyle: const TextStyle(
              fontWeight: FontWeight.w500,
              height: 1.8,
            ),
            items: const [
              BottomNavigationBarItem(
                icon: Icon(Icons.home_outlined),
                activeIcon: Icon(Icons.home_rounded),
                label: '홈',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.medical_services_outlined),
                activeIcon: Icon(Icons.medical_services_rounded),
                label: '진료과',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.health_and_safety_outlined),
                activeIcon: Icon(Icons.health_and_safety_rounded),
                label: '건강정보',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.notifications_none_rounded),
                activeIcon: Icon(Icons.notifications_rounded),
                label: '공지',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.info_outline_rounded),
                activeIcon: Icon(Icons.info_rounded),
                label: '이용안내',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.chat_bubble_outline_rounded),
                activeIcon: Icon(Icons.chat_bubble_rounded),
                label: '문의하기',
              ),
            ],
          ),
        ),
      ),
    );
  }
}
