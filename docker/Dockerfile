FROM ubuntu:24.04

ENV user newman
ENV group newman
ENV uid 1001
ENV gid 1001

RUN apt-get update -y
RUN apt-get install coreutils wget -y

ENV MAVEN_VERSION 3.6.3
ENV MAVEN_FILE apache-maven-${MAVEN_VERSION}-bin.tar.gz
ENV NODE_VERSION 10.9.0
ENV NODE_FILE node-v${NODE_VERSION}-linux-x64.tar.gz

# Jenkins is run with user `newman`, uid = 1000
# If you bind mount a volume from the host or a data container,
# ensure you use the same uid
RUN groupadd -g ${gid} ${group} \
    && useradd -d "/home/${user}" -u ${uid} -g ${gid} -m -s /bin/bash ${user}

# Install Java.
RUN apt-get update && apt-get install openjdk-8-jdk-headless openjdk-11-jdk-headless -y
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/


# Install Maven
RUN wget --no-verbose -O /tmp/${MAVEN_FILE} \
    https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_FILE} 
RUN wget --no-verbose -O /tmp/${MAVEN_FILE}.sha512 \
    https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_FILE}.sha512
# stop building if sha512sum does not match
RUN echo "$(cat /tmp/${MAVEN_FILE}.sha512) /tmp/${MAVEN_FILE}" | sha512sum -c


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
    npm --version && \
    npm install npx elm@latest-0.18.0 elm-test@0.18.13

ENV PATH=/node_modules/elm/bin:$PATH
#USER ${user}
#RUN echo 'installing elm' && \
#    mkdir /home/${user}/.npm-global && \
#    npm config set prefix "/home/${user}/.npm-global"

#ENV PATH=/home/${user}/.npm-global/:$PATH
#ENV globalDir="/home/${user}/.npm-global/"
#RUN cd ${globalDir} && wget --no-verbose -O "${globalDir}"/elm.tar.gz https://github.com/elm-lang/elm-platform/releases/download/0.18.0-exp/elm-platform-linux-64bit.tar.gz && \
#    tar -zxvf "${globalDir}"/elm.tar.gz 

#RUN npm install -g elm@0.18.0

VOLUME /newman/
ENV PATH=$JAVA_HOME/bin:$PATH

USER root
RUN apt-get install sudo curl -y netbase

USER ${user}
