# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
ARG APP_INSIGHTS_AGENT_VERSION=3.7.3
ARG PLATFORM=""

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:21-distroless

# Change to non-root privilege
USER hmcts

LABEL maintainer="https://github.com/hmcts/ccd-case-document-am-api"

COPY build/libs/ccd-case-document-am-api.jar /opt/app/
COPY lib/applicationinsights.json /opt/app

EXPOSE 4455

CMD ["ccd-case-document-am-api.jar"]