-- Product entity changed average_rating from BigDecimal NUMERIC(3,2) to primitive
-- double (Hibernate float8 / double precision). V4 created the column as NUMERIC,
-- so existing deployments fail Hibernate schema-validation:
--   wrong column type encountered in column [average_rating] in table [products];
--   found [numeric (Types#NUMERIC)], but expecting [float(53) (Types#FLOAT)]
-- Convert in place. Postgres preserves data; precision goes from 0.01 to IEEE-754
-- which is fine for an aggregate display value.
ALTER TABLE products
    ALTER COLUMN average_rating TYPE DOUBLE PRECISION
    USING average_rating::double precision;
