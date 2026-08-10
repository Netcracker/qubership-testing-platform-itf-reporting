
-- ALTER TABLE mb_tccontext ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
ALTER TABLE mb_tccontext ALTER COLUMN natural_id TYPE character varying(255) USING natural_id::varchar(255);
commit;

-- ALTER TABLE mb_context ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
ALTER TABLE mb_context ALTER COLUMN natural_id TYPE character varying(255) USING natural_id::varchar(255);
commit;

-- ALTER TABLE mb_upgrade_history ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
ALTER TABLE mb_upgrade_history ALTER COLUMN natural_id TYPE character varying(255) USING natural_id::varchar(255);
commit;

-- ALTER TABLE mb_instance ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
ALTER TABLE mb_instance ALTER COLUMN natural_id TYPE character varying(255) USING natural_id::varchar(255);
commit;

-- ALTER TABLE mb_message ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
ALTER TABLE mb_message ALTER COLUMN natural_id TYPE character varying(255) USING natural_id::varchar(255);
commit;

-- ALTER TABLE mb_message_param ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
ALTER TABLE mb_message_param ALTER COLUMN natural_id TYPE character varying(255) USING natural_id::varchar(255);
commit;
