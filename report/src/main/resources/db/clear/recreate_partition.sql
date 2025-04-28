SET search_path TO :"schema";
ALTER TABLE :"parent_table_name" DETACH PARTITION :"part_table_name" CONCURRENTLY;
drop table :"part_table_name";
select create_partition(:'parent_table_name'::varchar, :part_num::int2);