## Insert

### так будем смотреть

найдем ctid и будем дальше работать с расширением

```
	SELECT xmin, xmax, ctid, high_card
	FROM test_cat
	WHERE high_card = 'cat_1'
```

```
INSERT INTO test_cat
(high_card, low_card, num_range, fulltext, jsonb_col, tstzrange_col, point_col)
VALUES
('test2','A',15.5,'test text','{"a":1}','[2024-01-01,2024-12-31]', '(5,5)');
```

![alt text](images/5_1.png)
![alt text](images/5_2.png)

## update

```
UPDATE test_cat set fulltext = 'full text' WHERE id = 250005;
```

так как update = insert в новой строке
![alt text](images/5_3.png)

## delete

```
	DELETE FROM test_cat
	WHERE high_card = 'test2';
```

![alt text](images/5_4.png)

## Значение t_infomask представляет собой битовую маску, где каждая

HEAP_HASNULL 1
HEAP_HASVARWIDTH 2
HEAP_HASEXTERNAL 4
HEAP_HASOID_OLD 8
HEAP_XMAX_KEYSHR_LOCK 16
HEAP_COMBOCID 32
HEAP_XMAX_EXCL_LOCK 64
HEAP_XMAX_LOCK_ONLY 128
HEAP_XMIN_COMMITTED 256
HEAP_XMIN_INVALID 512
HEAP_XMAX_COMMITTED 1024
HEAP_XMAX_INVALID 2048
HEAP_UPDATED 4096

2307 = 2048 + 256 + 2 + 1
259 = 256 + 2 + 1
10243 = 8192 + 2048 + 2 + 1
1283 = 1024 + 256 + 2 + 1
8451 = 8192 + 256 + 2 + 1

## Просмотр в транзакции

создал новую таблицу, вставил и прочитал ее с xmin, xmax, ctid
![alt text](images/5_5.png)

```
	BEGIN;

	UPDATE mvcc_test
	SET value = 'test2'
	WHERE id = 1;

	SELECT xmin, xmax, ctid, value
	FROM mvcc_test;
```

![alt text](images/5_6.png)

```
	BEGIN;
	SELECT xmin, xmax, ctid, value
	FROM mvcc_test;
```

![alt text](images/5_7.png)

и после commit первого, у нас при поторном вызове во втором данные изменятся

![alt text](images/5_8.png)

## deadlock

```
	BEGIN;

	UPDATE deadlock_test
	SET value = 'T1'
	WHERE id = 1;

	------------

	UPDATE deadlock_test
	SET value = 'T1'
	WHERE id = 2;
```

![alt text](images/5_10.png)

```
	BEGIN;

	UPDATE deadlock_test
	SET value = 'T2'
	WHERE id = 2;

	------------

	UPDATE deadlock_test
	SET value = 'T2'
	WHERE id = 1;

```

![alt text](images/5_9.png)

СУБД блокирует работу транзакции, не давая сделать дедлок

## Режимы блокировок

```
	CREATE TABLE row_lock_test (
    id INT PRIMARY KEY,
    value TEXT
	);

	INSERT INTO row_lock_test VALUES
	(1,'A'),
	(2,'B');
```

### for update

```
	BEGIN;

	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR UPDATE;
```

![alt text](images/5_12.png)

```
	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR UPDATE;
```

ждет выполнения (окончания) второй транзакции
![alt text](images/5_11.png)

### for no key update

```
	BEGIN;

	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR NO KEY UPDATE;
```

```
	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR UPDATE;
```

аналогично ждет

### for share

```
	BEGIN;

	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR SHARE;
```

```
	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR SHARE;
```

нету конфликтов, они совместимы между собой

## for key share

```
	BEGIN;

	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR KEY SHARE;
```

```
	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR KEY SHARE;
```

## типичный конфликт на разные методы

```
	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR UPDATE;
```

![alt text](images/5_13.png)

```
	BEGIN;
	SELECT *
	FROM row_lock_test
	WHERE id = 1
	FOR SHARE;
```

![alt text](images/5_14.png)

## очистка данных

```
SELECT relname,
       n_live_tup AS живых_строк,
       n_dead_tup AS мёртвых_строк,
       last_vacuum,
       last_autovacuum
FROM pg_stat_all_tables
WHERE relname = 'test';
```

![alt text](images/5_15.png)

```
	vacuum verbose test;
```

![alt text](images/5_16.png)

```
SELECT relname,
       n_live_tup AS живых_строк,
       n_dead_tup AS мёртвых_строк,
       last_vacuum,
       last_autovacuum
FROM pg_stat_all_tables
WHERE relname = 'test';
```

![alt text](images/5_17.png)

```
	select pg_size_pretty(pg_total_relation_size('test')) as размер_до;
```

![alt text](images/5_18.png)

```
	VACUUM FULL test;
```

```
	select pg_size_pretty(pg_total_relation_size('test')) as размер_после;
```

![alt text](images/5_19.png)
