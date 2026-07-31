import 'package:flutter/material.dart';

import '../data/auth_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';
import 'register_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.authApi});

  final AuthApiClient authApi;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _obscurePassword = true;
  bool _validationStarted = false;
  bool _loading = false;
  String _error = '';
  String _success = '';

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  String? _validateUsername(String? value) {
    if (value == null || value.trim().isEmpty) {
      return '아이디를 입력해 주세요.';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return '비밀번호를 입력해 주세요.';
    }
    return null;
  }

  Future<void> _submit() async {
    if (_loading) return;
    FocusScope.of(context).unfocus();
    setState(() => _validationStarted = true);
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }

    setState(() {
      _loading = true;
      _error = '';
    });
    try {
      final user = await widget.authApi.login(
        username: _usernameController.text,
        password: _passwordController.text,
      );
      if (!mounted) return;
      Navigator.of(context).pop(user);
    } on AuthApiException catch (error) {
      if (!mounted) return;
      setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openRegisterPage() async {
    FocusScope.of(context).unfocus();
    final result = await Navigator.of(context).push<RegistrationResult>(
      MaterialPageRoute(builder: (_) => RegisterPage(authApi: widget.authApi)),
    );

    if (!mounted || result == null) return;
    _formKey.currentState?.reset();
    _usernameController.text = result.username;
    _passwordController.clear();
    setState(() {
      _validationStarted = false;
      _error = '';
      _success = '회원가입이 완료되었습니다. 비밀번호를 입력해 로그인해 주세요.';
    });
  }

  void _showPasswordHelp() {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('비밀번호 찾기는 추후 제공될 예정입니다.')));
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
          padding: const EdgeInsets.fromLTRB(20, 30, 20, 36),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: AutofillGroup(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 58,
                      height: 58,
                      decoration: BoxDecoration(
                        color: AppColors.primarySoft,
                        borderRadius: BorderRadius.circular(18),
                      ),
                      child: const Icon(
                        Icons.lock_person_outlined,
                        color: AppColors.primary,
                        size: 31,
                      ),
                    ),
                    const SizedBox(height: 19),
                    Text(
                      '로그인',
                      style: Theme.of(context).textTheme.displaySmall,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '로그인하고 저장한 의료기관과 내 정보를 확인하세요.',
                      style: Theme.of(
                        context,
                      ).textTheme.bodyLarge?.copyWith(color: AppColors.muted),
                    ),
                    if (_success.isNotEmpty) ...[
                      const SizedBox(height: 16),
                      SoftNotice(
                        icon: Icons.check_circle_outline_rounded,
                        text: _success,
                        color: AppColors.green,
                        backgroundColor: AppColors.greenSoft,
                        borderColor: const Color(0xFFBCE4D4),
                      ),
                    ],
                    if (_error.isNotEmpty) ...[
                      const SizedBox(height: 16),
                      SoftNotice(
                        icon: Icons.error_outline_rounded,
                        text: _error,
                        color: AppColors.red,
                        backgroundColor: AppColors.redSoft,
                        borderColor: const Color(0xFFF5C6CB),
                      ),
                    ],
                    const SizedBox(height: 25),
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
                            AppTextField(
                              label: '아이디',
                              hintText: '아이디를 입력해 주세요',
                              controller: _usernameController,
                              prefixIcon: Icons.person_outline_rounded,
                              validator: _validateUsername,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [AutofillHints.username],
                            ),
                            const SizedBox(height: 16),
                            AppTextField(
                              label: '비밀번호',
                              hintText: '비밀번호를 입력해 주세요',
                              controller: _passwordController,
                              obscureText: _obscurePassword,
                              prefixIcon: Icons.lock_outline_rounded,
                              validator: _validatePassword,
                              textInputAction: TextInputAction.done,
                              autofillHints: const [AutofillHints.password],
                              onFieldSubmitted: (_) => _submit(),
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
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton(
                                onPressed: _showPasswordHelp,
                                style: TextButton.styleFrom(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 4,
                                    vertical: 8,
                                  ),
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                child: const Text('비밀번호 찾기'),
                              ),
                            ),
                            const SizedBox(height: 12),
                            PrimaryButton(
                              label: _loading ? '로그인 중' : '로그인',
                              loading: _loading,
                              onPressed: _submit,
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 22),
                    Row(
                      children: [
                        const Expanded(child: Divider()),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 13),
                          child: Text(
                            '아직 계정이 없나요?',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ),
                        const Expanded(child: Divider()),
                      ],
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      height: 52,
                      child: OutlinedButton.icon(
                        onPressed: _loading ? null : _openRegisterPage,
                        icon: const Icon(
                          Icons.person_add_alt_1_rounded,
                          size: 20,
                        ),
                        label: const Text('회원가입'),
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
