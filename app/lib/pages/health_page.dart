import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:url_launcher/url_launcher.dart';

import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class HealthPage extends StatelessWidget {
  const HealthPage({
    super.key,
    required this.onOpenDepartments,
    required this.onOpenEmergency,
  });

  final VoidCallback onOpenDepartments;
  final VoidCallback onOpenEmergency;

  static const _guides = [
    _HealthGuideData(
      icon: LucideIcons.stethoscope,
      category: '진료과 찾기',
      title: '증상에 맞는 진료과 고르기',
      description: '어느 진료과를 선택해야 할지 막막할 때 참고할 수 있는 기본 안내입니다.',
      items: [
        '기침·콧물·인후통은 내과 또는 이비인후과',
        '뼈·관절·근육의 통증은 정형외과',
        '피부 발진이나 가려움은 피부과',
        '영유아와 청소년의 증상은 소아청소년과',
      ],
      color: AppColors.primary,
      backgroundColor: AppColors.primarySoft,
      action: _HealthGuideAction.departments,
      actionLabel: '전체 보기',
    ),
    _HealthGuideData(
      icon: LucideIcons.building_2,
      category: '의료기관 구분',
      title: '의원·병원·종합병원의 차이',
      description: '가벼운 증상부터 입원이 필요한 경우까지 의료기관의 역할을 구분해 보세요.',
      items: [
        '의원은 외래 진료를 중심으로 운영됩니다.',
        '병원은 30개 이상의 병상을 갖춘 입원 진료 기관입니다.',
        '종합병원은 100개 이상의 병상과 여러 진료과를 갖춥니다.',
      ],
      color: AppColors.green,
      backgroundColor: AppColors.greenSoft,
      action: _HealthGuideAction.external,
      actionLabel: '건강보험심사평가원 제도 안내',
      actionUrl:
          'https://www.hira.or.kr/dummy.do?WT.ac=&cmsurl=%2Fcms%2Fpolicy%2F02%2F01%2F1341852_27024.html&pgmid=HIRAA020006000000',
    ),
    _HealthGuideData(
      icon: LucideIcons.calendar_clock,
      category: '야간·휴일 진료',
      title: '운영시간을 한 번 더 확인하세요',
      description: '공휴일과 임시 휴진 등으로 실제 운영시간이 달라질 수 있습니다.',
      items: [
        '검색 결과에서 오늘의 운영시간을 확인하세요.',
        '접수 마감은 진료 종료보다 빠를 수 있습니다.',
        '출발 전 의료기관에 전화하면 헛걸음을 줄일 수 있습니다.',
      ],
      color: AppColors.amber,
      backgroundColor: AppColors.amberSoft,
    ),
    _HealthGuideData(
      icon: LucideIcons.clipboard_check,
      category: '방문 준비',
      title: '진료 전에 준비하면 좋은 것',
      description: '짧은 진료 시간에도 필요한 내용을 빠짐없이 전달할 수 있도록 준비해 보세요.',
      items: [
        '신분증과 복용 중인 약의 이름',
        '약물·음식 알레르기 여부',
        '증상이 시작된 시점과 달라진 과정',
        '이전에 받은 검사 결과나 처방전',
      ],
      color: AppColors.violet,
      backgroundColor: AppColors.violetSoft,
    ),
    _HealthGuideData(
      icon: LucideIcons.pill,
      category: '약국 이용',
      title: '약국 방문 전 확인 사항',
      description: '조제 가능 여부와 운영시간은 약국마다 다를 수 있습니다.',
      items: [
        '처방전의 사용기간을 먼저 확인하세요.',
        '복용 중인 약과 알레르기를 약사에게 알려주세요.',
        '야간·휴일에는 방문 전에 운영 여부를 확인하세요.',
      ],
      color: AppColors.mint,
      backgroundColor: AppColors.mintSoft,
    ),
    _HealthGuideData(
      icon: LucideIcons.hospital,
      category: '응급의료',
      title: '응급실이 필요한지 판단하기 어렵다면',
      description: '증상을 임의로 판단해 이동하기보다 전문적인 안내를 먼저 받는 것이 안전합니다.',
      items: [
        '의식 저하, 호흡 곤란, 심한 가슴 통증 등 위급한 상황에는 119에 도움을 요청하세요.',
        '가까운 응급실과 병·의원은 중앙응급의료센터 안내에서도 확인할 수 있습니다.',
      ],
      color: AppColors.red,
      backgroundColor: AppColors.redSoft,
      action: _HealthGuideAction.external,
      actionLabel: '중앙응급의료센터 응급의료 안내',
      actionUrl:
          'https://www.e-gen.or.kr/egen/notice_view.do?brdclscd=02&brdctsno=12899&currentPageNum=7024&searchDatayear=&searchKeyword=&searchTarget=ALL&upperfixyn=Y',
    ),
  ];

  Future<void> _openExternalLink(BuildContext context, String url) async {
    var opened = false;
    try {
      opened = await launchUrl(
        Uri.parse(url),
        mode: LaunchMode.externalApplication,
      );
    } catch (_) {
      opened = false;
    }
    if (!opened && context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('안내 페이지를 열 수 없습니다.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return ScreenFrame(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _HealthHero(),
          const SizedBox(height: 20),
          _EmergencyCallout(onOpenEmergency: onOpenEmergency),
          const SizedBox(height: 30),
          Text(
            '메디온 건강 가이드',
            style: Theme.of(
              context,
            ).textTheme.labelLarge?.copyWith(color: AppColors.primary),
          ),
          const SizedBox(height: 5),
          const SectionHeading(title: '상황별로 확인해 보세요'),
          const SizedBox(height: 7),
          Text(
            '최근 검토 2026.07.28',
            style: Theme.of(
              context,
            ).textTheme.bodySmall?.copyWith(color: AppColors.muted),
          ),
          const SizedBox(height: 16),
          for (var index = 0; index < _guides.length; index++) ...[
            _HealthGuideCard(
              data: _guides[index],
              onOpenDepartments: onOpenDepartments,
              onOpenExternal: (url) => _openExternalLink(context, url),
            ),
            if (index < _guides.length - 1) const SizedBox(height: 14),
          ],
          const SizedBox(height: 24),
          const _InformationFootnote(),
        ],
      ),
    );
  }
}

