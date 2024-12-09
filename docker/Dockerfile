FROM ubuntu:24.04

ARG user=newman
ARG group=newman
ARG uid=1001
ARG gid=1001

RUN apt-get update -y
RUN apt-get install coreutils wget -y

ENV MAVEN_VERSION 3.6.3
ENV MAVEN_FILE apache-maven-${MAVEN_VERSION}-bin.tar.gz
ENV NODE_VERSION 22.11.0
ENV NODE_FILE node-v${NODE_VERSION}-linux-x64.tar.gz

# Jenkins is run with user `newman`, uid = 1000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
RUN groupadd -g ${gid} ${group} \
    && useradd -d "/home/${user}" -u ${uid} -g ${gid} -m -s /bin/bash ${user}

# Install Java.
RUN apt-get install openjdk-8-jdk -y
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/

# Install Maven
RUN wget --no-verbose -O /tmp/${MAVEN_FILE} \
    http://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_FILE}

# stop building if md5sum does not match
RUN echo "9792c717f5845d952907d5144b8253c3  /tmp/${MAVEN_FILE}" | \
    md5sum -c


# install in /opt/maven
RUN mkdir -p /opt/maven

RUN tar xzf /tmp/${MAVEN_FILE} --strip-components=1 \
    -C /opt/maven

RUN ln -s /opt/maven/bin/mvn /usr/local/bin
RUN rm -f /tmp/${MAVEN_FILE}

# get node
RUN echo 'get node tar.gz'
RUN wget --no-verbose -O /tmp/${NODE_FILE} \
    https://nodejs.org/dist/v${NODE_VERSION}/${NODE_FILE}

RUN mkdir /opt/node

# unpack node tar.gz
RUN echo 'unpacking node tar.gz' && \
    tar zxvf /tmp/${NODE_FILE} --strip-components=1 \
    -C /opt/node && \
    rm -f /tmp/${NODE_FILE}

RUN ln -s /opt/node/bin/node /usr/local/bin && ln -s /opt/node/bin/npm /usr/local/bin

RUN node --version && \
     npm --version

VOLUME /newman/
ENV PATH=$JAVA_HOME/bin:$PATH

USER root
RUN apt-get install sudo curl -y netbase

USER ${user}
