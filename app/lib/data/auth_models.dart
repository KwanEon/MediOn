class AuthUser {
  const AuthUser({
    required this.id,
    required this.username,
    required this.name,
    required this.email,
    required this.phoneNumber,
    required this.address,
    required this.latitude,
    required this.longitude,
  });

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      id: (json['id'] as num).toInt(),
      username: json['username'] as String,
      name: json['name'] as String,
      email: json['email'] as String,
      phoneNumber: json['phoneNumber'] as String,
      address: json['address'] as String,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
    );
  }

  final int id;
  final String username;
  final String name;
  final String email;
  final String phoneNumber;
  final String address;
  final double latitude;
  final double longitude;
}

class AddressSearchResult {
  const AddressSearchResult({
    required this.address,
    required this.roadAddress,
    required this.jibunAddress,
    required this.latitude,
    required this.longitude,
  });

  factory AddressSearchResult.fromJson(Map<String, dynamic> json) {
    return AddressSearchResult(
      address: json['address'] as String,
      roadAddress: json['roadAddress'] as String?,
      jibunAddress: json['jibunAddress'] as String?,
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
    );
  }

  final String address;
  final String? roadAddress;
  final String? jibunAddress;
  final double latitude;
  final double longitude;

  String get displayAddress => roadAddress ?? address;
}

class RegistrationResult {
  const RegistrationResult({required this.username});

  final String username;
}

class AuthApiException implements Exception {
  const AuthApiException(this.message);

  final String message;

  @override
  String toString() => message;
}
