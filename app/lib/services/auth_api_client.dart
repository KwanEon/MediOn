import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../data/auth_models.dart';
import '../data/content_models.dart';
import '../data/institution_models.dart';
import 'auth_http_client.dart'
    if (dart.library.js_interop) 'auth_http_client_web.dart';

class AuthApiClient {
  AuthApiClient({http.Client? client, String? baseUrl})
    : _client = client ?? createAuthHttpClient(),
      _baseUrl = _normalizeBaseUrl(baseUrl ?? _defaultBaseUrl());

  final http.Client _client;
  final String _baseUrl;
  String? _sessionCookie;

  Future<AuthUser?> currentUser() async {
    final response = await _get('/api/v1/auth/me');
    if (response.statusCode == 401) return null;
    return AuthUser.fromJson(_decodeObject(response));
  }

  Future<AuthUser> login({
    required String username,
    required String password,
  }) async {
    final response = await _post('/api/v1/auth/login', {
      'username': username.trim(),
      'password': password,
    });
    return AuthUser.fromJson(_decodeObject(response));
  }

  Future<AuthUser> register({
    required String username,
    required String password,
    required String name,
    required String email,
    required String phoneNumber,
    required String address,
  }) async {
    final response = await _post('/api/v1/auth/register', {
      'username': username.trim(),
      'password': password,
      'name': name.trim(),
      'email': email.trim(),
      'phoneNumber': phoneNumber.trim(),
      'address': address.trim(),
    });
    return AuthUser.fromJson(_decodeObject(response));
  }

  Future<List<AddressSearchResult>> searchAddresses(String query) async {
    final response = await _get(
      '/api/v1/auth/addresses',
      queryParameters: {'query': query.trim()},
    );
    final decoded = _decode(response);
    if (decoded is! List<dynamic>) {
      throw const AuthApiException('주소 검색 결과를 확인하지 못했습니다.');
    }
    return decoded
        .map(
          (item) => AddressSearchResult.fromJson(item as Map<String, dynamic>),
        )
        .toList();
  }

  Future<AuthUser> updateAddress(String address) async {
    final response = await _patch('/api/v1/auth/me/address', {
      'address': address.trim(),
    });
    return AuthUser.fromJson(_decodeObject(response));
  }

  Future<AuthUser> updateProfile({
    required String name,
    required String email,
    required String address,
  }) async {
    final response = await _patch('/api/v1/auth/me', {
      'name': name.trim(),
      'email': email.trim(),
      'address': address.trim(),
    });
    return AuthUser.fromJson(_decodeObject(response));
  }

  Future<void> logout() async {
    final response = await _post('/api/v1/auth/logout');
    _ensureSuccess(response);
    _sessionCookie = null;
  }

  Future<NearbyInstitutionResult> searchNearbyInstitutions({
    required double latitude,
    required double longitude,
    required int radiusMeters,
    required String keyword,
    required List<String> types,
    required String hospitalDepartment,
    required String operatingSchedule,
    required bool openNowOnly,
    required int page,
  }) async {
    final response = await _get(
      '/api/v1/institutions/nearby',
      queryParameters: {
        'lat': latitude.toString(),
        'lng': longitude.toString(),
        'radiusMeters': radiusMeters.toString(),
        if (keyword.trim().isNotEmpty) 'keyword': keyword.trim(),
        'types': types.join(','),
        if (hospitalDepartment != 'ALL')
          'hospitalDepartment': hospitalDepartment,
        'operatingSchedule': operatingSchedule,
        'openNowOnly': openNowOnly.toString(),
        'page': page.toString(),
        'size': '30',
      },
    );
    return NearbyInstitutionResult.fromJson(_decodeObject(response));
  }

  Future<Map<int, int>> getEmergencyBedAvailability(
    List<int> institutionIds,
  ) async {
    if (institutionIds.isEmpty) return const {};

    final response = await _post('/api/v1/institutions/emergency-beds', {
      'institutionIds': institutionIds,
    });
    final decoded = _decodeObject(response);
    final availableBeds =
        decoded['availableBeds'] as Map<String, dynamic>? ?? const {};
    final result = <int, int>{};
    for (final entry in availableBeds.entries) {
      final institutionId = int.tryParse(entry.key);
      final availableBedCount = entry.value;
      if (institutionId != null && availableBedCount is num) {
        result[institutionId] = availableBedCount.toInt();
      }
    }
    return result;
  }

  Future<Set<int>> getFavoriteInstitutionIds() async {
    final response = await _get('/api/v1/favorites');
    final decoded = _decode(response);
    if (decoded is! List<dynamic>) {
      throw const AuthApiException('즐겨찾기 정보를 확인하지 못했습니다.');
    }
    return decoded.whereType<num>().map((id) => id.toInt()).toSet();
  }

  Future<void> addFavoriteInstitution(int institutionId) async {
    final response = await _put('/api/v1/favorites/$institutionId');
    _ensureSuccess(response);
  }

