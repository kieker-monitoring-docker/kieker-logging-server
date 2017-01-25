package kieker.tools.loggingserver;

import kieker.analysis.AnalysisController;
import kieker.analysis.IAnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.analysis.plugin.filter.forward.TeeFilter;
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

    public MonitoringServer() {
        this.jmsHost = System.getenv("KLS_JMS_HOST");
        this.jmsPort = System.getenv("KLS_JMS_PORT");
        this.jmsQueue = System.getenv("KLS_JMS_QUEUE");
    }

    public void run() {
        final IAnalysisController analysisController = new AnalysisController();
        final Configuration jmsReaderConfig = new Configuration();

        jmsReaderConfig.setProperty(JMSReader.CONFIG_PROPERTY_NAME_PROVIDERURL, "tcp://" + jmsHost + ":" + jmsPort);
        jmsReaderConfig.setProperty(JMSReader.CONFIG_PROPERTY_NAME_DESTINATION, jmsQueue);
        jmsReaderConfig.setProperty(JMSReader.CONFIG_PROPERTY_NAME_FACTORYLOOKUP, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");

        System.out.println(jmsReaderConfig.getStringProperty(JMSReader.CONFIG_PROPERTY_NAME_PROVIDERURL));

        final JMSReader jmsReader = new JMSReader(jmsReaderConfig, analysisController);
        final TeeFilter teeFilter = new TeeFilter(new Configuration(), analysisController);

        final MonitoringRecordLoggerFilter mrlf = new MonitoringRecordLoggerFilter(new Configuration(), analysisController);

        try {
            analysisController.connect(jmsReader, JMSReader.OUTPUT_PORT_NAME_RECORDS, teeFilter, TeeFilter.INPUT_PORT_NAME_EVENTS);
            analysisController.connect(teeFilter, TeeFilter.OUTPUT_PORT_NAME_RELAYED_EVENTS, mrlf, MonitoringRecordLoggerFilter.INPUT_PORT_NAME_RECORD);
            analysisController.run();
        } catch (final IllegalStateException e) {
            LOG.warn("An exception occurred", e);
        } catch (final AnalysisConfigurationException e) {
            LOG.warn("An exception occurred", e);
        }
    }
}
