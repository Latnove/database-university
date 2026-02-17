CREATE TABLE test_cat (
    id SERIAL PRIMARY KEY,
    high_card VARCHAR(50) UNIQUE NOT NULL,
    low_card VARCHAR(10) CHECK (low_card IN ('A','B','C')),
    num_range NUMERIC(5,2) CHECK (num_range BETWEEN 1 AND 100),
    fulltext TEXT,
    jsonb_col JSONB,
    tstzrange_col TSTZRANGE,
    point_col POINT,
    null_col TEXT
);

CREATE TABLE test_prod (
    id SERIAL PRIMARY KEY,
    cat_id INTEGER REFERENCES test_cat(id),
    high_card VARCHAR(30) UNIQUE NOT NULL,
    low_card VARCHAR(5) CHECK (low_card IN ('X','Y','Z')),
    num_range NUMERIC(4,1) CHECK (num_range BETWEEN 10 AND 500),
    fulltext TEXT,
    jsonb_col JSONB,
    tstzrange_col TSTZRANGE,
    null_col TEXT
);

CREATE TABLE test_cust (
    id SERIAL PRIMARY KEY,
    high_card VARCHAR(40) UNIQUE NOT NULL,
    low_card VARCHAR(8) CHECK (low_card IN ('VIP','Reg','New')),
    num_range NUMERIC(6,2) CHECK (num_range BETWEEN 0 AND 10000),
    fulltext TEXT,
    jsonb_col JSONB,
    tstzrange_col TSTZRANGE,
    point_col POINT,
    null_col TEXT
);

CREATE TABLE test_orders (
    id SERIAL PRIMARY KEY,
    cust_id INTEGER REFERENCES test_cust(id),
    prod_id INTEGER REFERENCES test_prod(id),
    high_card VARCHAR(30) UNIQUE NOT NULL,
    low_card VARCHAR(10) CHECK (low_card IN ('pend','ship','deliv')),
    num_range NUMERIC(6,2) CHECK (num_range BETWEEN 1 AND 5000),
    fulltext TEXT,
    jsonb_col JSONB,
    tstzrange_col TSTZRANGE,
    null_col TEXT
);
