# Session Replication Test

This is a very simple sample application that shows the ability to use a Redis
Database as a session replication tool. It is designed to be run in Cloud Foundry.

To run properly, this application needs to have a Redis service bound to it.

## Spring Boot

When built in jar mode (run `./mvnw clean package`), the JAR file is self-executable
and uses Spring Boot's autoconfiguration utilities to bind the first Redis service
it finds as a session replication database.

## Traditional Spring

When built in war mode (run `./mvnw clean package -Pspring-traditional`), the WAR
uses the Tomcat server bundled with the Java Buildpack. If there is a Redis session
whose name contains the substring `session-replication`, the buildpack will
reconfigure Tomcat to use that service for session replication.