class _HealthHero extends StatelessWidget {
  const _HealthHero();

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(20),
      backgroundColor: const Color(0xFFF8FBFF),
      borderColor: const Color(0xFFD8E7FC),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '건강 정보',
                  style: Theme.of(
                    context,
                  ).textTheme.labelLarge?.copyWith(color: AppColors.primary),
                ),
                const SizedBox(height: 8),
                Text(
                  '병원을 찾기 전,\n알아두면 좋은 정보',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 11),
                Text(
                  '의료기관을 더 알맞게 선택하고 안전하게 방문할 수 있도록 꼭 필요한 내용만 정리했습니다.',
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(color: AppColors.muted),
                ),
              ],
            ),
          ),
          const SizedBox(width: 14),
          const IconTile(icon: LucideIcons.heart_pulse, size: 62, iconSize: 31),
        ],
      ),
    );
  }
}

class _EmergencyCallout extends StatelessWidget {
  const _EmergencyCallout({required this.onOpenEmergency});

  final VoidCallback onOpenEmergency;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(18),
      backgroundColor: AppColors.redSoft,
      borderColor: const Color(0xFFF4C5CA),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const IconTile(
                icon: LucideIcons.heart_pulse,
                color: AppColors.red,
                backgroundColor: Colors.white,
                size: 48,
                iconSize: 24,
              ),
              const SizedBox(width: 13),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '위급한 상황인가요?',
                      style: Theme.of(
                        context,
                      ).textTheme.labelLarge?.copyWith(color: AppColors.red),
                    ),
                    const SizedBox(height: 5),
                    Text(
                      '의식 저하, 호흡 곤란 등 긴급한 증상은 즉시 119에 연락하세요.',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 7),
                    Text(
                      '이 페이지의 내용은 일반적인 정보이며 의료진의 진단을 대신하지 않습니다.',
                      style: Theme.of(
                        context,
                      ).textTheme.bodySmall?.copyWith(color: AppColors.muted),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 15),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: onOpenEmergency,
              style: FilledButton.styleFrom(backgroundColor: AppColors.red),
              icon: const Icon(LucideIcons.ambulance, size: 18),
              label: const Text('주변 응급실 찾기'),
            ),
          ),
        ],
      ),
    );
  }
}

