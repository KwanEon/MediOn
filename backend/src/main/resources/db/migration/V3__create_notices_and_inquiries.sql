CREATE TABLE notices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    published_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notices_created_by
        FOREIGN KEY (created_by) REFERENCES app_users (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_notices_pinned_published
    ON notices (pinned, published_at);

CREATE TABLE inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiries_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_inquiries_user_created
    ON inquiries (user_id, created_at);

CREATE INDEX idx_inquiries_status_created
    ON inquiries (status, created_at);

INSERT INTO notices (
    category, title, content, pinned, created_by, published_at, created_at, updated_at
) VALUES
(
    'UPDATE',
    '건강 정보와 이용 안내 콘텐츠를 새롭게 구성했습니다.',
    '건강 정보에서 의료기관 선택과 방문 준비에 필요한 내용을 확인할 수 있습니다.\n\n이용 안내에서 위치 검색, 필터, 즐겨찾기 사용법과 자주 묻는 질문을 확인할 수 있습니다.',
    TRUE,
    NULL,
    '2026-07-24 09:00:00',
    '2026-07-24 09:00:00',
    '2026-07-24 09:00:00'
),
(
    'IMPORTANT',
    '의료기관 운영시간은 방문 전에 다시 확인해 주세요.',
    '메디온의 운영시간은 공공 의료데이터를 기반으로 제공됩니다.\n\n임시 휴진, 접수 조기 마감, 공휴일 운영 등 현장 상황이 즉시 반영되지 않을 수 있으므로 방문 전 해당 기관에 전화로 확인해 주세요.',
    TRUE,
    NULL,
    '2026-07-24 08:50:00',
    '2026-07-24 08:50:00',
    '2026-07-24 08:50:00'
),
(
    'DATA',
    '응급실 병상 정보 이용 시 참고 사항을 안내합니다.',
    '응급실 병상 정보는 관계 기관이 제공하는 데이터를 바탕으로 표시합니다.\n\n정보가 갱신되는 사이 실제 수용 가능 여부가 달라질 수 있으며, 위급한 상황에는 직접 이동하기보다 119의 안내를 받아 주세요.',
    FALSE,
    NULL,
    '2026-07-24 08:40:00',
    '2026-07-24 08:40:00',
    '2026-07-24 08:40:00'
),
(
    'GUIDE',
    '현재 위치를 사용할 수 없을 때는 주소 검색을 이용해 주세요.',
    '브라우저나 앱에서 위치 권한이 차단되었거나 현재 위치가 정확하지 않다면 주소 검색 기능을 이용할 수 있습니다.\n\n로그인한 사용자는 내 정보에서 자주 사용하는 주소를 저장할 수 있습니다.',
    FALSE,
    NULL,
    '2026-07-24 08:30:00',
    '2026-07-24 08:30:00',
    '2026-07-24 08:30:00'
);
