import 'package:flutter/material.dart';

import 'navigation/main_shell.dart';
import 'theme/app_theme.dart';

class MediOnApp extends StatelessWidget {
  const MediOnApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '메디온',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: const MainShell(),
    );
  }
}
