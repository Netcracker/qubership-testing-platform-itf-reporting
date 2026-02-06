alter table if exists mb_tccontext
    add column if not exists situation_id int8 NULL,
    add column if not exists operation_name varchar(255) NULL,
	add column if not exists system_name varchar(255) NULL,
	add column if not exists system_id int8 NULL,
	add column if not exists chain_id int8 NULL,
	add column if not exists duration float8 NULL;
commit;
