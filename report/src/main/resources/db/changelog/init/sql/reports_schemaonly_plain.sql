--
-- PostgreSQL database dump
--

-- Dumped from database version 10.8
-- Dumped by pg_dump version 10.8

-- Started on 2023-10-27

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: tree_info; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tree_info AS (
                                    "CID" bigint,
                                    "PARENT" bigint,
                                    "TYPE" character varying(255),
                                    "DESCRIPTION" text,
                                    "STATUS" character varying(255),
                                    "DURATION" character varying(255)
                                );


--
-- Name: tree_info_full; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tree_info_full AS (
                                         "CID" bigint,
                                         "PARENT" bigint,
                                         "TYPE" character varying(255),
                                         "DESCRIPTION" text,
                                         "PATH" character varying(255),
                                         "LEVEL" bigint,
                                         "STATUS" character varying(255),
                                         "DURATION" character varying(255)
                                     );


--
-- Name: clean_constraints(bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.clean_constraints(context_max_id_to_delete bigint) RETURNS void
    LANGUAGE plpgsql
AS $$
BEGIN
    update mb_context
    set initiator_id = null,
        step_id = null,
        parent_ctx_id = null,
        incoming_message_id = null,
        outgoing_message_id = null,
        environment_id = null,
        instance = null,
        tc_id = null
    where id <= context_max_id_to_delete;

    update mb_instance
    set step_id = null,
        chain_id = null,
        situation_id = null,
        parent_id = null
    where context_id <= context_max_id_to_delete;
END;
$$;


--
-- Name: clear_monitoring_data(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.clear_monitoring_data() RETURNS character varying
    LANGUAGE plpgsql
AS $$
declare
    leave_days integer;
    clear_hours integer;
    p_id bigint;
    result varchar;
    final_result varchar := '';
begin
    select coalesce(value, '1')::integer from mb_config where key = 'leaveDays' into leave_days;
    select coalesce(value, '3')::integer from mb_config where key = 'clearHours' into clear_hours;
    <<clear>>
    for p_id in
        select distinct project_id
        from mb_context mc
        where project_id is not null
        loop
            select clear_monitoring_data_func(leave_days, clear_hours, p_id) into result;
            final_result := final_result || E'\n' || 'ProjectID: ' || p_id || ' - ' || result;
        end loop clear;
    return substring(final_result, 2);
end;
$$;


--
-- Name: clear_monitoring_data_func(integer, integer, bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.clear_monitoring_data_func(leave_days integer, clear_hours integer, project_id_to_clear bigint) RETURNS character varying
    LANGUAGE plpgsql
AS $$
declare
    mindate timestamp;
    cnt int;
begin
    SET session_replication_role = 'replica';

    drop table if exists copy_mb_message_param;
    drop table if exists copy_mb_tccontext;
    drop table if exists copy_messageids;
    drop table if exists copy_situation_instances;
    drop table if exists copy_spcontext;
    drop table if exists copy_step_instance_context;
    drop table if exists copy_step_instances;

-- #1 Get tc context ids to delete
    select least(min(start_time), now() - make_interval(days => leave_days)) + make_interval(hours => clear_hours)
    into mindate
    from mb_context where "type" = 'TcContext'
                      and project_id = project_id_to_clear;

    create unlogged table copy_mb_tccontext
        with (autovacuum_enabled=false, toast.autovacuum_enabled=false)
    as select id, initiator_id from mb_context
       where "type" = 'TcContext' and start_time < mindate
         and project_id = project_id_to_clear;

    select count(*) into cnt from copy_mb_tccontext;
    if cnt = 0 then
        SET session_replication_role = 'origin';
        drop table if exists copy_mb_tccontext;
        return 'No data to delete; a new minimal timestamp is ' || mindate;
    end if;

    create index idx00 on copy_mb_tccontext(id);

-- #2 - Delete from mb_context_binding_keys (children of tc-context)

    delete from mb_context_binding_keys where id in (select id from copy_mb_tccontext);

-- #3 - Delete from mb_context_report_links (children of tc-context)

    delete from mb_context_report_links where parent_id in (select id from copy_mb_tccontext);

    -- Delete like get_messages_from_tc_context_no_message does, step by step (in opposite order)...
-- #4 - Situation instances - we need only 'id' column

    create unlogged table copy_situation_instances
        with (autovacuum_enabled=false, toast.autovacuum_enabled=false)
    as select mb_instance.id from mb_instance inner join copy_mb_tccontext on mb_instance.context_id=copy_mb_tccontext.id;

-- #5 - Step instances - we need only 'id' column

    create index idx01 on copy_situation_instances(id);
    create unlogged table copy_step_instances
        with (autovacuum_enabled=false, toast.autovacuum_enabled=false)
    as select mb_instance.id from mb_instance inner join copy_situation_instances on mb_instance.parent_id=copy_situation_instances.id;

-- #6 - Instance context - we need only 'id' column

    create index idx02 on copy_step_instances(id);
    create unlogged table copy_step_instance_context
        with (autovacuum_enabled=false, toast.autovacuum_enabled=false)
    as select mb_context.id from mb_context inner join copy_step_instances on mb_context."instance"=copy_step_instances.id;

-- #7 Delete from mb_context (situation instance contexts) - could be slow

    delete from mb_context where mb_context."instance" in (select id from copy_situation_instances);

-- #8 - SpContext - we need only id, incoming_message_id, outgoing_message_id

    create index idx03 on copy_step_instance_context(id);
    create unlogged table copy_spcontext
        with (autovacuum_enabled=false, toast.autovacuum_enabled=false)
    as select mb_context.id, mb_context.incoming_message_id, mb_context.outgoing_message_id
       from mb_context inner join copy_step_instance_context on mb_context.parent_ctx_id=copy_step_instance_context.id;

-- #9 - copy mb_message_param (children of sp-context) - we need only id

    create index idx04 on copy_spcontext(id);
    create unlogged table copy_mb_message_param
        with (autovacuum_enabled=false, toast.autovacuum_enabled=false)
    as select mb_message_param.id from mb_message_param where context_id in (select id from copy_spcontext);

-- #10 - Delete from mb_message_param_multiple_value (children of mb_message_param), then from mb_message_param

    create index idx05 on copy_mb_message_param(id);
    delete from mb_message_param_multiple_value where message_param_id in (select id from copy_mb_message_param);
    delete from mb_message_param where context_id in (select id from copy_spcontext);

-- #11 - Prepare message ids to delete messages and subordinates

    create index copy_spcontext_incidx on copy_spcontext(incoming_message_id) where incoming_message_id is not null;
    create index copy_spcontext_outidx on copy_spcontext(outgoing_message_id) where outgoing_message_id is not null;

    create unlogged table copy_messageids
        with (autovacuum_enabled=false, toast.autovacuum_enabled=false)
    as select incoming_message_id as id from copy_spcontext where incoming_message_id is not null;

    insert into copy_messageids
    select outgoing_message_id as id from copy_spcontext where outgoing_message_id is not null;

    create index copy_messageids_idx on copy_messageids(id);

-- #12 - Delete from mb_message_headers and mb_message_connection_properties

    delete from mb_message_headers where parent_id in (select id from copy_messageids);
    delete from mb_message_connection_properties where parent_id in (select id from copy_messageids);

    -- Messages itselves can be deleted after spcontexts related only
-- #13 - Delete from mb_context (spcontexts) Very slow (7875 rows, 7m 11s) --> (7326 rows, 16s)

    delete from mb_context where id in (select id from copy_spcontext);

-- #14 - Delete messages

    delete from mb_message where id in (select id from copy_messageids);

-- #15 - Delete from mb_context (step instance context)

    delete from mb_context where id in (select id from copy_step_instance_context);

-- #16 - Delete from mb_instance (step instance)

    delete from mb_instance where id in (select id from copy_step_instances);

-- #17 - Delete from mb_instance (situation instance)

    delete from mb_instance where id in (select id from copy_situation_instances);

-- #18 - Delete from mb_instance (initiator)

    create index copy_mb_tccontext_initiator_idx on copy_mb_tccontext(initiator_id);
    delete from mb_instance where id in (select initiator_id from copy_mb_tccontext);

-- #19 - Delete tc-contexts

    delete from mb_tccontext where id in (select id from copy_mb_tccontext);
    delete from mb_context where id in (select id from copy_mb_tccontext);

    SET session_replication_role = 'origin';

    drop table if exists copy_mb_message_param;
    drop table if exists copy_mb_tccontext;
    drop table if exists copy_messageids;
    drop table if exists copy_situation_instances;
    drop table if exists copy_spcontext;
    drop table if exists copy_step_instance_context;
    drop table if exists copy_step_instances;

    return 'Success; a new minimal timestamp is ' || mindate;
END;
$$;


--
-- Name: current_partition_number(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.current_partition_number() RETURNS smallint
    LANGUAGE sql
AS $$
select 1::smallint;
$$;


--
-- Name: delete_tccontext_after(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.delete_tccontext_after() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    delete from mb_tccontext where id=OLD.id and part_num = OLD.part_num;
    Return OLD;
END;
$$;


--
-- Name: get_messages_from_tc_context(numeric); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_messages_from_tc_context(tc_context_id numeric) RETURNS TABLE("ID" bigint, "PARENT" bigint, "TYPE" character varying, "DESCRIPTION" text, "PATH" character varying, "LEVEL" integer, "STATUS" character varying, "DURATION" character varying, "START_TIME" character varying, "END_TIME" character varying)
    LANGUAGE plpgsql
AS $$
BEGIN
    DROP TABLE IF EXISTS tmp;
    create temporary table tmp (
                                   "ID" int8,
                                   "PARENT" int8,
                                   "TYPE" varchar(50),
                                   "DESCRIPTION" text,
                                   "STATUS" varchar(20),
                                   "DURATION" varchar(20),
                                   "START_TIME" varchar(20),
                                   "END_TIME" varchar(20),
                                   in_com_msg int8,
                                   out_com_msg int8
    ) on COMMIT DROP;

    INSERT INTO tmp
    SELECT
        tc.id as "ID",
        null as "PARENT",
        tc.type as "TYPE",
        tc.name as "DESCRIPTION",
        tc.status as "STATUS",
        -- to_char(tc.end_time-tc.start_time,'SS.MS')
        case when EXTRACT(EPOCH FROM tc.end_time-tc.start_time)<60
                 then to_char(tc.end_time-tc.start_time,'SS.MS')
             else to_char(tc.end_time-tc.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(tc.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(tc.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_context tc
    WHERE tc.id = cast(tc_context_id as int8);

    INSERT INTO tmp
    SELECT
        sii.id as "ID",
        sii.context_id as "PARENT",
        sii.type as "TYPE",
        sii.name as "DESCRIPTION" ,
        sii.status as "STATUS",
        -- to_char(sii.end_time-sii.start_time, 'SS.MS')
        case when EXTRACT(EPOCH FROM sii.end_time-sii.start_time)<60
                 then to_char(sii.end_time-sii.start_time,'SS.MS')
             else to_char(sii.end_time-sii.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(sii.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(sii.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_instance sii
             JOIN tmp ON (tmp."ID"= sii.context_id);

    INSERT INTO tmp
    SELECT
        sti.id as "ID",
        sti.parent_id as "PARENT",
        sti.type as "TYPE",
        sti.name as "DESCRIPTION",
        sti.status as "STATUS",
        -- to_char(sti.end_time-sti.start_time, 'SS.MS')
        case when EXTRACT(EPOCH FROM sti.end_time-sti.start_time)<60
                 then to_char(sti.end_time-sti.start_time,'SS.MS')
             else to_char(sti.end_time-sti.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(sti.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(sti.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_instance sti
             JOIN tmp ON (tmp."ID"= sti.parent_id);

    INSERT INTO tmp
    SELECT
        ic.id as "ID",
        ic.instance as "PARENT",
        ic.type as "TYPE",
        ic.name as "DESCRIPTION",
        ic.status as "STATUS",
        -- to_char(ic.end_time-ic.start_time,'SS.MS')
        case when EXTRACT(EPOCH FROM ic.end_time-ic.start_time)<60
                 then to_char(ic.end_time-ic.start_time,'SS.MS')
             else to_char(ic.end_time-ic.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(ic.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(ic.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_context ic
             JOIN tmp ON (tmp."ID"= ic.instance);

    INSERT INTO tmp
    SELECT
        sp.id as "ID",
        sp.parent_ctx_id as "PARENT",
        sp.type as "TYPE",
        sp.name as "DESCRIPTION",
        sp.status as "STATUS",
        -- to_char(sp.end_time-sp.start_time,'SS.MS')
        case when EXTRACT(EPOCH FROM sp.end_time-sp.start_time)<60
                 then to_char(sp.end_time-sp.start_time,'SS.MS')
             else to_char(sp.end_time-sp.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(sp.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(sp.end_time, 'HH24:MI:SS.MS') as "END_TIME",
        sp.incoming_message_id as in_com_msg,
        sp.outgoing_message_id as out_com_msg
    FROM mb_context sp
             JOIN tmp ON (tmp."ID"= sp.parent_ctx_id);

    INSERT INTO tmp
    SELECT
        ms.id as "ID",
        tmp."ID" as "PARENT",
        'incoming message' as "TYPE",
        ms.text as "DESCRIPTION"
    FROM mb_message ms
             JOIN tmp ON (tmp.in_com_msg= ms.id);

    INSERT INTO tmp
    SELECT
        ms.id as "ID",
        tmp."ID" as "PARENT",
        'outgoing message' as "TYPE",
        ms.text as "DESCRIPTION"
    FROM mb_message ms
             JOIN tmp ON( tmp.out_com_msg= ms.id);

    RETURN QUERY
        WITH RECURSIVE tmp2 ("ID","PARENT","TYPE","DESCRIPTION","PATH", "LEVEL",
                             "STATUS", "DURATION", "START_TIME", "END_TIME")
                           AS (
                SELECT
                    T1."ID", T1."PARENT", T1."TYPE", T1."DESCRIPTION",
                    CAST (T1."ID" AS VARCHAR (255)) as "PATH",
                    1 as "LEVEL",
                    T1."STATUS", T1."DURATION", T1."START_TIME", T1."END_TIME"
                FROM tmp T1
                WHERE T1."PARENT" IS NULL
                UNION
                SELECT
                    T2."ID", T2."PARENT", T2."TYPE", T2."DESCRIPTION",
                    CAST ( tmp2."PATH" ||'->'|| T2."ID" AS VARCHAR(255)) as "PATH",
                    tmp2."LEVEL" + 1 as "LEVEL",
                    T2."STATUS", T2."DURATION", T2."START_TIME", T2."END_TIME"
                FROM tmp T2
                         INNER JOIN tmp2 ON (tmp2."ID"= T2."PARENT")
            )
        SELECT * from tmp2 ORDER BY "PATH";
end;
$$;


--
-- Name: get_messages_from_tc_context_no_message(numeric, smallint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_messages_from_tc_context_no_message(tc_context_id numeric, context_part_num smallint) RETURNS TABLE("ID" bigint, "PARENT" bigint, "TYPE" character varying, "DESCRIPTION" text, "PATH" character varying, "LEVEL" integer, "STATUS" character varying, "DURATION" character varying, "START_TIME" character varying, "END_TIME" character varying)
    LANGUAGE plpgsql
AS $$
BEGIN
    DROP TABLE IF EXISTS tmp;
    create temporary table tmp (
                                   "ID" int8,
                                   "PARENT" int8,
                                   "TYPE" varchar(50),
                                   "DESCRIPTION" text,
                                   "STATUS" varchar(20),
                                   "DURATION" varchar(20),
                                   "START_TIME" varchar(20),
                                   "END_TIME" varchar(20)
    ) on COMMIT DROP;

    INSERT INTO tmp
    SELECT
        tc.id as "ID",
        null as "PARENT",
        tc.type as "TYPE",
        tc.name as "DESCRIPTION",
        tc.status as "STATUS",
        -- to_char(tc.end_time-tc.start_time,'SS.MS')
        case when EXTRACT(EPOCH FROM tc.end_time-tc.start_time)<60
                 then to_char(tc.end_time-tc.start_time,'SS.MS')
             else to_char(tc.end_time-tc.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(tc.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(tc.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_context tc
    WHERE tc.id = cast(tc_context_id as int8)
      AND tc.part_num = context_part_num
    union all
    SELECT
        sii.id as "ID",
        sii.context_id as "PARENT",
        sii.type as "TYPE",
        sii.name as "DESCRIPTION" ,
        sii.status as "STATUS",
        -- to_char(sii.end_time-sii.start_time, 'SS.MS')
        case when EXTRACT(EPOCH FROM sii.end_time-sii.start_time)<60
                 then to_char(sii.end_time-sii.start_time,'SS.MS')
             else to_char(sii.end_time-sii.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(sii.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(sii.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_instance sii
    where sii.context_id = cast(tc_context_id as int8)
      AND sii.part_num = context_part_num;

    INSERT INTO tmp
    SELECT
        sti.id as "ID",
        sti.parent_id as "PARENT",
        sti.type as "TYPE",
        sti.name as "DESCRIPTION",
        sti.status as "STATUS",
        -- to_char(sti.end_time-sti.start_time, 'SS.MS')
        case when EXTRACT(EPOCH FROM sti.end_time-sti.start_time)<60
                 then to_char(sti.end_time-sti.start_time,'SS.MS')
             else to_char(sti.end_time-sti.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(sti.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(sti.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_instance sti
             JOIN tmp ON (tmp."ID"= sti.parent_id)
    WHERE sti.part_num = context_part_num;

    INSERT INTO tmp
    SELECT
        ic.id as "ID",
        ic.instance as "PARENT",
        ic.type as "TYPE",
        ic.name as "DESCRIPTION",
        ic.status as "STATUS",
        -- to_char(ic.end_time-ic.start_time,'SS.MS')
        case when EXTRACT(EPOCH FROM ic.end_time-ic.start_time)<60
                 then to_char(ic.end_time-ic.start_time,'SS.MS')
             else to_char(ic.end_time-ic.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(ic.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(ic.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_context ic
             JOIN tmp ON (tmp."ID"= ic.instance)
    WHERE ic.part_num = context_part_num;

    INSERT INTO tmp
    SELECT
        sp.id as "ID",
        sp.parent_ctx_id as "PARENT",
        sp.type as "TYPE",
        sp.name as "DESCRIPTION",
        sp.status as "STATUS",
        -- to_char(sp.end_time-sp.start_time,'SS.MS')
        case when EXTRACT(EPOCH FROM sp.end_time-sp.start_time)<60
                 then to_char(sp.end_time-sp.start_time,'SS.MS')
             else to_char(sp.end_time-sp.start_time,'MI:SS.MS')
            end as "DURATION",
        to_char(sp.start_time, 'HH24:MI:SS.MS') as "START_TIME",
        to_char(sp.end_time, 'HH24:MI:SS.MS') as "END_TIME"
    FROM mb_context sp
             JOIN tmp ON (tmp."ID"= sp.parent_ctx_id)
    WHERE sp.part_num = context_part_num;

    RETURN QUERY
        WITH RECURSIVE tmp2 ("ID","PARENT","TYPE","DESCRIPTION","PATH", "LEVEL",
                             "STATUS", "DURATION", "START_TIME", "END_TIME")
                           AS (
                SELECT
                    T1."ID", T1."PARENT", T1."TYPE", T1."DESCRIPTION",
                    CAST (T1."ID" AS VARCHAR (255)) as "PATH",
                    1 as "LEVEL",
                    T1."STATUS", T1."DURATION", T1."START_TIME", T1."END_TIME"
                FROM tmp T1 WHERE T1."PARENT" IS NULL
                UNION
                SELECT
                    T2."ID", T2."PARENT", T2."TYPE", T2."DESCRIPTION",
                    CAST ( tmp2."PATH" ||'->'|| T2."ID" AS VARCHAR(255)) as "PATH",
                    tmp2."LEVEL" + 1 as "LEVEL",
                    T2."STATUS", T2."DURATION", T2."START_TIME", T2."END_TIME"
                FROM tmp T2
                         INNER JOIN tmp2 ON (tmp2."ID"= T2."PARENT")
            )
        SELECT * from tmp2 ORDER BY "PATH";
end;
$$;


--
-- Name: get_params_for_delete_contexts(integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_params_for_delete_contexts(date_day_shift integer, OUT context_min_id_to_delete bigint, OUT context_max_id_to_delete bigint, OUT context_rows_count_to_delete integer) RETURNS record
    LANGUAGE plpgsql
AS $$
BEGIN
    select min(context.id), max(context.id), count(1)
    INTO context_min_id_to_delete,context_max_id_to_delete, context_rows_count_to_delete
    from mb_context context
    where context."type" = 'TcContext' and
        ( context.end_time < (CURRENT_DATE - date_day_shift)::timestamp
            or (context.end_time is null and context.start_time < (CURRENT_DATE - date_day_shift)::timestamp));
END;
$$;


--
-- Name: get_switch_date(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.get_switch_date() RETURNS timestamp without time zone
    LANGUAGE sql IMMUTABLE
AS $$
select cast(to_timestamp('01012009','MMDDYYYY') as timestamp)
$$;


--
-- Name: gethostid(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.gethostid() RETURNS numeric
    LANGUAGE sql
AS $_$select coalesce ((select value from mb$host_id limit 1), 11)$_$;


--
-- Name: getid(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.getid() RETURNS numeric
    LANGUAGE plpgsql
AS $$
declare
    SWITCH_DATE constant timestamp := get_switch_date();
    cdate timestamp := current_timestamp;
    v_sq   numeric;
begin
    select /*+getid*/ nextval('sqsystem') into v_sq;

    RETURN TO_CHAR(91232000000 + cast(round(extract(epoch from (cdate-SWITCH_DATE))) as numeric),'99999999999')
               || TRIM (TO_CHAR (gethostid(), '00'))
        || TRIM (TO_CHAR (v_sq, '000000'));
END;
$$;


--
-- Name: hex_to_int2(text); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.hex_to_int2(text) RETURNS integer
    LANGUAGE sql
AS $_$
select ('x'||substr(md5($1),1,8))::bit(6)::int;
$_$;


--
-- Name: increment_range(bigint, bigint, bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.increment_range(curval bigint, inc bigint, maxval bigint) RETURNS bigint
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN least(curval + inc, maxval);
EXCEPTION
    WHEN OTHERS THEN
        RETURN maxval;
END;
$$;


--
-- Name: insert_bindingkeys_before(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.insert_bindingkeys_before() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    if exists (select 1 from mb_context_binding_keys where id = new.id and key=new.key)
    THEN
        return null;
    END IF;
    Return NEW;
END;
$$;


--
-- Name: insert_tccontext_after(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.insert_tccontext_after() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    insert into mb_tccontext (id, "name",	initiator_id, environment_id, environment_name,	status, start_time, end_time, project_id, client)
    values (NEW.id, NEW."name", NEW.initiator_id, NEW.environment_id, NEW.environment_name, NEW.status, NEW.start_time, NEW.end_time, NEW.project_id, NEW.client);
    Return NEW;
END;
$$;


--
-- Name: pre_store_instance(bigint, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.pre_store_instance(instance_id bigint, instance_type character varying) RETURNS bigint
    LANGUAGE plpgsql
AS $$
declare
    id_exists INT8;
    partnum smallint;
begin
    select id into id_exists from mb_instance where id=instance_id;
    if id_exists is null then
        partnum = current_partition_number();
        INSERT INTO mb_instance (id, "type", part_num) VALUES (instance_id, instance_type, partnum)
        ON CONFLICT (id, part_num) DO NOTHING;
    end if;
    return instance_id;
END;
$$;


--
-- Name: pre_store_tc_context(bigint, character varying, character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.pre_store_tc_context(context_id bigint, context_type character varying, context_status character varying) RETURNS bigint
    LANGUAGE plpgsql
AS $$
declare
    id_exists INT8;
    partnum smallint;
begin
    select id into id_exists from mb_context where id=context_id;
    if id_exists is null then
        partnum = current_partition_number();
        INSERT INTO mb_context (id, "type", status, part_num) VALUES (context_id, context_type, context_status, partnum)
        ON CONFLICT (id, part_num) DO NOTHING;
    end if;
    return context_id;
END;
$$;


--
-- Name: update_tccontext_after(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_tccontext_after() RETURNS trigger
    LANGUAGE plpgsql
AS $$
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
                project_id=NEW.project_id, client=NEW.client
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
            project_id=NEW.project_id, client=NEW.client
        where id=NEW.id and part_num = NEW.part_num;
    end if;
    Return NEW;
END;
$$;


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: mb$host_id; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."mb$host_id" (
    value numeric(2,0) NOT NULL
);


--
-- Name: mb_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_config (
                                  key character varying(255) NOT NULL,
                                  value text
);


--
-- Name: mb_context; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_context (
                                   id bigint NOT NULL,
                                   type character varying(255) NOT NULL,
                                   name character varying(255),
                                   description character varying(255),
                                   natural_id character varying(255),
                                   prefix character varying(255),
                                   extensions text,
                                   json_string text,
                                   session_id character varying(255),
                                   instance bigint,
                                   tc_id bigint,
                                   initiator_id bigint,
                                   status character varying(255),
                                   start_time timestamp without time zone,
                                   end_time timestamp without time zone,
                                   step_id bigint,
                                   incoming_message_id bigint,
                                   outgoing_message_id bigint,
                                   parent_ctx_id bigint,
                                   validation_results text,
                                   project_id bigint,
                                   client character varying(255),
                                   environment_id bigint,
                                   environment_name character varying(255),
                                   part_num smallint DEFAULT public.current_partition_number() NOT NULL,
                                   last_update_time bigint,
                                   time_to_live bigint,
                                   pod_name character varying(255)
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_context_binding_keys; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_context_binding_keys (
                                                id bigint NOT NULL,
                                                key character varying(255) NOT NULL,
                                                part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005');


--
-- Name: mb_context_report_links; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_context_report_links (
                                                parent_id bigint NOT NULL,
                                                value text,
                                                key text NOT NULL,
                                                part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_install_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_install_history (
                                           release_version character varying(30) NOT NULL,
                                           script_type character varying(12) NOT NULL,
                                           execution_date timestamp with time zone NOT NULL,
                                           filename text NOT NULL
);


--
-- Name: mb_instance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_instance (
                                    id bigint NOT NULL,
                                    type character varying(255) NOT NULL,
                                    name character varying(255),
                                    natural_id character varying(255),
                                    status character varying(255),
                                    start_time timestamp without time zone,
                                    end_time timestamp without time zone,
                                    error_name text,
                                    error_message text,
                                    extensions text,
                                    parent_id bigint,
                                    step_id bigint,
                                    context_id bigint,
                                    situation_id bigint,
                                    chain_id bigint,
                                    dataset_name text,
                                    callchain_execution_data text,
                                    operation_name character varying(255),
                                    system_name character varying(255),
                                    system_id bigint,
                                    part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_message; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message (
                                   id bigint NOT NULL,
                                   natural_id character varying(255),
                                   text text,
                                   part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_message_connection_properties; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_connection_properties (
                                                         parent_id bigint NOT NULL,
                                                         value character varying,
                                                         key character varying(255) NOT NULL,
                                                         part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_message_headers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_headers (
                                           parent_id bigint NOT NULL,
                                           value character varying,
                                           key character varying(255) NOT NULL,
                                           part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_message_param; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_param (
                                         id bigint NOT NULL,
                                         natural_id character varying(255),
                                         param_name character varying(255),
                                         multiple boolean,
                                         context_id bigint,
                                         part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_message_param_multiple_value; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_message_param_multiple_value (
                                                        message_param_id bigint NOT NULL,
                                                        value text,
                                                        part_num smallint DEFAULT public.current_partition_number()
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_tccontext; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_tccontext (
                                     id bigint NOT NULL,
                                     name character varying(255),
                                     initiator_id bigint,
                                     initiator_name character varying(255),
                                     initiator_type character varying(255),
                                     environment_id bigint,
                                     status character varying(255),
                                     start_time timestamp without time zone,
                                     end_time timestamp without time zone,
                                     project_id bigint,
                                     client character varying,
                                     situation_id bigint,
                                     operation_name character varying(255),
                                     system_name character varying(255),
                                     system_id bigint,
                                     chain_id bigint,
                                     duration double precision,
                                     environment_name character varying(255),
                                     natural_id character varying(255),
                                     part_num smallint DEFAULT public.current_partition_number() NOT NULL
)
    WITH (autovacuum_enabled='true', autovacuum_vacuum_scale_factor='0.0005', autovacuum_analyze_scale_factor='0.0005', toast.autovacuum_enabled='true', toast.autovacuum_vacuum_scale_factor='0.0005');


--
-- Name: mb_upgrade_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mb_upgrade_history (
                                           id bigint NOT NULL,
                                           upgrade_datetime timestamp without time zone,
                                           build_number character varying(255),
                                           natural_id character varying(255)
);


--
-- Name: mindate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mindate (
    "?column?" timestamp with time zone
);


--
-- Name: shedlock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shedlock (
                                 name character varying(64) NOT NULL,
                                 lock_until timestamp without time zone NOT NULL,
                                 locked_at timestamp without time zone NOT NULL,
                                 locked_by character varying(255) NOT NULL
);


--
-- Name: sqsystem; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sqsystem
    START WITH 870000
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 999999
    CACHE 100
    CYCLE;


--
-- Name: mb_context_binding_keys bindingkeys_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context_binding_keys
    ADD CONSTRAINT bindingkeys_pk PRIMARY KEY (id, key, part_num);

--
-- Name: mb_config mb_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_config
    ADD CONSTRAINT mb_config_pkey PRIMARY KEY (key);


--
-- Name: mb_context mb_context_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_pkey PRIMARY KEY (id, part_num);


--
-- Name: mb_context_report_links mb_context_reportLinks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context_report_links
    ADD CONSTRAINT "mb_context_reportLinks_pkey" PRIMARY KEY (parent_id, key, part_num);


--
-- Name: mb_context mb_context_uniqueinstance; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_uniqueinstance UNIQUE (instance, part_num);


--
-- Name: mb_install_history mb_install_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_install_history
    ADD CONSTRAINT mb_install_history_pk PRIMARY KEY (release_version, script_type, filename);


--
-- Name: mb_instance mb_instance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_instance
    ADD CONSTRAINT mb_instance_pkey PRIMARY KEY (id, part_num);


--
-- Name: mb_message_connection_properties mb_message_connectionProperties_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_connection_properties
    ADD CONSTRAINT "mb_message_connectionProperties_pkey" PRIMARY KEY (parent_id, key, part_num);


--
-- Name: mb_message_headers mb_message_headers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_headers
    ADD CONSTRAINT mb_message_headers_pkey PRIMARY KEY (parent_id, key, part_num);


--
-- Name: mb_message_param mb_message_param_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_param
    ADD CONSTRAINT mb_message_param_pkey PRIMARY KEY (id, part_num);


--
-- Name: mb_message mb_message_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message
    ADD CONSTRAINT mb_message_pkey PRIMARY KEY (id, part_num);


--
-- Name: mb_tccontext mb_tccontext_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_tccontext
    ADD CONSTRAINT mb_tccontext_pkey PRIMARY KEY (id, part_num);


--
-- Name: mb_upgrade_history mb_upgrade_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_upgrade_history
    ADD CONSTRAINT mb_upgrade_history_pkey PRIMARY KEY (id);


--
-- Name: shedlock shedlock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shedlock
    ADD CONSTRAINT shedlock_pkey PRIMARY KEY (name);


--
-- Name: context_by_environment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX context_by_environment_id ON public.mb_context USING btree (environment_id);


--
-- Name: context_by_initiator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX context_by_initiator ON public.mb_context USING btree (initiator_id);


--
-- Name: context_by_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX context_by_parent ON public.mb_context USING btree (parent_ctx_id);


--
-- Name: context_by_type_id_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX context_by_type_id_index ON public.mb_context USING btree (type, id DESC);


--
-- Name: contexts_illegal; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX contexts_illegal ON public.mb_context USING btree (id) WHERE (((type)::text = 'TcContext'::text) AND ((status)::text = ANY (ARRAY[('NOT_STARTED'::character varying)::text, ('IN_PROGRESS'::character varying)::text, ('PAUSED'::character varying)::text])));


--
-- Name: contexts_inprogress; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX contexts_inprogress ON public.mb_context USING btree (id) WHERE (((type)::text = 'TcContext'::text) AND ((status)::text = 'IN_PROGRESS'::text));


--
-- Name: instance_context_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX instance_context_id ON public.mb_instance USING btree (context_id);


--
-- Name: instance_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX instance_parent_id ON public.mb_instance USING btree (parent_id);


--
-- Name: mb_context_initiator_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_context_initiator_id_idx ON public.mb_context USING btree (initiator_id);


--
-- Name: mb_context_instance_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_context_instance_idx ON public.mb_context USING btree (instance);


--
-- Name: mb_context_parent_ctx_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_context_parent_ctx_id_idx ON public.mb_context USING btree (parent_ctx_id);


--
-- Name: mb_context_type_time_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_context_type_time_idx ON public.mb_context USING btree (type, start_time);


--
-- Name: mb_instance_by_parent_step_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_instance_by_parent_step_name ON public.mb_instance USING btree (parent_id, step_id, name);


--
-- Name: mb_message_param_by_context_id_and_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_message_param_by_context_id_and_name ON public.mb_message_param USING btree (context_id, param_name);


--
-- Name: mb_message_param_value_by_param_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mb_message_param_value_by_param_id_idx ON public.mb_message_param_multiple_value USING btree (message_param_id);


--
-- Name: tccontext_by_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tccontext_by_client ON public.mb_tccontext USING btree (client);


--
-- Name: tccontext_by_end_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tccontext_by_end_time ON public.mb_tccontext USING btree (end_time);


--
-- Name: tccontext_by_environment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tccontext_by_environment_id ON public.mb_tccontext USING btree (environment_id, id DESC);


--
-- Name: tccontext_by_initiator_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tccontext_by_initiator_id ON public.mb_tccontext USING btree (initiator_id, id DESC);


--
-- Name: tccontext_by_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tccontext_by_name ON public.mb_tccontext USING btree (name, id DESC);


--
-- Name: tccontext_by_start_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tccontext_by_start_time ON public.mb_tccontext USING btree (start_time);


--
-- Name: tccontext_by_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tccontext_by_status ON public.mb_tccontext USING btree (status, id DESC);


--
-- Name: mb_context delete_tccontext_after_delete; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER delete_tccontext_after_delete AFTER DELETE ON public.mb_context FOR EACH ROW WHEN (((old.type)::text = 'TcContext'::text)) EXECUTE PROCEDURE public.delete_tccontext_after();


--
-- Name: mb_context_binding_keys insert_bindingkeys_before_insert; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER insert_bindingkeys_before_insert BEFORE INSERT ON public.mb_context_binding_keys FOR EACH ROW EXECUTE PROCEDURE public.insert_bindingkeys_before();


--
-- Name: mb_context insert_tccontext_after_insert; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER insert_tccontext_after_insert AFTER INSERT ON public.mb_context FOR EACH ROW WHEN (((new.type)::text = 'TcContext'::text)) EXECUTE PROCEDURE public.insert_tccontext_after();


--
-- Name: mb_context update_tccontext_after_update; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_tccontext_after_update AFTER UPDATE OF id, name, initiator_id, environment_id, status, start_time, end_time ON public.mb_context FOR EACH ROW WHEN (((old.type)::text = 'TcContext'::text)) EXECUTE PROCEDURE public.update_tccontext_after();


--
-- Name: mb_context_binding_keys mb_context_binding_keys_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context_binding_keys
    ADD CONSTRAINT mb_context_binding_keys_id_part_num_fk FOREIGN KEY (id, part_num) REFERENCES public.mb_context(id, part_num);


--
-- Name: mb_context mb_context_incoming_message_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_incoming_message_id_part_num_fk FOREIGN KEY (incoming_message_id, part_num) REFERENCES public.mb_message(id, part_num);


--
-- Name: mb_context mb_context_initiator_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_initiator_id_part_num_fk FOREIGN KEY (initiator_id, part_num) REFERENCES public.mb_instance(id, part_num);


--
-- Name: mb_context mb_context_instance_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_instance_part_num_fk FOREIGN KEY (instance, part_num) REFERENCES public.mb_instance(id, part_num);


--
-- Name: mb_context mb_context_outgoing_message_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_outgoing_message_id_part_num_fk FOREIGN KEY (outgoing_message_id, part_num) REFERENCES public.mb_message(id, part_num);


--
-- Name: mb_context_report_links mb_context_report_links_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context_report_links
    ADD CONSTRAINT mb_context_report_links_parent_id_part_num_fk FOREIGN KEY (parent_id, part_num) REFERENCES public.mb_context(id, part_num);


--
-- Name: mb_context mb_context_step_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_step_id_part_num_fk FOREIGN KEY (step_id, part_num) REFERENCES public.mb_instance(id, part_num);


--
-- Name: mb_context mb_context_tc_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_context
    ADD CONSTRAINT mb_context_tc_id_part_num_fk FOREIGN KEY (tc_id, part_num) REFERENCES public.mb_context(id, part_num);


--
-- Name: mb_message_connection_properties mb_message_connection_properties_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_connection_properties
    ADD CONSTRAINT mb_message_connection_properties_parent_id_part_num_fk FOREIGN KEY (parent_id, part_num) REFERENCES public.mb_message(id, part_num);


--
-- Name: mb_message_headers mb_message_headers_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_headers
    ADD CONSTRAINT mb_message_headers_parent_id_part_num_fk FOREIGN KEY (parent_id, part_num) REFERENCES public.mb_message(id, part_num);


--
-- Name: mb_message_param mb_message_param_context_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_param
    ADD CONSTRAINT mb_message_param_context_id_part_num_fk FOREIGN KEY (context_id, part_num) REFERENCES public.mb_context(id, part_num);


--
-- Name: mb_message_param_multiple_value mb_message_param_multiple_value_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mb_message_param_multiple_value
    ADD CONSTRAINT mb_message_param_multiple_value_id_part_num_fk FOREIGN KEY (message_param_id, part_num) REFERENCES public.mb_message_param(id, part_num);


--
-- PostgreSQL database dump complete
--

