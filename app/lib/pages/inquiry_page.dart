import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../data/content_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class InquiryPage extends StatefulWidget {
  const InquiryPage({
    super.key,
    required this.api,
    required this.user,
    required this.onLoginRequested,
  });

  final AuthApiClient api;
  final AuthUser? user;
  final VoidCallback onLoginRequested;

  @override
  State<InquiryPage> createState() => _InquiryPageState();
}

class _InquiryPageState extends State<InquiryPage> {
  static const _categoryLabels = {
    'GENERAL': '서비스 이용',
    'ACCOUNT': '계정',
    'DATA': '의료데이터',
    'ERROR': '오류 신고',
    'OTHER': '기타',
  };
  static const _statusLabels = {
    'RECEIVED': '접수',
    'REVIEWING': '확인 중',
    'ANSWERED': '답변 완료',
    'CLOSED': '종료',
  };

  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _contentController = TextEditingController();
  String _category = 'GENERAL';
  List<InquiryItem> _inquiries = const [];
  bool _loading = false;
  bool _submitting = false;
  int? _deletingInquiryId;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (widget.user != null) _loadInquiries();
  }

  @override
  void didUpdateWidget(covariant InquiryPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.user?.id != widget.user?.id) {
      if (widget.user == null) {
        setState(() => _inquiries = const []);
      } else {
        _loadInquiries();
      }
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _loadInquiries() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final inquiries = await widget.api.getMyInquiries();
      if (!mounted) return;
      setState(() => _inquiries = inquiries);
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final inquiry = await widget.api.createInquiry(
        category: _category,
        title: _titleController.text,
        content: _contentController.text,
      );
      if (!mounted) return;
      setState(() {
        _inquiries = [inquiry, ..._inquiries];
        _category = 'GENERAL';
        _titleController.clear();
        _contentController.clear();
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('문의가 접수되었습니다.')));
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _deleteInquiry(InquiryItem inquiry) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        icon: const Icon(
          Icons.delete_outline_rounded,
          color: AppColors.red,
          size: 30,
        ),
        title: const Text('문의를 삭제할까요?'),
        content: Text('‘${inquiry.title}’ 문의를 삭제하면 복구할 수 없습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: FilledButton.styleFrom(backgroundColor: AppColors.red),
            child: const Text('삭제'),
          ),
        ],
      ),
    );
    if (!mounted || confirmed != true) return;

    setState(() {
      _deletingInquiryId = inquiry.id;
      _error = null;
    });
    try {
      await widget.api.deleteMyInquiry(inquiry.id);
      if (!mounted) return;
      setState(() {
        _inquiries = _inquiries
            .where((item) => item.id != inquiry.id)
            .toList(growable: false);
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('문의를 삭제했습니다.')));
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _deletingInquiryId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ScreenFrame(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeading(title: '문의하기', subtitle: '궁금한 점이나 데이터 오류를 알려주세요.'),
          const SizedBox(height: 24),
          if (widget.user == null)
            SurfaceCard(
              child: Column(
                children: [
                  const Icon(
                    Icons.mark_chat_unread_outlined,
                    color: AppColors.primary,
                    size: 42,
                  ),
                  const SizedBox(height: 14),
                  Text(
                    '로그인 후 문의할 수 있습니다.',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '작성자 확인과 문의 내역 제공을 위해 로그인이 필요합니다.',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 18),
                  FilledButton.icon(
                    onPressed: widget.onLoginRequested,
                    icon: const Icon(Icons.login_rounded),
                    label: const Text('로그인하기'),
                  ),
                ],
              ),
            )
          else ...[
            SurfaceCard(
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('새 문의', style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 5),
                    Text(
                      '${widget.user!.name}님의 문의로 접수됩니다.',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 14),
                      SoftNotice(
                        icon: Icons.error_outline_rounded,
                        text: _error!,
                        color: AppColors.red,
                        backgroundColor: AppColors.redSoft,
                        borderColor: const Color(0xFFFFC5C5),
                      ),
                    ],
                    const SizedBox(height: 18),
                    DropdownButtonFormField<String>(
                      initialValue: _category,
                      decoration: const InputDecoration(labelText: '문의 유형'),
                      items: _categoryLabels.entries
                          .map(
                            (entry) => DropdownMenuItem(
                              value: entry.key,
                              child: Text(entry.value),
                            ),
                          )
                          .toList(),
                      onChanged: _submitting
                          ? null
                          : (value) {
                              if (value != null) {
                                setState(() => _category = value);
                              }
                            },
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _titleController,
                      maxLength: 150,
                      decoration: const InputDecoration(
                        labelText: '제목',
                        hintText: '문의 제목을 입력해 주세요.',
                      ),
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                          ? '문의 제목을 입력해 주세요.'
                          : null,
                    ),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _contentController,
                      maxLength: 10000,
                      minLines: 6,
                      maxLines: 10,
                      decoration: const InputDecoration(
                        labelText: '문의 내용',
                        hintText: '확인이 필요한 내용을 자세히 적어 주세요.',
                        alignLabelWithHint: true,
                      ),
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                          ? '문의 내용을 입력해 주세요.'
                          : null,
                    ),
                    const SizedBox(height: 10),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _submitting ? null : _submit,
                        icon: _submitting
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Icon(Icons.send_rounded),
                        label: Text(_submitting ? '접수 중' : '문의 접수'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),
            SectionHeading(
              title: '내 문의 내역',
              trailing: IconButton(
                tooltip: '새로고침',
                onPressed: _loading ? null : _loadInquiries,
                icon: const Icon(Icons.refresh_rounded),
              ),
            ),
            const SizedBox(height: 12),
            if (_loading)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 44),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_inquiries.isEmpty)
              const SurfaceCard(
                child: Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Center(child: Text('아직 등록한 문의가 없습니다.')),
                ),
              )
            else
              for (var index = 0; index < _inquiries.length; index++) ...[
                _InquiryCard(
                  data: _inquiries[index],
                  categoryLabel:
                      _categoryLabels[_inquiries[index].category] ?? '기타',
                  statusLabel: _statusLabels[_inquiries[index].status] ?? '접수',
                  deleting: _deletingInquiryId == _inquiries[index].id,
                  onDelete: () => _deleteInquiry(_inquiries[index]),
                ),
                if (index != _inquiries.length - 1) const SizedBox(height: 10),
              ],
          ],
        ],
      ),
    );
  }
}

