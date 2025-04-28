CREATE OR REPLACE FUNCTION public.update_context_before()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
begin
	if NEW."type" = 'TcContext'
		and NEW.status in ('NOT_STARTED','PAUSED','IN_PROGRESS')
		and OLD.status in ('PASSED','FAILED','STOPPED','FAILED_BY_TIMEOUT') then
	    Return OLD;
	else
		Return NEW;
    end if;
END;
$function$
;
create trigger update_context_before_update
before update
    of id, "type", "name", extensions, json_string, initiator_id,
    status, start_time, end_time, project_id, environment_id,
	environment_name, part_num, last_update_time, pod_name
on
    public.mb_context for each row
    when (((old."type")::text = 'TcContext'::text)) execute procedure update_context_before();