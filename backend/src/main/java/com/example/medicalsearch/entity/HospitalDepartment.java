package com.example.medicalsearch.entity;

public enum HospitalDepartment {
    INTERNAL_MEDICINE("D001", "내과"),
    PEDIATRICS("D002", "소아청소년과", "소아과"),
    NEUROLOGY("D003", "신경과"),
    MENTAL_HEALTH_MEDICINE("D004", "정신건강의학과"),
    DERMATOLOGY("D005", "피부과"),
    SURGERY("D006", "외과"),
    CARDIOTHORACIC_SURGERY("D007", "심장혈관흉부외과", "흉부외과"),
    ORTHOPEDICS("D008", "정형외과"),
    NEUROSURGERY("D009", "신경외과"),
    PLASTIC_SURGERY("D010", "성형외과"),
    OBSTETRICS_GYNECOLOGY("D011", "산부인과"),
    OPHTHALMOLOGY("D012", "안과"),
    OTOLARYNGOLOGY("D013", "이비인후과"),
    UROLOGY("D014", "비뇨의학과"),
    REHABILITATION_MEDICINE("D016", "재활의학과"),
    ANESTHESIOLOGY_PAIN_MEDICINE("D017", "마취통증의학과", "마취통증과"),
    RADIOLOGY("D018", "영상의학과"),
    FAMILY_MEDICINE("D022", "가정의학과"),
    DENTISTRY("D026", "치과"),
    KOREAN_CLINIC(null, "한의원");

    private final String publicDataCode;
    private final String officialName;
    private final String displayName;

    HospitalDepartment(String publicDataCode, String officialName) {
        this(publicDataCode, officialName, officialName);
    }

    HospitalDepartment(String publicDataCode, String officialName, String displayName) {
        this.publicDataCode = publicDataCode;
        this.officialName = officialName;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean identifies(String value) {
        return value != null && (
                (publicDataCode != null && publicDataCode.equalsIgnoreCase(value))
                        || officialName.equals(value)
                        || displayName.equals(value)
        );
    }

}
