SET search_path TO public;

insert into mb_config (key, value)
values
    ('partitioning','false'),
    ('leaveDays','7'),
    ('clearHours','6')
on conflict (key)
do update set value = EXCLUDED.value;
