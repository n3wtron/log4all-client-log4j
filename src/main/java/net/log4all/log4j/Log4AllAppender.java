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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by igor on 05/06/14.
 */
public class Log4AllAppender extends AppenderSkeleton {
    private static Log4AllClient log4allClient=null;
    private String cacheFile;
    private String log4allUrl;
    private String hashtagLevel;
    private final Object cacheLock = new Object();

    @Override
    protected void append(LoggingEvent event) {
        if (log4allClient == null) {
            log4allClient = new Log4AllClient(getLog4allUrl());
        }
        Map<String,String> additionalTags=new HashMap<String,String>();
        if (hashtagLevel!=null){
            additionalTags.put(hashtagLevel,event.getLevel().toString());
        }
        sendLog(event, additionalTags);
    }

    private void sendLog(LoggingEvent event, Map<String, String> additionalTags) {
        try {
            System.out.println("sendlog");
            String message=event.getMessage().toString();
            for (Map.Entry<String, String> hashEntry : additionalTags.entrySet()){
                message+=" #"+hashEntry.getKey()+":"+hashEntry.getValue();
            }
            log4allClient.log(message,event.getThrowableInformation().getThrowableStrRep());
            flushCache();
        } catch (Log4AllException e) {
            e.printStackTrace();
            cacheLog(event,additionalTags);
        }
    }

    private void flushCache() {
        String nwCacheFile = null;
        synchronized (cacheLock){
            File cacheFl = new File(getCacheFile());
            if (cacheFl.exists()) {
                //rename cacheFile for the thread to prevent multi flush
                nwCacheFile = getCacheFile()+ UUID.randomUUID().toString()+".toFlush";
                try {
                    FileUtils.moveFile(cacheFl,new File(nwCacheFile));
                } catch (IOException e) {
                    e.printStackTrace();
                    nwCacheFile=null;
                }
            }
        }
        CacheFlusher flusher = new CacheFlusher(getLog4allUrl(),getCacheFile());
        Thread flusherTh = new Thread(flusher);
        flusherTh.start();
    }

    public static JSONArray loadCacheFile(String cacheFile) throws IOException, JSONException {
        if (cacheFile != null && !cacheFile.isEmpty()) {
            File cFile = new File(cacheFile);
            if (cFile.exists()) {
                JSONArray logs = new JSONArray(FileUtils.readFileToString(cFile));
                return logs;
            }
        }
        return null;
    }

    private void cacheLog(LoggingEvent event, Map<String, String> additionalTags) {
        if (getCacheFile() != null && !getCacheFile().isEmpty()) {
            synchronized (cacheLock) {
                File outFile = new File(getCacheFile());
                FileWriter fwriter = null;
                try {
                    String message = event.getMessage().toString();
                    for (Map.Entry<String, String> hashEntry : additionalTags.entrySet()) {
                        message += " #" + hashEntry.getKey() + ":" + hashEntry.getValue();
                    }
                    JSONObject jsonLog = Log4AllClient.toJSON(message, event.getThrowableInformation().getThrowableStrRep(),new Date());

                    JSONArray logs = loadCacheFile(getCacheFile());
                    if (logs == null) {
                        logs = new JSONArray();
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

    public String getHashtagLevel() {
        return hashtagLevel;
    }

    public void setHashtagLevel(String hashtagLevel) {
        this.hashtagLevel = hashtagLevel;
    }
}
