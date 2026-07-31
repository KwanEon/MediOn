import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class AppInfoPage extends StatelessWidget {
  const AppInfoPage({super.key});

  static const version = '1.0.0';
  static const buildNumber = '1';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('앱 정보'),
        centerTitle: true,
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
      ),
      body: const SafeArea(
        top: false,
        child: ScreenFrame(
          padding: EdgeInsets.fromLTRB(20, 24, 20, 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _AppSummary(),
              SizedBox(height: 24),
              SectionHeading(title: '버전 정보'),
              SizedBox(height: 12),
              _VersionCard(),
              SizedBox(height: 24),
              SectionHeading(title: '주요 기능'),
              SizedBox(height: 12),
              _FeatureCard(),
              SizedBox(height: 14),
              SoftNotice(
                icon: Icons.medical_information_outlined,
                text:
                    '의료기관의 운영 시간과 진료 가능 여부는 실제 상황과 다를 수 있으므로 방문 전에 해당 기관에 확인해 주세요.',
              ),
              SizedBox(height: 24),
              Center(
                child: Text(
                  '© 2026 MediOn. All rights reserved.',
                  style: TextStyle(color: AppColors.muted, fontSize: 12),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AppSummary extends StatelessWidget {
  const _AppSummary();

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(22),
      borderColor: const Color(0xFFBED6FF),
      backgroundColor: const Color(0xFFF7FAFF),
      child: Column(
        children: [
          const IconTile(
            icon: Icons.local_hospital_rounded,
            size: 68,
            iconSize: 36,
          ),
          const SizedBox(height: 15),
          Text('메디온', style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 7),
          Text(
            '내 주변 의료기관을 쉽고 빠르게 찾도록 돕는 의료정보 서비스',
            textAlign: TextAlign.center,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(height: 1.55),
          ),
          const SizedBox(height: 13),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(color: AppColors.line),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Text(
              '버전 ${AppInfoPage.version}',
              style: TextStyle(
                color: AppColors.primary,
                fontSize: 13,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _VersionCard extends StatelessWidget {
  const _VersionCard();

  @override
  Widget build(BuildContext context) {
    return const SurfaceCard(
      padding: EdgeInsets.zero,
      child: Column(
        children: [
          _InfoRow(label: '현재 버전', value: AppInfoPage.version),
          Divider(),
          _InfoRow(label: '빌드 번호', value: AppInfoPage.buildNumber),
          Divider(),
          _InfoRow(label: '최종 업데이트', value: '2026.07.31'),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 17, vertical: 15),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: Theme.of(context).textTheme.bodyMedium),
          ),
          Text(
            value,
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontSize: 14),
          ),
        ],
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard();

  @override
  Widget build(BuildContext context) {
    return const SurfaceCard(
      child: Column(
        children: [
          _FeatureRow(icon: Icons.near_me_outlined, title: '내 주변 의료기관 검색'),
          SizedBox(height: 15),
          _FeatureRow(
            icon: Icons.bookmark_border_rounded,
            title: '즐겨찾기와 회원 정보 관리',
          ),
          SizedBox(height: 15),
          _FeatureRow(
            icon: Icons.notifications_none_rounded,
            title: '건강 정보와 중요 공지 알림',
          ),
          SizedBox(height: 15),
          _FeatureRow(
            icon: Icons.chat_bubble_outline_rounded,
            title: '서비스 문의 등록과 관리',
          ),
        ],
      ),
    );
  }
}

class _FeatureRow extends StatelessWidget {
  const _FeatureRow({required this.icon, required this.title});

  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        IconTile(
          icon: icon,
          size: 42,
          iconSize: 22,
          color: AppColors.primary,
          backgroundColor: AppColors.primarySoft,
        ),
        const SizedBox(width: 13),
        Expanded(
          child: Text(
            title,
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontSize: 14),
          ),
        ),
      ],
    );
  }
}
