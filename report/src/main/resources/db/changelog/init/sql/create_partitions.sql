SET search_path TO public;

insert into mb_config (key, value)
values
    ('partitioning','true'),
    ('partitionsPeriodDays','5'),
    ('partitionsAmount','6'),
    ('partitionsZeroPoint',current_date)
on conflict (key)
    do update set value = EXCLUDED.value;

select create_partitions();