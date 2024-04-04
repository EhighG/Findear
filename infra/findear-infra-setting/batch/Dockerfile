FROM ubuntu:focal
USER root
WORKDIR /
RUN apt update && apt install -y openjdk-17-jre curl
ENV TZ=Asia/Seoul
EXPOSE 8082
COPY batch-0.0.1-SNAPSHOT.jar .
COPY application.yml . 
COPY output.cert .
RUN keytool -importcert -cacerts -storepass changeit -file output.cert -alias letsencrypt -noprompt
HEALTHCHECK CMD curl -f http://localhost:8082/actuator/health || exit 1
CMD ["java", "-jar", "/batch-0.0.1-SNAPSHOT.jar"]
