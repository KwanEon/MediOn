import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';

import '../constants/institution_icons.dart';
import '../data/auth_models.dart';
import '../data/institution_models.dart';
import '../services/auth_api_client.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';
import 'address_search_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({
    super.key,
    required this.authApi,
    required this.user,
    required this.favoriteIds,
    required this.onFavoriteToggle,
    required this.onOpenInstitution,
    required this.savedAddress,
    required this.locationSearchEnabled,
    this.initialCategory = '전체',
  });

  final AuthApiClient authApi;
  final AuthUser? user;
  final Set<int> favoriteIds;
  final Future<void> Function(Institution) onFavoriteToggle;
  final ValueChanged<Institution> onOpenInstitution;
  final String savedAddress;
  final bool locationSearchEnabled;
  final String initialCategory;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final TextEditingController _searchController = TextEditingController();
  Timer? _searchDebounce;

  late String _selectedCategory;
  String _selectedDepartment = 'ALL';
  String _operatingSchedule = 'ALL';
  int _radiusMeters = 3000;
  bool _openNowOnly = true;
  bool _favoritesOnly = false;
  bool _loading = false;
  bool _locating = false;
  String _query = '';
  String _error = '';
  int _requestSequence = 0;
  int _locationRequestSequence = 0;
  int _totalElements = 0;
  int _currentPage = 0;
  int _totalPages = 0;
  int _hospitalCount = 0;
  int _pharmacyCount = 0;
  int _emergencyRoomCount = 0;
  int? _selectedInstitutionId;
  _SearchLocation? _location;
  List<Institution> _institutions = const [];

  List<Institution> get _visibleInstitutions {
    final results = List<Institution>.of(_institutions);

    results.sort(
      (left, right) => left.distanceMeters.compareTo(right.distanceMeters),
    );
    return results;
  }

  @override
  void initState() {
    super.initState();
    _selectedCategory = widget.initialCategory;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        unawaited(_initializeLocation());
      }
    });
  }

  @override
  void didUpdateWidget(covariant HomePage oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.initialCategory != widget.initialCategory) {
      setState(() {
        _selectedCategory = widget.initialCategory;
        if (_selectedCategory != '병원') {
          _selectedDepartment = 'ALL';
        }
      });
      unawaited(_loadInstitutions(page: 0));
    }

    final oldUser = oldWidget.user;
    final newUser = widget.user;
    final accountLocationChanged =
        oldUser?.id != newUser?.id ||
        oldUser?.latitude != newUser?.latitude ||
        oldUser?.longitude != newUser?.longitude;
    if (accountLocationChanged && newUser != null) {
      _setAccountLocation(newUser);
    } else if (oldUser != null && newUser == null) {
      setState(() => _favoritesOnly = false);
      unawaited(_useCurrentLocation(showDisabledMessage: false));
    }

    if (oldWidget.locationSearchEnabled != widget.locationSearchEnabled) {
      if (widget.locationSearchEnabled) {
        unawaited(_useCurrentLocation());
      } else if (widget.user != null) {
        _setAccountLocation(widget.user!);
      }
    }
  }

  Future<void> _initializeLocation() async {
    if (widget.user != null) {
      _setAccountLocation(widget.user!);
      return;
    }
    await _useCurrentLocation(showDisabledMessage: false);
  }

  void _setAccountLocation(AuthUser user) {
    _locationRequestSequence += 1;
    setState(() {
      _location = _SearchLocation(
        latitude: user.latitude,
        longitude: user.longitude,
        label: '내 주소 · ${user.address}',
      );
      _selectedInstitutionId = null;
      _error = '';
      _locating = false;
    });
    unawaited(_loadInstitutions(page: 0));
  }

  Future<void> _useCurrentLocation({bool showDisabledMessage = true}) async {
    FocusScope.of(context).unfocus();
    if (!widget.locationSearchEnabled) {
      if (showDisabledMessage) {
        _showMessage('마이페이지에서 위치 기반 검색을 켜 주세요.');
      }
      return;
    }

    setState(() {
      _locating = true;
      _error = '';
    });
    final locationRequestSequence = ++_locationRequestSequence;

    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        throw const _LocationSearchException('기기의 위치 서비스를 켜 주세요.');
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied) {
        throw const _LocationSearchException('현재 위치를 사용하려면 위치 권한이 필요합니다.');
      }
      if (permission == LocationPermission.deniedForever) {
        throw const _LocationSearchException('앱 설정에서 메디온의 위치 권한을 허용해 주세요.');
      }

      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 15),
        ),
      );
      if (!mounted || locationRequestSequence != _locationRequestSequence) {
        return;
      }
      setState(() {
        _location = _SearchLocation(
          latitude: position.latitude,
          longitude: position.longitude,
          label: '현재 위치',
        );
        _selectedInstitutionId = null;
      });
      await _loadInstitutions(page: 0);
    } on _LocationSearchException catch (error) {
      if (!mounted || locationRequestSequence != _locationRequestSequence) {
        return;
      }
      setState(() => _error = error.message);
      if (showDisabledMessage) {
        _showMessage(error.message);
      }
    } on TimeoutException {
      if (!mounted || locationRequestSequence != _locationRequestSequence) {
        return;
      }
      const message = '현재 위치를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.';
      setState(() => _error = message);
      if (showDisabledMessage) {
        _showMessage(message);
      }
    } catch (_) {
      if (!mounted || locationRequestSequence != _locationRequestSequence) {
        return;
      }
      const message = '현재 위치를 확인하는 중 문제가 발생했습니다.';
      setState(() => _error = message);
      if (showDisabledMessage) {
        _showMessage(message);
      }
    } finally {
      if (mounted && locationRequestSequence == _locationRequestSequence) {
        setState(() => _locating = false);
      }
    }
  }

  Future<void> _openAddressSearch() async {
    FocusScope.of(context).unfocus();
    final selectedAddress = await Navigator.of(context)
        .push<AddressSearchResult>(
          MaterialPageRoute(
            builder: (_) => AddressSearchPage(
              authApi: widget.authApi,
              initialQuery: widget.user?.address ?? widget.savedAddress,
            ),
          ),
        );
    if (!mounted || selectedAddress == null) return;

    _locationRequestSequence += 1;
    setState(() {
      _location = _SearchLocation(
        latitude: selectedAddress.latitude,
        longitude: selectedAddress.longitude,
        label: '주소 검색 · ${selectedAddress.displayAddress}',
      );
      _selectedInstitutionId = null;
      _error = '';
    });
    await _loadInstitutions(page: 0);
  }

  Future<void> _loadInstitutions({int? page}) async {
    final location = _location;
    if (location == null) return;

    final requestedPage = page ?? _currentPage;
    final requestSequence = ++_requestSequence;
    setState(() {
      _loading = true;
      _error = '';
    });

    try {
      final result = await widget.authApi.searchNearbyInstitutions(
        latitude: location.latitude,
        longitude: location.longitude,
        radiusMeters: _radiusMeters,
        keyword: _query,
        types: _categoryTypes[_selectedCategory] ?? _categoryTypes['전체']!,
        hospitalDepartment: _selectedDepartment,
        operatingSchedule: _operatingSchedule,
        openNowOnly: _openNowOnly,
        favoritesOnly: _favoritesOnly,
        page: requestedPage,
      );
      if (!mounted || requestSequence != _requestSequence) return;
      final institutions = result.items
          .map(
            (institution) => institution.kind == InstitutionKind.emergency
                ? institution.withEmergencyBedAvailability(
                    loading: true,
                    availableBeds: null,
                  )
                : institution,
          )
          .toList(growable: false);
      setState(() {
        _institutions = institutions;
        _currentPage = result.pageNumber;
        _totalElements = result.totalElements;
        _totalPages = result.totalPages;
        _hospitalCount = result.hospitalCount;
        _pharmacyCount = result.pharmacyCount;
        _emergencyRoomCount = result.emergencyRoomCount;
        _selectedInstitutionId = null;
      });
      final emergencyInstitutionIds = institutions
          .where((institution) => institution.kind == InstitutionKind.emergency)
          .map((institution) => institution.id)
          .toList(growable: false);
      if (emergencyInstitutionIds.isNotEmpty) {
        unawaited(
          _loadEmergencyBedAvailability(
            requestSequence,
            emergencyInstitutionIds,
          ),
        );
      }
    } on AuthApiException catch (error) {
      if (!mounted || requestSequence != _requestSequence) return;
      setState(() {
        _institutions = const [];
        _totalElements = 0;
        _totalPages = 0;
        _hospitalCount = 0;
        _pharmacyCount = 0;
        _emergencyRoomCount = 0;
        _error = error.message;
      });
    } finally {
      if (mounted && requestSequence == _requestSequence) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _loadEmergencyBedAvailability(
    int requestSequence,
    List<int> institutionIds,
  ) async {
    Map<int, int> availableBeds = const {};
    try {
      availableBeds = await widget.authApi.getEmergencyBedAvailability(
        institutionIds,
      );
    } on AuthApiException {
      // 의료기관 목록은 유지하고 병상 정보만 '정보 없음'으로 표시합니다.
    }
    if (!mounted || requestSequence != _requestSequence) return;

    final updatedInstitutions = _institutions
        .map(
          (institution) => institution.kind == InstitutionKind.emergency
              ? institution.withEmergencyBedAvailability(
                  loading: false,
                  availableBeds: availableBeds[institution.id],
                )
              : institution,
        )
        .toList(growable: false);
    setState(() => _institutions = updatedInstitutions);
  }

  void _setCategory(String category) {
    setState(() {
      _selectedCategory = category;
      _selectedInstitutionId = null;
      if (category != '병원') {
        _selectedDepartment = 'ALL';
      }
    });
    unawaited(_loadInstitutions(page: 0));
  }

  void _onSearchChanged(String value) {
    _searchDebounce?.cancel();
    if (value.trim().isEmpty) {
      _applySearch('');
      return;
    }
    _searchDebounce = Timer(
      const Duration(milliseconds: 400),
      () => _applySearch(value),
    );
  }

  void _submitSearch(String value) {
    _searchDebounce?.cancel();
    FocusScope.of(context).unfocus();
    _applySearch(value);
  }

  void _applySearch(String value) {
    if (!mounted) return;
    final normalizedQuery = value.trim();
    if (_query == normalizedQuery) return;
    setState(() {
      _query = normalizedQuery;
      _selectedInstitutionId = null;
    });
    unawaited(_loadInstitutions(page: 0));
  }

  void _setOpenNowOnly(bool selected) {
    setState(() {
      _openNowOnly = selected;
      _selectedInstitutionId = null;
    });
    unawaited(_loadInstitutions(page: 0));
  }

  void _setFavoritesOnly(bool selected) {
    if (widget.user == null) {
      _showMessage('내 즐겨찾기는 로그인 후 사용할 수 있습니다.');
      return;
    }
    setState(() {
      _favoritesOnly = selected;
      _selectedInstitutionId = null;
    });
    unawaited(_loadInstitutions(page: 0));
  }

  Future<void> _toggleFavorite(Institution institution) async {
    await widget.onFavoriteToggle(institution);
    if (mounted && _favoritesOnly) {
      await _loadInstitutions(page: 0);
    }
  }

  Future<void> _selectDepartment() async {
    final selected = await _showFilterSheet<String>(
      title: '진료과목',
      selectedValue: _selectedDepartment,
      options: _departmentOptions,
    );
    if (!mounted || selected == null || selected == _selectedDepartment) return;
    setState(() {
      _selectedDepartment = selected;
      _selectedCategory = '병원';
      _selectedInstitutionId = null;
    });
    unawaited(_loadInstitutions(page: 0));
  }

  Future<void> _selectRadius() async {
    final selected = await _showFilterSheet<int>(
      title: '검색 반경',
      selectedValue: _radiusMeters,
      options: _radiusOptions,
    );
    if (!mounted || selected == null || selected == _radiusMeters) return;
    setState(() {
      _radiusMeters = selected;
      _selectedInstitutionId = null;
    });
    unawaited(_loadInstitutions(page: 0));
  }

  Future<void> _selectOperatingSchedule() async {
    final selected = await _showFilterSheet<String>(
      title: '진료 시간',
      selectedValue: _operatingSchedule,
      options: _scheduleOptions,
    );
    if (!mounted || selected == null || selected == _operatingSchedule) return;
    setState(() {
      _operatingSchedule = selected;
      _selectedInstitutionId = null;
    });
    unawaited(_loadInstitutions(page: 0));
  }

  void _goToPage(int page) {
    if (_loading || page < 0 || page >= _totalPages) return;
    unawaited(_loadInstitutions(page: page));
  }

  Future<T?> _showFilterSheet<T>({
    required String title,
    required T selectedValue,
    required List<_FilterOption<T>> options,
  }) {
    return showModalBottomSheet<T>(
      context: context,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (context) {
        return SafeArea(
          top: false,
          child: ConstrainedBox(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.sizeOf(context).height * 0.72,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(22, 0, 22, 10),
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                ),
                Flexible(
                  child: ListView.builder(
                    shrinkWrap: true,
                    padding: const EdgeInsets.only(bottom: 12),
                    itemCount: options.length,
                    itemBuilder: (context, index) {
                      final option = options[index];
                      final selected = option.value == selectedValue;
                      return ListTile(
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 22,
                        ),
                        title: Text(option.label),
                        trailing: selected
                            ? const Icon(
                                Icons.check_rounded,
                                color: AppColors.primary,
                              )
                            : null,
                        selected: selected,
                        selectedColor: AppColors.primary,
                        onTap: () => Navigator.of(context).pop(option.value),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  void dispose() {
    _requestSequence += 1;
    _locationRequestSequence += 1;
    _searchDebounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final visibleInstitutions = _visibleInstitutions;
    Institution? selectedInstitution;
    for (final institution in visibleInstitutions) {
      if (institution.id == _selectedInstitutionId) {
        selectedInstitution = institution;
        break;
      }
    }

    return ScreenFrame(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeading(
            title: '내 주변 의료기관을\n찾아보세요',
            subtitle: '병원, 약국, 응급실 정보를 실제 데이터로 확인하세요.',
          ),
          const SizedBox(height: 18),
          _LocationCard(
            locationLabel: _location?.label ?? '검색할 위치를 선택해 주세요.',
            locating: _locating,
          ),
          const SizedBox(height: 14),
          AppSearchField(
            hintText: '병원명, 약국명, 주소로 검색',
            controller: _searchController,
            onChanged: _onSearchChanged,
            onSubmitted: _submitSearch,
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: _locating || !widget.locationSearchEnabled
                      ? null
                      : () => unawaited(_useCurrentLocation()),
                  icon: _locating
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.my_location_rounded, size: 19),
                  label: Text(
                    !widget.locationSearchEnabled
                        ? '위치 검색 꺼짐'
                        : _locating
                        ? '위치 확인 중'
                        : '내 위치로 찾기',
                  ),
                ),
              ),
              const SizedBox(width: 9),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _openAddressSearch,
                  icon: const Icon(Icons.map_outlined, size: 19),
                  label: const Text('주소로 찾기'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            physics: const BouncingScrollPhysics(),
            child: Row(
              children: _categoryTypes.keys
                  .map(
                    (category) => Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: MedionChip(
                        label: category,
                        selected: _selectedCategory == category,
                        icon: switch (category) {
                          '병원' => institutionIconFor(InstitutionKind.hospital),
                          '약국' => institutionIconFor(InstitutionKind.pharmacy),
                          '응급실' => institutionIconFor(
                            InstitutionKind.emergency,
                          ),
                          _ => null,
                        },
                        foregroundColor: switch (category) {
                          '병원' => AppColors.primary,
                          '약국' => AppColors.green,
                          '응급실' => AppColors.red,
                          _ => null,
                        },
                        onTap: () => _setCategory(category),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
          const SizedBox(height: 12),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            physics: const BouncingScrollPhysics(),
            child: Row(
              children: [
                if (widget.user != null) ...[
                  _FilterPill(
                    icon: Icons.star_rounded,
                    label: '내 즐겨찾기',
                    selected: _favoritesOnly,
                    selectedColor: AppColors.amber,
                    showDropdownIndicator: false,
                    onTap: () => _setFavoritesOnly(!_favoritesOnly),
                  ),
                  const SizedBox(width: 8),
                ],
                _FilterPill(
                  icon: Icons.schedule_rounded,
                  label: '진료 중',
                  selected: _openNowOnly,
                  selectedColor: AppColors.green,
                  showDropdownIndicator: false,
                  onTap: () => _setOpenNowOnly(!_openNowOnly),
                ),
                const SizedBox(width: 8),
                _FilterPill(
                  icon: Icons.medical_services_outlined,
                  label: _labelFor(_departmentOptions, _selectedDepartment),
                  onTap: _selectDepartment,
                ),
                const SizedBox(width: 8),
                _FilterPill(
                  icon: Icons.location_on_outlined,
                  label: _labelFor(_radiusOptions, _radiusMeters),
                  onTap: _selectRadius,
                ),
                const SizedBox(width: 8),
                _FilterPill(
                  icon: Icons.calendar_month_outlined,
                  label: _labelFor(_scheduleOptions, _operatingSchedule),
                  onTap: _selectOperatingSchedule,
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          if (_loading) ...[
            const LinearProgressIndicator(minHeight: 3),
            const SizedBox(height: 12),
          ],
          if (_error.isNotEmpty) ...[
            _ErrorNotice(
              message: _error,
              onRetry: _location == null
                  ? () => unawaited(_useCurrentLocation())
                  : () => unawaited(_loadInstitutions()),
            ),
            const SizedBox(height: 14),
          ],
          if (_location == null)
            _LocationPending(onLocate: () => unawaited(_useCurrentLocation()))
          else
            _MedicalMap(
              center: _location!,
              institutions: visibleInstitutions,
              selectedInstitution: selectedInstitution,
              locating: _locating,
              onSelect: (institution) {
                setState(() => _selectedInstitutionId = institution.id);
              },
              onClearSelection: () {
                setState(() => _selectedInstitutionId = null);
              },
              onOpenInstitution: widget.onOpenInstitution,
              onLocate: widget.locationSearchEnabled
                  ? () => unawaited(_useCurrentLocation())
                  : null,
            ),
          const SizedBox(height: 14),
          _ResultsPanel(
            institutions: visibleInstitutions,
            totalElements: _totalElements,
            currentPage: _currentPage,
            totalPages: _totalPages,
            hospitalCount: _hospitalCount,
            pharmacyCount: _pharmacyCount,
            emergencyRoomCount: _emergencyRoomCount,
            loading: _loading,
            favoriteIds: widget.favoriteIds,
            canFavorite: widget.user != null,
            onFavoriteToggle: _toggleFavorite,
            onOpenInstitution: widget.onOpenInstitution,
            onPreviousPage: () => _goToPage(_currentPage - 1),
            onNextPage: () => _goToPage(_currentPage + 1),
            onPageRequested: _goToPage,
          ),
        ],
      ),
    );
  }
}

class _MedicalMap extends StatefulWidget {
  const _MedicalMap({
    required this.center,
    required this.institutions,
    required this.selectedInstitution,
    required this.locating,
    required this.onSelect,
    required this.onClearSelection,
    required this.onOpenInstitution,
    required this.onLocate,
  });

  final _SearchLocation center;
  final List<Institution> institutions;
  final Institution? selectedInstitution;
  final bool locating;
  final ValueChanged<Institution> onSelect;
  final VoidCallback onClearSelection;
  final ValueChanged<Institution> onOpenInstitution;
  final VoidCallback? onLocate;

  @override
  State<_MedicalMap> createState() => _MedicalMapState();
}

class _MedicalMapState extends State<_MedicalMap> {
  final MapController _mapController = MapController();
  bool _mapReady = false;

  @override
  void didUpdateWidget(covariant _MedicalMap oldWidget) {
    super.didUpdateWidget(oldWidget);
    final centerChanged =
        oldWidget.center.latitude != widget.center.latitude ||
        oldWidget.center.longitude != widget.center.longitude;
    final selectionChanged =
        oldWidget.selectedInstitution?.id != widget.selectedInstitution?.id;
    if (centerChanged || selectionChanged) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _moveToTarget());
    }
  }

  void _moveToTarget() {
    if (!_mapReady) return;
    final selected = widget.selectedInstitution;
    final target = selected == null
        ? LatLng(widget.center.latitude, widget.center.longitude)
        : LatLng(selected.latitude, selected.longitude);
    final zoom = selected == null
        ? _mapController.camera.zoom
        : _mapController.camera.zoom < 16
        ? 16.0
        : _mapController.camera.zoom;
    _mapController.move(target, zoom);
  }

  void _zoomBy(double delta) {
    if (!_mapReady) return;
    final camera = _mapController.camera;
    _mapController.move(
      camera.center,
      (camera.zoom + delta).clamp(3.0, 18.0).toDouble(),
    );
  }

  @override
  void dispose() {
    _mapController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final center = LatLng(widget.center.latitude, widget.center.longitude);
    return ClipRRect(
      borderRadius: BorderRadius.circular(18),
      child: Container(
        height: 340,
        decoration: BoxDecoration(
          color: const Color(0xFFE9EEF4),
          border: Border.all(color: AppColors.line),
          borderRadius: BorderRadius.circular(18),
        ),
        child: Stack(
          children: [
            FlutterMap(
              mapController: _mapController,
              options: MapOptions(
                initialCenter: center,
                initialZoom: 14,
                minZoom: 3,
                maxZoom: 18,
                onMapReady: () {
                  _mapReady = true;
                  _moveToTarget();
                },
                onTap: (_, _) => widget.onClearSelection(),
              ),
              children: [
                TileLayer(
                  urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                  userAgentPackageName: 'com.medion.medion_app',
                ),
                MarkerLayer(
                  markers: [
                    Marker(
                      point: center,
                      width: 42,
                      height: 42,
                      child: const _CurrentLocationMarker(),
                    ),
                    ...widget.institutions.map(
                      (institution) => Marker(
                        point: LatLng(
                          institution.latitude,
                          institution.longitude,
                        ),
                        width: 46,
                        height: 46,
                        child: _InstitutionMarker(
                          institution: institution,
                          selected:
                              widget.selectedInstitution?.id == institution.id,
                          onTap: () => widget.onSelect(institution),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
            Positioned(
              left: 10,
              top: 10,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.92),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  '© OpenStreetMap contributors',
                  style: TextStyle(
                    color: AppColors.muted,
                    fontSize: 10,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
            Positioned(
              right: 12,
              top: 12,
              child: Column(
                children: [
                  _MapControlButton(
                    tooltip: '확대',
                    icon: Icons.add_rounded,
                    onPressed: () => _zoomBy(1),
                  ),
                  const SizedBox(height: 7),
                  _MapControlButton(
                    tooltip: '축소',
                    icon: Icons.remove_rounded,
                    onPressed: () => _zoomBy(-1),
                  ),
                  const SizedBox(height: 7),
                  _MapControlButton(
                    tooltip: '현재 위치로 이동',
                    icon: widget.locating
                        ? Icons.hourglass_top_rounded
                        : Icons.my_location_rounded,
                    color: AppColors.primary,
                    onPressed: widget.locating ? null : widget.onLocate,
                  ),
                ],
              ),
            ),
            if (widget.selectedInstitution != null)
              Positioned(
                left: 12,
                right: 12,
                bottom: 12,
                child: _MapInstitutionCard(
                  institution: widget.selectedInstitution!,
                  onTap: () =>
                      widget.onOpenInstitution(widget.selectedInstitution!),
                  onClose: widget.onClearSelection,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _CurrentLocationMarker extends StatelessWidget {
  const _CurrentLocationMarker();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0x441769E8),
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Container(
        width: 17,
        height: 17,
        decoration: BoxDecoration(
          color: AppColors.primary,
          shape: BoxShape.circle,
          border: Border.all(color: Colors.white, width: 3),
          boxShadow: const [BoxShadow(color: Color(0x331769E8), blurRadius: 6)],
        ),
      ),
    );
  }
}

class _InstitutionMarker extends StatelessWidget {
  const _InstitutionMarker({
    required this.institution,
    required this.selected,
    required this.onTap,
  });

  final Institution institution;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final icon = institutionIconFor(institution.kind);
    final color = switch (institution.kind) {
      InstitutionKind.hospital => AppColors.primary,
      InstitutionKind.pharmacy => AppColors.green,
      InstitutionKind.emergency => AppColors.red,
    };
    final markerSize = selected ? 43.0 : 36.0;

    return GestureDetector(
      onTap: onTap,
      child: Center(
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 160),
          width: markerSize,
          height: markerSize,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: selected ? 4 : 3),
            boxShadow: const [
              BoxShadow(
                color: Color(0x40000000),
                blurRadius: 7,
                offset: Offset(0, 3),
              ),
            ],
          ),
          child: Icon(icon, color: Colors.white, size: selected ? 23 : 19),
        ),
      ),
    );
  }
}

class _MapControlButton extends StatelessWidget {
  const _MapControlButton({
    required this.tooltip,
    required this.icon,
    required this.onPressed,
    this.color = AppColors.ink,
  });

  final String tooltip;
  final IconData icon;
  final VoidCallback? onPressed;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      elevation: 2,
      shape: const CircleBorder(),
      child: IconButton(
        tooltip: tooltip,
        onPressed: onPressed,
        icon: Icon(icon, color: onPressed == null ? AppColors.muted : color),
        iconSize: 21,
        constraints: const BoxConstraints.tightFor(width: 42, height: 42),
      ),
    );
  }
}

class _MapInstitutionCard extends StatelessWidget {
  const _MapInstitutionCard({
    required this.institution,
    required this.onTap,
    required this.onClose,
  });

  final Institution institution;
  final VoidCallback onTap;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final mapStatus = institution.kind == InstitutionKind.emergency
        ? institution.emergencyBedSummary
        : institution.isOpen
        ? '진료 중'
        : '진료 종료';

    return Material(
      color: Colors.white,
      elevation: 4,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 11, 8, 11),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      institution.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${institution.typeLabel} · $mapStatus · '
                      '${institution.distanceSummary}',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: institution.isOpen
                            ? AppColors.green
                            : AppColors.muted,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      institution.address,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              IconButton(
                tooltip: '닫기',
                onPressed: onClose,
                icon: const Icon(Icons.close_rounded, size: 20),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ResultsPanel extends StatelessWidget {
  const _ResultsPanel({
    required this.institutions,
    required this.totalElements,
    required this.currentPage,
    required this.totalPages,
    required this.hospitalCount,
    required this.pharmacyCount,
    required this.emergencyRoomCount,
    required this.loading,
    required this.favoriteIds,
    required this.canFavorite,
    required this.onFavoriteToggle,
    required this.onOpenInstitution,
    required this.onPreviousPage,
    required this.onNextPage,
    required this.onPageRequested,
  });

  final List<Institution> institutions;
  final int totalElements;
  final int currentPage;
  final int totalPages;
  final int hospitalCount;
  final int pharmacyCount;
  final int emergencyRoomCount;
  final bool loading;
  final Set<int> favoriteIds;
  final bool canFavorite;
  final Future<void> Function(Institution) onFavoriteToggle;
  final ValueChanged<Institution> onOpenInstitution;
  final VoidCallback onPreviousPage;
  final VoidCallback onNextPage;
  final ValueChanged<int> onPageRequested;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: RichText(
                  text: TextSpan(
                    style: Theme.of(context).textTheme.titleLarge,
                    children: [
                      const TextSpan(text: '검색 결과 '),
                      TextSpan(
                        text: '$totalElements곳',
                        style: const TextStyle(color: AppColors.primary),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 9),
          Wrap(
            spacing: 8,
            runSpacing: 7,
            children: [
              _ResultCount(
                label: '병원',
                count: hospitalCount,
                color: AppColors.primary,
              ),
              _ResultCount(
                label: '약국',
                count: pharmacyCount,
                color: AppColors.green,
              ),
              _ResultCount(
                label: '응급실',
                count: emergencyRoomCount,
                color: AppColors.red,
              ),
            ],
          ),
          const SizedBox(height: 11),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 9),
            decoration: BoxDecoration(
              color: const Color(0xFFF5F9FF),
              border: Border.all(color: const Color(0xFFD7E4F5)),
              borderRadius: BorderRadius.circular(9),
            ),
            child: const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  Icons.near_me_outlined,
                  size: 18,
                  color: AppColors.primary,
                ),
                SizedBox(width: 7),
                Expanded(
                  child: Text(
                    '표시된 거리는 직선거리 기준이며, 도보나 대중교통,\n'
                    '자가용 이용 시 실제 이동 거리가 달라질 수 있습니다.',
                    style: TextStyle(
                      color: Color(0xFF52657E),
                      fontSize: 12,
                      height: 1.45,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 5),
          if (institutions.isEmpty && !loading)
            const _EmptyResult()
          else
            for (var index = 0; index < institutions.length; index++) ...[
              _InstitutionCard(
                institution: institutions[index],
                isFavorite: favoriteIds.contains(institutions[index].id),
                canFavorite: canFavorite,
                onFavoriteToggle: () => onFavoriteToggle(institutions[index]),
                onTap: () => onOpenInstitution(institutions[index]),
              ),
              if (index != institutions.length - 1) const SizedBox(height: 10),
            ],
          if (totalPages > 1) ...[
            const SizedBox(height: 18),
            _PageSelector(
              currentPage: currentPage,
              totalPages: totalPages,
              enabled: !loading,
              onPageRequested: onPageRequested,
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: !loading && currentPage > 0
                        ? onPreviousPage
                        : null,
                    icon: const Icon(Icons.chevron_left_rounded),
                    label: const Text('이전'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: !loading && currentPage + 1 < totalPages
                        ? onNextPage
                        : null,
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('다음'),
                        SizedBox(width: 4),
                        Icon(Icons.chevron_right_rounded),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _PageSelector extends StatefulWidget {
  const _PageSelector({
    required this.currentPage,
    required this.totalPages,
    required this.enabled,
    required this.onPageRequested,
  });

  final int currentPage;
  final int totalPages;
  final bool enabled;
  final ValueChanged<int> onPageRequested;

  @override
  State<_PageSelector> createState() => _PageSelectorState();
}

class _PageSelectorState extends State<_PageSelector> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: '${widget.currentPage + 1}');
  }

  @override
  void didUpdateWidget(covariant _PageSelector oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.currentPage != widget.currentPage ||
        oldWidget.totalPages != widget.totalPages) {
      _setCurrentPageText();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _setCurrentPageText() {
    final pageText = '${widget.currentPage + 1}';
    _controller.value = TextEditingValue(
      text: pageText,
      selection: TextSelection.collapsed(offset: pageText.length),
    );
  }

  void _submit() {
    FocusScope.of(context).unfocus();
    final requestedPage = int.tryParse(_controller.text);
    if (requestedPage == null || widget.totalPages < 1) {
      _setCurrentPageText();
      return;
    }

    final targetPage = requestedPage.clamp(1, widget.totalPages).toInt();
    _controller.text = '$targetPage';
    _controller.selection = TextSelection.collapsed(
      offset: _controller.text.length,
    );
    if (targetPage - 1 != widget.currentPage) {
      widget.onPageRequested(targetPage - 1);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: 48,
          child: TextField(
            controller: _controller,
            enabled: widget.enabled,
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.go,
            textAlign: TextAlign.center,
            onSubmitted: (_) => _submit(),
            decoration: const InputDecoration(
              isDense: true,
              contentPadding: EdgeInsets.symmetric(horizontal: 6, vertical: 11),
              border: OutlineInputBorder(),
            ),
          ),
        ),
        const SizedBox(width: 5),
        Text(
          '/ ${widget.totalPages}',
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: AppColors.muted,
            fontWeight: FontWeight.w700,
          ),
        ),
        IconButton(
          tooltip: '페이지 이동',
          onPressed: widget.enabled ? _submit : null,
          icon: const Icon(Icons.arrow_forward_rounded, size: 19),
          visualDensity: VisualDensity.compact,
        ),
      ],
    );
  }
}

class _ResultCount extends StatelessWidget {
  const _ResultCount({
    required this.label,
    required this.count,
    required this.color,
  });

  final String label;
  final int count;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        '$label $count',
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _InstitutionCard extends StatelessWidget {
  const _InstitutionCard({
    required this.institution,
    required this.isFavorite,
    required this.canFavorite,
    required this.onFavoriteToggle,
    required this.onTap,
  });

  final Institution institution;
  final bool isFavorite;
  final bool canFavorite;
  final Future<void> Function() onFavoriteToggle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final icon = institutionIconFor(institution.kind);
    final (iconColor, iconBackground) = switch (institution.kind) {
      InstitutionKind.hospital => (AppColors.primary, AppColors.primarySoft),
      InstitutionKind.pharmacy => (AppColors.green, AppColors.greenSoft),
      InstitutionKind.emergency => (AppColors.red, AppColors.redSoft),
    };
    final statusLabel = institution.kind == InstitutionKind.emergency
        ? institution.emergencyBedSummary
        : institution.isOpen
        ? '진료 중'
        : '진료 종료';
    final statusColor = institution.kind == InstitutionKind.emergency
        ? AppColors.red
        : institution.isOpen
        ? AppColors.green
        : AppColors.muted;
    final statusBackground = institution.kind == InstitutionKind.emergency
        ? AppColors.redSoft
        : institution.isOpen
        ? AppColors.greenSoft
        : const Color(0xFFF0F2F5);

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Container(
          padding: const EdgeInsets.all(13),
          decoration: BoxDecoration(
            color: Colors.white,
            border: Border.all(color: AppColors.line),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              IconTile(
                icon: icon,
                color: iconColor,
                backgroundColor: iconBackground,
                size: 55,
                iconSize: 28,
              ),
              const SizedBox(width: 13),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      institution.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 5),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: TinyTag(
                        label: institution.typeLabel,
                        color: iconColor,
                        backgroundColor: iconBackground,
                      ),
                    ),
                    const SizedBox(height: 5),
                    Wrap(
                      spacing: 6,
                      runSpacing: 5,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        TinyTag(
                          label: statusLabel,
                          color: statusColor,
                          backgroundColor: statusBackground,
                        ),
                        Text(
                          institution.distanceSummary,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      institution.address,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              if (canFavorite)
                IconButton(
                  tooltip: isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가',
                  onPressed: () => unawaited(onFavoriteToggle()),
                  icon: Icon(
                    isFavorite ? Icons.star_rounded : Icons.star_border_rounded,
                    color: isFavorite
                        ? AppColors.amber
                        : const Color(0xFF8E99A9),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _FilterPill extends StatelessWidget {
  const _FilterPill({
    required this.icon,
    required this.label,
    required this.onTap,
    this.selected = false,
    this.selectedColor = AppColors.primary,
    this.showDropdownIndicator = true,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool selected;
  final Color selectedColor;
  final bool showDropdownIndicator;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Container(
          height: 42,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          decoration: BoxDecoration(
            color: selected
                ? selectedColor.withValues(alpha: 0.10)
                : Colors.white,
            border: Border.all(
              color: selected ? selectedColor : AppColors.line,
            ),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                icon,
                size: 18,
                color: selected ? selectedColor : AppColors.muted,
              ),
              const SizedBox(width: 7),
              Text(
                label,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: selected ? selectedColor : AppColors.ink,
                  fontWeight: FontWeight.w700,
                ),
              ),
              if (showDropdownIndicator) ...[
                const SizedBox(width: 3),
                const Icon(
                  Icons.keyboard_arrow_down_rounded,
                  size: 17,
                  color: AppColors.muted,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _LocationCard extends StatelessWidget {
  const _LocationCard({required this.locationLabel, required this.locating});

  final String locationLabel;
  final bool locating;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: [
          const IconTile(
            icon: Icons.location_on_outlined,
            size: 42,
            iconSize: 22,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('검색 기준 위치', style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 2),
                Text(
                  locationLabel,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ],
            ),
          ),
          if (locating)
            const SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(strokeWidth: 2.2),
            ),
        ],
      ),
    );
  }
}

class _ErrorNotice extends StatelessWidget {
  const _ErrorNotice({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(14, 12, 10, 12),
      decoration: BoxDecoration(
        color: AppColors.redSoft,
        border: Border.all(color: const Color(0xFFFFCDD1)),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline_rounded, color: AppColors.red),
          const SizedBox(width: 9),
          Expanded(
            child: Text(message, style: Theme.of(context).textTheme.bodySmall),
          ),
          TextButton(onPressed: onRetry, child: const Text('다시 시도')),
        ],
      ),
    );
  }
}

class _LocationPending extends StatelessWidget {
  const _LocationPending({required this.onLocate});

  final VoidCallback onLocate;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 42),
      child: Column(
        children: [
          const IconTile(
            icon: Icons.location_searching_rounded,
            size: 64,
            iconSize: 32,
          ),
          const SizedBox(height: 14),
          Text(
            '검색 기준 위치가 필요합니다.',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 6),
          Text(
            '현재 위치를 허용하거나 주소를 검색해 주세요.',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: onLocate,
            icon: const Icon(Icons.my_location_rounded, size: 19),
            label: const Text('내 위치 확인'),
          ),
        ],
      ),
    );
  }
}

class _EmptyResult extends StatelessWidget {
  const _EmptyResult();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 34, horizontal: 20),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        children: [
          const Icon(
            Icons.search_off_rounded,
            color: AppColors.muted,
            size: 34,
          ),
          const SizedBox(height: 10),
          Text(
            '조건에 맞는 의료기관이 없습니다.',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 4),
          Text(
            '검색 반경을 넓히거나 필터를 변경해 보세요.',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }
}

class _SearchLocation {
  const _SearchLocation({
    required this.latitude,
    required this.longitude,
    required this.label,
  });

  final double latitude;
  final double longitude;
  final String label;
}

class _LocationSearchException implements Exception {
  const _LocationSearchException(this.message);

  final String message;
}

class _FilterOption<T> {
  const _FilterOption(this.value, this.label);

  final T value;
  final String label;
}

String _labelFor<T>(List<_FilterOption<T>> options, T value) {
  return options
      .firstWhere(
        (option) => option.value == value,
        orElse: () => options.first,
      )
      .label;
}

const _categoryTypes = <String, List<String>>{
  '전체': ['HOSPITAL', 'PHARMACY', 'EMERGENCY_ROOM'],
  '병원': ['HOSPITAL'],
  '약국': ['PHARMACY'],
  '응급실': ['EMERGENCY_ROOM'],
};

const _radiusOptions = [
  _FilterOption(1000, '반경 1km'),
  _FilterOption(2000, '반경 2km'),
  _FilterOption(3000, '반경 3km'),
  _FilterOption(5000, '반경 5km'),
];

const _scheduleOptions = [
  _FilterOption('ALL', '전체 진료시간'),
  _FilterOption('NIGHT', '야간진료'),
  _FilterOption('TWENTY_FOUR_HOURS', '24시간진료'),
  _FilterOption('SATURDAY', '토요일진료'),
  _FilterOption('SUNDAY', '일요일진료'),
  _FilterOption('HOLIDAY', '공휴일진료'),
];

const _departmentOptions = [
  _FilterOption('ALL', '전체 진료과목'),
  _FilterOption('INTERNAL_MEDICINE', '내과'),
  _FilterOption('PEDIATRICS', '소아청소년과'),
  _FilterOption('NEUROLOGY', '신경과'),
  _FilterOption('MENTAL_HEALTH_MEDICINE', '정신건강의학과'),
  _FilterOption('DERMATOLOGY', '피부과'),
  _FilterOption('SURGERY', '외과'),
  _FilterOption('CARDIOTHORACIC_SURGERY', '흉부외과'),
  _FilterOption('ORTHOPEDICS', '정형외과'),
  _FilterOption('NEUROSURGERY', '신경외과'),
  _FilterOption('PLASTIC_SURGERY', '성형외과'),
  _FilterOption('OBSTETRICS_GYNECOLOGY', '산부인과'),
  _FilterOption('OPHTHALMOLOGY', '안과'),
  _FilterOption('OTOLARYNGOLOGY', '이비인후과'),
  _FilterOption('UROLOGY', '비뇨기과'),
  _FilterOption('TUBERCULOSIS', '결핵과'),
  _FilterOption('REHABILITATION_MEDICINE', '재활의학과'),
  _FilterOption('ANESTHESIOLOGY_PAIN_MEDICINE', '마취통증의학과'),
  _FilterOption('RADIOLOGY', '영상의학과'),
  _FilterOption('THERAPEUTIC_RADIOLOGY', '치료방사선과'),
  _FilterOption('CLINICAL_PATHOLOGY', '임상병리과'),
  _FilterOption('ANATOMICAL_PATHOLOGY', '해부병리과'),
  _FilterOption('FAMILY_MEDICINE', '가정의학과'),
  _FilterOption('NUCLEAR_MEDICINE', '핵의학과'),
  _FilterOption('EMERGENCY_MEDICINE', '응급의학과'),
  _FilterOption('OCCUPATIONAL_MEDICINE', '산업의학과'),
  _FilterOption('DENTISTRY', '치과'),
  _FilterOption('KOREAN_INTERNAL_MEDICINE', '한방내과'),
  _FilterOption('KOREAN_GYNECOLOGY', '한방부인과'),
  _FilterOption('PREVENTIVE_MEDICINE', '예방의학과'),
  _FilterOption('KOREAN_CLINIC', '한의원·한방병원'),
];
