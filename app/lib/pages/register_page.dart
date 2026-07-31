import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../utils/phone_number_formatter.dart';
import '../widgets/common_widgets.dart';
import 'address_search_page.dart';

class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key, required this.authApi});

  final AuthApiClient authApi;

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _passwordConfirmController = TextEditingController();
  final _addressController = TextEditingController();

  AddressSearchResult? _selectedAddress;
  bool _obscurePassword = true;
  bool _obscurePasswordConfirm = true;
  bool _agreedToTerms = false;
  bool _showTermsError = false;
  bool _validationStarted = false;
  bool _loading = false;
  String _apiError = '';

  @override
  void dispose() {
    _usernameController.dispose();
    _nameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _passwordConfirmController.dispose();
    _addressController.dispose();
    super.dispose();
  }

  String? _validateUsername(String? value) {
    final username = value?.trim() ?? '';
    if (username.isEmpty) return '아이디를 입력해 주세요.';
    if (username.length < 4 || username.length > 30) {
      return '아이디는 4자 이상 30자 이하여야 합니다.';
    }
    if (!RegExp(r'^[A-Za-z0-9_]+$').hasMatch(username)) {
      return '아이디는 영문, 숫자, 밑줄만 사용할 수 있습니다.';
    }
    return null;
  }

  String? _validateName(String? value) {
    final name = value?.trim() ?? '';
    if (name.isEmpty) return '이름을 입력해 주세요.';
    if (name.length < 2) return '이름은 2자 이상 입력해 주세요.';
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

  String? _validatePhone(String? value) {
    final phone = value?.trim() ?? '';
    if (phone.isEmpty) return '전화번호를 입력해 주세요.';
    if (!RegExp(r'^[0-9+() -]{8,30}$').hasMatch(phone)) {
      return '올바른 전화번호를 입력해 주세요.';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    final password = value ?? '';
    if (password.isEmpty) return '비밀번호를 입력해 주세요.';
    if (password.length < 8) return '비밀번호는 8자 이상 입력해 주세요.';
    if (password.length > 72) return '비밀번호는 72자 이하여야 합니다.';
    return null;
  }

  String? _validatePasswordConfirm(String? value) {
    if (value == null || value.isEmpty) {
      return '비밀번호를 한 번 더 입력해 주세요.';
    }
    if (value != _passwordController.text) {
      return '입력한 비밀번호가 서로 다릅니다.';
    }
    return null;
  }

  String? _validateAddress(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '주소를 검색해 선택해 주세요.';
    }
    if (_selectedAddress?.address != value.trim()) {
      return '검색 결과에서 정확한 주소를 선택해 주세요.';
    }
    return null;
  }

  Future<void> _searchAddress() async {
    FocusScope.of(context).unfocus();
    final result = await Navigator.of(context).push<AddressSearchResult>(
      MaterialPageRoute(
        builder: (_) => AddressSearchPage(
          authApi: widget.authApi,
          initialQuery: _addressController.text,
        ),
      ),
    );
    if (!mounted || result == null) return;
    _addressController.text = result.address;
    setState(() {
      _selectedAddress = result;
    });
    if (_validationStarted) {
      _formKey.currentState?.validate();
    }
  }

  Future<void> _submit() async {
    if (_loading) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _validationStarted = true;
      _showTermsError = !_agreedToTerms;
      _apiError = '';
    });
    final formIsValid = _formKey.currentState?.validate() ?? false;
    if (!formIsValid || !_agreedToTerms) {
      return;
    }

    setState(() => _loading = true);
    try {
      await widget.authApi.register(
        username: _usernameController.text,
        password: _passwordController.text,
        name: _nameController.text,
        email: _emailController.text,
        phoneNumber: _phoneController.text,
        address: _selectedAddress!.address,
      );
      if (!mounted) return;
      Navigator.of(
        context,
      ).pop(RegistrationResult(username: _usernameController.text.trim()));
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() => _apiError = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _setTerms(bool value) {
    setState(() {
      _agreedToTerms = value;
      if (value) _showTermsError = false;
    });
  }

  void _showTerms() {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (context) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('필수 약관 안내', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 14),
              Text(
                '서비스 이용약관과 개인정보 수집·이용 안내에 동의하면 메디온의 '
                '의료기관 검색 및 저장 기능을 이용할 수 있습니다. 입력한 회원 '
                '정보는 계정 생성과 서비스 제공을 위해 저장됩니다.',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 22),
              PrimaryButton(
                label: '확인',
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 68,
        titleSpacing: 4,
        title: const MediOnBrand(),
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
      ),
      body: SafeArea(
        top: false,
        child: SingleChildScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.fromLTRB(20, 28, 20, 36),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: AutofillGroup(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '메디온 시작하기',
                      style: Theme.of(context).textTheme.displaySmall,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '기본 정보를 입력하고 내 주변 의료기관을 더 편리하게 찾아보세요.',
                      style: Theme.of(
                        context,
                      ).textTheme.bodyLarge?.copyWith(color: AppColors.muted),
                    ),
                    if (_apiError.isNotEmpty) ...[
                      const SizedBox(height: 16),
                      SoftNotice(
                        icon: Icons.error_outline_rounded,
                        text: _apiError,
                        color: AppColors.red,
                        backgroundColor: AppColors.redSoft,
                        borderColor: const Color(0xFFF5C6CB),
                      ),
                    ],
                    const SizedBox(height: 22),
                    SurfaceCard(
                      padding: const EdgeInsets.all(20),
                      child: Form(
                        key: _formKey,
                        autovalidateMode: _validationStarted
                            ? AutovalidateMode.onUserInteraction
                            : AutovalidateMode.disabled,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '계정 정보',
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                            const SizedBox(height: 15),
                            AppTextField(
                              label: '아이디',
                              hintText: '영문, 숫자, 밑줄 4~30자',
                              controller: _usernameController,
                              prefixIcon: Icons.person_outline_rounded,
                              validator: _validateUsername,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [AutofillHints.newUsername],
                            ),
                            const SizedBox(height: 15),
                            AppTextField(
                              label: '비밀번호',
                              hintText: '8자 이상 입력해 주세요',
                              controller: _passwordController,
                              obscureText: _obscurePassword,
                              prefixIcon: Icons.lock_outline_rounded,
                              validator: _validatePassword,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [AutofillHints.newPassword],
                              suffixIcon: IconButton(
                                tooltip: _obscurePassword
                                    ? '비밀번호 표시'
                                    : '비밀번호 숨기기',
                                onPressed: () => setState(
                                  () => _obscurePassword = !_obscurePassword,
                                ),
                                icon: Icon(
                                  _obscurePassword
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                ),
                              ),
                            ),
                            const SizedBox(height: 15),
                            AppTextField(
                              label: '비밀번호 확인',
                              hintText: '비밀번호를 다시 입력해 주세요',
                              controller: _passwordConfirmController,
                              obscureText: _obscurePasswordConfirm,
                              prefixIcon: Icons.verified_user_outlined,
                              validator: _validatePasswordConfirm,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [AutofillHints.newPassword],
                              suffixIcon: IconButton(
                                tooltip: _obscurePasswordConfirm
                                    ? '비밀번호 확인 표시'
                                    : '비밀번호 확인 숨기기',
                                onPressed: () => setState(
                                  () => _obscurePasswordConfirm =
                                      !_obscurePasswordConfirm,
                                ),
                                icon: Icon(
                                  _obscurePasswordConfirm
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                ),
                              ),
                            ),
                            const SizedBox(height: 15),
                            AppTextField(
                              label: '이름',
                              hintText: '이름을 입력해 주세요',
                              controller: _nameController,
                              keyboardType: TextInputType.name,
                              prefixIcon: Icons.badge_outlined,
                              validator: _validateName,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [AutofillHints.name],
                            ),
                            const SizedBox(height: 15),
                            AppTextField(
                              label: '이메일',
                              hintText: 'example@medion.com',
                              controller: _emailController,
                              keyboardType: TextInputType.emailAddress,
                              prefixIcon: Icons.mail_outline_rounded,
                              validator: _validateEmail,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [AutofillHints.email],
                            ),
                            const SizedBox(height: 15),
                            AppTextField(
                              label: '전화번호',
                              hintText: '010-1234-5678',
                              controller: _phoneController,
                              keyboardType: TextInputType.phone,
                              prefixIcon: Icons.phone_outlined,
                              validator: _validatePhone,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [
                                AutofillHints.telephoneNumber,
                              ],
                              inputFormatters: const [
                                KoreanPhoneNumberInputFormatter(),
                              ],
                            ),
                            const SizedBox(height: 15),
                            AppTextField(
                              label: '주소',
                              hintText: '눌러서 도로명 또는 지번 주소 검색',
                              controller: _addressController,
                              prefixIcon: Icons.location_on_outlined,
                              validator: _validateAddress,
                              readOnly: true,
                              onTap: _searchAddress,
                              suffixIcon: IconButton(
                                tooltip: '주소 검색',
                                onPressed: _searchAddress,
                                icon: const Icon(Icons.search_rounded),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        border: Border.all(
                          color: _showTermsError
                              ? AppColors.red
                              : AppColors.line,
                        ),
                        borderRadius: BorderRadius.circular(13),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: [
                              Checkbox(
                                value: _agreedToTerms,
                                activeColor: AppColors.primary,
                                onChanged: _loading
                                    ? null
                                    : (value) => _setTerms(value ?? false),
                              ),
                              Expanded(
                                child: GestureDetector(
                                  onTap: _loading
                                      ? null
                                      : () => _setTerms(!_agreedToTerms),
                                  behavior: HitTestBehavior.opaque,
                                  child: Text(
                                    '이용약관 및 개인정보 수집·이용에 동의합니다. (필수)',
                                    style: Theme.of(context).textTheme.bodySmall
                                        ?.copyWith(
                                          color: AppColors.ink,
                                          fontWeight: FontWeight.w600,
                                        ),
                                  ),
                                ),
                              ),
                              IconButton(
                                tooltip: '약관 내용 보기',
                                onPressed: _showTerms,
                                icon: const Icon(
                                  Icons.chevron_right_rounded,
                                  color: AppColors.muted,
                                ),
                              ),
                            ],
                          ),
                          if (_showTermsError)
                            Padding(
                              padding: const EdgeInsets.only(
                                left: 50,
                                bottom: 4,
                              ),
                              child: Text(
                                '필수 약관에 동의해 주세요.',
                                style: Theme.of(context).textTheme.bodySmall
                                    ?.copyWith(color: AppColors.red),
                              ),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 18),
                    PrimaryButton(
                      label: _loading ? '가입 중' : '회원가입',
                      loading: _loading,
                      onPressed: _submit,
                    ),
                    const SizedBox(height: 16),
                    Center(
                      child: TextButton(
                        onPressed: _loading
                            ? null
                            : () => Navigator.pop(context),
                        child: const Text('이미 계정이 있어요  ·  로그인'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
