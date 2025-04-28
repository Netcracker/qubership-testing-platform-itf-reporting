FROM artifactory-service-address/path-to-java-image

LABEL maintainer="our-team@qubership.org"
LABEL atp.service="atp-itf-reporting"

ENV HOME_EX=/itf
ENV ITF_REPORTING_DB_USER=itfu
ENV ITF_REPORTING_DB_PASSWORD=X8eXuS
ENV JDBC_URL=jdbc:postgresql://kube01nd04cn:5433/itf

WORKDIR $HOME_EX

COPY --chmod=775 dist/atp /atp/
COPY --chown=atp:root ./build $HOME_EX/

RUN apk add --update --no-cache libpq && \
    chmod a+x $HOME_EX/run.sh && \
    find $HOME_EX -type f -exec chmod a+x {} \; && \
    find $HOME_EX -type d -exec chmod 777 {} \;

EXPOSE 10002 8080 8161 61616 61617

USER atp

CMD [ "./run.sh" ]
