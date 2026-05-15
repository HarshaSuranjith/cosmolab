FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM quay.io/wildfly/wildfly:30.0.Final
USER root
RUN microdnf install -y curl && microdnf clean all
USER jboss
COPY --from=build /app/target/cosmolab-backend.war /opt/jboss/wildfly/standalone/deployments/ROOT.war
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m -XX:+FlightRecorder -XX:StartFlightRecording=duration=0,filename=/tmp/cosmolab.jfr,settings=profile,maxsize=256m,dumponexit=true -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.rmi.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=0.0.0.0"
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
