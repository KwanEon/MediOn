import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class PrivacyTermsPage extends StatelessWidget {
  const PrivacyTermsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('개인정보 및 약관'),
          centerTitle: true,
          shape: const Border(bottom: BorderSide(color: AppColors.line)),
          bottom: const TabBar(
            tabs: [
              Tab(text: '개인정보처리방침'),
              Tab(text: '이용약관'),
            ],
          ),
        ),
        body: const SafeArea(
          top: false,
          child: TabBarView(
            children: [
              _PolicyContent(
                title: '개인정보처리방침',
                subtitle: '메디온이 어떤 정보를 수집하고 이용하는지 확인하세요.',
                sections: _privacySections,
              ),
              _PolicyContent(
                title: '이용약관',
                subtitle: '메디온 서비스를 이용할 때 필요한 기본 사항을 안내합니다.',
                sections: _termsSections,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PolicyContent extends StatelessWidget {
  const _PolicyContent({
    required this.title,
    required this.subtitle,
    required this.sections,
  });

  final String title;
  final String subtitle;
  final List<_PolicySectionData> sections;

  @override
  Widget build(BuildContext context) {
    return ScreenFrame(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 40),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeading(title: title, subtitle: subtitle),
          const SizedBox(height: 18),
          const SoftNotice(
            icon: Icons.event_note_outlined,
            text: '최종 업데이트 2026년 7월 31일',
          ),
          const SizedBox(height: 14),
          for (var index = 0; index < sections.length; index++) ...[
            _PolicySection(number: index + 1, data: sections[index]),
            if (index != sections.length - 1) const SizedBox(height: 12),
          ],
        ],
      ),
    );
  }
}

class _PolicySection extends StatelessWidget {
  const _PolicySection({required this.number, required this.data});

  final int number;
  final _PolicySectionData data;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 28,
                height: 28,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: AppColors.primarySoft,
                  borderRadius: BorderRadius.circular(9),
                ),
                child: Text(
                  '$number',
                  style: Theme.of(
                    context,
                  ).textTheme.labelLarge?.copyWith(color: AppColors.primary),
                ),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(top: 3),
                  child: Text(
                    data.title,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ),
            ],
          ),
          if (data.description != null) ...[
            const SizedBox(height: 12),
            Text(
              data.description!,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(height: 1.65),
            ),
          ],
          if (data.items.isNotEmpty) ...[
            const SizedBox(height: 10),
            for (final item in data.items)
              Padding(
                padding: const EdgeInsets.only(bottom: 7),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Padding(
                      padding: EdgeInsets.only(top: 8),
                      child: Icon(
                        Icons.circle,
                        size: 5,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(width: 9),
                    Expanded(
                      child: Text(
                        item,
                        style: Theme.of(
                          context,
                        ).textTheme.bodyMedium?.copyWith(height: 1.6),
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ],
      ),
    );
  }
}

class _PolicySectionData {
  const _PolicySectionData({
    required this.title,
    this.description,
    this.items = const [],
  });

  final String title;
  final String? description;
  final List<String> items;
}

const _privacySections = [
  _PolicySectionData(
    title: '수집하는 개인정보',
    description: '메디온은 회원가입과 서비스 제공에 필요한 범위에서 다음 정보를 처리합니다.',
    items: [
      '회원 정보: 아이디, 암호화된 비밀번호, 이름, 이메일, 전화번호, 주소',
      '서비스 이용 정보: 즐겨찾기, 알림 및 위치 검색 설정, 문의 내용과 처리 상태',
      '위치 정보: 사용자가 위치 기반 검색을 켠 경우 주변 의료기관 검색 요청을 처리하기 위한 현재 위치',
    ],
  ),
  _PolicySectionData(
    title: '개인정보 이용 목적',
    items: [
      '회원 식별, 로그인과 회원정보 관리',
      '현재 위치 또는 저장 주소를 기준으로 한 의료기관 검색',
      '즐겨찾기, 공지·건강 정보 알림과 문의 서비스 제공',
      '서비스 오류 확인, 보안 유지와 품질 개선',
    ],
  ),
  _PolicySectionData(
    title: '보유 및 이용 기간',
    description:
        '회원정보는 서비스 이용 기간 동안 보관하며, 처리 목적이 달성되거나 관련 요청이 접수되면 관계 법령에서 정한 보관 의무를 제외하고 안전하게 처리합니다. 문의 내역은 사용자가 직접 삭제할 수 있습니다.',
  ),
  _PolicySectionData(
    title: '제3자 제공 및 보호 조치',
    description:
        '메디온은 사용자의 동의가 있거나 법령에서 허용하는 경우를 제외하고 개인정보를 제3자에게 판매하거나 제공하지 않습니다. 비밀번호 암호화, 접근 권한 제한 등 필요한 보호 조치를 적용합니다.',
  ),
  _PolicySectionData(
    title: '사용자의 권리',
    items: [
      '마이페이지에서 이름, 이메일과 주소를 확인하고 변경할 수 있습니다.',
      '문의하기에서 본인이 등록한 문의를 확인하거나 삭제할 수 있습니다.',
      '건강 정보 알림과 위치 기반 검색은 언제든지 끌 수 있습니다.',
      '개인정보 관련 요청은 문의하기를 통해 접수할 수 있습니다.',
    ],
  ),
  _PolicySectionData(
    title: '방침 변경 및 문의',
    description:
        '개인정보처리방침이 변경되면 시행 전에 공지사항으로 안내합니다. 개인정보 처리에 관한 질문이나 요청은 앱의 문의하기를 이용해 주세요.',
  ),
];

const _termsSections = [
  _PolicySectionData(
    title: '목적',
    description:
        '이 약관은 메디온이 제공하는 의료기관 정보 검색과 관련 서비스의 이용 조건 및 사용자와 서비스의 권리·의무를 정하는 것을 목적으로 합니다.',
  ),
  _PolicySectionData(
    title: '제공하는 서비스',
    items: [
      '현재 위치 또는 주소 기반 의료기관 검색과 상세정보 제공',
      '진료과·건강 정보·공지사항과 이용 안내 제공',
      '회원정보 관리, 즐겨찾기, 알림 설정과 문의 접수',
      '서비스 운영에 필요하다고 판단되는 기타 기능',
    ],
  ),
  _PolicySectionData(
    title: '회원의 의무',
    items: [
      '가입 정보는 정확하게 입력하고 변경된 정보는 최신 상태로 유지해야 합니다.',
      '계정과 비밀번호를 안전하게 관리하며 타인의 정보를 무단으로 사용하지 않아야 합니다.',
      '서비스 운영을 방해하거나 법령과 공공질서에 어긋나는 방식으로 이용해서는 안 됩니다.',
    ],
  ),
  _PolicySectionData(
    title: '의료정보 이용 안내',
    description:
        '메디온의 의료기관 정보는 공공 의료데이터 등을 바탕으로 제공되며 실제 운영 상황과 다를 수 있습니다. 진료 가능 여부, 운영 시간과 응급 상황은 방문 전에 해당 기관 또는 관계 기관에 직접 확인해야 합니다. 앱의 정보는 의료진의 진단이나 처방을 대신하지 않습니다.',
  ),
  _PolicySectionData(
    title: '서비스 변경 및 중단',
    description:
        '점검, 데이터 제공기관의 사정, 통신 장애 또는 불가피한 운영상 사유로 서비스의 일부가 변경되거나 일시 중단될 수 있습니다. 중요한 변경 사항은 공지사항을 통해 안내합니다.',
  ),
  _PolicySectionData(
    title: '약관의 변경',
    description:
        '관련 법령이나 서비스 내용이 변경되는 경우 약관을 수정할 수 있으며, 변경 내용과 적용일은 앱의 공지사항을 통해 안내합니다.',
  ),
];
