import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class NoticesPage extends StatefulWidget {
  const NoticesPage({super.key});

  static const _pinnedNotice = _NoticeData(
    category: '중요',
    title: '의료기관 운영시간은 방문 전에\n다시 확인해 주세요.',
    date: '2026.07.24',
    icon: Icons.notifications_none_rounded,
    color: AppColors.primary,
    backgroundColor: AppColors.primarySoft,
    details: [
      '의료기관의 접수 마감 시간과 휴진 일정은 당일 변경될 수 있습니다.',
      '방문 전 해당 기관에 전화해 진료 가능 여부를 확인해 주세요.',
    ],
  );

  static const _notices = [
    _NoticeData(
      category: '업데이트',
      title: '건강 정보와 이용 안내 콘텐츠를\n새롭게 구성했습니다.',
      date: '2026.07.24',
      icon: Icons.campaign_outlined,
      color: AppColors.green,
      backgroundColor: AppColors.greenSoft,
      details: [
        '건강 정보와 이용 안내를 주제별로 더 쉽게 찾아볼 수 있도록 화면 구성을 개선했습니다.',
        '각 안내는 일반적인 참고 정보이며 개인의 진단이나 치료를 대신하지 않습니다.',
      ],
    ),
    _NoticeData(
      category: '데이터',
      title: '응급실 병상 정보 이용 시 참고 사항을\n안내합니다.',
      date: '2026.07.24',
      icon: Icons.storage_rounded,
      color: AppColors.violet,
      backgroundColor: AppColors.violetSoft,
      details: [
        '응급실 병상 정보는 제공 기관의 갱신 시점에 따라 실제 현황과 차이가 날 수 있습니다.',
        '긴급한 상황에서는 119에 연락하고 안내에 따라 이동해 주세요.',
      ],
    ),
    _NoticeData(
      category: '안내',
      title: '현재 위치를 사용할 수 없을 때는\n주소 검색을 이용해 주세요.',
      date: '2026.07.24',
      icon: Icons.location_on_outlined,
      color: AppColors.amber,
      backgroundColor: AppColors.amberSoft,
      details: [
        '위치 권한이 꺼져 있거나 현재 위치를 불러오지 못한 경우 주소를 직접 입력할 수 있습니다.',
        '도로명이나 동 이름으로 검색하면 주변 의료기관을 확인할 수 있습니다.',
      ],
    ),
  ];

  @override
  State<NoticesPage> createState() => _NoticesPageState();
}

class _NoticesPageState extends State<NoticesPage> {
  String _selectedCategory = '전체';
  final Set<String> _expandedNotices = {};

  List<_NoticeData> get _filteredNotices {
    if (_selectedCategory == '전체') return NoticesPage._notices;
    return NoticesPage._notices
        .where((notice) => notice.category == _selectedCategory)
        .toList();
  }

  bool get _showPinnedNotice =>
      _selectedCategory == '전체' || _selectedCategory == '중요';

  void _selectCategory(String category) {
    setState(() => _selectedCategory = category);
  }

  void _toggleNotice(String title) {
    setState(() {
      if (!_expandedNotices.add(title)) {
        _expandedNotices.remove(title);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final filteredNotices = _filteredNotices;
    final resultCount = filteredNotices.length + (_showPinnedNotice ? 1 : 0);

    return ScreenFrame(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeading(title: '공지사항', subtitle: '메디온의 새로운 소식을 확인하세요.'),
          const SizedBox(height: 26),
          _NoticeTabs(
            selectedCategory: _selectedCategory,
            onSelected: _selectCategory,
          ),
          const SizedBox(height: 20),
          Text(
            '${_selectedCategory == '전체' ? '전체' : _selectedCategory} 공지 $resultCount건',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w600,
              color: AppColors.muted,
            ),
          ),
          const SizedBox(height: 14),
          if (_showPinnedNotice) ...[
            _PinnedNoticeCard(
              data: NoticesPage._pinnedNotice,
              expanded: _expandedNotices.contains(
                NoticesPage._pinnedNotice.title,
              ),
              onTap: () => _toggleNotice(NoticesPage._pinnedNotice.title),
            ),
            if (filteredNotices.isNotEmpty) const SizedBox(height: 13),
          ],
          for (var index = 0; index < filteredNotices.length; index++) ...[
            _NoticeCard(
              data: filteredNotices[index],
              expanded: _expandedNotices.contains(filteredNotices[index].title),
              onTap: () => _toggleNotice(filteredNotices[index].title),
            ),
            if (index != filteredNotices.length - 1) const SizedBox(height: 13),
          ],
          const SizedBox(height: 24),
          const SoftNotice(
            icon: Icons.info_outline_rounded,
            text: '중요 공지를 먼저 확인해 주세요.',
            color: AppColors.muted,
            backgroundColor: Color(0xFFF8FAFD),
            borderColor: Color(0xFFCED9E8),
          ),
        ],
      ),
    );
  }
}

