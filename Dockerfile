# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.3

# ---- builder stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder

ARG JAR_FILE=build/libs/ccd-case-document-am-api.jar
COPY ${JAR_FILE} application.jar

# extract layers (Spring Boot layered jar)
RUN java -Djarmode=layertools -jar application.jar extract

# ---- runtime helper stage (prepare /opt/app) ----
FROM eclipse-temurin:21-jdk-alpine AS runtime-helper

# create runtime user and app dir
RUN addgroup -S hmcts && adduser -S -G hmcts hmcts && mkdir -p /opt/app && chown -R hmcts:hmcts /opt/app

# copy extras (run as root here)
USER root
COPY lib/applicationinsights.json /opt/app/

# ARG names (spellings must match builder output)
ARG DIR_LAYER_APPLICATION=application/
ARG DIR_LAYER_DEPENDENCIES=dependencies/
ARG DIR_LAYER_SPRING_BOOT_LOADER=spring-boot-loader/
ARG DIR_LAYER_SNAPSHOT_DEPENDENCIES=snapshot-dependencies/

COPY --from=builder ${DIR_LAYER_SPRING_BOOT_LOADER} /opt/app/spring-boot-loader/
COPY --from=builder ${DIR_LAYER_DEPENDENCIES} /opt/app/dependencies/
COPY --from=builder ${DIR_LAYER_SNAPSHOT_DEPENDENCIES} /opt/app/snapshot-dependencies/
COPY --from=builder ${DIR_LAYER_APPLICATION} /opt/app/application/

RUN chown -R hmcts:hmcts /opt/app

# ---- final runtime (use same Temurin base so shell exists) ----
FROM eclipse-temurin:21-jdk-alpine

# create user (again) and set permissions
RUN addgroup -S hmcts && adduser -S -G hmcts hmcts
COPY --from=runtime-helper /opt/app /opt/app
RUN chown -R hmcts:hmcts /opt/app

USER hmcts

EXPOSE 4455

ENTRYPOINT ["java", "-cp", "/opt/app/spring-boot-loader/spring-boot-loader.jar:/opt/app/dependencies/*:/opt/app/snapshot-dependencies/*:/opt/app/application/", "org.springframework.boot.loader.launch.JarLauncher"]
