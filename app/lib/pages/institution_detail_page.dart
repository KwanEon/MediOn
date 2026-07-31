import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:url_launcher/url_launcher.dart';

import '../constants/institution_icons.dart';
import '../data/institution_models.dart';
import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class InstitutionDetailPage extends StatefulWidget {
  const InstitutionDetailPage({
    super.key,
    required this.institution,
    required this.initiallyFavorite,
    required this.onFavoriteChanged,
  });

  final Institution institution;
  final bool initiallyFavorite;
  final ValueChanged<bool> onFavoriteChanged;

  @override
  State<InstitutionDetailPage> createState() => _InstitutionDetailPageState();
}

class _InstitutionDetailPageState extends State<InstitutionDetailPage> {
  late bool _isFavorite;

  Institution get institution => widget.institution;

  @override
  void initState() {
    super.initState();
    _isFavorite = widget.initiallyFavorite;
  }

  @override
  void didUpdateWidget(covariant InstitutionDetailPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.institution != widget.institution ||
        oldWidget.initiallyFavorite != widget.initiallyFavorite) {
      _isFavorite = widget.initiallyFavorite;
    }
  }

  void _toggleFavorite() {
    setState(() => _isFavorite = !_isFavorite);
    widget.onFavoriteChanged(_isFavorite);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(_isFavorite ? '즐겨찾기에 추가했어요.' : '즐겨찾기에서 해제했어요.'),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _openPhoneDialer() async {
    final phoneNumber = institution.phone.replaceAll(RegExp(r'[^0-9+]'), '');
    if (phoneNumber.isEmpty) {
      _showMessage('등록된 전화번호가 없습니다.');
      return;
    }

    try {
      final launched = await launchUrl(
        Uri(scheme: 'tel', path: phoneNumber),
        mode: LaunchMode.externalApplication,
      );
      if (!launched && mounted) {
        _showMessage('전화 앱을 열 수 없습니다.');
      }
    } catch (_) {
      if (mounted) {
        _showMessage('전화 앱을 열 수 없습니다.');
      }
    }
  }

  Future<void> _openKakaoDirections() async {
    final destinationName = Uri.encodeComponent(institution.name);
    final directionsUri = Uri.parse(
      'https://map.kakao.com/link/to/'
      '$destinationName,${institution.latitude},${institution.longitude}',
    );

    try {
      final launched = await launchUrl(
        directionsUri,
        mode: LaunchMode.externalApplication,
      );
      if (!launched && mounted) {
        _showMessage('카카오맵 길찾기를 열 수 없습니다.');
      }
    } catch (_) {
      if (mounted) {
        _showMessage('카카오맵 길찾기를 열 수 없습니다.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final visuals = _InstitutionVisuals.from(institution.kind);

    return Scaffold(
      appBar: AppBar(
        title: const Text('의료기관 상세'),
        centerTitle: true,
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
        actions: [
          IconButton(
            tooltip: _isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가',
            onPressed: _toggleFavorite,
            icon: Icon(
              _isFavorite ? Icons.star_rounded : Icons.star_border_rounded,
              color: _isFavorite ? AppColors.amber : AppColors.muted,
            ),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: ScreenFrame(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SurfaceCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      IconTile(
                        icon: visuals.icon,
                        color: visuals.color,
                        backgroundColor: visuals.backgroundColor,
                        size: 58,
                        iconSize: 29,
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Wrap(
                              spacing: 7,
                              runSpacing: 7,
                              children: [
                                TinyTag(
                                  label: institution.typeLabel,
                                  color: visuals.color,
                                  backgroundColor: visuals.backgroundColor,
                                ),
                                TinyTag(
                                  label:
                                      institution.kind ==
                                          InstitutionKind.emergency
                                      ? institution.emergencyBedSummary
                                      : institution.isOpen
                                      ? '진료 중'
                                      : '진료 종료',
                                  color:
                                      institution.kind ==
                                          InstitutionKind.emergency
                                      ? AppColors.red
                                      : institution.isOpen
                                      ? AppColors.green
                                      : AppColors.muted,
                                  backgroundColor:
                                      institution.kind ==
                                          InstitutionKind.emergency
                                      ? AppColors.redSoft
                                      : institution.isOpen
                                      ? AppColors.greenSoft
                                      : const Color(0xFFF0F2F5),
                                ),
                              ],
                            ),
                            const SizedBox(height: 9),
                            Text(
                              institution.name,
                              style: Theme.of(context).textTheme.headlineSmall,
                            ),
                            const SizedBox(height: 5),
                            Row(
                              children: [
                                const Icon(
                                  Icons.near_me_outlined,
                                  color: AppColors.muted,
                                  size: 16,
                                ),
                                const SizedBox(width: 5),
                                Text(
                                  '현재 위치에서 ${institution.distanceSummary}',
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 22),
            const SectionHeading(title: '위치'),
            const SizedBox(height: 12),
            _MiniMap(
              markerColor: visuals.color,
              institutionName: institution.name,
              address: institution.address,
              latitude: institution.latitude,
              longitude: institution.longitude,
            ),
            const SizedBox(height: 12),
            SurfaceCard(
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  _InformationRow(
                    icon: Icons.location_on_outlined,
                    title: '주소',
                    value: institution.address,
                  ),
                  const Divider(),
                  _InformationRow(
                    icon: Icons.phone_outlined,
                    title: '전화',
                    value: institution.phone,
                    accent: true,
                    onTap: () => unawaited(_openPhoneDialer()),
                  ),
                  const Divider(),
                  if (institution.kind == InstitutionKind.emergency) ...[
                    _InformationRow(
                      icon: Icons.bed_outlined,
                      title: '가용 병상',
                      value: institution.emergencyBedLabel,
                      valueColor: institution.availableEmergencyBeds == 0
                          ? AppColors.red
                          : AppColors.primary,
                    ),
                    const Divider(),
                  ],
                  _InformationRow(
                    icon: Icons.schedule_rounded,
                    title: '운영 시간',
                    value: institution.hours,
                    valueColor: institution.isOpen
                        ? AppColors.green
                        : AppColors.muted,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            const SoftNotice(
              icon: Icons.info_outline_rounded,
              text: '운영 시간과 진료 가능 여부는 방문 전 기관에 확인해 주세요.',
            ),
          ],
        ),
      ),
      bottomNavigationBar: DecoratedBox(
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: AppColors.line)),
          boxShadow: [
            BoxShadow(
              color: Color(0x0F1E375A),
              blurRadius: 14,
              offset: Offset(0, -4),
            ),
          ],
        ),
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: 50,
                    child: OutlinedButton.icon(
                      onPressed: () => unawaited(_openPhoneDialer()),
                      icon: const Icon(Icons.phone_outlined, size: 20),
                      label: const Text('전화 문의'),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: SizedBox(
                    height: 50,
                    child: FilledButton.icon(
                      onPressed: () => unawaited(_openKakaoDirections()),
                      icon: const Icon(Icons.directions_rounded, size: 20),
                      label: const Text('길찾기'),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _InstitutionVisuals {
  const _InstitutionVisuals({
    required this.icon,
    required this.color,
    required this.backgroundColor,
  });

  factory _InstitutionVisuals.from(InstitutionKind kind) {
    return switch (kind) {
      InstitutionKind.hospital => _InstitutionVisuals(
        icon: institutionIconFor(kind),
        color: AppColors.primary,
        backgroundColor: AppColors.primarySoft,
      ),
      InstitutionKind.pharmacy => _InstitutionVisuals(
        icon: institutionIconFor(kind),
        color: AppColors.green,
        backgroundColor: AppColors.greenSoft,
      ),
      InstitutionKind.emergency => _InstitutionVisuals(
        icon: institutionIconFor(kind),
        color: AppColors.red,
        backgroundColor: AppColors.redSoft,
      ),
    };
  }

  final IconData icon;
  final Color color;
  final Color backgroundColor;
}

class _InformationRow extends StatelessWidget {
  const _InformationRow({
    required this.icon,
    required this.title,
    required this.value,
    this.valueColor,
    this.accent = false,
    this.onTap,
  });

  final IconData icon;
  final String title;
  final String value;
  final Color? valueColor;
  final bool accent;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final content = Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: AppColors.muted, size: 21),
          const SizedBox(width: 13),
          SizedBox(
            width: 64,
            child: Text(title, style: Theme.of(context).textTheme.bodyMedium),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              value,
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                color:
                    valueColor ?? (accent ? AppColors.primary : AppColors.ink),
                fontWeight: accent || valueColor != null
                    ? FontWeight.w700
                    : FontWeight.w500,
              ),
            ),
          ),
          if (onTap != null)
            const Padding(
              padding: EdgeInsets.only(left: 5, top: 2),
              child: Icon(
                Icons.chevron_right_rounded,
                color: Color(0xFF9AA5B5),
                size: 21,
              ),
            ),
        ],
      ),
    );

    if (onTap == null) return content;
    return InkWell(onTap: onTap, child: content);
  }
}

class _MiniMap extends StatelessWidget {
  const _MiniMap({
    required this.markerColor,
    required this.institutionName,
    required this.address,
    required this.latitude,
    required this.longitude,
  });

  final Color markerColor;
  final String institutionName;
  final String address;
  final double latitude;
  final double longitude;

  void _openFullMap(BuildContext context) {
    Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => _FullInstitutionMapPage(
          markerColor: markerColor,
          institutionName: institutionName,
          address: address,
          latitude: latitude,
          longitude: longitude,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(16),
      child: Container(
        height: 205,
        decoration: BoxDecoration(
          color: const Color(0xFFF0F4EF),
          border: Border.all(color: AppColors.line),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Stack(
          fit: StackFit.expand,
          children: [
            _InstitutionMapCanvas(
              markerColor: markerColor,
              institutionName: institutionName,
              latitude: latitude,
              longitude: longitude,
              initialZoom: 16,
            ),
            Positioned(
              right: 12,
              top: 12,
              child: Material(
                color: Colors.white,
                shape: const CircleBorder(),
                elevation: 2,
                child: IconButton(
                  tooltip: '지도 크게 보기',
                  onPressed: () => _openFullMap(context),
                  icon: const Icon(
                    Icons.open_in_full_rounded,
                    color: AppColors.primary,
                    size: 20,
                  ),
                ),
              ),
            ),
            const Positioned(left: 8, bottom: 6, child: _MapAttribution()),
          ],
        ),
      ),
    );
  }
}

class _FullInstitutionMapPage extends StatelessWidget {
  const _FullInstitutionMapPage({
    required this.markerColor,
    required this.institutionName,
    required this.address,
    required this.latitude,
    required this.longitude,
  });

  final Color markerColor;
  final String institutionName;
  final String address;
  final double latitude;
  final double longitude;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('지도 보기'),
        centerTitle: true,
        shape: const Border(bottom: BorderSide(color: AppColors.line)),
      ),
      body: Stack(
        children: [
          Positioned.fill(
            child: _InstitutionMapCanvas(
              markerColor: markerColor,
              institutionName: institutionName,
              latitude: latitude,
              longitude: longitude,
              initialZoom: 17,
              showZoomControls: true,
            ),
          ),
          Positioned(
            left: 16,
            right: 16,
            bottom: 20,
            child: SafeArea(
              top: false,
              child: Material(
                color: Colors.white,
                elevation: 5,
                borderRadius: BorderRadius.circular(16),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(
                        Icons.location_on_rounded,
                        color: markerColor,
                        size: 28,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              institutionName,
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              address,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
          const Positioned(left: 8, top: 4, child: _MapAttribution()),
        ],
      ),
    );
  }
}

class _InstitutionMapCanvas extends StatefulWidget {
  const _InstitutionMapCanvas({
    required this.markerColor,
    required this.institutionName,
    required this.latitude,
    required this.longitude,
    required this.initialZoom,
    this.showZoomControls = false,
  });

  final Color markerColor;
  final String institutionName;
  final double latitude;
  final double longitude;
  final double initialZoom;
  final bool showZoomControls;

  @override
  State<_InstitutionMapCanvas> createState() => _InstitutionMapCanvasState();
}

class _InstitutionMapCanvasState extends State<_InstitutionMapCanvas> {
  final MapController _mapController = MapController();
  bool _mapReady = false;

  void _zoomBy(double delta) {
    if (!_mapReady) return;
    final camera = _mapController.camera;
    _mapController.move(
      camera.center,
      (camera.zoom + delta).clamp(3.0, 18.0).toDouble(),
    );
  }

  void _moveToInstitution() {
    if (!_mapReady) return;
    _mapController.move(
      LatLng(widget.latitude, widget.longitude),
      widget.initialZoom,
    );
  }

  @override
  void didUpdateWidget(covariant _InstitutionMapCanvas oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.latitude != widget.latitude ||
        oldWidget.longitude != widget.longitude) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _moveToInstitution();
      });
    }
  }

  @override
  void dispose() {
    _mapController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final point = LatLng(widget.latitude, widget.longitude);

    return Stack(
      children: [
        FlutterMap(
          mapController: _mapController,
          options: MapOptions(
            initialCenter: point,
            initialZoom: widget.initialZoom,
            minZoom: 3,
            maxZoom: 18,
            onMapReady: () => _mapReady = true,
          ),
          children: [
            TileLayer(
              urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
              userAgentPackageName: 'com.medion.medion_app',
            ),
            MarkerLayer(
              markers: [
                Marker(
                  point: point,
                  width: 210,
                  height: 82,
                  child: _InstitutionMapMarker(
                    markerColor: widget.markerColor,
                    institutionName: widget.institutionName,
                  ),
                ),
              ],
            ),
          ],
        ),
        if (widget.showZoomControls)
          Positioned(
            top: 14,
            right: 14,
            child: Column(
              children: [
                _MapControlButton(
                  tooltip: '확대',
                  icon: Icons.add_rounded,
                  onPressed: () => _zoomBy(1),
                ),
                const SizedBox(height: 8),
                _MapControlButton(
                  tooltip: '축소',
                  icon: Icons.remove_rounded,
                  onPressed: () => _zoomBy(-1),
                ),
                const SizedBox(height: 8),
                _MapControlButton(
                  tooltip: '의료기관 위치로 이동',
                  icon: Icons.my_location_rounded,
                  onPressed: _moveToInstitution,
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _InstitutionMapMarker extends StatelessWidget {
  const _InstitutionMapMarker({
    required this.markerColor,
    required this.institutionName,
  });

  final Color markerColor;
  final String institutionName;

  @override
  Widget build(BuildContext context) {
    return Transform.translate(
      offset: const Offset(0, -25),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            constraints: const BoxConstraints(maxWidth: 190),
            padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(9),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x1A1E375A),
                  blurRadius: 10,
                  offset: Offset(0, 3),
                ),
              ],
            ),
            child: Text(
              institutionName,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: AppColors.ink,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          Icon(Icons.location_on_rounded, color: markerColor, size: 45),
        ],
      ),
    );
  }
}

class _MapControlButton extends StatelessWidget {
  const _MapControlButton({
    required this.tooltip,
    required this.icon,
    required this.onPressed,
  });

  final String tooltip;
  final IconData icon;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      elevation: 2,
      borderRadius: BorderRadius.circular(12),
      child: IconButton(
        tooltip: tooltip,
        onPressed: onPressed,
        icon: Icon(icon, color: AppColors.primary),
      ),
    );
  }
}

class _MapAttribution extends StatelessWidget {
  const _MapAttribution();

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.82),
        borderRadius: BorderRadius.circular(4),
      ),
      child: const Padding(
        padding: EdgeInsets.symmetric(horizontal: 5, vertical: 2),
        child: Text(
          '© OpenStreetMap contributors',
          style: TextStyle(
            color: Color(0xFF596579),
            fontSize: 9,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}
