FROM openjdk:11

WORKDIR /app

COPY target/testms-*-SNAPSHOT.jar /app/app.jar

CMD ["java", "-jar", "app.jar"]

EXPOSE 8081
