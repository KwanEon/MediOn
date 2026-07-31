import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';

import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class GuidePage extends StatelessWidget {
  const GuidePage({super.key, required this.onFindInstitution});

  final VoidCallback onFindInstitution;

  @override
  Widget build(BuildContext context) {
    return ScreenFrame(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _GuideHero(onTap: onFindInstitution),
          const SizedBox(height: 32),
          const _SectionLabel('빠른 시작'),
          const SizedBox(height: 5),
          const SectionHeading(title: '네 단계로 찾아보세요'),
          const SizedBox(height: 16),
          const _GuideTimeline(),
          const SizedBox(height: 32),
          const _SectionLabel('기능별 안내'),
          const SizedBox(height: 5),
          const SectionHeading(title: '표시와 기능을 알아보세요'),
          const SizedBox(height: 16),
          ..._features.map(
            (feature) => Padding(
              padding: const EdgeInsets.only(bottom: 11),
              child: _FeatureCard(feature: feature),
            ),
          ),
          const SizedBox(height: 9),
          const _VisitNotice(),
          const SizedBox(height: 32),
          const _SectionLabel('자주 묻는 질문'),
          const SizedBox(height: 5),
          const SectionHeading(title: '도움이 더 필요하신가요?'),
          const SizedBox(height: 16),
          ..._faqs.map(
            (faq) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _FaqCard(faq: faq),
            ),
          ),
        ],
      ),
    );
  }
}

class _GuideHero extends StatelessWidget {
  const _GuideHero({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(20),
      borderColor: const Color(0xFFBED6FF),
      backgroundColor: const Color(0xFFF7FAFF),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const _SectionLabel('서비스 안내'),
                    const SizedBox(height: 6),
                    Text(
                      '메디온 이용 안내',
                      style: Theme.of(context).textTheme.headlineMedium,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 14),
              const IconTile(
                icon: LucideIcons.circle_question_mark,
                color: AppColors.green,
                backgroundColor: AppColors.greenSoft,
                size: 58,
                iconSize: 31,
              ),
            ],
          ),
          const SizedBox(height: 13),
          Text(
            '내 주변 의료기관을 찾는 순간부터 방문 전 확인까지, 필요한 기능을 순서대로 안내합니다.',
            style: Theme.of(
              context,
            ).textTheme.bodyLarge?.copyWith(height: 1.55),
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: onTap,
            style: FilledButton.styleFrom(
              minimumSize: const Size(0, 46),
              padding: const EdgeInsets.symmetric(horizontal: 18),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('의료기관 찾아보기'),
                SizedBox(width: 10),
                Icon(LucideIcons.arrow_right, size: 19),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: Theme.of(context).textTheme.labelLarge?.copyWith(
        color: AppColors.primary,
        fontWeight: FontWeight.w800,
      ),
    );
  }
}

class _GuideTimeline extends StatelessWidget {
  const _GuideTimeline();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(
        _steps.length,
        (index) => _TimelineStep(
          step: _steps[index],
          showConnector: index != _steps.length - 1,
        ),
      ),
    );
  }
}

class _TimelineStep extends StatelessWidget {
  const _TimelineStep({required this.step, required this.showConnector});

  final _GuideStep step;
  final bool showConnector;

