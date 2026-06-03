-- Creates one database per ArcPay service plus Temporal's databases.
-- Runs once on first Postgres container start (mounted into /docker-entrypoint-initdb.d).
-- The role/owner is the POSTGRES_USER (arcpay); each service runs Flyway against its own DB.
CREATE DATABASE arcpay_identity;
CREATE DATABASE arcpay_policy;
CREATE DATABASE arcpay_compliance;
CREATE DATABASE arcpay_payment;
CREATE DATABASE arcpay_settlement;

-- Temporal (auto-setup runs its schema tooling against these; SKIP_DB_CREATE=true).
CREATE DATABASE temporal;
CREATE DATABASE temporal_visibility;
