```
	CREATE TABLE wal_test (
    id SERIAL PRIMARY KEY,
    data TEXT
	);
```

## проанализируем lsn

```
	SELECT pg_current_wal_lsn();
	INSERT INTO wal_test (data) VALUES ('abc');
	SELECT pg_current_wal_lsn();
```

![alt text](images/6_1.png)

![alt text](images/6_2.png)

```
	SELECT pg_wal_lsn_diff('0/3D373418', '0/3D373270');
```

Столько байт записалось
![alt text](images/6_3.png)

## проверим wal в транзакции

```
	SELECT wal_records, wal_bytes FROM pg_stat_wal;
	BEGIN;
	INSERT INTO wal_test (data) VALUES ('tx test');
	SELECT wal_records, wal_bytes FROM pg_stat_wal;
	COMMIT;
	SELECT wal_records, wal_bytes FROM pg_stat_wal;
```

![alt text](images/6_4.png)
![alt text](images/6_5.png)
![alt text](images/6_6.png)

если посмотреть по lsn, то
![alt text](images/6_7.png)
![alt text](images/6_8.png)

спустя время если вызвать не завершая коммит, то он обновится это связано с тем, что wal не всегда сразу обновляется в основном с задержкой
![alt text](images/6_9.png)

## массовые действия

```
	SELECT wal_records, wal_bytes FROM pg_stat_wal;
	INSERT INTO wal_test (data)
	SELECT 'bulk ' || generate_series(1,10000);
	SELECT wal_records, wal_bytes FROM pg_stat_wal;

```

![alt text](images/6_10.png)
![alt text](images/6_11.png)

```
	docker exec -t shop-postgres-service pg_dump -U postgres -s shopdb > structure.sql
	cat structure.sql | docker exec -i shop-postgres-service psql -U postgres -d test_db
```

![alt text](images/6_12.png)

```
	docker exec -t shop-postgres-service pg_dump -U postgres -t users shopdb > users.sql
	cat users.sql | docker exec -i shop-postgres-service psql -U postgres -d test_db
```

![alt text](images/6_13.png)

```
	cat seed.sql | docker exec -i shop-postgres-service psql -U postgres -d shopdb
```

![alt text](images/6_14.png)
![alt text](images/6_15.png)
