import 'package:shared_preferences/shared_preferences.dart';

class AppPreferences {
  AppPreferences();

  static const _healthNoticeKey = 'health_notice_enabled';
  static const _locationSearchKey = 'location_search_enabled';
  static const _lastHealthNoticeIdKey = 'last_health_notice_id';

  SharedPreferences? _preferences;

  Future<SharedPreferences> _store() async {
    return _preferences ??= await SharedPreferences.getInstance();
  }

  Future<bool> healthNoticeEnabled() async {
    return (await _store()).getBool(_healthNoticeKey) ?? false;
  }

  Future<void> setHealthNoticeEnabled(bool enabled) async {
    await (await _store()).setBool(_healthNoticeKey, enabled);
  }

  Future<bool> locationSearchEnabled() async {
    return (await _store()).getBool(_locationSearchKey) ?? true;
  }

  Future<void> setLocationSearchEnabled(bool enabled) async {
    await (await _store()).setBool(_locationSearchKey, enabled);
  }

  Future<int?> lastHealthNoticeId() async {
    return (await _store()).getInt(_lastHealthNoticeIdKey);
  }

  Future<void> setLastHealthNoticeId(int noticeId) async {
    await (await _store()).setInt(_lastHealthNoticeIdKey, noticeId);
  }
}
