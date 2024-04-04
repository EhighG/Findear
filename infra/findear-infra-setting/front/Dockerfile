FROM nginx:1.25.3
ENV TZ=Asia/Seoul
USER root
# RUN apt update && apt install -y git vim net-tools sudo lsof procps
# COPY nginx.conf /etc/nginx/
COPY default.conf /etc/nginx/conf.d/
COPY dist /etc/nginx/dist/
EXPOSE 80
# COPY env.inc /etc/nginx/conf.d/
# COPY cert /etc/nginx/ssl/
