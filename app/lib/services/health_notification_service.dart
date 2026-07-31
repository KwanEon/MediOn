import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class HealthNotificationService {
  HealthNotificationService({FlutterLocalNotificationsPlugin? plugin})
    : _plugin = plugin ?? FlutterLocalNotificationsPlugin();

  static const _notificationId = 7101;
  static const _channelId = 'medion_health_information';
  static const _channelName = '건강 정보 알림';
  static const _channelDescription = '새 건강 가이드와 중요 공지를 알려드립니다.';

  final FlutterLocalNotificationsPlugin _plugin;
  bool _initialized = false;

  bool get _supportsNotifications {
    return !kIsWeb && defaultTargetPlatform == TargetPlatform.android;
  }

  Future<void> initialize() async {
    if (_initialized || !_supportsNotifications) return;
    const settings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
    );
    await _plugin.initialize(settings: settings);
    _initialized = true;
  }

  Future<bool> requestPermission() async {
    if (!_supportsNotifications) return false;
    await initialize();
    final android = _plugin
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >();
    return await android?.requestNotificationsPermission() ?? true;
  }

  Future<void> show({required String title, required String body}) async {
    if (!_supportsNotifications) return;
    await initialize();
    const details = NotificationDetails(
      android: AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: _channelDescription,
        importance: Importance.high,
        priority: Priority.high,
      ),
    );
    await _plugin.show(
      id: _notificationId,
      title: title,
      body: body,
      notificationDetails: details,
    );
  }

  Future<void> cancel() async {
    if (!_supportsNotifications) return;
    await initialize();
    await _plugin.cancel(id: _notificationId);
  }
}
