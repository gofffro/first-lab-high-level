FROM eclipse-temurin:21-jre-jammy
ARG VCS_REF
LABEL org.opencontainers.image.source="https://github.com/gofffro/first-lab-high-level" \
      org.opencontainers.image.revision="${VCS_REF}"
WORKDIR /app
COPY app.jar ./
COPY scripts/init.sql scripts/fill.sql /tmp/db/
RUN mkdir data \
    && java -cp app.jar org.h2.tools.RunScript -url jdbc:h2:file:/app/data/access -user sa -script /tmp/db/init.sql \
    && java -cp app.jar org.h2.tools.RunScript -url jdbc:h2:file:/app/data/access -user sa -script /tmp/db/fill.sql \
    && chown -R 10001:10001 data
ENV APP_DB_PATH=/app/data/access
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
