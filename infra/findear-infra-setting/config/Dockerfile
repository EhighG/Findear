# FROM ubuntu:focal as builder
# USER root
# 
# WORKDIR /
# RUN chmod 1777 /tmp
# 
# FROM ubuntu/jre:17_edge
# USER root
# EXPOSE 9000
# 
# WORKDIR /
# COPY config-0.0.1-SNAPSHOT.jar .
# # COPY --from=builder tmp .
# CMD ["-jar", "/config-0.0.1-SNAPSHOT.jar"]
# # CMD ["config-0.0.1-SNAPSHOT.jar"]
FROM ubuntu:focal
USER root
WORKDIR /
WORKDIR /
RUN apt update && apt install -y openjdk-17-jre 
COPY config-0.0.1-SNAPSHOT.jar .
COPY application.yml .
EXPOSE 9000
CMD ["java", "-jar", "/config-0.0.1-SNAPSHOT.jar"]
