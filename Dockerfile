FROM adoptopenjdk:11-jre-hotspot as builder
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY lib/AI-Agent.xml /opt/app/

COPY --from=builder application/ /opt/app/
COPY --from=builder dependencies/ /opt/app/
# Consecutive COPY commands failing intermittently.
# See https://github.com/moby/moby/issues/37965#issuecomment-771526632
CMD true
COPY --from=builder spring-boot-loader/ /opt/app/
CMD true
COPY --from=builder snapshot-dependencies/ /opt/app/

EXPOSE 4455
ENTRYPOINT ["/usr/bin/java", "org.springframework.boot.loader.JarLauncher"]
