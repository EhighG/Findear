FROM python:3.12
ENV TZ=Asia/Seoul
EXPOSE 8000
USER root
RUN curl -sSL https://install.python-poetry.org | python3 -
ENV PATH "/root/.local/bin:$PATH"
WORKDIR /
COPY model.bin .


RUN apt update && apt install -y wget unzip && \
	  chown -R _apt:root /var/cache/apt/archives/partial/ && \
	  chmod -R 700 /var/cache/apt/archives/partial/
RUN wget http://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_123.0.6312.86-1_amd64.deb && \
	  apt install -y ./google-chrome-stable_123.0.6312.86-1_amd64.deb

RUN	  wget https://storage.googleapis.com/chrome-for-testing-public/123.0.6312.86/linux64/chromedriver-linux64.zip && \
		unzip chromedriver-linux64.zip 
RUN	  mv chromedriver-linux64 /usr/bin/chromedriver

COPY match/poetry.lock ./match/
COPY match/pyproject.toml ./match/
WORKDIR /match
RUN poetry install --no-root
COPY docker-entrypoint.sh .
COPY match /match 
COPY .env .
HEALTHCHECK CMD curl -f http://localhost:8000/actuator/health || exit 1
ENV PATH "/usr/bin/google-chrome:/usr/bin/chromedriver:$PATH"
ENTRYPOINT ["/match/docker-entrypoint.sh"]