  @override
  Widget build(BuildContext context) {
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            width: 48,
            child: Column(
              children: [
                Container(
                  width: 42,
                  height: 42,
                  alignment: Alignment.center,
                  decoration: const BoxDecoration(
                    color: AppColors.primary,
                    shape: BoxShape.circle,
                  ),
                  child: Text(
                    step.number,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: Colors.white,
                      fontSize: 13,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                if (showConnector)
                  Expanded(
                    child: Container(width: 2, color: const Color(0xFF8CB7F9)),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Padding(
              padding: EdgeInsets.only(bottom: showConnector ? 11 : 0),
              child: SurfaceCard(
                padding: const EdgeInsets.all(15),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    IconTile(icon: step.icon, size: 50, iconSize: 27),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            step.title,
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          const SizedBox(height: 5),
                          Text(
                            step.description,
                            style: Theme.of(
                              context,
                            ).textTheme.bodyMedium?.copyWith(height: 1.5),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  const _FeatureCard({required this.feature});

  final _GuideFeature feature;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          IconTile(
            icon: feature.icon,
            color: feature.color,
            backgroundColor: feature.backgroundColor,
            size: 50,
            iconSize: 27,
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  feature.title,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 5),
                Text(
                  feature.description,
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(height: 1.5),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _VisitNotice extends StatelessWidget {
  const _VisitNotice();

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(18),
      borderColor: const Color(0xFFBCE4D4),
      backgroundColor: AppColors.greenSoft,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            LucideIcons.shield_check,
            color: AppColors.green,
            size: 27,
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '방문 전 마지막으로 확인해 주세요',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: AppColors.green,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  '운영시간과 응급실 병상 정보는 실제 현장과 차이가 있을 수 있습니다. 상세정보에서 전화번호를 확인해 방문 전에 문의하는 것이 가장 정확합니다.',
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(height: 1.55),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _FaqCard extends StatelessWidget {
  const _FaqCard({required this.faq});

  final _GuideFaq faq;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: EdgeInsets.zero,
      child: Theme(
        data: Theme.of(context).copyWith(
          dividerColor: Colors.transparent,
          splashColor: AppColors.primarySoft,
          highlightColor: AppColors.primarySoft,
        ),
        child: ExpansionTile(
          tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          childrenPadding: const EdgeInsets.fromLTRB(56, 0, 18, 17),
          leading: const Icon(
            LucideIcons.info,
            color: AppColors.primary,
            size: 23,
          ),
          title: Text(
            faq.question,
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
          trailing: const Icon(
            LucideIcons.chevron_down,
            color: AppColors.primary,
            size: 21,
          ),
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                faq.answer,
                style: Theme.of(
                  context,
                ).textTheme.bodyMedium?.copyWith(height: 1.55),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _GuideStep {
  const _GuideStep({
    required this.number,
    required this.icon,
    required this.title,
    required this.description,
  });

  final String number;
  final IconData icon;
  final String title;
  final String description;
}

class _GuideFeature {
  const _GuideFeature({
    required this.icon,
    required this.title,
    required this.description,
    required this.color,
    required this.backgroundColor,
  });

  final IconData icon;
  final String title;
  final String description;
  final Color color;
  final Color backgroundColor;
}

class _GuideFaq {
  const _GuideFaq({required this.question, required this.answer});

  final String question;
  final String answer;
}

const _steps = [
  _GuideStep(
    number: '01',
    icon: LucideIcons.map_pinned,
    title: '검색 위치 정하기',
    description: '현재 위치를 허용하거나 원하는 주소를 직접 검색하세요.',
  ),
  _GuideStep(
    number: '02',
    icon: LucideIcons.funnel,
    title: '조건 선택하기',
    description: '기관 종류, 진료과, 운영 상태, 검색 반경을 선택하세요.',
  ),
  _GuideStep(
    number: '03',
    icon: LucideIcons.search,
    title: '결과 비교하기',
    description: '거리와 운영시간을 목록과 지도에서 함께 확인하세요.',
  ),
  _GuideStep(
    number: '04',
    icon: LucideIcons.bell_ring,
    title: '방문 전 확인하기',
    description: '상세정보를 살펴보고 해당 기관에 전화로 문의하세요.',
  ),
];

const _features = [
  _GuideFeature(
    icon: LucideIcons.locate_fixed,
    title: '현재 위치·주소 검색',
    description:
        '위치 권한을 허용하면 현재 위치를 기준으로 검색합니다. 권한을 사용할 수 없을 때는 주소 검색으로 기준 위치를 바꿀 수 있습니다.',
    color: AppColors.green,
    backgroundColor: AppColors.greenSoft,
  ),
  _GuideFeature(
    icon: LucideIcons.clock_3,
    title: '진료 중 표시',
    description:
        '등록된 운영 일정과 현재 시간을 비교한 안내입니다. 접수 마감, 임시 휴진, 공휴일 일정은 실제 상황과 다를 수 있습니다.',
    color: AppColors.primary,
    backgroundColor: AppColors.primarySoft,
  ),
  _GuideFeature(
    icon: LucideIcons.heart_pulse,
    title: '응급실 정보',
    description:
        '응급실을 선택하면 가까운 응급의료기관과 제공 가능한 병상 정보를 확인할 수 있습니다. 위급한 상황에는 119의 안내를 우선해 주세요.',
    color: AppColors.red,
    backgroundColor: AppColors.redSoft,
  ),
  _GuideFeature(
    icon: LucideIcons.star,
    title: '즐겨찾기',
    description: '로그인한 사용자는 자주 방문하는 병원과 약국을 저장할 수 있습니다. 즐겨찾기만 모아 보는 필터도 제공합니다.',
    color: AppColors.amber,
    backgroundColor: AppColors.amberSoft,
  ),
  _GuideFeature(
    icon: LucideIcons.user_round,
    title: '내 주소 저장',
    description: '내 정보에서 자주 검색하는 주소를 등록하면 다음 방문부터 해당 위치를 편리하게 사용할 수 있습니다.',
    color: AppColors.violet,
    backgroundColor: AppColors.violetSoft,
  ),
  _GuideFeature(
    icon: LucideIcons.database,
    title: '공공데이터 기준',
    description:
        '메디온은 공공 의료데이터를 기반으로 정보를 제공합니다. 데이터 갱신 시점과 현장 상황 사이에 차이가 생길 수 있습니다.',
    color: AppColors.mint,
    backgroundColor: AppColors.mintSoft,
  ),
];

const _faqs = [
  _GuideFaq(
    question: '현재 위치가 정확하지 않게 표시돼요.',
    answer:
        '건물 내부나 기기의 위치 권한 설정에 따라 오차가 발생할 수 있습니다. 위치 권한을 다시 허용하거나 주소 검색으로 기준 위치를 직접 지정해 주세요.',
  ),
  _GuideFaq(
    question: '진료 중으로 표시되는데 문이 닫혀 있어요.',
    answer:
        '진료 중 표시는 등록된 운영 일정에 따른 안내입니다. 임시 휴진, 공휴일, 접수 조기 마감은 즉시 반영되지 않을 수 있으므로 방문 전 전화 확인을 권장합니다.',
  ),
  _GuideFaq(
    question: '검색 결과가 너무 적거나 나오지 않아요.',
    answer: '검색 반경을 넓히고 진료과나 운영 상태 필터를 일부 해제해 보세요. 검색 위치가 올바른지도 함께 확인해 주세요.',
  ),
  _GuideFaq(
    question: '응급실 병상이 있다고 표시되면 바로 이용할 수 있나요?',
    answer:
        '병상 정보는 갱신되는 동안 실제 수용 상황과 달라질 수 있습니다. 환자의 상태와 의료진·장비 상황도 함께 고려되므로 위급할 때는 119의 안내를 받아 주세요.',
  ),
  _GuideFaq(
    question: '즐겨찾기는 어떻게 사용할 수 있나요?',
    answer:
        '로그인 후 검색 결과의 별 아이콘을 누르면 저장됩니다. 검색 화면에서 즐겨찾기 필터를 켜면 저장한 기관만 모아 볼 수 있습니다.',
  ),
  _GuideFaq(
    question: '내 위치 정보가 계속 저장되나요?',
    answer:
        '현재 위치는 주변 의료기관을 검색하는 데 사용됩니다. 계정에 주소를 직접 등록하는 경우를 제외하고, 위치 권한은 기기 설정에서 관리할 수 있습니다.',
  ),
];
