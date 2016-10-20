call mvn clean package spring-boot:repackage
call cf push dev-sync-backend -p ./target/sync-server-1.0-SNAPSHOT.jar