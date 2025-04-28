CREATE OR REPLACE FUNCTION public.update_tccontext_after()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare
    initiator record;
    found boolean := false;
begin
    begin
        select i.type, i.name into initiator from mb_instance i
            where i.id = NEW.initiator_id and i.part_num = NEW.part_num;
        found := true;
        exception
            when no_data_found then
                update mb_tccontext
                    set "name"=NEW."name",
                        initiator_id=NEW.initiator_id,
                        environment_id=NEW.environment_id, environment_name=NEW.environment_name,
                        status=NEW.status, start_time=NEW.start_time, end_time=NEW.end_time,
                        project_id=NEW.project_id, client=NEW.client,
                        duration=
                        (case
                            when NEW.start_time IS null THEN null
                            else (extract(epoch from coalesce(date_trunc('second', NEW.end_time), CURRENT_TIMESTAMP(0))
                              - date_trunc('second', NEW.start_time)))*1000
                        END)
                    where id=NEW.id and part_num = NEW.part_num;
    end;
    if found then
        update mb_tccontext
            set "name"=NEW."name",
                initiator_id=NEW.initiator_id,
                initiator_name=initiator.name,
                initiator_type=initiator.type,
                environment_id=NEW.environment_id, environment_name=NEW.environment_name,
                status=NEW.status, start_time=NEW.start_time, end_time=NEW.end_time,
                project_id=NEW.project_id, client=NEW.client,
                duration=
                (case
                    when NEW.start_time IS null THEN null
                    else (extract(epoch from coalesce(date_trunc('second', NEW.end_time), CURRENT_TIMESTAMP(0))
                      - date_trunc('second', NEW.start_time)))*1000
                END)
            where id=NEW.id and part_num = NEW.part_num;
    end if;
    Return NEW;
END;
$function$
;
CREATE OR REPLACE FUNCTION public.insert_tccontext_after()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    insert into mb_tccontext
        (id, "name", initiator_id, environment_id, environment_name, status,
        start_time, end_time,
        project_id, client, duration)
    values
        (NEW.id, NEW."name", NEW.initiator_id, NEW.environment_id, NEW.environment_name, NEW.status,
        NEW.start_time, NEW.end_time,
        NEW.project_id, NEW.client,
        (case
            when NEW.start_time IS null THEN null
            else (extract(epoch from coalesce(date_trunc('second', NEW.end_time), CURRENT_TIMESTAMP(0))
              - date_trunc('second', NEW.start_time)))*1000
        END)
    );
    Return NEW;
END;
$function$
;
