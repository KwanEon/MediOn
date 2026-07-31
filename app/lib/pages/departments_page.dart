import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../widgets/common_widgets.dart';

class DepartmentsPage extends StatefulWidget {
  const DepartmentsPage({super.key});

  @override
  State<DepartmentsPage> createState() => _DepartmentsPageState();
}

class _DepartmentsPageState extends State<DepartmentsPage> {
  final TextEditingController _searchController = TextEditingController();
  final Set<String> _expandedDepartments = {};
  String _selectedCategory = '전체';
  String _query = '';

  List<_Department> get _filteredDepartments {
    final query = _query.trim().toLowerCase();

    return _departments.where((department) {
      final matchesCategory =
          _selectedCategory == '전체' || department.category == _selectedCategory;
      if (!matchesCategory) return false;
      if (query.isEmpty) return true;

      final searchableText = [
        department.name,
        ...department.areas,
        department.summary,
        ...department.symptoms,
      ].join(' ').toLowerCase();

      return searchableText.contains(query);
    }).toList();
  }

  void _selectCategory(String category) {
    setState(() => _selectedCategory = category);
  }

  void _updateQuery(String value) {
    setState(() => _query = value);
  }

  void _toggleDepartment(String name) {
    setState(() {
      if (!_expandedDepartments.add(name)) {
        _expandedDepartments.remove(name);
      }
    });
  }

  void _resetFilters() {
    _searchController.clear();
    setState(() {
      _query = '';
      _selectedCategory = '전체';
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final filteredDepartments = _filteredDepartments;

    return ScreenFrame(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeading(title: '진료과 찾기', subtitle: '증상과 진료 영역을 확인해 보세요.'),
          const SizedBox(height: 22),
          const EmergencyBanner(
            title: '위급한 증상은 즉시 119에 연락하세요.',
            message: '의식 저하, 호흡 곤란, 갑작스러운 마비는 신고가 우선입니다.',
          ),
          const SizedBox(height: 20),
          AppSearchField(
            hintText: '진료과 또는 증상 검색',
            controller: _searchController,
            onChanged: _updateQuery,
          ),
          const SizedBox(height: 18),
          SizedBox(
            height: 40,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              physics: const BouncingScrollPhysics(),
              itemCount: _categories.length,
              separatorBuilder: (_, _) => const SizedBox(width: 9),
              itemBuilder: (context, index) {
                final category = _categories[index];
                return MedionChip(
                  label: category,
                  selected: category == _selectedCategory,
                  onTap: () => _selectCategory(category),
                );
              },
            ),
          ),
          const SizedBox(height: 28),
          SectionHeading(
            title: _selectedCategory == '전체'
                ? '전체 진료과'
                : '$_selectedCategory 진료과',
            trailing: Text(
              '${filteredDepartments.length}개',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
          const SizedBox(height: 14),
          if (filteredDepartments.isEmpty)
            _EmptyDepartmentState(onReset: _resetFilters)
          else
            ...filteredDepartments.map(
              (department) => Padding(
                padding: const EdgeInsets.only(bottom: 14),
                child: _DepartmentCard(
                  department: department,
                  expanded: _expandedDepartments.contains(department.name),
                  onTap: () => _toggleDepartment(department.name),
                ),
              ),
            ),
          const SizedBox(height: 4),
          const SoftNotice(
            icon: Icons.info_outline_rounded,
            text: '어느 진료과로 가야 할지 어렵다면 내과나 가정의학과에서 먼저 상담해 보세요.',
            color: AppColors.mint,
            backgroundColor: AppColors.mintSoft,
            borderColor: Color(0xFFBCE4DD),
          ),
        ],
      ),
    );
  }
}

class _EmptyDepartmentState extends StatelessWidget {
  const _EmptyDepartmentState({required this.onReset});

