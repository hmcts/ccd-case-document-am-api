# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.3

# ---- builder stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder

ARG JAR_FILE=build/libs/ccd-case-document-am-api.jar
COPY ${JAR_FILE} application.jar

# extract layers (Spring Boot layered jar)
RUN java -Djarmode=layertools -jar application.jar extract

# ---- runtime stage (also Temurin, includes /bin/sh) ----
FROM eclipse-temurin:21-jdk-alpine

# create hmcts user if you expect non-root user
RUN addgroup -S hmcts && adduser -S -G hmcts hmcts

USER hmcts

# copy app-insights and extracted layers from builder
COPY lib/applicationinsights.json /app

# The following layer ARGs are only needed to stop Fortify flagging an issue with the COPY instructions
ARG DIR_LAYER_APPLICATION=application/
ARG DIR_LAYER_DEPENDENCIES=dependencies/
ARG DIR_LAYER_SPRING_BOOT_LOADER=spring-boot-loader/
ARG DIR_LAYER_SNAPSHOT_DEPENDENCIES=snapshot-dependencies/

COPY --from=builder application/ /opt/app/application/
COPY --from=builder dependencies/ /opt/app/dependencies/
COPY --from=builder spring-boot-loader/ /opt/app/spring-boot-loader/
COPY --from=builder snapshot-dependencies/ /opt/app/snapshot-dependencies/
COPY lib/applicationinsights.json /opt/app/

EXPOSE 4455

ENTRYPOINT [
    "java",
    " -cp",
    "/opt/app/spring-boot-loader/spring-boot-loader.jar:/opt/app/dependencies/*:/opt/app/snapshot-dependencies/*:/opt/app/application/",
    "org.springframework.boot.loader.launch.JarLauncher"
]