  Future<void> removeFavoriteInstitution(int institutionId) async {
    final response = await _delete('/api/v1/favorites/$institutionId');
    _ensureSuccess(response);
  }

  Future<List<NoticeItem>> getNotices() async {
    final response = await _get('/api/v1/notices');
    final decoded = _decode(response);
    if (decoded is! List<dynamic>) {
      throw const AuthApiException('공지사항을 확인하지 못했습니다.');
    }
    return decoded
        .map((item) => NoticeItem.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<InquiryItem>> getMyInquiries() async {
    final response = await _get('/api/v1/inquiries/me');
    final decoded = _decode(response);
    if (decoded is! List<dynamic>) {
      throw const AuthApiException('문의 내역을 확인하지 못했습니다.');
    }
    return decoded
        .map((item) => InquiryItem.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<InquiryItem> createInquiry({
    required String category,
    required String title,
    required String content,
  }) async {
    final response = await _post('/api/v1/inquiries', {
      'category': category,
      'title': title.trim(),
      'content': content.trim(),
    });
    return InquiryItem.fromJson(_decodeObject(response));
  }

  Future<void> deleteMyInquiry(int inquiryId) async {
    final response = await _delete('/api/v1/inquiries/$inquiryId');
    _ensureSuccess(response);
  }

  void close() {
    _client.close();
  }

  Future<http.Response> _get(
    String path, {
    Map<String, String>? queryParameters,
  }) {
    return _request('GET', path, queryParameters: queryParameters);
  }

  Future<http.Response> _post(String path, [Map<String, dynamic>? body]) {
    return _request('POST', path, body: body);
  }

  Future<http.Response> _patch(String path, Map<String, dynamic> body) {
    return _request('PATCH', path, body: body);
  }

  Future<http.Response> _put(String path) {
    return _request('PUT', path);
  }

  Future<http.Response> _delete(String path) {
    return _request('DELETE', path);
  }

  Future<http.Response> _request(
    String method,
    String path, {
    Map<String, String>? queryParameters,
    Map<String, dynamic>? body,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl$path',
    ).replace(queryParameters: queryParameters);
    final request = http.Request(method, uri)
      ..headers.addAll({
        'Accept': 'application/json',
        if (body != null) 'Content-Type': 'application/json',
        'Cookie': ?_sessionCookie,
      });
    if (body != null) {
      request.body = jsonEncode(body);
    }

    try {
      final streamedResponse = await _client
          .send(request)
          .timeout(const Duration(seconds: 15));
      final response = await http.Response.fromStream(streamedResponse);
      _captureSessionCookie(response);
      return response;
    } on TimeoutException {
      throw const AuthApiException('서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.');
    } on AuthApiException {
      rethrow;
    } catch (_) {
      throw const AuthApiException('서버에 연결하지 못했습니다. 백엔드 실행 상태와 주소를 확인해 주세요.');
    }
  }

  Map<String, dynamic> _decodeObject(http.Response response) {
    final decoded = _decode(response);
    if (decoded is! Map<String, dynamic>) {
      throw const AuthApiException('서버 응답을 확인하지 못했습니다.');
    }
    return decoded;
  }

  dynamic _decode(http.Response response) {
    _ensureSuccess(response);
    if (response.bodyBytes.isEmpty) return null;
    try {
      return jsonDecode(utf8.decode(response.bodyBytes));
    } on FormatException {
      throw const AuthApiException('서버 응답 형식이 올바르지 않습니다.');
    }
  }

  void _ensureSuccess(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) return;

    String? message;
    try {
      final decoded =
          jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
      final fieldErrors = decoded['fieldErrors'];
      if (fieldErrors is List<dynamic> && fieldErrors.isNotEmpty) {
        message =
            (fieldErrors.first as Map<String, dynamic>)['message'] as String?;
      }
      message ??= decoded['message'] as String?;
    } catch (_) {
      // 서버가 JSON 오류 본문을 반환하지 않으면 상태 코드별 기본 문구를 사용합니다.
    }

    if (response.statusCode == 401) {
      throw AuthApiException(message ?? '아이디 또는 비밀번호가 올바르지 않습니다.');
    }
    throw AuthApiException(message ?? '요청을 처리하지 못했습니다.');
  }

  void _captureSessionCookie(http.Response response) {
    final rawCookie = response.headers['set-cookie'];
    if (rawCookie == null) return;
    final match = RegExp(r'JSESSIONID=[^;,\s]+').firstMatch(rawCookie);
    if (match != null) {
      _sessionCookie = match.group(0);
    }
  }

  static String _defaultBaseUrl() {
    const configured = String.fromEnvironment('API_BASE_URL');
    if (configured.isNotEmpty) return configured;
    if (kIsWeb) return 'http://localhost:8080';
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static String _normalizeBaseUrl(String value) {
    return value.endsWith('/') ? value.substring(0, value.length - 1) : value;
  }
}
