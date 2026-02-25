ALTER TABLE test_cat DROP CONSTRAINT IF EXISTS test_cat_high_card_key CASCADE;
ALTER TABLE test_prod DROP CONSTRAINT IF EXISTS test_prod_high_card_key CASCADE;
ALTER TABLE test_cust DROP CONSTRAINT IF EXISTS test_cust_high_card_key CASCADE;
ALTER TABLE test_orders DROP CONSTRAINT IF EXISTS test_orders_high_card_key CASCADE;