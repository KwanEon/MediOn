import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class AddressSearchPage extends StatefulWidget {
  const AddressSearchPage({
    super.key,
    required this.authApi,
    this.initialQuery = '',
    this.includeStations = false,
  });

  final AuthApiClient authApi;
  final String initialQuery;
  final bool includeStations;

  @override
  State<AddressSearchPage> createState() => _AddressSearchPageState();
}

class _AddressSearchPageState extends State<AddressSearchPage> {
  late final TextEditingController _controller;
  List<AddressSearchResult> _results = const [];
  bool _searching = false;
  String _error = '';

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialQuery);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    FocusScope.of(context).unfocus();
    final query = _controller.text.trim();
    if (query.length < 2) {
      setState(() {
        _error = widget.includeStations
            ? '도로명, 지번 주소 또는 역 이름을 두 글자 이상 입력해 주세요.'
            : '도로명이나 지번 주소를 두 글자 이상 입력해 주세요.';
        _results = const [];
      });
      return;
    }

    setState(() {
      _searching = true;
      _error = '';
      _results = const [];
    });
    try {
      final results = await widget.authApi.searchAddresses(
        query,
        includeStations: widget.includeStations,
      );
      if (!mounted) return;
      setState(() {
        _results = results;
        if (results.isEmpty) {
          _error = widget.includeStations
              ? '검색 결과가 없습니다. 주소나 역 이름을 확인해 주세요.'
              : '검색 결과가 없습니다. 도로명과 건물번호를 확인해 주세요.';
        }
      });
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _searching = false);
    }
  }

  void _select(AddressSearchResult result) {
    Navigator.of(context).pop(result);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.includeStations ? '주소·역 검색' : '주소 검색'),
        centerTitle: true,
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
      ),
      body: ScreenFrame(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PageHeading(
              title: widget.includeStations ? '주소나 역을 검색하세요' : '주소를 검색하세요',
              subtitle: widget.includeStations
                  ? '도로명·지번 주소 또는 역 이름으로 위치를 선택할 수 있어요.'
                  : '도로명이나 지번 주소로 정확한 위치를 선택할 수 있어요.',
            ),
            const SizedBox(height: 22),
            AppSearchField(
              hintText: widget.includeStations
                  ? '예: 서울시 중구 세종대로 110 또는 강남역'
                  : '예: 서울시 중구 세종대로 110',
              controller: _controller,
              autofocus: widget.initialQuery.isEmpty,
              onSubmitted: (_) => _search(),
              onChanged: (_) {
                if (_error.isNotEmpty || _results.isNotEmpty) {
                  setState(() {
                    _error = '';
                    _results = const [];
                  });
                }
              },
            ),
            const SizedBox(height: 12),
            PrimaryButton(
              label: _searching
                  ? '검색 중'
                  : widget.includeStations
                  ? '위치 검색'
                  : '주소 검색',
              loading: _searching,
              onPressed: _search,
            ),
            if (_error.isNotEmpty) ...[
              const SizedBox(height: 14),
              _AddressMessage(
                icon: Icons.info_outline_rounded,
                message: _error,
                isError: true,
              ),
            ],
            const SizedBox(height: 24),
            if (_results.isNotEmpty) ...[
              SectionHeading(
                title: '검색 결과',
                trailing: Text(
                  '${_results.length}건',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ),
              const SizedBox(height: 12),
              for (final result in _results)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: _AddressResultCard(
                    result: result,
                    onTap: () => _select(result),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

class _AddressResultCard extends StatelessWidget {
  const _AddressResultCard({required this.result, required this.onTap});

  final AddressSearchResult result;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final showJibun =
        result.jibunAddress != null &&
        result.jibunAddress != result.roadAddress;

    return SurfaceCard(
      onTap: onTap,
      padding: const EdgeInsets.all(15),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          IconTile(
            icon: result.isStation
                ? Icons.train_rounded
                : Icons.location_on_outlined,
            size: 46,
            iconSize: 23,
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  result.displayAddress,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                if (showJibun) ...[
                  const SizedBox(height: 5),
                  Text(
                    result.isStation
                        ? result.jibunAddress!
                        : '지번 ${result.jibunAddress}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 8),
          const Icon(Icons.chevron_right_rounded, color: AppColors.primary),
        ],
      ),
    );
  }
}

class _AddressMessage extends StatelessWidget {
  const _AddressMessage({
    required this.icon,
    required this.message,
    this.isError = false,
  });

  final IconData icon;
  final String message;
  final bool isError;

  @override
  Widget build(BuildContext context) {
    final color = isError ? AppColors.red : AppColors.primary;
    return SoftNotice(
      icon: icon,
      text: message,
      color: color,
      backgroundColor: isError ? AppColors.redSoft : AppColors.primarySoft,
      borderColor: isError ? const Color(0xFFF5C6CB) : const Color(0xFFD4E4FB),
    );
  }
}
