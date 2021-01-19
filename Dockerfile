FROM openjdk:8-alpine

COPY target/uberjar/luminus-scratchpad.jar /luminus-scratchpad/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/luminus-scratchpad/app.jar"]
