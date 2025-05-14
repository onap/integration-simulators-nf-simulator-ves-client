FROM python:3.6-alpine

ARG VERSION=${version}

ADD target/libs /app/libs
ADD config /opt/db/config
ADD ./templates /app/templates
ADD ./src/main/resources/application.properties /app/application.properties
ADD target/vesclient-${VERSION}.jar /app/vesclient.jar

RUN apk --no-cache add openjdk11-jre --repository=http://dl-cdn.alpinelinux.org/alpine/main/community
RUN python -m pip install -r /opt/db/config/requirements.txt
ADD certificates  /usr/local/share/ca-certificates/
RUN update-ca-certificates
CMD python /opt/db/config/mongo_db_schema_creation.py \
    && if [ -d /app/certs ]; then mkdir -p /app/store; cp /app/certs/keystore.p12 /app/store/cert.p12; cp /app/certs/p12.pass /app/store/p12.pass;  cp /app/certs/truststore.jks /app/store/trust.jks; cp /app/certs/p12.pass /app/store/truststore.pass; fi \
    && if [ -f /app/store/trust.pass ]; then cp /app/store/trust.pass /app/store/truststore.pass; fi \
    && java -Dspring.config.location=file:/app/application.properties  -cp /app/libs/*:/app/vesclient.jar org.onap.integration.simulators.nfsimulator.vesclient.Main \