class _InquiryCard extends StatelessWidget {
  const _InquiryCard({
    required this.data,
    required this.categoryLabel,
    required this.statusLabel,
    required this.deleting,
    required this.onDelete,
  });

  final InquiryItem data;
  final String categoryLabel;
  final String statusLabel;
  final bool deleting;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: EdgeInsets.zero,
      child: ExpansionTile(
        tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        childrenPadding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
        title: Text(data.title),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 5),
          child: Text(
            '$categoryLabel · ${_formatDateTime(data.createdAt)} · $statusLabel',
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
          const SizedBox(height: 14),
          Align(
            alignment: Alignment.centerRight,
            child: OutlinedButton.icon(
              onPressed: deleting ? null : onDelete,
              style: OutlinedButton.styleFrom(foregroundColor: AppColors.red),
              icon: deleting
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.delete_outline_rounded, size: 18),
              label: Text(deleting ? '삭제 중' : '문의 삭제'),
            ),
          ),
        ],
      ),
    );
  }
}

String _formatDateTime(DateTime value) {
  final month = value.month.toString().padLeft(2, '0');
  final day = value.day.toString().padLeft(2, '0');
  final hour = value.hour.toString().padLeft(2, '0');
  final minute = value.minute.toString().padLeft(2, '0');
  return '${value.year}.$month.$day $hour:$minute';
}
