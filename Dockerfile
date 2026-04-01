FROM gradle:8.6.0-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble
RUN gradle fatJar

FROM eclipse-temurin:19-jre

RUN mkdir -p /app/log

COPY --from=build /home/gradle/src/app/build/libs/ /app/
COPY --from=build /home/gradle/src/app/log /app/log

EXPOSE 3003
ENTRYPOINT ["java","-jar","/app/Middleware.jar"]
