FROM eclipse-temurin:21-jre

RUN mkdir -p /output

ADD fakeSMTP-2.1-SNAPSHOT.jar /fakeSMTP.jar

VOLUME /output

EXPOSE 25

ENTRYPOINT ["java","-jar","/fakeSMTP.jar","--background", "--output-dir", "/output", "--port", "25", "--start-server"]
