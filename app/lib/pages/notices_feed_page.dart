import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../data/content_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class NoticesFeedPage extends StatefulWidget {
  const NoticesFeedPage({super.key, required this.api});

  final AuthApiClient api;

  @override
  State<NoticesFeedPage> createState() => _NoticesFeedPageState();
}

class _NoticesFeedPageState extends State<NoticesFeedPage> {
  static const _categories = ['전체', 'IMPORTANT', 'UPDATE', 'DATA', 'GUIDE'];

  List<NoticeItem> _notices = const [];
  String _selectedCategory = '전체';
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadNotices();
  }

  Future<void> _loadNotices() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final notices = await widget.api.getNotices();
      if (!mounted) return;
      setState(() => _notices = notices);
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<NoticeItem> get _filteredNotices {
    if (_selectedCategory == '전체') return _notices;
    return _notices
        .where((notice) => notice.category == _selectedCategory)
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final filteredNotices = _filteredNotices;
    return ScreenFrame(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeading(title: '공지사항', subtitle: '메디온의 새로운 소식을 확인하세요.'),
          const SizedBox(height: 24),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                for (final category in _categories) ...[
                  ChoiceChip(
                    label: Text(_categoryLabel(category)),
                    selected: _selectedCategory == category,
                    onSelected: (_) {
                      setState(() => _selectedCategory = category);
                    },
                  ),
                  const SizedBox(width: 8),
                ],
              ],
            ),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: Text(
                  '${_categoryLabel(_selectedCategory)} 공지 ${filteredNotices.length}건',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: AppColors.muted,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              IconButton(
                tooltip: '새로고침',
                onPressed: _loading ? null : _loadNotices,
                icon: const Icon(Icons.refresh_rounded),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (_loading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 70),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_error != null)
            SurfaceCard(
              child: Column(
                children: [
                  const Icon(
                    Icons.error_outline_rounded,
                    color: AppColors.red,
                    size: 32,
                  ),
                  const SizedBox(height: 10),
                  Text(_error!, textAlign: TextAlign.center),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: _loadNotices,
                    child: const Text('다시 시도'),
                  ),
                ],
              ),
            )
          else if (filteredNotices.isEmpty)
            const SurfaceCard(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 30),
                child: Center(child: Text('등록된 공지사항이 없습니다.')),
              ),
            )
          else
            for (var index = 0; index < filteredNotices.length; index++) ...[
              _NoticeCard(data: filteredNotices[index]),
              if (index != filteredNotices.length - 1)
                const SizedBox(height: 12),
            ],
          const SizedBox(height: 24),
          const SoftNotice(
            icon: Icons.info_outline_rounded,
            text: '웹과 앱에서 동일한 공지사항을 확인할 수 있습니다.',
            color: AppColors.muted,
            backgroundColor: Color(0xFFF8FAFD),
            borderColor: Color(0xFFCED9E8),
          ),
        ],
      ),
    );
  }
}

class _NoticeCard extends StatelessWidget {
  const _NoticeCard({required this.data});

  final NoticeItem data;

  @override
  Widget build(BuildContext context) {
    final meta = _noticeMeta(data.category);
    return SurfaceCard(
      padding: EdgeInsets.zero,
      borderColor: data.pinned ? const Color(0xFFBCD4FF) : AppColors.line,
      backgroundColor: data.pinned ? const Color(0xFFF8FBFF) : Colors.white,
      child: ExpansionTile(
        tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        childrenPadding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
        leading: IconTile(
          icon: meta.icon,
          color: meta.color,
          backgroundColor: meta.backgroundColor,
          size: 54,
          iconSize: 28,
        ),
        title: Text(
          data.title,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(height: 1.4),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 6),
          child: Text(
            '${_categoryLabel(data.category)} · ${_formatDate(data.publishedAt)}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ),
        children: [
          const Divider(),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              data.content,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: AppColors.ink,
                height: 1.7,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

({IconData icon, Color color, Color backgroundColor}) _noticeMeta(
  String category,
) {
  return switch (category) {
    'IMPORTANT' => (
      icon: Icons.priority_high_rounded,
      color: AppColors.red,
      backgroundColor: AppColors.redSoft,
    ),
    'UPDATE' => (
      icon: Icons.campaign_outlined,
      color: AppColors.green,
      backgroundColor: AppColors.greenSoft,
    ),
    'DATA' => (
      icon: Icons.storage_rounded,
      color: AppColors.violet,
      backgroundColor: AppColors.violetSoft,
    ),
    _ => (
      icon: Icons.info_outline_rounded,
      color: AppColors.primary,
      backgroundColor: AppColors.primarySoft,
    ),
  };
}

String _categoryLabel(String category) {
  return switch (category) {
    'IMPORTANT' => '중요',
    'UPDATE' => '업데이트',
    'DATA' => '데이터',
    'GUIDE' => '안내',
    _ => '전체',
  };
}

String _formatDate(DateTime value) {
  final month = value.month.toString().padLeft(2, '0');
  final day = value.day.toString().padLeft(2, '0');
  return '${value.year}.$month.$day';
}
