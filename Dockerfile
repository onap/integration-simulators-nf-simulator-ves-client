FROM docker.io/openjdk:11-jre-slim
COPY --from=python:3.6 / /

ARG VERSION=${version}

ADD target/libs /app/libs
ADD config /opt/db/config
ADD ./templates /app/templates
ADD ./src/main/resources/application.properties /app/application.properties
ADD target/vesclient-${VERSION}.jar /app/vesclient.jar
CMD apk update
CMD apk add ca-certificates
RUN python -m pip install -r /opt/db/config/requirements.txt
ADD certificates  /usr/local/share/ca-certificates/
RUN update-ca-certificates
CMD python /opt/db/config/mongo_db_schema_creation.py \
    && if [ -f /app/store/trust.pass ]; then cp /app/store/trust.pass /app/store/truststore.pass; fi \
    && java -Dspring.config.location=file:/app/application.properties  -cp /app/libs/*:/app/vesclient.jar org.onap.pnfsimulator.Main \
