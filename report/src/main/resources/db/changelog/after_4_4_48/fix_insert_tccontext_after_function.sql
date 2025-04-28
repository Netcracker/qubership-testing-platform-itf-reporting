CREATE OR REPLACE FUNCTION public.insert_tccontext_after()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare
    initiator record;
    found boolean := false;
BEGIN
    begin
        select i.type, i.name into initiator from mb_instance i
            where i.id = NEW.initiator_id and i.part_num = NEW.part_num;
        found := true;
        exception
            when no_data_found then
				insert into mb_tccontext
					(id, "name", part_num, initiator_id, environment_id, environment_name,
					status, start_time, end_time,
					project_id, client, duration)
				values
					(NEW.id, NEW."name", NEW.part_num, NEW.initiator_id, NEW.environment_id, NEW.environment_name,
					NEW.status, NEW.start_time, NEW.end_time,
					NEW.project_id, NEW.client,
					(case
						when NEW.start_time IS null THEN null
						else (extract(epoch from coalesce(date_trunc('second', NEW.end_time), CURRENT_TIMESTAMP(0))
						  - date_trunc('second', NEW.start_time)))*1000
					END)
				);
    end;
    if found then
		insert into mb_tccontext
			(id, "name", part_num, initiator_id, environment_id, environment_name,
			status, start_time, end_time, initiator_name, initiator_type,
			project_id, client, duration)
		values
			(NEW.id, NEW."name", NEW.part_num, NEW.initiator_id, NEW.environment_id, NEW.environment_name,
			NEW.status, NEW.start_time, NEW.end_time, initiator.name, initiator.type,
			NEW.project_id, NEW.client,
			(case
				when NEW.start_time IS null THEN null
				else (extract(epoch from coalesce(date_trunc('second', NEW.end_time), CURRENT_TIMESTAMP(0))
				  - date_trunc('second', NEW.start_time)))*1000
			END)
		);
    end if;
    Return NEW;
END;
$function$
;