class _PinnedNoticeCard extends StatelessWidget {
  const _PinnedNoticeCard({
    required this.data,
    required this.expanded,
    required this.onTap,
  });

  final _NoticeData data;
  final bool expanded;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(18),
      borderColor: const Color(0xFFBCD4FF),
      backgroundColor: const Color(0xFFF8FBFF),
      onTap: onTap,
      child: Column(
        children: [
          Row(
            children: [
              IconTile(
                icon: data.icon,
                color: data.color,
                backgroundColor: data.backgroundColor,
                size: 76,
                iconSize: 43,
              ),
              const SizedBox(width: 17),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TinyTag(label: data.category),
                    const SizedBox(height: 8),
                    Text(
                      data.title,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      data.date,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(
                expanded
                    ? Icons.expand_less_rounded
                    : Icons.expand_more_rounded,
                color: AppColors.primary,
                size: 28,
              ),
            ],
          ),
          if (expanded) _NoticeDetails(paragraphs: data.details),
        ],
      ),
    );
  }
}

class _NoticeTabs extends StatelessWidget {
  const _NoticeTabs({required this.selectedCategory, required this.onSelected});

  static const _labels = ['전체', '중요', '업데이트', '데이터', '안내'];

  final String selectedCategory;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.line)),
      ),
      child: Row(
        children: [
          for (var index = 0; index < _labels.length; index++)
            Expanded(
              child: Semantics(
                button: true,
                selected: _labels[index] == selectedCategory,
                child: InkWell(
                  onTap: () => onSelected(_labels[index]),
                  child: Container(
                    height: 52,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      border: _labels[index] == selectedCategory
                          ? const Border(
                              bottom: BorderSide(
                                color: AppColors.primary,
                                width: 3,
                              ),
                            )
                          : null,
                    ),
                    child: Text(
                      _labels[index],
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: _labels[index] == selectedCategory
                            ? AppColors.primary
                            : AppColors.muted,
                        fontSize: 13,
                        fontWeight: _labels[index] == selectedCategory
                            ? FontWeight.w800
                            : FontWeight.w500,
                      ),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _NoticeCard extends StatelessWidget {
  const _NoticeCard({
    required this.data,
    required this.expanded,
    required this.onTap,
  });

  final _NoticeData data;
  final bool expanded;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(18),
      onTap: onTap,
      child: Column(
        children: [
          Row(
            children: [
              IconTile(
                icon: data.icon,
                color: data.color,
                backgroundColor: data.backgroundColor,
                size: 72,
                iconSize: 39,
              ),
              const SizedBox(width: 17),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      data.category,
                      style: Theme.of(
                        context,
                      ).textTheme.labelLarge?.copyWith(color: data.color),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      data.title,
                      style: Theme.of(
                        context,
                      ).textTheme.titleMedium?.copyWith(height: 1.45),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      data.date,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(
                expanded
                    ? Icons.expand_less_rounded
                    : Icons.expand_more_rounded,
                color: AppColors.primary,
                size: 27,
              ),
            ],
          ),
          if (expanded) _NoticeDetails(paragraphs: data.details),
        ],
      ),
    );
  }
}

class _NoticeDetails extends StatelessWidget {
  const _NoticeDetails({required this.paragraphs});

  final List<String> paragraphs;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 16),
        const Divider(),
        const SizedBox(height: 12),
        for (var index = 0; index < paragraphs.length; index++) ...[
          Text(
            paragraphs[index],
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: AppColors.ink, height: 1.6),
          ),
          if (index != paragraphs.length - 1) const SizedBox(height: 9),
        ],
      ],
    );
  }
}

class _NoticeData {
  const _NoticeData({
    required this.category,
    required this.title,
    required this.date,
    required this.icon,
    required this.color,
    required this.backgroundColor,
    required this.details,
  });

  final String category;
  final String title;
  final String date;
  final IconData icon;
  final Color color;
  final Color backgroundColor;
  final List<String> details;
}
