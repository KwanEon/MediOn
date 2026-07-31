enum InstitutionKind { hospital, pharmacy, emergency }

class Institution {
  const Institution({
    required this.id,
    required this.name,
    required this.kind,
    required this.institutionKind,
    required this.distanceMeters,
    required this.address,
    required this.phone,
    required this.latitude,
    required this.longitude,
    required this.isOpen,
    required this.operatingHoursKnown,
    required this.todayOpenTime,
    required this.todayCloseTime,
    required this.availableEmergencyBeds,
    required this.emergencyBedAvailabilityLoading,
    required this.operatingSchedules,
    required this.lastSyncedAt,
    this.departments = const [],
  });

  factory Institution.fromJson(Map<String, dynamic> json) {
    return Institution(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      kind: _institutionKindFromApiValue(json['type'] as String),
      institutionKind: json['institutionKind'] as String?,
      distanceMeters: (json['distanceMeters'] as num).toInt(),
      address: json['roadAddress'] as String? ?? '주소 정보 없음',
      phone: json['phoneNumber'] as String? ?? '전화번호 정보 없음',
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      isOpen: json['open'] as bool? ?? false,
      operatingHoursKnown: json['operatingHoursKnown'] as bool? ?? false,
      todayOpenTime: json['todayOpenTime'] as String?,
      todayCloseTime: json['todayCloseTime'] as String?,
      availableEmergencyBeds: (json['availableEmergencyBeds'] as num?)?.toInt(),
      emergencyBedAvailabilityLoading: false,
      operatingSchedules:
          (json['operatingSchedules'] as List<dynamic>? ?? const [])
              .whereType<String>()
              .toList(growable: false),
      lastSyncedAt: json['lastSyncedAt'] as String?,
      departments: (json['medicalDepartments'] as List<dynamic>? ?? const [])
          .whereType<String>()
          .map((department) => department.trim())
          .where((department) => department.isNotEmpty)
          .toList(growable: false),
    );
  }

  final int id;
  final String name;
  final InstitutionKind kind;
  final String? institutionKind;
  final int distanceMeters;
  final String address;
  final String phone;
  final double latitude;
  final double longitude;
  final bool isOpen;
  final bool operatingHoursKnown;
  final String? todayOpenTime;
  final String? todayCloseTime;
  final int? availableEmergencyBeds;
  final bool emergencyBedAvailabilityLoading;
  final List<String> operatingSchedules;
  final String? lastSyncedAt;
  final List<String> departments;

  Institution withEmergencyBedAvailability({
    required bool loading,
    required int? availableBeds,
  }) {
    return Institution(
      id: id,
      name: name,
      kind: kind,
      institutionKind: institutionKind,
      distanceMeters: distanceMeters,
      address: address,
      phone: phone,
      latitude: latitude,
      longitude: longitude,
      isOpen: isOpen,
      operatingHoursKnown: operatingHoursKnown,
      todayOpenTime: todayOpenTime,
      todayCloseTime: todayCloseTime,
      availableEmergencyBeds: availableBeds,
      emergencyBedAvailabilityLoading: loading,
      operatingSchedules: operatingSchedules,
      lastSyncedAt: lastSyncedAt,
      departments: departments,
    );
  }

  String get typeLabel => switch (kind) {
    InstitutionKind.hospital =>
      departments.isNotEmpty
          ? departments.join(' · ')
          : institutionKind?.trim().isNotEmpty == true
          ? institutionKind!.trim()
          : '병원',
    InstitutionKind.pharmacy => '약국',
    InstitutionKind.emergency => '응급실',
  };

  String get distanceLabel {
    final distance = distanceMeters;
    if (distance < 1000) return '${distance}m';
    return '${(distance / 1000).toStringAsFixed(1)}km';
  }

  String get distanceSummary => distanceLabel;

  String get hours {
    if (kind == InstitutionKind.emergency) {
      return '24시간 응급 진료';
    }
    if (!operatingHoursKnown) return '운영 시간 정보 없음';
    if (todayOpenTime == null || todayCloseTime == null) return '오늘 휴무';
    return '오늘 ${_formatTime(todayOpenTime!)} - ${_formatTime(todayCloseTime!)}';
  }

  String get emergencyBedLabel {
    if (emergencyBedAvailabilityLoading) return '불러오는 중';
    if (availableEmergencyBeds == null) return '정보 없음';
    return '$availableEmergencyBeds개';
  }

  String get emergencyBedSummary => '실시간 가용 병상 $emergencyBedLabel';

  static String _formatTime(String value) {
    return value.length >= 5 ? value.substring(0, 5) : value;
  }
}

InstitutionKind _institutionKindFromApiValue(String value) {
  return switch (value) {
    'HOSPITAL' => InstitutionKind.hospital,
    'PHARMACY' => InstitutionKind.pharmacy,
    'EMERGENCY_ROOM' => InstitutionKind.emergency,
    _ => throw FormatException('지원하지 않는 의료기관 유형입니다: $value'),
  };
}

class NearbyInstitutionResult {
  const NearbyInstitutionResult({
    required this.items,
    required this.pageNumber,
    required this.pageSize,
    required this.totalElements,
    required this.totalPages,
    required this.hospitalCount,
    required this.pharmacyCount,
    required this.emergencyRoomCount,
    required this.radiusMeters,
    required this.lastSyncedAt,
  });

  factory NearbyInstitutionResult.fromJson(Map<String, dynamic> json) {
    final page = json['page'] as Map<String, dynamic>? ?? const {};
    final typeCounts = json['typeCounts'] as Map<String, dynamic>? ?? const {};
    return NearbyInstitutionResult(
      items: (json['items'] as List<dynamic>? ?? const [])
          .map((item) => Institution.fromJson(item as Map<String, dynamic>))
          .toList(growable: false),
      pageNumber: (page['number'] as num?)?.toInt() ?? 0,
      pageSize: (page['size'] as num?)?.toInt() ?? 0,
      totalElements: (page['totalElements'] as num?)?.toInt() ?? 0,
      totalPages: (page['totalPages'] as num?)?.toInt() ?? 0,
      hospitalCount: (typeCounts['HOSPITAL'] as num?)?.toInt() ?? 0,
      pharmacyCount: (typeCounts['PHARMACY'] as num?)?.toInt() ?? 0,
      emergencyRoomCount: (typeCounts['EMERGENCY_ROOM'] as num?)?.toInt() ?? 0,
      radiusMeters: (json['radiusMeters'] as num?)?.toInt() ?? 0,
      lastSyncedAt: json['lastSyncedAt'] as String?,
    );
  }

  final List<Institution> items;
  final int pageNumber;
  final int pageSize;
  final int totalElements;
  final int totalPages;
  final int hospitalCount;
  final int pharmacyCount;
  final int emergencyRoomCount;
  final int radiusMeters;
  final String? lastSyncedAt;
}
