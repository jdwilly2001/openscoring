FROM java:8

COPY openscoring-client/target/openscoring-client-executable-1.4-SNAPSHOT.jar /opt/openscoring/lib/
COPY openscoring-server/target/openscoring-server-executable-1.4-SNAPSHOT.jar /opt/openscoring/lib/
COPY openscoring-webapp/target/openscoring-webapp-1.4-SNAPSHOT.war /opt/openscoring/lib/

COPY docker-entrypoint.sh /docker-entrypoint.sh

EXPOSE 8080
WORKDIR /opt/openscoring/lib

ENTRYPOINT ["/docker-entrypoint.sh"]
