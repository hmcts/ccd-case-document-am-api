FROM adoptopenjdk:11-jre-hotspot as builder

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
ARG PLATFORM=""

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:11-distroless

COPY lib/AI-Agent.xml /opt/app/

COPY --from=builder application/ /opt/app/
COPY --from=builder dependencies/ /opt/app/
# Add 'CMD true or RUN true' if consecutive COPY commands are failing in case (intermittently).
# See https://github.com/moby/moby/issues/37965#issuecomment-771526632
COPY --from=builder spring-boot-loader/ /opt/app/
COPY --from=builder snapshot-dependencies/ /opt/app/

EXPOSE 4455
ENTRYPOINT ["/usr/bin/java", "org.springframework.boot.loader.JarLauncher"]
