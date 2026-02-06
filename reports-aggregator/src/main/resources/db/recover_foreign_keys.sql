-- Script to recover foreign key constraints in atp-itf-reporting database.
-- They should persist due to object model, but were dropped by me (Aleksandr Kapustin), because:
--	- They were checked even when partitions were tried to detach, so fully blocked detaching on huge prod amouns of data...

--
-- Name: mb_context_binding_keys mb_context_binding_keys_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context_binding_keys
    ADD CONSTRAINT mb_context_binding_keys_id_part_num_fk FOREIGN KEY (id, part_num) REFERENCES mb_context(id, part_num);

--
-- Name: mb_context mb_context_incoming_message_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context
    ADD CONSTRAINT mb_context_incoming_message_id_part_num_fk FOREIGN KEY (incoming_message_id, part_num) REFERENCES mb_message(id, part_num);

--
-- Name: mb_context mb_context_initiator_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context
    ADD CONSTRAINT mb_context_initiator_id_part_num_fk FOREIGN KEY (initiator_id, part_num) REFERENCES mb_instance(id, part_num);

--
-- Name: mb_context mb_context_instance_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context
    ADD CONSTRAINT mb_context_instance_part_num_fk FOREIGN KEY (instance, part_num) REFERENCES mb_instance(id, part_num);

--
-- Name: mb_context mb_context_outgoing_message_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context
    ADD CONSTRAINT mb_context_outgoing_message_id_part_num_fk FOREIGN KEY (outgoing_message_id, part_num) REFERENCES mb_message(id, part_num);

--
-- Name: mb_context_report_links mb_context_report_links_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context_report_links
    ADD CONSTRAINT mb_context_report_links_parent_id_part_num_fk FOREIGN KEY (parent_id, part_num) REFERENCES mb_context(id, part_num);

--
-- Name: mb_context mb_context_step_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context
    ADD CONSTRAINT mb_context_step_id_part_num_fk FOREIGN KEY (step_id, part_num) REFERENCES mb_instance(id, part_num);

--
-- Name: mb_context mb_context_tc_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_context
    ADD CONSTRAINT mb_context_tc_id_part_num_fk FOREIGN KEY (tc_id, part_num) REFERENCES mb_context(id, part_num);

--
-- Name: mb_message_connection_properties mb_message_connection_properties_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_connection_properties
    ADD CONSTRAINT mb_message_connection_properties_parent_id_part_num_fk FOREIGN KEY (parent_id, part_num) REFERENCES mb_message(id, part_num);

--
-- Name: mb_message_headers mb_message_headers_parent_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_headers
    ADD CONSTRAINT mb_message_headers_parent_id_part_num_fk FOREIGN KEY (parent_id, part_num) REFERENCES mb_message(id, part_num);

--
-- Name: mb_message_param mb_message_param_context_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_param
    ADD CONSTRAINT mb_message_param_context_id_part_num_fk FOREIGN KEY (context_id, part_num) REFERENCES mb_context(id, part_num);

--
-- Name: mb_message_param_multiple_value mb_message_param_multiple_value_id_part_num_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE mb_message_param_multiple_value
    ADD CONSTRAINT mb_message_param_multiple_value_id_part_num_fk FOREIGN KEY (message_param_id, part_num) REFERENCES mb_message_param(id, part_num);

commit;