import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_colors.dart';

class MediOnBrand extends StatelessWidget {
  const MediOnBrand({super.key});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        const _BrandMark(),
        const SizedBox(width: 10),
        Text(
          '메디온',
          style: Theme.of(
            context,
          ).textTheme.headlineSmall?.copyWith(fontSize: 24, letterSpacing: -1),
        ),
      ],
    );
  }
}

class _BrandMark extends StatelessWidget {
  const _BrandMark();

  @override
  Widget build(BuildContext context) {
    Widget block(Color color) => Container(
      width: 13,
      height: 13,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(4),
      ),
    );

    return SizedBox(
      width: 34,
      height: 34,
      child: Stack(
        children: [
          Positioned(top: 0, left: 10.5, child: block(const Color(0xFF35A7E4))),
          Positioned(top: 10.5, right: 0, child: block(AppColors.green)),
          Positioned(
            bottom: 0,
            left: 10.5,
            child: block(const Color(0xFF54C4C9)),
          ),
          Positioned(top: 10.5, left: 0, child: block(AppColors.primary)),
        ],
      ),
    );
  }
}

class ScreenFrame extends StatelessWidget {
  const ScreenFrame({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.fromLTRB(20, 26, 20, 32),
  });

  final Widget child;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return CustomScrollView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      physics: const BouncingScrollPhysics(),
      slivers: [
        SliverToBoxAdapter(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
              child: Padding(padding: padding, child: child),
            ),
          ),
        ),
      ],
    );
  }
}

class PageHeading extends StatelessWidget {
  const PageHeading({super.key, required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.displaySmall),
        const SizedBox(height: 9),
        Text(
          subtitle,
          style: Theme.of(
            context,
          ).textTheme.bodyLarge?.copyWith(color: AppColors.muted),
        ),
      ],
    );
  }
}

class SectionHeading extends StatelessWidget {
  const SectionHeading({super.key, required this.title, this.trailing});

  final String title;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Expanded(
          child: Text(title, style: Theme.of(context).textTheme.headlineSmall),
        ),
        if (trailing != null) ...[const SizedBox(width: 12), trailing!],
      ],
    );
  }
}

class StaticSearchField extends StatelessWidget {
  const StaticSearchField({super.key, required this.hintText});

  final String hintText;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: const Color(0xFFCBD4E0)),
        borderRadius: BorderRadius.circular(28),
      ),
      child: Row(
        children: [
          const Icon(Icons.search_rounded, color: Color(0xFF929DAF), size: 25),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              hintText,
              style: Theme.of(
                context,
              ).textTheme.bodyLarge?.copyWith(color: const Color(0xFF929DAF)),
            ),
          ),
        ],
      ),
    );
  }
}

class AppSearchField extends StatelessWidget {
  const AppSearchField({
    super.key,
    required this.hintText,
    this.controller,
    this.focusNode,
    this.onChanged,
    this.onSubmitted,
    this.autofocus = false,
  });

  final String hintText;
  final TextEditingController? controller;
  final FocusNode? focusNode;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      focusNode: focusNode,
      autofocus: autofocus,
      onChanged: onChanged,
      onSubmitted: onSubmitted,
      textInputAction: TextInputAction.search,
      decoration: InputDecoration(
        hintText: hintText,
        prefixIcon: const Icon(
          Icons.search_rounded,
          color: Color(0xFF929DAF),
          size: 25,
        ),
        suffixIcon: controller == null
            ? null
            : ValueListenableBuilder<TextEditingValue>(
                valueListenable: controller!,
                builder: (context, value, _) {
                  if (value.text.isEmpty) return const SizedBox.shrink();
                  return IconButton(
                    tooltip: '검색어 지우기',
                    onPressed: () {
                      controller!.clear();
                      onChanged?.call('');
                    },
                    icon: const Icon(Icons.close_rounded, size: 20),
                  );
                },
              ),
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 18,
          vertical: 17,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: const BorderSide(color: Color(0xFFCBD4E0)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: const BorderSide(color: Color(0xFFCBD4E0)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.6),
        ),
      ),
    );
  }
}

class MedionChip extends StatelessWidget {
  const MedionChip({
    super.key,
    required this.label,
    this.selected = false,
    this.icon,
    this.foregroundColor,
    this.onTap,
  });

  final String label;
  final bool selected;
  final IconData? icon;
  final Color? foregroundColor;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final color = foregroundColor ?? AppColors.primary;
    final content = Container(
      height: 40,
      padding: EdgeInsets.symmetric(horizontal: icon == null ? 17 : 14),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: selected ? color : Colors.white,
        border: Border.all(color: selected ? color : const Color(0xFFCDD5E0)),
        borderRadius: BorderRadius.circular(22),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(
              icon,
              size: 18,
              color: selected
                  ? Colors.white
                  : (foregroundColor ?? AppColors.ink),
            ),
            const SizedBox(width: 7),
          ],
          Text(
            label,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: selected
                  ? Colors.white
                  : (foregroundColor ?? AppColors.ink),
              fontSize: 13,
            ),
          ),
        ],
      ),
    );

    if (onTap == null) return content;
    return Semantics(
      button: true,
      selected: selected,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(22),
          child: content,
        ),
      ),
    );
  }
}

