FROM ubuntu

ARG user=newman
ARG group=newman
ARG uid=1025
ARG gid=1025

RUN apt-get update -y
RUN apt-get install wget vim nano -y


# Jenkins is run with user `newman`, uid = 1000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
RUN groupadd -g ${gid} ${group} \
    && useradd -d "/home/${user}" -u ${uid} -g ${gid} -m -s /bin/bash ${user}

# Install Java.
RUN apt-get install openjdk-17-jdk -y && apt-get clean
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64/

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
RUN wget --no-verbose -O /tmp/node-v8.9.3-linux-x64.tar.gz \
    https://nodejs.org/dist/v8.9.3/node-v8.9.3-linux-x64.tar.gz

RUN mkdir /opt/node

# unpack node tar.gz
RUN echo 'unpacking node tar.gz' && \
    tar zxvf /tmp/node-v8.9.3-linux-x64.tar.gz --strip-components=1 \
    -C /opt/node && \
    rm -f /tmp/node-v8.9.3-linux-x64.tar.gz

RUN ln -s /opt/node/bin/node /usr/local/bin && ln -s /opt/node/bin/npm /usr/local/bin

RUN node --version && \
     npm --version


USER ${user}
RUN echo 'installing elm' && \
    mkdir /home/${user}/.npm-global && \
     npm config set prefix "/home/${user}/.npm-global"

ENV PATH=/home/${user}/.npm-global/:$PATH
ENV globalDir="/home/${user}/.npm-global/"
RUN echo ELM
RUN cd ${globalDir} && \
    wget --no-verbose -O "${globalDir}"/elm.tar.gz https://github.com/elm-lang/elm-platform/releases/download/0.18.0-exp/elm-platform-linux-64bit.tar.gz && \
    tar -zxvf "${globalDir}"/elm.tar.gz

#RUN npm install -g elm@0.19.0

VOLUME /newman/
ENV PATH=$JAVA_HOME/bin:$PATH

USER root
RUN apt-get install sudo curl -y netbase

USER ${user}