  final VoidCallback onReset;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 34),
      child: Column(
        children: [
          const IconTile(
            icon: Icons.search_off_rounded,
            color: AppColors.muted,
            backgroundColor: Color(0xFFF1F4F8),
            size: 64,
            iconSize: 33,
          ),
          const SizedBox(height: 16),
          Text(
            '조건에 맞는 진료과가 없어요.',
            style: Theme.of(context).textTheme.titleLarge,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 7),
          Text(
            '다른 증상을 입력하거나 카테고리를 변경해 보세요.',
            style: Theme.of(context).textTheme.bodyMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 18),
          OutlinedButton.icon(
            onPressed: onReset,
            icon: const Icon(Icons.refresh_rounded, size: 19),
            label: const Text('검색 조건 초기화'),
          ),
        ],
      ),
    );
  }
}

class _DepartmentCard extends StatelessWidget {
  const _DepartmentCard({
    required this.department,
    required this.expanded,
    required this.onTap,
  });

  final _Department department;
  final bool expanded;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      onTap: onTap,
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          IconTile(
            icon: department.icon,
            color: department.color,
            backgroundColor: department.backgroundColor,
            size: 56,
            iconSize: 30,
          ),
          const SizedBox(width: 15),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        department.name,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                    ),
                    AnimatedRotation(
                      turns: expanded ? 0.25 : 0,
                      duration: const Duration(milliseconds: 180),
                      child: const Icon(
                        Icons.chevron_right_rounded,
                        color: Color(0xFF9AA6B5),
                        size: 24,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 7,
                  runSpacing: 7,
                  children: department.areas
                      .map(
                        (area) => TinyTag(
                          label: area,
                          color: AppColors.green,
                          backgroundColor: AppColors.greenSoft,
                        ),
                      )
                      .toList(),
                ),
                const SizedBox(height: 10),
                Text(
                  department.summary,
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(color: AppColors.ink),
                ),
                if (expanded) ...[
                  const SizedBox(height: 13),
                  Text(
                    '이럴 때 고려해 보세요',
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: const Color(0xFF435166),
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(height: 5),
                  ...department.symptoms.map(
                    (symptom) => Padding(
                      padding: const EdgeInsets.only(bottom: 3),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Padding(
                            padding: EdgeInsets.only(top: 7),
                            child: Icon(
                              Icons.circle,
                              size: 4,
                              color: AppColors.muted,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              symptom,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Divider(),
                  const SizedBox(height: 9),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(
                        Icons.info_outline_rounded,
                        color: AppColors.amber,
                        size: 19,
                      ),
                      const SizedBox(width: 7),
                      Text(
                        '참고',
                        style: Theme.of(context).textTheme.labelLarge?.copyWith(
                          color: AppColors.amber,
                          fontSize: 12,
                        ),
                      ),
                      const SizedBox(width: 7),
                      Expanded(
                        child: Text(
                          department.note,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Department {
  const _Department({
    required this.name,
    required this.category,
    required this.icon,
    required this.color,
    required this.backgroundColor,
    required this.areas,
    required this.summary,
    required this.symptoms,
    required this.note,
  });

  final String name;
  final String category;
  final IconData icon;
  final Color color;
  final Color backgroundColor;
  final List<String> areas;
  final String summary;
  final List<String> symptoms;
  final String note;
}

const _categories = [
  '전체',
  '일반 진료',
  '수술·근골격',
  '뇌·감각기관',
  '피부·비뇨',
  '여성·소아',
  '마음·회복',
  '치과',
];

const _departments = [
  _Department(
    name: '내과',
    category: '일반 진료',
    icon: Icons.medical_services_outlined,
    color: AppColors.primary,
    backgroundColor: AppColors.primarySoft,
    areas: ['소화기', '호흡기', '심장·혈관', '내분비', '신장'],
    summary: '성인의 여러 내과 질환을 진단하고 약물치료와 생활 관리를 중심으로 진료합니다.',
    symptoms: [
      '발열이나 전신 피로가 지속될 때',
      '기침·가래·숨참 등 호흡기 증상이 있을 때',
      '속쓰림·복통·소화불량 등 소화기 증상이 있을 때',
      '고혈압·당뇨 등 만성질환 관리가 필요할 때',
    ],
    note: '어느 진료과를 가야 할지 불분명한 성인 증상은 내과에서 먼저 상담할 수 있습니다.',
  ),
  _Department(
    name: '가정의학과',
    category: '일반 진료',
    icon: Icons.family_restroom_rounded,
    color: AppColors.green,
    backgroundColor: AppColors.greenSoft,
    areas: ['건강상담', '만성질환', '예방접종', '건강검진', '생활습관'],
    summary: '연령과 질환의 종류를 폭넓게 살피며 예방, 건강관리, 만성질환을 종합적으로 진료합니다.',
    symptoms: [
      '증상이 여러 부위에 걸쳐 있거나 진료과를 정하기 어려울 때',
      '건강검진 결과에 대한 상담이 필요할 때',
      '금연·체중·운동 등 생활습관 관리가 필요할 때',
    ],
    note: '진료 후 필요하면 증상에 맞는 전문 진료과로 안내받을 수 있습니다.',
  ),
  _Department(
    name: '외과',
    category: '수술·근골격',
    icon: Icons.local_hospital_outlined,
    color: AppColors.red,
    backgroundColor: AppColors.redSoft,
    areas: ['복부 장기', '유방', '갑상선', '탈장', '피부 종괴'],
    summary: '복부 장기, 유방, 갑상선, 피부 아래 종괴 등 수술적 치료가 필요한 질환을 주로 다룹니다.',
    symptoms: [
      '복부에 지속적인 통증이나 덩이가 느껴질 때',
      '유방 또는 목 부위에 멍울이 만져질 때',
      '상처 처치나 수술 후 관리가 필요할 때',
    ],
    note: '기관마다 세부 전문 분야가 다르므로 방문 전에 진료 범위를 확인하는 것이 좋습니다.',
  ),
  _Department(
    name: '정형외과',
    category: '수술·근골격',
    icon: Icons.accessibility_new_rounded,
    color: AppColors.violet,
    backgroundColor: AppColors.violetSoft,
    areas: ['뼈·관절', '척추', '인대·힘줄', '스포츠 손상', '골절'],
    summary: '뼈, 관절, 인대, 힘줄, 근육 등 근골격계의 손상과 질환을 진료합니다.',
    symptoms: [
      '넘어지거나 부딪힌 뒤 통증과 부기가 있을 때',
      '목·허리·어깨·무릎 등 관절 통증이 지속될 때',
      '움직임이 제한되거나 골절이 의심될 때',
    ],
    note: '심한 변형, 감각 저하, 움직일 수 없을 정도의 손상은 신속한 진료가 필요합니다.',
  ),
  _Department(
    name: '신경외과',
    category: '수술·근골격',
    icon: Icons.monitor_heart_outlined,
    color: AppColors.primary,
    backgroundColor: AppColors.primarySoft,
    areas: ['뇌', '척수', '척추', '말초신경', '신경 손상'],
    summary: '뇌, 척수, 말초신경과 척추의 구조적 질환을 수술적·비수술적으로 진료합니다.',
    symptoms: [
      '목이나 허리 통증과 함께 팔·다리가 저리거나 힘이 빠질 때',
      '머리를 다친 뒤 두통이나 구토가 지속될 때',
      '척추 질환에 대한 시술·수술 상담이 필요할 때',
    ],
    note: '갑작스러운 마비, 언어 장애, 의식 변화가 나타나면 일반 외래보다 응급 대응이 우선입니다.',
  ),
  _Department(
    name: '마취통증의학과',
    category: '수술·근골격',
    icon: Icons.healing_outlined,
    color: AppColors.amber,
    backgroundColor: AppColors.amberSoft,
    areas: ['목·허리 통증', '신경통', '관절 통증', '대상포진 후 통증', '수술 마취'],
    summary: '수술 마취와 함께 급성·만성 통증을 평가하고 주사, 시술, 약물 등으로 치료합니다.',
    symptoms: [
      '통증이 오래 지속되어 일상생활이 어려울 때',
      '신경통이나 대상포진 이후 통증이 남아 있을 때',
      '통증 조절을 위한 시술 상담이 필요할 때',
    ],
    note: '통증의 원인이 불명확하면 다른 진료과 검사나 협진이 함께 필요할 수 있습니다.',
  ),
  _Department(
    name: '신경과',
    category: '뇌·감각기관',
    icon: Icons.psychology_outlined,
    color: AppColors.violet,
    backgroundColor: AppColors.violetSoft,
    areas: ['두통', '어지럼증', '뇌전증', '치매', '손발 저림'],
    summary: '뇌와 척수, 말초신경, 근육에서 생기는 질환을 주로 약물과 비수술적 방법으로 진료합니다.',
    symptoms: [
      '두통이나 어지럼증이 반복될 때',
      '손발 떨림·저림·근력 저하가 지속될 때',
      '기억력 저하, 수면 이상, 경련 등에 대한 평가가 필요할 때',
    ],
    note: '갑작스러운 한쪽 마비나 말이 어눌해지는 증상은 뇌졸중 가능성이 있어 즉시 119에 도움을 요청해야 합니다.',
  ),
  _Department(
    name: '안과',
    category: '뇌·감각기관',
    icon: Icons.visibility_outlined,
    color: AppColors.primary,
    backgroundColor: AppColors.primarySoft,
    areas: ['시력', '각막·결막', '망막', '녹내장', '백내장'],
    summary: '눈과 시력에 관련된 질환, 손상, 시각 기능의 이상을 진료합니다.',
    symptoms: [
      '눈이 충혈되거나 통증·이물감이 있을 때',
      '시야가 흐리거나 시력이 갑자기 달라졌을 때',
      '눈 외상이나 정기적인 안질환 검사가 필요할 때',
    ],
    note: '갑작스러운 시력 저하, 시야 가림, 심한 눈 통증은 빠른 안과 진료가 필요합니다.',
  ),
  _Department(
    name: '이비인후과',
    category: '뇌·감각기관',
    icon: Icons.hearing_rounded,
    color: AppColors.mint,
    backgroundColor: AppColors.mintSoft,
    areas: ['귀', '코·부비동', '목', '청각', '어지럼증'],
    summary: '귀, 코, 목과 머리·목 부위에 생기는 질환과 청각·후각·목소리 문제를 진료합니다.',
    symptoms: [
      '코막힘·콧물·인후통이 지속될 때',
      '귀 통증, 이명, 청력 변화가 있을 때',
      '목소리 변화나 삼킴 불편이 이어질 때',
    ],
    note: '어지럼증은 원인에 따라 신경과 등 다른 진료과 평가가 필요할 수 있습니다.',
  ),
  _Department(
    name: '피부과',
    category: '피부·비뇨',
    icon: Icons.spa_outlined,
    color: AppColors.green,
    backgroundColor: AppColors.greenSoft,
    areas: ['피부 발진', '알레르기', '여드름', '탈모', '손발톱'],
    summary: '피부, 모발, 손발톱과 관련된 염증·감염·알레르기·종양성 질환을 진료합니다.',
    symptoms: [
      '발진·두드러기·가려움이 지속될 때',
      '점이나 피부 병변의 모양이 변할 때',
      '탈모, 손발톱 변화, 반복되는 피부 감염이 있을 때',
    ],
    note: '호흡 곤란이나 얼굴·입술 부종을 동반한 급성 알레르기는 즉시 응급 도움을 받아야 합니다.',
  ),
  _Department(
    name: '비뇨의학과',
    category: '피부·비뇨',
    icon: Icons.water_drop_outlined,
    color: AppColors.mint,
    backgroundColor: AppColors.mintSoft,
    areas: ['신장·요관', '방광', '배뇨 장애', '요로결석', '남성 건강'],
    summary: '남녀의 소변길과 신장·방광 질환, 남성 생식기 관련 질환을 진료합니다.',
    symptoms: [
      '소변을 볼 때 아프거나 피가 보일 때',
      '소변이 자주 마렵거나 잘 나오지 않을 때',
      '옆구리 통증, 요로결석 또는 남성 생식기 증상이 있을 때',
    ],
    note: '소변을 전혀 보지 못하거나 심한 옆구리 통증과 발열이 함께 나타나면 빠른 진료가 필요합니다.',
  ),
  _Department(
    name: '산부인과',
    category: '여성·소아',
    icon: Icons.female_rounded,
    color: AppColors.red,
    backgroundColor: AppColors.redSoft,
    areas: ['임신·출산', '월경', '여성 질환', '피임', '갱년기'],
    summary: '여성 생식기 건강과 임신·출산, 월경, 갱년기와 관련된 건강 문제를 진료합니다.',
    symptoms: [
      '월경 주기 변화나 심한 생리통이 있을 때',
      '임신 확인·산전 관리가 필요할 때',
      '골반 통증, 비정상 출혈, 분비물 변화가 있을 때',
    ],
    note: '임신 중 심한 복통이나 출혈, 의식 저하가 있으면 즉시 응급 진료가 필요합니다.',
  ),
  _Department(
    name: '소아청소년과',
    category: '여성·소아',
    icon: Icons.child_care_rounded,
    color: AppColors.amber,
    backgroundColor: AppColors.amberSoft,
    areas: ['감염 질환', '성장·발달', '예방접종', '알레르기', '소아 만성질환'],
    summary: '신생아부터 청소년까지 성장 과정에서 나타나는 질환과 발달·예방 관리를 담당합니다.',
    symptoms: [
      '아이에게 발열·기침·복통·발진 등이 있을 때',
      '성장과 발달에 대한 상담이 필요할 때',
      '예방접종이나 영유아 건강관리가 필요할 때',
    ],
    note: '축 처짐, 호흡 곤란, 경련, 심한 탈수 징후가 있으면 지체하지 말고 응급 도움을 받아야 합니다.',
  ),
  _Department(
    name: '정신건강의학과',
    category: '마음·회복',
    icon: Icons.psychology_alt_outlined,
    color: AppColors.violet,
    backgroundColor: AppColors.violetSoft,
    areas: ['우울·불안', '수면', '공황', '중독', '집중력'],
    summary: '생각, 감정, 행동, 수면과 관련된 어려움을 평가하고 상담·약물 등으로 치료합니다.',
    symptoms: [
      '우울감·불안·무기력이 오래 지속될 때',
      '불면이나 공황 증상으로 일상이 어려울 때',
      '집중력, 충동, 음주·중독 문제에 대한 도움이 필요할 때',
    ],
    note: '자해나 극단적인 선택의 위험이 있거나 자신·타인의 안전이 위협받는 상황은 즉시 응급 도움을 요청해야 합니다.',
  ),
  _Department(
    name: '재활의학과',
    category: '마음·회복',
    icon: Icons.directions_walk_rounded,
    color: AppColors.green,
    backgroundColor: AppColors.greenSoft,
    areas: ['운동 기능', '통증 재활', '뇌신경 재활', '척수 손상', '연하 재활'],
    summary: '질환이나 손상 이후 떨어진 신체 기능을 평가하고 일상으로의 회복을 돕습니다.',
    symptoms: [
      '뇌졸중·외상·수술 후 기능 회복이 필요할 때',
      '근력·보행·균형 능력이 떨어졌을 때',
      '통증과 움직임 제한에 대한 재활치료가 필요할 때',
    ],
    note: '운동치료, 작업치료, 언어·삼킴 치료 등 필요한 재활 계획을 종합적으로 세울 수 있습니다.',
  ),
  _Department(
    name: '치과',
    category: '치과',
    icon: Icons.health_and_safety_outlined,
    color: AppColors.primary,
    backgroundColor: AppColors.primarySoft,
    areas: ['충치', '잇몸', '치아 손상', '턱관절', '구강 검진'],
    summary: '치아, 잇몸, 턱관절과 구강 건강 전반을 검사하고 치료합니다.',
    symptoms: [
      '치아가 아프거나 시리고 깨졌을 때',
      '잇몸 출혈·부기·구취가 지속될 때',
      '턱관절 통증이나 입을 벌리기 어려울 때',
    ],
    note: '얼굴이 심하게 붓거나 출혈이 멎지 않는 구강 외상은 신속한 진료가 필요합니다.',
  ),
];
