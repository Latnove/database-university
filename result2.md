## =

```
	EXPLAIN (ANALYZE)
	SELECT * FROM test_prod WHERE high_card = 'PROD_150000';

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE high_card = 'PROD_150000';
```

### без индексов

![alt text](images/21.png)

### с b-tree индексом

![alt text](images/27.png)

### с hash индексом

![alt text](images/32.png)

## >

```
	EXPLAIN (ANALYZE)
	SELECT * FROM test_prod WHERE num_range > 450;

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE num_range > 450;
```

### без индексов

![alt text](images/22.png)

### с b-tree индексом

![alt text](images/28.png)

### с hash индексом

не поддерживает

## <

```
	EXPLAIN (ANALYZE)
	SELECT * FROM test_prod WHERE num_range < 50;

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE num_range < 50;
```

### без индексов

![alt text](images/23.png)

### с b-tree индексом

![alt text](images/29.png)

### с hash индексом

не поддерживает

## LIKE с суффиксом (%like)

```
	EXPLAIN (ANALYZE)
	SELECT * FROM test_prod WHERE low_card LIKE '%Y';

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE low_card LIKE '%Y';
```

### без индексов

![alt text](images/24.png)

### с b-tree индексом

Не поддерживается b-tree индексом

### с hash индексом

Не поддерживается hash индексом

## LIKE с префиксом (like%)

```
	EXPLAIN (ANALYZE)
	SELECT * FROM test_prod WHERE high_card LIKE 'PROD_1%';

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE high_card LIKE 'PROD_1%';
```

### без индексов

![alt text](images/25.png)

### с b-tree индексом (само выбрало не использовать индексы, связано с тем, что около 38% всех записей в бд, это то что мы ищем и это дольше, чем простой поиск)

![alt text](images/30.png)

### с hash индексом

аналогично b-tree

![alt text](images/33.png)

## IN (множественный поиск)

```
	EXPLAIN (ANALYZE)
	SELECT * FROM test_prod WHERE high_card IN ('PROD_150000', 'PROD_150001', 'PROD_150002');

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE high_card IN ('PROD_150000', 'PROD_150001', 'PROD_150002');
```

### без индексов

![alt text](images/26.png)

### с b-tree индексом

![alt text](images/31.png)

### с hash индексом

![alt text](images/34.png)

# Составной индекс

## без индекса

```
	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE low_card = 'X' AND num_range > 400;

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE low_card = 'X' ORDER BY num_range LIMIT 10;

	EXPLAIN (ANALYZE, BUFFERS)
	SELECT * FROM test_prod WHERE low_card IN ('X', 'Y') AND num_range BETWEEN 100 AND 200;
```

![alt text](images/36.png)
![alt text](images/35.png)

## с составным индексом

```
	CREATE INDEX idx_prod_composite1 ON test_prod(low_card, num_range);
	CREATE INDEX idx_prod_composite2 ON test_prod(num_range, low_card);
	CREATE INDEX idx_prod_composite3 ON test_prod(low_card) INCLUDE (num_range, high_card);
```

![alt text](images/37.png)
![alt text](images/38.png)
