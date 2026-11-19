FROM eclipse-temurin:21-jre-jammy
ARG VCS_REF
LABEL org.opencontainers.image.source="https://github.com/gofffro/first-lab-high-level" \
      org.opencontainers.image.revision="${VCS_REF}"
WORKDIR /app
COPY app.jar ./
COPY scripts/init.sql scripts/fill.sql /tmp/db/
RUN mkdir data logs \
    && java -Dloader.main=org.h2.tools.RunScript -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher \
       -url jdbc:h2:file:/app/data/access -user sa -script /tmp/db/init.sql \
    && java -Dloader.main=org.h2.tools.RunScript -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher \
       -url jdbc:h2:file:/app/data/access -user sa -script /tmp/db/fill.sql \
    && chown -R 10001:10001 data logs
ENV APP_DB_PATH=/app/data/access APP_LOG_FILE=/app/logs/access.log
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
