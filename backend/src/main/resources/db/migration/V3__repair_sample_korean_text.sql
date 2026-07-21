UPDATE medical_institutions
SET name = '서울중앙의원',
    road_address = '서울특별시 중구 세종대로 110',
    lot_address = '서울특별시 중구 태평로1가'
WHERE external_id = 'sample-hospital-001';

UPDATE medical_institutions
SET name = '광화문내과의원',
    road_address = '서울특별시 종로구 세종대로 175',
    lot_address = '서울특별시 종로구 세종로'
WHERE external_id = 'sample-hospital-002';

UPDATE medical_institutions
SET name = '시청온누리약국',
    road_address = '서울특별시 중구 을지로 12',
    lot_address = '서울특별시 중구 을지로1가'
WHERE external_id = 'sample-pharmacy-001';

UPDATE medical_institutions
SET name = '정수권약국',
    road_address = '서울특별시 중구 세종대로 99',
    lot_address = '서울특별시 중구 정동'
WHERE external_id = 'sample-pharmacy-002';

UPDATE medical_institutions
SET name = '마포야간의원',
    road_address = '서울특별시 마포구 마포대로 92',
    lot_address = '서울특별시 마포구 도화동'
WHERE external_id = 'sample-hospital-003';
