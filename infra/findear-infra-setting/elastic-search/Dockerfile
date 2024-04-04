FROM elasticsearch:7.17.3
# USER root
WORKDIR /
ENV TZ=Asia/Seoul
ENV discovery.type=single-node
COPY elasticsearch.yml ./usr/share/elasticsearch/config/
EXPOSE 9200
EXPOSE 9300
