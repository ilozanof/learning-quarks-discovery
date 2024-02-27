FROM openjdk:17
MAINTAINER Ivan Lozano
WORKDIR .
COPY target/quarkus-app/lib lib/
COPY target/quarkus-app/app app/
COPY target/quarkus-app/quarkus quarkus/
COPY target/quarkus-app/quarkus-run.jar app.jar
ENTRYPOINT ["java","-jar", "/app.jar"]