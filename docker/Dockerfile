FROM ubuntu

ARG user=newman
ARG group=newman
ARG uid=1000
ARG gid=1000

RUN apt-get update -y
RUN apt-get install wget nano -y


# Jenkins is run with user `newman`, uid = 1000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
RUN groupadd -g ${gid} ${group} \
    && useradd -d "/home/${user}" -u ${uid} -g ${gid} -m -s /bin/bash ${user}

# Install Java.
RUN apt-get install openjdk-8-jdk -y
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/

# Install Maven
RUN wget --no-verbose -O /tmp/apache-maven-3.3.9.tar.gz \
    http://archive.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz

# stop building if md5sum does not match
RUN echo "516923b3955b6035ba6b0a5b031fbd8b  /tmp/apache-maven-3.3.9.tar.gz" | \
    md5sum -c


# install in /opt/maven
RUN mkdir -p /opt/maven

RUN tar xzf /tmp/apache-maven-3.3.9.tar.gz --strip-components=1 \
    -C /opt/maven

RUN ln -s /opt/maven/bin/mvn /usr/local/bin
RUN rm -f /tmp/apache-maven-3.3.9.tar.gz

# get node
RUN echo 'get node tar.gz'
RUN wget --no-verbose -O /tmp/node-v22.11.0-linux-x64.tar.gz \
    https://nodejs.org/dist/v22.11.0/node-v22.11.0-linux-x64.tar.gz

RUN mkdir /opt/node

# unpack node tar.gz
RUN echo 'unpacking node tar.gz' && \
    tar zxvf /tmp/node-v22.11.0-linux-x64.tar.gz --strip-components=1 \
    -C /opt/node && \
    rm -f /tmp/node-v22.11.0-linux-x64.tar.gz

RUN ln -s /opt/node/bin/node /usr/local/bin && ln -s /opt/node/bin/npm /usr/local/bin

RUN node --version && \
     npm --version

VOLUME /newman/
ENV PATH=$JAVA_HOME/bin:$PATH

USER root
RUN apt-get install sudo curl -y netbase

USER ${user}
