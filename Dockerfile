FROM postgres:11.5-alpine AS builder

# Set default values for database user and passwords
ARG EHRBASE_USER="ehrbase"
ARG EHRBASE_PASSWORD="ehrbase"
ENV EHRBASE_USER=${EHRBASE_USER:-ehrbase}
ENV EHRBASE_PASSWORD=${EHRBASE_PASSWORD:-ehrbase}

# Set Postgres data directory to custom folder
ENV PGDATA="/var/lib/postgresql/pgdata"

# Create custom data directory and change ownership to postgres user
USER root
RUN mkdir -p ${PGDATA} /usr/local/share/doc/
RUN chown postgres: ${PGDATA}
RUN chmod 0700 ${PGDATA}

# Define Postgres version for easier upgrades for the future
ENV PG_MAJOR=11.11

# Copy system scripts to the scripts directory on root
COPY .docker_scripts /scripts
RUN chmod +x /scripts/*

# Install python and dependencies
RUN apk add --update postgresql=${PG_MAJOR}-r0 \
    build-base  \
    git         \
    flex        \
    bison       \
    psutils

# Install JSQuery
RUN cd  /scripts/                                                           \
    &&  git clone https://github.com/postgrespro/jsquery.git                \
    &&  cd jsquery                                                          \
    &&  make USE_PGXS=1                                                     \
    &&  make USE_PGXS=1 install

# Install temporal tables 
RUN cd  /scripts/                                                           \
    &&  git clone https://github.com/mlt/temporal_tables.git --branch mlt   \
    &&  cd temporal_tables                                                  \
    &&  make PGUSER=postgres                                                \
    &&  make install

# Allow regression test to run from /scripts/temporal
RUN chown -R root:postgres  /scripts
RUN chmod -R g+rwx          /scripts

# Initialize basic database cluster
RUN su postgres -c sh -c "/scripts/chain-load.sh SETUP"         \
    &&  su postgres -c sh -c "/usr/local/bin/docker-entrypoint.sh postgres &"   \
    &&  su postgres -c sh -c "/scripts/chain-load.sh WAIT"       \
    &&  su postgres -c sh -c "/scripts/chain-load.sh SPAWNSET"   \
    &&  su postgres -c sh -c "/scripts/chain-load.sh STOP"

RUN echo "DONE" > /scripts/.pginit

# Copy init scripts to init directory
COPY .docker_scripts/chain-load.sh /docker-entrypoint-initdb.d/
# COPY the pg_hba to the PGDATA directory
COPY .docker_scripts/etc/pg_hba.conf ${PGDATA}/
RUN echo "Enforcing ownership, one moment" \
    && chown -R postgres:postgres ${PGDATA}

EXPOSE 5432

# INSTALL JAVA 11 JDK
RUN apk --no-cache add openjdk11 --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community \
  && java --version

# INSTALL MAVEN
ENV MAVEN_VERSION 3.6.3
ENV MAVEN_HOME /usr/lib/mvn
ENV PATH $MAVEN_HOME/bin:$PATH
RUN wget http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  tar -zxvf apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  rm apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  mv apache-maven-$MAVEN_VERSION /usr/lib/mvn \
  && mvn --version

# CACHE EHRBASE DEPENDENCIES
RUN ls -la
COPY ./pom.xml ./pom.xml
COPY ./api/pom.xml ./api/pom.xml
COPY ./application/pom.xml ./application/pom.xml
COPY ./base/pom.xml ./base/pom.xml
COPY ./jooq-pq/pom.xml ./jooq-pq/pom.xml
COPY ./rest-ehr-scape/pom.xml ./rest-ehr-scape/pom.xml
COPY ./rest-openehr/pom.xml ./rest-openehr/pom.xml
COPY ./service/pom.xml ./service/pom.xml
COPY ./test-coverage/pom.xml ./test-coverage/pom.xml 
COPY ./api/src ./api/src
COPY ./application/src ./application/src
COPY ./base/src ./base/src
COPY ./jooq-pq/src ./jooq-pq/src
COPY ./rest-ehr-scape/src ./rest-ehr-scape/src
COPY ./rest-openehr/src ./rest-openehr/src
COPY ./service/src ./service/src
RUN mvn compile dependency:go-offline \
  -Dflyway.skip=true \
  -Djooq.codegen.skip=true \
  -Dmaven.main.skip

# COMPILE EHRBASE
RUN su - postgres -c "pg_ctl -D ${PGDATA} -w start" \
  && mvn compile

# PACKAGE EHRBASE .JAR
RUN ls -la
RUN su - postgres -c "pg_ctl -D ${PGDATA} -w start" \
  && mvn package -Dmaven.javadoc.skip=true -Djacoco.skip=true

RUN ls -la
RUN EHRBASE_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec) \
  && echo ${EHRBASE_VERSION} > /tmp/ehrbase_version \
  && cp application/target/application-${EHRBASE_VERSION}.jar /tmp/ehrbase.jar



# FINAL IMAGE WITH JRE AND JAR ONLY
FROM openjdk:11-jre-slim AS pusher
COPY --from=builder /tmp/ehrbase.jar .
COPY --from=builder /tmp/ehrbase_version .
COPY .docker_scripts/docker-entrypoint.sh .
RUN chmod +x ./docker-entrypoint.sh
RUN echo "EHRBASE_VERSION: $(cat ehrbase_version)"


# Set default ENVs (can be overriten from cli via --build-arg flag)
ARG DB_URL=jdbc:postgresql://ehrdb:5432/ehrbase
ARG DB_USER="ehrbase"
ARG DB_PASS="ehrbase"
ARG SYSTEM_NAME=docker.ehrbase.org

# These environment variables are also applied to startup of the container and can be overwritten by setting it with
# the '-e' flag on 'docker run' command
ENV EHRBASE_VERSION=${EHRBASE_VERSION}
ENV DB_USER=$DB_USER
ENV DB_PASS=$DB_PASS
ENV DB_URL=$DB_URL
ENV SYSTEM_NAME=$SYSTEM_NAME

# Security
ENV SECURITY_AUTHTYPE="NONE"
ENV SECURITY_AUTHUSER="ehrbase-user"
ENV SECURITY_AUTHPASSWORD="SuperSecretPassword"
ENV SECURITY_AUTHADMINUSER="ehrbase-admin"
ENV SECURITY_AUTHADMINPASSWORD="EvenMoreSecretPassword"
ENV SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI=""

# Status Metric endpoint envs
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE="env,health,info,metrics,prometheus"
ENV MANAGEMENT_ENDPOINTS_WEB_BASEPATH="/status"
ENV MANAGEMENT_ENDPOINT_ENV_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_HEALTH_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_HEALTH_DATASOURCE_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_INFO_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_METRICS_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="true"

EXPOSE 8080
CMD ./docker-entrypoint.sh
