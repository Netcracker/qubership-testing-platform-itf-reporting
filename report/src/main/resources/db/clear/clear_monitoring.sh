#!/bin/bash
while getopts d:u:p:s: flag
do
    case "${flag}" in
        d) POSTGRESQL_DATABASE=${OPTARG};;
        u) POSTGRESQL_USER=${OPTARG};;
		    p) POSTGRESQL_PASSWORD=${OPTARG};;
		    s) SCHEMA=${OPTARG};;
		    *)
            echo "ERROR: Unknown flag '${flag}'. Expected values: d, u, p, s"
            echo "Example:"
            echo " ./clear_monitoring.sh -d itf_rep_db -u itf_rep_user -p itf_rep_pwd -s public"
            exit 1
    esac
done
export PGPASSWORD=$POSTGRESQL_PASSWORD
declare -a table
table[0]="mb_message_param_multiple_value"
table[1]="mb_message_param"
table[2]="mb_message_connection_properties"
table[3]="mb_message_headers"
table[4]="mb_context_binding_keys"
table[5]="mb_context_report_links"
table[6]="mb_context"
table[7]="mb_instance"
table[8]="mb_message"
table[9]="mb_tccontext"

part_num=$(psql -d $POSTGRESQL_DATABASE -U $POSTGRESQL_USER -AXqtc "SET search_path TO $SCHEMA; select current_partition_number() % partitions_amount() + 1")
echo "part_num for clearing - ""$part_num"
for table_name in "${table[@]}"
do
    parent_table_name=$table_name
    part_table_name="$table_name"_part"$part_num"
    echo "Processing tables: Table - $parent_table_name, Partition - $part_table_name"
    psql -d $POSTGRESQL_DATABASE -U "$POSTGRESQL_USER" -AXqt -v schema="$SCHEMA" -v parent_table_name="$table_name" -v part_table_name="$table_name"_part"$part_num" -v part_num="$part_num" -f recreate_partition.sql
done