FROM java:openjdk-8

WORKDIR /opt

EXPOSE 22 8080

RUN apt-get update && apt-get -y upgrade && apt-get install -y openssh-server zip
RUN sed -i "s%PermitRootLogin without-password%PermitRootLogin yes%g" /etc/ssh/sshd_config
RUN echo "root:toor" | chpasswd

ADD https://github.com/kieker-monitoring-docker/kieker-logging-server/releases/download/0.1/kieker-logging-server-0.1.jar /opt/kls.jar
ADD ziptool.sh /opt/ziptool.sh

CMD \
/etc/init.d/ssh start && \
java -Dorg.apache.activemq.SERIALIZABLE_PACKAGES=* -jar kls.jar 
