INSERT INTO test_cat (high_card, low_card, num_range, fulltext, jsonb_col, tstzrange_col, point_col, null_col)
SELECT 
    'CAT_' || gs.i::text,
    CASE (random()*3)::int WHEN 0 THEN 'A' WHEN 1 THEN 'B' ELSE 'C' END,
    (random() * 99 + 1)::numeric(5,2),
    md5(random()::text) || ' ' || md5(random()::text),
    jsonb_build_object('id', gs.i, 'type', 'category'),
    tstzrange(now() - (random()*365)::int * interval '1 day', now()),
    point(random()*360-180, random()*180-90),
    CASE WHEN random() < 0.10 THEN NULL ELSE 'data_'||gs.i::text END
FROM generate_series(1, 250000) gs(i);

INSERT INTO test_prod (cat_id, high_card, low_card, num_range, fulltext, jsonb_col, tstzrange_col, null_col)
SELECT 
    ((random()*249999)::int + 1),
    'PROD_' || gs.i::text,
    CASE (random()*3)::int WHEN 0 THEN 'X' WHEN 1 THEN 'Y' ELSE 'Z' END,
    (random() * 490 + 10)::numeric(4,1),
    repeat(md5(random()::text), 5),
    jsonb_build_object('sku', gs.i, 'stock', (random()*100)::int),
    tstzrange(now() - (random()*180)::int * interval '1 day', now()),
    CASE WHEN random() < 0.15 THEN NULL ELSE 'spec_'||gs.i::text END
FROM generate_series(1, 300000) gs(i);

INSERT INTO test_cust (high_card, low_card, num_range, fulltext, jsonb_col, tstzrange_col, point_col, null_col)
SELECT 
    'CUST_' || gs.i::text || '_' || substring(md5(random()::text),1,10),
    CASE 
        WHEN random() < 0.70 THEN 'Reg'
        WHEN random() < 0.90 THEN 'VIP'
        ELSE 'New' END,
    (random() * 9999.99)::numeric(6,2),
    repeat('customer_bio_', (random()*10)::int),
    jsonb_build_object('tier', 'gold', 'points', (random()*1000)::int),
    tstzrange(current_date - (random()*730)::int, current_date),
    point(random()*360-180, random()*180-90),
    CASE WHEN random() < 0.20 THEN NULL ELSE 'ref_'||gs.i::text END
FROM generate_series(1, 250000) gs(i);

INSERT INTO test_orders (cust_id, prod_id, high_card, low_card, num_range, fulltext, jsonb_col, tstzrange_col, null_col)
SELECT 
    ((random()*249999)::int + 1),
    ((random()*299999)::int + 1),
    'ORD_' || gs.i::text,
    CASE (random()*3)::int WHEN 0 THEN 'pend' WHEN 1 THEN 'ship' ELSE 'deliv' END,
    (random() * 4999 + 1)::numeric(6,2),
    'Order notes: ' || md5(random()::text),
    jsonb_build_object('items', (random()*10)::int, 'promo', random() < 0.3),
    tstzrange(now() - (random()*90)::int * interval '1 day', now()),
    CASE WHEN random() < 0.05 THEN NULL ELSE 'track_'||gs.i::text END
FROM generate_series(1, 250000) gs(i);
