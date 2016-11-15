FROM ubuntu:16.04
RUN rm /bin/sh && ln -s /bin/bash /bin/sh
MAINTAINER Barak <barakb@gmail.com>

# install prerequisites
RUN apt-get update && \
    apt-get install -y software-properties-common && \
    add-apt-repository ppa:webupd8team/java && \
    apt-get install -y unzip && \
    apt-get -y install wget && \
    apt-get -y install git && \
    apt-get -y install python-httplib2 && \
    apt-get -y install aptitude && \
    apt-get -y install nodejs npm && \
    apt-get -y install emacs24 && \
    apt-get install python-pip -y && \
    apt-get update

RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections

RUN apt-get install -y oracle-java8-installer ca-certificates

ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

RUN wget --no-verbose -O /tmp/apache-maven-3.0.5.tar.gz \
    http://archive.apache.org/dist/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz

# stop building if md5sum does not match
RUN echo "94c51f0dd139b4b8549204d0605a5859  /tmp/apache-maven-3.0.5.tar.gz" | \
    md5sum -c

# install in /opt/maven
RUN mkdir -p /opt/maven && \
    tar xzf /tmp/apache-maven-3.0.5.tar.gz --strip-components=1 \
    -C /opt/maven

RUN ln -s /opt/maven/bin/mvn /usr/local/bin && \
    ln -s /opt/maven/bin/mvn /usr/bin && \
    rm -f /tmp/apache-maven-3.0.5.tar.gz

RUN echo "===> Adding Ansible's PPA..."  && \
    echo "deb http://ppa.launchpad.net/ansible/ansible/ubuntu xenial main" | tee /etc/apt/sources.list.d/ansible.list           && \
    echo "deb-src http://ppa.launchpad.net/ansible/ansible/ubuntu xenial main" | tee -a /etc/apt/sources.list.d/ansible.list    && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 7BB9C367    && \
    DEBIAN_FRONTEND=noninteractive  apt-get update  && \
    \
    \
    echo "===> Installing Ansible..."  && \
    apt-get install -y ansible  && \
    \
    \
    echo "===> Removing Ansible PPA..."  && \
    rm -rf /var/lib/apt/lists/*  /etc/apt/sources.list.d/ansible.list  && \
    \
    \
    echo "===> Adding hosts for convenience..."  && \
    echo 'localhost' > /etc/ansible/hosts

RUN git clone https://github.com/giga-dev/newman.git /newman

RUN ln -s /usr/bin/nodejs /usr/bin/node && \
    npm install -g bower && \
    pip install boto



