FROM eclipse-temurin:21-jre

WORKDIR /app

ARG APP_UID=10001
ARG APP_GID=10001

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r -g ${APP_GID} app \
    && useradd -r -u ${APP_UID} -g app -d /app -s /usr/sbin/nologin app

COPY build-output/app.jar app.jar

RUN mkdir -p /app/logs/archive \
    && chown -R app:app /app

USER app

EXPOSE 8090

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]