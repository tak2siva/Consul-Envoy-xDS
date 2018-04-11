FROM openjdk:8-jre-alpine

COPY xDS-Server.jar .
COPY xDSServer.properties .

ENTRYPOINT ["java","-jar", "xDS-Server.jar"]