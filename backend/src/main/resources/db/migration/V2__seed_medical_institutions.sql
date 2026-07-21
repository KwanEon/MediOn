INSERT INTO medical_institutions (
    external_id, type, name, phone_number, road_address, lot_address,
    latitude, longitude, active, last_synced_at, created_at, updated_at
) VALUES
('sample-hospital-001', 'HOSPITAL', '서울중앙의원', '02-1000-1000', '서울특별시 중구 세종대로 110', '서울특별시 중구 태평로1가', 37.5667800, 126.9784300, TRUE, '2026-07-13 09:00:00', NOW(), NOW()),
('sample-hospital-002', 'HOSPITAL', '광화문내과의원', '02-1000-2000', '서울특별시 종로구 세종대로 175', '서울특별시 종로구 세종로', 37.5720100, 126.9769300, TRUE, '2026-07-13 09:00:00', NOW(), NOW()),
('sample-pharmacy-001', 'PHARMACY', '시청온누리약국', '02-2000-1000', '서울특별시 중구 을지로 12', '서울특별시 중구 을지로1가', 37.5656100, 126.9822200, TRUE, '2026-07-13 09:00:00', NOW(), NOW()),
('sample-pharmacy-002', 'PHARMACY', '덕수궁약국', '02-2000-2000', '서울특별시 중구 세종대로 99', '서울특별시 중구 정동', 37.5659100, 126.9747800, TRUE, '2026-07-13 09:00:00', NOW(), NOW()),
('sample-hospital-003', 'HOSPITAL', '마포야간의원', '02-3000-1000', '서울특별시 마포구 마포대로 92', '서울특별시 마포구 도화동', 37.5411300, 126.9457700, TRUE, '2026-07-13 09:00:00', NOW(), NOW());

INSERT INTO operating_hours (institution_id, day_of_week, open_time, close_time, closed, lunch_start_time, lunch_end_time)
SELECT id, day_name, '09:00:00', '18:00:00', FALSE, '13:00:00', '14:00:00'
FROM medical_institutions
JOIN (
    SELECT 'MONDAY' AS day_name UNION ALL
    SELECT 'TUESDAY' UNION ALL
    SELECT 'WEDNESDAY' UNION ALL
    SELECT 'THURSDAY' UNION ALL
    SELECT 'FRIDAY'
) weekdays
WHERE external_id IN ('sample-hospital-001', 'sample-hospital-002');

INSERT INTO operating_hours (institution_id, day_of_week, open_time, close_time, closed)
SELECT id, day_name, '08:30:00', '22:00:00', FALSE
FROM medical_institutions
JOIN (
    SELECT 'MONDAY' AS day_name UNION ALL
    SELECT 'TUESDAY' UNION ALL
    SELECT 'WEDNESDAY' UNION ALL
    SELECT 'THURSDAY' UNION ALL
    SELECT 'FRIDAY' UNION ALL
    SELECT 'SATURDAY' UNION ALL
    SELECT 'SUNDAY'
) all_days
WHERE external_id IN ('sample-pharmacy-001', 'sample-pharmacy-002');

INSERT INTO operating_hours (institution_id, day_of_week, open_time, close_time, closed)
SELECT id, day_name, '18:00:00', '02:00:00', FALSE
FROM medical_institutions
JOIN (
    SELECT 'MONDAY' AS day_name UNION ALL
    SELECT 'TUESDAY' UNION ALL
    SELECT 'WEDNESDAY' UNION ALL
    SELECT 'THURSDAY' UNION ALL
    SELECT 'FRIDAY' UNION ALL
    SELECT 'SATURDAY'
) night_days
WHERE external_id = 'sample-hospital-003';

INSERT INTO data_sync_histories (source_name, target_type, status, synced_at, message)
VALUES ('sample-seed', 'MEDICAL_INSTITUTION', 'SUCCESS', '2026-07-13 09:00:00', 'Initial sample data loaded.');

