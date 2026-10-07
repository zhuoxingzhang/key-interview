# Runs the interview tool, Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar, as a web service.
#
#   docker build -t key-interview .
#   docker run -p 8080:8080 key-interview      # then open http://localhost:8080
#
# The port is taken from $PORT when the hosting platform sets it, and is 8080 otherwise.
# The memory settings keep the JVM within a 512 MB container.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
CMD ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
