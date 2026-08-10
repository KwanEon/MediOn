import 'dart:async';

import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';
import 'app_info_page.dart';
import 'member_info_page.dart';
import 'privacy_terms_page.dart';

class MyPage extends StatefulWidget {
  const MyPage({
    super.key,
    required this.user,
    required this.authApi,
    required this.onUserChanged,
    required this.healthNoticeEnabled,
    required this.onHealthNoticeChanged,
    required this.locationSearchEnabled,
    required this.onLocationSearchChanged,
  });

  final AuthUser user;
  final AuthApiClient authApi;
  final ValueChanged<AuthUser> onUserChanged;
  final bool healthNoticeEnabled;
  final Future<bool> Function(bool) onHealthNoticeChanged;
  final bool locationSearchEnabled;
  final Future<void> Function(bool) onLocationSearchChanged;

  @override
  State<MyPage> createState() => _MyPageState();
}

class _MyPageState extends State<MyPage> {
  late AuthUser _currentUser;
  late bool _healthNoticeEnabled;
  late bool _locationSearchEnabled;
  bool _healthSettingSaving = false;
  bool _locationSettingSaving = false;

  @override
  void initState() {
    super.initState();
    _currentUser = widget.user;
    _healthNoticeEnabled = widget.healthNoticeEnabled;
    _locationSearchEnabled = widget.locationSearchEnabled;
  }

