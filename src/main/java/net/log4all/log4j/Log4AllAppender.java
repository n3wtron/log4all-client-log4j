package net.log4all.log4j;

import net.log4all.client.Log4AllClient;
import net.log4all.client.exceptions.Log4AllException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by igor on 05/06/14.
 */
public class Log4AllAppender extends AppenderSkeleton {
    private static Log4AllClient log4allClient=null;
    private String cacheFile;
    private String log4allUrl;

    @Override
    protected void append(LoggingEvent event) {
        if (log4allClient == null) {
            log4allClient = new Log4AllClient(getLog4allUrl());
        }
        sendLog(event);
    }

    private void sendLog(LoggingEvent event) {
        try {
            log4allClient.log(event.getMessage().toString());
            flushCache();
        } catch (Log4AllException e) {
            e.printStackTrace();
            cacheLog(event);
        }
    }

    private synchronized void flushCache() {
        try {
            JSONArray logs = loadCacheFile();
            if (logs!=null) {
                for (int i = 0; i < logs.length(); i++) {
                    log4allClient.log(logs.getJSONObject(i));
                }
                File cacheF = new File(getCacheFile());
                cacheF.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Log4AllException e) {
            e.printStackTrace();
        }
    }

    private JSONArray loadCacheFile() throws IOException, JSONException {
        if (getCacheFile() != null && !getCacheFile().isEmpty()) {
            File cFile = new File(getCacheFile());
            if (cFile.exists()) {
                JSONArray logs = new JSONArray(FileUtils.readFileToString(cFile));
                return logs;
            }
        }
        return null;
    }

    private synchronized void cacheLog(LoggingEvent event) {
        if (getCacheFile() != null && !getCacheFile().isEmpty()) {
            File outFile = new File(getCacheFile());
            FileWriter fwriter = null;
            try {
                JSONObject jsonLog = Log4AllClient.toJSON(event.getMessage().toString());
                JSONArray logs = loadCacheFile();
                if (logs==null){
                    logs=new JSONArray();
                }
                logs.put(jsonLog);
                fwriter = new FileWriter(outFile, false);
                logs.write(fwriter);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fwriter != null) {
                    try {
                        fwriter.flush();
                        fwriter.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }


    public String getCacheFile() {
        return cacheFile;
    }

    public void setCacheFile(String cacheFile) {
        this.cacheFile = cacheFile;
    }

    public String getLog4allUrl() {
        return log4allUrl;
    }

    public void setLog4allUrl(String log4allUrl) {
        this.log4allUrl = log4allUrl;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
