package kieker.analysis.plugin.filter.sink;

import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.Property;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.record.IMonitoringRecord;
import kieker.common.record.controlflow.OperationExecutionRecord;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by tp on 2/11/17.
 */
@Plugin(description = "A filter to write Kieker records to InfluxDB",
        configuration = {
            @Property(name = InfluxDBWriterFilter.CONFIG_PROPERTY_DB_URL, defaultValue = "localhost",
                    description = "InfluxDB URL"),
            @Property(name = InfluxDBWriterFilter.CONFIG_PROPERTY_DB_PORT, defaultValue = "8086",
                    description = "InfluxDB port"),
            @Property(name = InfluxDBWriterFilter.CONFIG_PROPERTY_DB_USERNAME, defaultValue = "root",
                    description = "InfluxDB username (default: root)"),
            @Property(name = InfluxDBWriterFilter.CONFIG_PROPERTY_DB_PASSWORD, defaultValue = "root",
                    description = "InfluxDB password (default: root)"),
            @Property(name = InfluxDBWriterFilter.CONFIG_PROPERTY_DB_NAME, defaultValue = "kieker",
                    description = "InfluxDB database name")
        }
)
public class InfluxDBWriterFilter extends AbstractFilterPlugin {
    public static final String INPUT_PORT_NAME_RECORD = "record";

    public static final String CONFIG_PROPERTY_DB_URL = "databaseURL";
    public static final String CONFIG_PROPERTY_DB_PORT = "databasePort";
    public static final String CONFIG_PROPERTY_DB_USERNAME = "databaseUsername";
    public static final String CONFIG_PROPERTY_DB_PASSWORD = "databasePassword";
    public static final String CONFIG_PROPERTY_DB_NAME = "databaseName";

    private final String dbURL;
    private final int dbPort;
    private final String dbUsername;
    private final String dbPassword;
    private final String dbName;
    private final InfluxDB influxDB;

    /**
     * Creates a new instance of this class using the given parameters.
     *
     * @param configuration
     *            The configuration for this component.
     * @param projectContext
     *            The project context for this component.
     */
    public InfluxDBWriterFilter(final Configuration configuration, final IProjectContext projectContext) {
        super(configuration, projectContext);

        this.dbURL = this.configuration.getStringProperty(CONFIG_PROPERTY_DB_URL);
        this.dbPort = this.configuration.getIntProperty(CONFIG_PROPERTY_DB_PORT);
        this.dbUsername = this.configuration.getStringProperty(CONFIG_PROPERTY_DB_USERNAME);
        this.dbPassword = this.configuration.getStringProperty(CONFIG_PROPERTY_DB_PASSWORD);
        this.dbName = this.configuration.getStringProperty(CONFIG_PROPERTY_DB_NAME);

        System.out.println("Connecting to database");
        System.out.println("dbURL = " + dbURL);
        System.out.println("dbPort = " + dbPort);
        System.out.println("dbUsername = " + dbUsername);
        System.out.println("dbPassword = " + dbPassword);
        System.out.println("dbName = " + dbName);
		// TODO: Properly handle exceptions and try to reconnect if it fails
        this.influxDB = InfluxDBFactory.connect(this.dbURL + ":" + this.dbPort, this.dbUsername, this.dbPassword);
        System.out.println("Connecting to database done");
        this.initialize();
    }

    private void initialize() {
        Pong pong = this.influxDB.ping();
        long responseTime = pong.getResponseTime();
        String version = pong.getVersion();
        System.out.println("Connected to database at " + this.dbURL + ":" + this.dbPort);
        System.out.println("Version: " + version);
        System.out.println("Response time: " + responseTime);
        List<String> dbList = this.influxDB.describeDatabases();
        for (String db : dbList) {
            System.out.println("Existing database: " + db);
        }
//		TODO: Create database if it does not exist
//        if (!dbList.contains(this.dbName)) {
//            System.out.println("Creating database");
//            this.influxDB.createDatabase(this.dbName);
//            System.out.println("Creating database done");
//        }
        this.influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
    }

    @InputPort(name = INPUT_PORT_NAME_RECORD,
            description = "Receives incoming records and writes to InfluxDB",
            eventTypes = { IMonitoringRecord.class }
    )
    public final void inputRecord(final IMonitoringRecord monitoringRecord) {
        if (monitoringRecord instanceof OperationExecutionRecord) {
            final OperationExecutionRecord operationExecutionRecord = (OperationExecutionRecord) monitoringRecord;

            final long timestamp = operationExecutionRecord.getLoggingTimestamp();
            final String hostname = operationExecutionRecord.getHostname();
            final String operationSignature = operationExecutionRecord.getOperationSignature();
            final String sessionId = operationExecutionRecord.getSessionId();
            final int eoi = operationExecutionRecord.getEoi();
            final int ess = operationExecutionRecord.getEss();
            final long tin = operationExecutionRecord.getTin();
            final long tout = operationExecutionRecord.getTout();
            final long traceId = operationExecutionRecord.getTraceId();
            final long responseTime = tout - tin;
            final double responseTimeInMillisecond = TimeUnit.MILLISECONDS.convert(responseTime, TimeUnit.NANOSECONDS);

            final String container_name = hostname.replaceAll("-[^-]+$", "");

            Point point = Point.measurement("operation_execution")
                    .time(timestamp, TimeUnit.NANOSECONDS)
                    .addField("operation_signature", operationSignature)
                    .addField("session_id", sessionId)
                    .addField("trace_id", traceId)
                    .addField("tin", tin)
                    .addField("tout", tout)
                    .addField("hostname", hostname)
                    .addField("eoi", eoi)
                    .addField("ess", ess)
                    .addField("response_time", responseTime)
                    .addField("response_time_in_millisecond", responseTimeInMillisecond)
                    .tag("operation_signature", operationSignature)
                    .tag("hostname", hostname)
                    .tag("container_name", container_name)
                    .build();
            influxDB.write(dbName, "default", point);
        }
    }

    @Override
    public void terminate(boolean error) {
        System.out.println("Closing database");
        this.influxDB.close();
        System.out.println("Closing database done");
    }

    @Override
    public final Configuration getCurrentConfiguration() {
        final Configuration configuration = new Configuration();
        configuration.setProperty(CONFIG_PROPERTY_DB_URL, this.dbURL);
        configuration.setProperty(CONFIG_PROPERTY_DB_PORT, Integer.toString(this.dbPort));
        configuration.setProperty(CONFIG_PROPERTY_DB_USERNAME, this.dbUsername);
        configuration.setProperty(CONFIG_PROPERTY_DB_PASSWORD, this.dbPassword);
        configuration.setProperty(CONFIG_PROPERTY_DB_NAME, this.dbName);
        return configuration;
    }

}