class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: FilledButton(
        onPressed: loading ? null : onPressed,
        child: loading
            ? const SizedBox(
                width: 22,
                height: 22,
                child: CircularProgressIndicator(
                  strokeWidth: 2.5,
                  color: Colors.white,
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(label),
                  if (icon != null) ...[
                    const SizedBox(width: 8),
                    Icon(icon, size: 19),
                  ],
                ],
              ),
      ),
    );
  }
}

class AppTextField extends StatelessWidget {
  const AppTextField({
    super.key,
    required this.label,
    required this.hintText,
    this.controller,
    this.keyboardType,
    this.obscureText = false,
    this.prefixIcon,
    this.suffixIcon,
    this.validator,
    this.onChanged,
    this.onTap,
    this.onFieldSubmitted,
    this.maxLines = 1,
    this.readOnly = false,
    this.textInputAction,
    this.autofillHints,
    this.inputFormatters,
  });

  final String label;
  final String hintText;
  final TextEditingController? controller;
  final TextInputType? keyboardType;
  final bool obscureText;
  final IconData? prefixIcon;
  final Widget? suffixIcon;
  final String? Function(String?)? validator;
  final ValueChanged<String>? onChanged;
  final VoidCallback? onTap;
  final ValueChanged<String>? onFieldSubmitted;
  final int maxLines;
  final bool readOnly;
  final TextInputAction? textInputAction;
  final Iterable<String>? autofillHints;
  final List<TextInputFormatter>? inputFormatters;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: keyboardType,
      obscureText: obscureText,
      validator: validator,
      onChanged: onChanged,
      onTap: onTap,
      onFieldSubmitted: onFieldSubmitted,
      maxLines: maxLines,
      readOnly: readOnly,
      textInputAction: textInputAction,
      autofillHints: autofillHints,
      inputFormatters: inputFormatters,
      decoration: InputDecoration(
        labelText: label,
        hintText: hintText,
        prefixIcon: prefixIcon == null ? null : Icon(prefixIcon, size: 21),
        suffixIcon: suffixIcon,
      ),
    );
  }
}

class IconTile extends StatelessWidget {
  const IconTile({
    super.key,
    required this.icon,
    this.color = AppColors.primary,
    this.backgroundColor = AppColors.primarySoft,
    this.size = 50,
    this.iconSize = 27,
  });

  final IconData icon;
  final Color color;
  final Color backgroundColor;
  final double size;
  final double iconSize;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(size * 0.28),
      ),
      child: Icon(icon, color: color, size: iconSize),
    );
  }
}

class SurfaceCard extends StatelessWidget {
  const SurfaceCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(18),
    this.borderColor = AppColors.line,
    this.backgroundColor = Colors.white,
    this.onTap,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color borderColor;
  final Color backgroundColor;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final cardContent = Padding(padding: padding, child: child);
    final interactiveContent = onTap == null
        ? cardContent
        : InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(16),
            child: cardContent,
          );

    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A1E375A),
            blurRadius: 16,
            offset: Offset(0, 5),
          ),
        ],
      ),
      child: Material(
        color: backgroundColor,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: borderColor),
        ),
        clipBehavior: Clip.antiAlias,
        child: SizedBox(width: double.infinity, child: interactiveContent),
      ),
    );
  }
}

class EmergencyBanner extends StatelessWidget {
  const EmergencyBanner({
    super.key,
    required this.title,
    required this.message,
    this.buttonLabel,
    this.onButtonPressed,
  });

  final String title;
  final String message;
  final String? buttonLabel;
  final VoidCallback? onButtonPressed;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.redSoft,
        border: Border.all(color: const Color(0xFFF5C6CB)),
        borderRadius: BorderRadius.circular(15),
      ),
      child: Row(
        children: [
          const IconTile(
            icon: Icons.emergency_rounded,
            color: AppColors.red,
            backgroundColor: Colors.white,
            size: 44,
            iconSize: 23,
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: Theme.of(
                    context,
                  ).textTheme.titleMedium?.copyWith(color: AppColors.red),
                ),
                const SizedBox(height: 2),
                Text(message, style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
          if (buttonLabel != null) ...[
            const SizedBox(width: 8),
            Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: onButtonPressed,
                borderRadius: BorderRadius.circular(9),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 11,
                    vertical: 9,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    border: Border.all(color: AppColors.red),
                    borderRadius: BorderRadius.circular(9),
                  ),
                  child: Text(
                    buttonLabel!,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: AppColors.red,
                      fontSize: 12,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class SoftNotice extends StatelessWidget {
  const SoftNotice({
    super.key,
    required this.icon,
    required this.text,
    this.color = AppColors.primary,
    this.backgroundColor = AppColors.primarySoft,
    this.borderColor = const Color(0xFFD4E4FB),
  });

  final IconData icon;
  final String text;
  final Color color;
  final Color backgroundColor;
  final Color borderColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 14),
      decoration: BoxDecoration(
        color: backgroundColor,
        border: Border.all(color: borderColor),
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 21),
          const SizedBox(width: 11),
          Expanded(
            child: Text(
              text,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: color,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class TinyTag extends StatelessWidget {
  const TinyTag({
    super.key,
    required this.label,
    this.color = AppColors.primary,
    this.backgroundColor = AppColors.primarySoft,
  });

  final String label;
  final Color color;
  final Color backgroundColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w700,
          fontSize: 11,
        ),
      ),
    );
  }
}
