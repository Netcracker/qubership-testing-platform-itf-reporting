
ALTER TABLE mb_tccontext ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_context ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_upgrade_history ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_instance ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_message ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;

ALTER TABLE mb_message_param ALTER COLUMN natural_id TYPE bigint USING natural_id::bigint;
commit;
