package kieker.tools.loggingserver;

import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;


@RestController
public class LogProviderService {

    private static final Log LOG = LogFactory.getLog(MonitoringServer.class);

    @RequestMapping(value = "/logs")
    public void getLogs(HttpServletResponse response, @RequestParam(value="count", defaultValue="4") int count) {
        try {
            final File zipFile = createZip(count);

            if(null != zipFile && zipFile.exists()) {

                final InputStream inputStream = new FileInputStream(zipFile);
                response.addHeader("Content-disposition", "attachment;filename=" + zipFile.getName());
                response.setContentType("application/octet-stream");

                IOUtils.copy(inputStream, response.getOutputStream());
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ZIP error");
            }
        } catch (IOException ioe) {
            LOG.debug(ioe.getMessage());
        }
    }

    private File createZip(int count)  {
        File zipFile = null;
        try {
            Process p = Runtime.getRuntime().exec("/usr/bin/bash /opt/ziptool.sh " + count);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            if((line = reader.readLine()) != null) {
                zipFile = new File(line);
            }
        } catch (IOException ioe) {
            LOG.debug(ioe.getMessage());
        } catch (InterruptedException ire) {
            LOG.debug(ire.getMessage());
        }
        return zipFile;
    }
}
