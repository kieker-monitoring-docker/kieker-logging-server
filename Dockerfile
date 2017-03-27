FROM openjdk:8-jre-alpine

WORKDIR /opt

EXPOSE 8080

ADD target/kieker-logging-server-*.jar /opt/kls.jar
ADD ziptool.sh /opt/ziptool.sh

CMD java -Dorg.apache.activemq.SERIALIZABLE_PACKAGES=* -jar kls.jar 
