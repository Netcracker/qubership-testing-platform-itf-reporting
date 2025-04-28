-- Script to drop foreign key constraints in atp-itf-reporting database.
-- They should persist due to object model, but are dropping by me (Aleksandr Kapustin), because:
--	- They are checked even when partitions were tried to detach, so fully block detaching on huge prod amouns of data...

--
-- Name: mb_context_binding_keys mb_context_binding_keys_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context_binding_keys DROP CONSTRAINT IF EXISTS mb_context_binding_keys_id_part_num_fk RESTRICT;

--
-- Name: mb_context mb_context_incoming_message_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context DROP CONSTRAINT IF EXISTS mb_context_incoming_message_id_part_num_fk RESTRICT;

--
-- Name: mb_context mb_context_initiator_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context DROP CONSTRAINT IF EXISTS mb_context_initiator_id_part_num_fk RESTRICT;

--
-- Name: mb_context mb_context_instance_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context DROP CONSTRAINT IF EXISTS mb_context_instance_part_num_fk RESTRICT;

--
-- Name: mb_context mb_context_outgoing_message_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context DROP CONSTRAINT IF EXISTS mb_context_outgoing_message_id_part_num_fk RESTRICT;

--
-- Name: mb_context_report_links mb_context_report_links_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context_report_links DROP CONSTRAINT IF EXISTS mb_context_report_links_parent_id_part_num_fk RESTRICT;

--
-- Name: mb_context mb_context_step_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context DROP CONSTRAINT IF EXISTS mb_context_step_id_part_num_fk RESTRICT;

--
-- Name: mb_context mb_context_tc_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context DROP CONSTRAINT IF EXISTS mb_context_tc_id_part_num_fk RESTRICT;

--
-- Name: mb_message_connection_properties mb_message_connection_properties_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_connection_properties DROP CONSTRAINT IF EXISTS mb_message_connection_properties_parent_id_part_num_fk RESTRICT;

--
-- Name: mb_message_headers mb_message_headers_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_headers DROP CONSTRAINT IF EXISTS mb_message_headers_parent_id_part_num_fk RESTRICT;

--
-- Name: mb_message_param mb_message_param_context_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_param DROP CONSTRAINT IF EXISTS mb_message_param_context_id_part_num_fk RESTRICT;

--
-- Name: mb_message_param_multiple_value mb_message_param_multiple_value_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_param_multiple_value DROP CONSTRAINT IF EXISTS mb_message_param_multiple_value_id_part_num_fk RESTRICT;

commit;