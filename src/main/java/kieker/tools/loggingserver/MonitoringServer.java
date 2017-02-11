package kieker.tools.loggingserver;

import kieker.analysis.AnalysisController;
import kieker.analysis.IAnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.analysis.plugin.filter.forward.TeeFilter;
import kieker.analysis.plugin.filter.sink.InfluxDBWriterFilter;
import kieker.analysis.plugin.reader.jms.JMSReader;
import kieker.common.configuration.Configuration;
import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;
import kieker.tools.logReplayer.filter.MonitoringRecordLoggerFilter;

public class MonitoringServer implements Runnable {

    private static final Log LOG = LogFactory.getLog(MonitoringServer.class);

    private final String jmsHost;
    private final String jmsPort;
    private final String jmsQueue;
    private final String influxDBURL;
    private final String influxDBPort;
    private final String influxDBUsername;
    private final String influxDBPassword;
    private final String influxDBDatabaseName;

    public MonitoringServer() {
        this.jmsHost = System.getenv("KLS_JMS_HOST");
        this.jmsPort = System.getenv("KLS_JMS_PORT");
        this.jmsQueue = System.getenv("KLS_JMS_QUEUE");
        this.influxDBURL = System.getenv("KLS_INFLUXDB_URL");
        this.influxDBPort = System.getenv("KLS_INFLUXDB_PORT");
        this.influxDBUsername = System.getenv("KLS_INFLUXDB_USERNAME");
        this.influxDBPassword = System.getenv("KLS_INFLUXDB_PASSWORD");
        this.influxDBDatabaseName = System.getenv("KLS_INFLUXDB_DATABASE_NAME");
    }

    public void run() {
        final IAnalysisController analysisController = new AnalysisController();
        final Configuration jmsReaderConfig = new Configuration();
        jmsReaderConfig.setProperty(JMSReader.CONFIG_PROPERTY_NAME_PROVIDERURL, "tcp://" + jmsHost + ":" + jmsPort);
        jmsReaderConfig.setProperty(JMSReader.CONFIG_PROPERTY_NAME_DESTINATION, jmsQueue);
        jmsReaderConfig.setProperty(JMSReader.CONFIG_PROPERTY_NAME_FACTORYLOOKUP, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        System.out.println(jmsReaderConfig.getStringProperty(JMSReader.CONFIG_PROPERTY_NAME_PROVIDERURL));
        final JMSReader jmsReader = new JMSReader(jmsReaderConfig, analysisController);

        final Configuration influxDBConfig = new Configuration();
        influxDBConfig.setProperty(InfluxDBWriterFilter.CONFIG_PROPERTY_DB_URL, this.influxDBURL);
        influxDBConfig.setProperty(InfluxDBWriterFilter.CONFIG_PROPERTY_DB_PORT, this.influxDBPort);
        influxDBConfig.setProperty(InfluxDBWriterFilter.CONFIG_PROPERTY_DB_USERNAME, this.influxDBUsername);
        influxDBConfig.setProperty(InfluxDBWriterFilter.CONFIG_PROPERTY_DB_PASSWORD, this.influxDBPassword);
        influxDBConfig.setProperty(InfluxDBWriterFilter.CONFIG_PROPERTY_DB_NAME, this.influxDBDatabaseName);
        final InfluxDBWriterFilter influxDBWriterFilter = new InfluxDBWriterFilter(influxDBConfig, analysisController);

        final MonitoringRecordLoggerFilter mrlf = new MonitoringRecordLoggerFilter(new Configuration(), analysisController);

        try {
            analysisController.connect(jmsReader, JMSReader.OUTPUT_PORT_NAME_RECORDS, mrlf, MonitoringRecordLoggerFilter.INPUT_PORT_NAME_RECORD);
            analysisController.connect(jmsReader, JMSReader.OUTPUT_PORT_NAME_RECORDS, influxDBWriterFilter, InfluxDBWriterFilter.INPUT_PORT_NAME_RECORD);
            analysisController.run();
        } catch (final IllegalStateException e) {
            LOG.warn("An exception occurred", e);
        } catch (final AnalysisConfigurationException e) {
            LOG.warn("An exception occurred", e);
        }
    }
}