  Future<void> _openMemberInfo() async {
    final user = await Navigator.of(context).push<AuthUser>(
      MaterialPageRoute(
        builder: (_) =>
            MemberInfoPage(user: _currentUser, authApi: widget.authApi),
      ),
    );
    if (!mounted || user == null) return;
    setState(() => _currentUser = user);
    widget.onUserChanged(user);
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('회원정보를 저장했습니다.')));
  }

  Future<void> _changeHealthNoticeSetting(bool enabled) async {
    setState(() => _healthSettingSaving = true);
    final actualValue = await widget.onHealthNoticeChanged(enabled);
    if (!mounted) return;
    setState(() {
      _healthNoticeEnabled = actualValue;
      _healthSettingSaving = false;
    });
    if (enabled && !actualValue) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('기기 알림 권한을 허용해야 건강 정보 알림을 받을 수 있어요.')),
      );
    }
  }

  Future<void> _changeLocationSearchSetting(bool enabled) async {
    setState(() => _locationSettingSaving = true);
    await widget.onLocationSearchChanged(enabled);
    if (!mounted) return;
    setState(() {
      _locationSearchEnabled = enabled;
      _locationSettingSaving = false;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          enabled
              ? '현재 위치를 사용하는 검색을 켰어요.'
              : '현재 위치 사용을 끄고 저장 주소를 검색 기준으로 사용해요.',
        ),
      ),
    );
  }

  Future<void> _confirmLogout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          icon: const Icon(
            Icons.logout_rounded,
            color: AppColors.primary,
            size: 30,
          ),
          title: const Text('로그아웃할까요?'),
          content: const Text('로그아웃해도 즐겨찾기와 저장 주소는 유지됩니다.'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('취소'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('로그아웃'),
            ),
          ],
        );
      },
    );

    if (!mounted || confirmed != true) return;
    Navigator.of(context).pop(true);
  }

  void _openPrivacyTerms() {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => const PrivacyTermsPage()));
  }

  void _openAppInfo() {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => const AppInfoPage()));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('마이페이지'),
        centerTitle: true,
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
      ),
      body: SafeArea(
        top: false,
        child: ScreenFrame(
          padding: const EdgeInsets.fromLTRB(20, 26, 20, 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _ProfileCard(user: _currentUser),
              const SizedBox(height: 28),
              const SectionHeading(title: '앱 설정'),
              const SizedBox(height: 12),
              SurfaceCard(
                padding: EdgeInsets.zero,
                child: Column(
                  children: [
                    _SwitchMenuRow(
                      icon: Icons.notifications_none_rounded,
                      title: '건강 정보 알림',
                      subtitle: '새로운 건강 가이드와 중요 공지를 받아요',
                      value: _healthNoticeEnabled,
                      onChanged: _healthSettingSaving
                          ? null
                          : (value) =>
                                unawaited(_changeHealthNoticeSetting(value)),
                    ),
                    const Divider(),
                    _SwitchMenuRow(
                      icon: Icons.my_location_rounded,
                      title: '위치 기반 검색',
                      subtitle: '현재 위치를 기준으로 가까운 기관을 찾아요',
                      value: _locationSearchEnabled,
                      onChanged: _locationSettingSaving
                          ? null
                          : (value) =>
                                unawaited(_changeLocationSearchSetting(value)),
                    ),
                    const Divider(),
                    _MenuRow(
                      icon: Icons.manage_accounts_outlined,
                      title: '회원 정보 관리',
                      onTap: _openMemberInfo,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              const SectionHeading(title: '서비스 정보'),
              const SizedBox(height: 12),
              SurfaceCard(
                padding: EdgeInsets.zero,
                child: Column(
                  children: [
                    _MenuRow(
                      icon: Icons.shield_outlined,
                      title: '개인정보 및 약관',
                      onTap: _openPrivacyTerms,
                    ),
                    const Divider(),
                    _MenuRow(
                      icon: Icons.info_outline_rounded,
                      title: '앱 정보',
                      trailing: 'v${AppInfoPage.version}',
                      onTap: _openAppInfo,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: OutlinedButton.icon(
                  onPressed: _confirmLogout,
                  icon: const Icon(Icons.logout_rounded, size: 19),
                  label: const Text('로그아웃'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.red,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ProfileCard extends StatelessWidget {
  const _ProfileCard({required this.user});

  final AuthUser user;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(20),
      borderColor: const Color(0xFFD4E4FB),
      backgroundColor: AppColors.primarySoft,
      child: Row(
        children: [
          Container(
            width: 66,
            height: 66,
            decoration: BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              border: Border.all(color: const Color(0xFFC8DCF9)),
            ),
            child: const Icon(
              Icons.person_rounded,
              color: AppColors.primary,
              size: 34,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                      child: Text(
                        user.name,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                    ),
                    const SizedBox(width: 7),
                    const TinyTag(label: '일반 회원'),
                  ],
                ),
                const SizedBox(height: 5),
                Text(user.email, style: Theme.of(context).textTheme.bodyMedium),
                const SizedBox(height: 2),
                Text(
                  user.phoneNumber,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _MenuRow extends StatelessWidget {
  const _MenuRow({
    required this.icon,
    required this.title,
    required this.onTap,
    this.trailing,
  });

  final IconData icon;
  final String title;
  final VoidCallback onTap;
  final String? trailing;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
        child: Row(
          children: [
            Icon(icon, color: AppColors.muted, size: 22),
            const SizedBox(width: 13),
            Expanded(
              child: Text(
                title,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontSize: 15),
              ),
            ),
            if (trailing != null) ...[
              Text(trailing!, style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(width: 5),
            ],
            const Icon(
              Icons.chevron_right_rounded,
              color: Color(0xFF9AA5B5),
              size: 22,
            ),
          ],
        ),
      ),
    );
  }
}

class _SwitchMenuRow extends StatelessWidget {
  const _SwitchMenuRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool>? onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 13, 10, 13),
      child: Row(
        children: [
          Icon(icon, color: AppColors.muted, size: 22),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: Theme.of(
                    context,
                  ).textTheme.titleMedium?.copyWith(fontSize: 15),
                ),
                const SizedBox(height: 2),
                Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
          Switch(
            value: value,
            onChanged: onChanged,
            thumbColor: WidgetStateProperty.resolveWith((states) {
              return states.contains(WidgetState.selected)
                  ? Colors.white
                  : const Color(0xFF667085);
            }),
            trackColor: WidgetStateProperty.resolveWith((states) {
              return states.contains(WidgetState.selected)
                  ? AppColors.primary
                  : const Color(0xFFD0D5DD);
            }),
            trackOutlineColor: WidgetStateProperty.resolveWith((states) {
              return states.contains(WidgetState.selected)
                  ? AppColors.primary
                  : const Color(0xFF98A2B3);
            }),
          ),
        ],
      ),
    );
  }
}
