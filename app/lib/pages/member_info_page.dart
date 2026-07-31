import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';
import 'address_search_page.dart';

class MemberInfoPage extends StatefulWidget {
  const MemberInfoPage({super.key, required this.user, required this.authApi});

  final AuthUser user;
  final AuthApiClient authApi;

  @override
  State<MemberInfoPage> createState() => _MemberInfoPageState();
}

class _MemberInfoPageState extends State<MemberInfoPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _emailController;
  late final TextEditingController _addressController;
  late String _selectedAddress;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.user.name);
    _emailController = TextEditingController(text: widget.user.email);
    _addressController = TextEditingController(text: widget.user.address);
    _selectedAddress = widget.user.address;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _addressController.dispose();
    super.dispose();
  }

  String? _validateName(String? value) {
    final name = value?.trim() ?? '';
    if (name.isEmpty) return '이름을 입력해 주세요.';
    if (name.length > 50) return '이름은 50자 이하여야 합니다.';
    return null;
  }

  String? _validateEmail(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) return '이메일을 입력해 주세요.';
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) {
      return '올바른 이메일 형식을 입력해 주세요.';
    }
    return null;
  }

  Future<void> _selectAddress() async {
    final selectedAddress = await Navigator.of(context)
        .push<AddressSearchResult>(
          MaterialPageRoute(
            builder: (_) => AddressSearchPage(
              authApi: widget.authApi,
              initialQuery: _addressController.text,
            ),
          ),
        );
    if (!mounted || selectedAddress == null) return;
    setState(() {
      _selectedAddress = selectedAddress.address;
      _addressController.text = selectedAddress.address;
    });
  }

  Future<void> _save() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    if (_selectedAddress != _addressController.text) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('주소 검색에서 주소를 선택해 주세요.')));
      return;
    }

    setState(() => _saving = true);
    try {
      final user = await widget.authApi.updateProfile(
        name: _nameController.text,
        email: _emailController.text,
        address: _selectedAddress,
      );
      if (!mounted) return;
      Navigator.of(context).pop(user);
    } on AuthApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('회원 정보 관리'),
        centerTitle: true,
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
      ),
      body: SafeArea(
        top: false,
        child: ScreenFrame(
          padding: const EdgeInsets.fromLTRB(20, 24, 20, 40),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const PageHeading(
                  title: '회원 정보 관리',
                  subtitle: '이름, 이메일과 의료기관 검색에 사용할 주소를 관리하세요.',
                ),
                const SizedBox(height: 22),
                SurfaceCard(
                  child: Column(
                    children: [
                      AppTextField(
                        label: '이름',
                        hintText: '이름을 입력해 주세요.',
                        controller: _nameController,
                        prefixIcon: Icons.person_outline_rounded,
                        validator: _validateName,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [AutofillHints.name],
                      ),
                      const SizedBox(height: 15),
                      AppTextField(
                        label: '이메일',
                        hintText: 'example@email.com',
                        controller: _emailController,
                        keyboardType: TextInputType.emailAddress,
                        prefixIcon: Icons.mail_outline_rounded,
                        validator: _validateEmail,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [AutofillHints.email],
                      ),
                      const SizedBox(height: 15),
                      AppTextField(
                        label: '주소',
                        hintText: '눌러서 주소를 검색해 주세요.',
                        controller: _addressController,
                        prefixIcon: Icons.location_on_outlined,
                        suffixIcon: const Icon(
                          Icons.search_rounded,
                          color: AppColors.primary,
                        ),
                        readOnly: true,
                        onTap: _saving ? null : _selectAddress,
                        validator: (value) =>
                            value == null || value.trim().isEmpty
                            ? '주소를 선택해 주세요.'
                            : null,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: FilledButton.icon(
                    onPressed: _saving ? null : _save,
                    icon: _saving
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Icon(Icons.save_outlined),
                    label: Text(_saving ? '저장 중' : '회원정보 저장'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