class _HealthGuideCard extends StatelessWidget {
  const _HealthGuideCard({
    required this.data,
    required this.onOpenDepartments,
    required this.onOpenExternal,
  });

  final _HealthGuideData data;
  final VoidCallback onOpenDepartments;
  final ValueChanged<String> onOpenExternal;

  @override
  Widget build(BuildContext context) {
    final action = data.action;

    return SurfaceCard(
      padding: const EdgeInsets.all(19),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              IconTile(
                icon: data.icon,
                color: data.color,
                backgroundColor: data.backgroundColor,
                size: 48,
                iconSize: 24,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  data.category,
                  style: Theme.of(
                    context,
                  ).textTheme.labelLarge?.copyWith(color: data.color),
                ),
              ),
            ],
          ),
          const SizedBox(height: 15),
          Text(data.title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          Text(
            data.description,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: AppColors.muted),
          ),
          const SizedBox(height: 14),
          for (var index = 0; index < data.items.length; index++) ...[
            _GuideListItem(text: data.items[index], color: data.color),
            if (index < data.items.length - 1) const SizedBox(height: 9),
          ],
          if (action != null && data.actionLabel != null) ...[
            const SizedBox(height: 14),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: () {
                  if (action == _HealthGuideAction.departments) {
                    onOpenDepartments();
                    return;
                  }
                  final url = data.actionUrl;
                  if (url != null) onOpenExternal(url);
                },
                iconAlignment: IconAlignment.end,
                icon: Icon(
                  action == _HealthGuideAction.external
                      ? LucideIcons.external_link
                      : LucideIcons.arrow_right,
                  size: 16,
                ),
                label: Text(data.actionLabel!),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _GuideListItem extends StatelessWidget {
  const _GuideListItem({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 7),
          child: Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
        ),
        const SizedBox(width: 9),
        Expanded(
          child: Text(
            text,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(height: 1.4),
          ),
        ),
      ],
    );
  }
}

class _InformationFootnote extends StatelessWidget {
  const _InformationFootnote();

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(17),
      backgroundColor: const Color(0xFFF8FAFD),
      borderColor: const Color(0xFFCED9E8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.info_outline_rounded,
            color: AppColors.muted,
            size: 22,
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '정보 이용 안내',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 6),
                Text(
                  '의료기관의 진료과목과 운영시간은 현장 사정에 따라 달라질 수 있습니다. '
                  '방문 전 상세정보와 전화 문의를 통해 다시 확인해 주세요.',
                  style: Theme.of(
                    context,
                  ).textTheme.bodySmall?.copyWith(color: AppColors.muted),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

enum _HealthGuideAction { departments, external }

class _HealthGuideData {
  const _HealthGuideData({
    required this.icon,
    required this.category,
    required this.title,
    required this.description,
    required this.items,
    required this.color,
    required this.backgroundColor,
    this.action,
    this.actionLabel,
    this.actionUrl,
  });

  final IconData icon;
  final String category;
  final String title;
  final String description;
  final List<String> items;
  final Color color;
  final Color backgroundColor;
  final _HealthGuideAction? action;
  final String? actionLabel;
  final String? actionUrl;
}
