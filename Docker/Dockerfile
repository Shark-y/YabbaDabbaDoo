# https://docs.docker.com/get-started/part2/#dockerfile
# Docker build: https://docs.docker.com/engine/reference/commandline/build/
#		Usage: docker build [OPTIONS] PATH | URL | -
# BUILD: docker build -t c1as-connector-aes .
# RUN: docker run -p 4000:8080 c1as-connector-aes
# Where is your built image? It’s in your machine’s local Docker image registry:
# docker image ls

FROM tomcat:8.5-alpine

WORKDIR /usr/local/tomcat

# Make port 80 available to the world outside this container
EXPOSE 8080

# allows remote access to the TC manager
COPY Docker_tomcat_context.xml $CATALINA_HOME/webapps/manager/META-INF/context.xml

# sets default manager user/pwd tomcat/tomact
COPY Docker_tomcat-users.xml $CATALINA_HOME/conf/tomcat-users.xml

# product file. Must replace w/ product name
# 1) sed 's/__PRODUCT__/War##release-1.1-20190413.war/g' Dockerfile_template > Dockerfile
# 2) docker build -t c1as-connector-[VENDOR] .
COPY ../WebConsole/WebConsole##1.0.war $CATALINA_HOME/webapps
