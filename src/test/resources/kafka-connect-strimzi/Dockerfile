#https://strimzi.io/docs/operators/latest/overview#kafka-connect-components_str
FROM quay.io/strimzi/kafka:0.38.0-kafka-3.6.0

USER root:root

#https://strimzi.io/blog/2020/05/07/camel-kafka-connectors/
RUN mkdir -p /opt/kafka/plugins/camel && \
    cd /opt/kafka/plugins/camel && \
    curl --remote-name --location --silent https://repo1.maven.org/maven2/org/apache/camel/kafkaconnector/camel-rabbitmq-source-kafka-connector/3.21.0/camel-rabbitmq-source-kafka-connector-3.21.0-package.tar.gz && \
    tar -xvzf /opt/kafka/plugins/camel/camel-rabbitmq-source-kafka-connector-3.21.0-package.tar.gz --directory /opt/kafka/plugins/camel && \
    rm /opt/kafka/plugins/camel/camel-rabbitmq-source-kafka-connector-3.21.0-package.tar.gz

USER 1001

CMD ["/opt/kafka/bin/connect-standalone.sh", "/opt/kafka/config/connect-standalone.properties"]

#https://github.com/apache/camel-kafka-connector-examples/tree/main/rabbitmq/rabbitmq-source
#https://github.com/apache/camel-kafka-connector/tree/camel-kafka-connector-3.21.0/connectors/camel-rabbitmq-source-kafka-connector
#https://camel.apache.org/camel-kafka-connector/3.20.x/reference/connectors/camel-rabbitmq-source-kafka-source-connector.html
#https://github.com/apache/camel-kafka-connector/commit/e0bd0ebc959cd69b666e508abfd47517e8b59e41