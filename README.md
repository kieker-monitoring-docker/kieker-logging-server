## Kieker Logging Server
Springboot-based service receiving Kieker monitoring data via JMS and 
* providing the logs via REST
* writing the logs to InfluxDB

Note: The current implementation of InfluxDB writer can only write `OperationExeuctionRecord` to the database.

### What it does

The Kieker Logging Server (KLS) connects to a JMS Server to receive Kieker monitoring data.
This data is then stored within the container and InfluxDB with extra derived metrics.
The stored logs can be requested via a REST API.
The data stored in InfluxDB can be visualized by Grafana.

### Usage

In the following the usage of the KLS is explained.

#### Receiving Kieker Monitoring Data
The KLS uses environment variables for the definition of the JMS server it connects to.
Before starting KLS, the following environment variables have to be set:
* `KLS_JMS_HOST`: Host name of the JMS server
* `KLS_JMS_PORT`: Port of the JMS server
* `KLS_JMS_QUEUE`: Queue name of the JMS server
* `KLS_INFLUXDB_URL`: Hostname of InfluxDB (e.g. `http://monitoring-influxdb.kube-system`)
* `KLS_INFLUXDB_PORT`: InfluxDB port (default: `8086`)
* `KLS_INFLUXDB_USERNAME`: InfluxDB username (default: `root`)
* `KLS_INFLUXDB_PASSWORD`: InfluxDB password (default: `root`)
* `KLS_INFLUXDB_DATABASE_NAME`: Database name (needs to be manually created before running KLS)


If these variables are set, the server can be started.

### Retrieving the Logging Data
When the KLS is started, it provides an API to request the stored logging data via REST.
The amount of logging data can be specified with the optional parameter `count`.

`GET: http://<hostname>:8080/logs[?count=<n>]`
* `<hostname>` has to be replaced with the actual hostname where the server is started.
* `<n>` specifies the amount of recent log files to be included. If the parameter is not given it **defaults to 4**.

* It returns a ZIP-file named `kiekerlogs.zip` containing the log files in the `*.dat` file format and the `kieker.map` file.

The log files are selected as follows:
If there are less or equal to `<n>` log files, all log files will be included in the response. If `<n>` is greater than the number of log files, the most recent `<n>` log files will be returned **excluding** the last one.

### Visualizing data with Grafana
1. Setup datasource in Grafana to use InfluxDB
1. Select the same database name as set in the environment variable
1. Create queries
