FROM eclipse-temurin:8-jre

RUN mkdir -p /output

ADD MailBox.jar /MailBox.jar

VOLUME /output

EXPOSE 25

ENTRYPOINT ["java","-jar","/MailBox.jar","--background", "--output-dir", "/output", "--port", "25", "--start-server"]
