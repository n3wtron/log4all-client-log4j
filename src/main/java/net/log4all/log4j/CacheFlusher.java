package net.log4all.log4j;

import net.log4all.client.Log4AllClient;
import net.log4all.client.exceptions.Log4AllException;
import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

/**
 * Created by igor on 16/06/14.
 */
public class CacheFlusher implements  Runnable{
    private static Boolean lockFlusher=new Boolean(false);
    private final String log4allUrl;
    private final String application;
    private File basicCacheFile;

    public CacheFlusher(String log4allUrl, String application,String basicCacheFile) {
        this.basicCacheFile = new File(basicCacheFile);
        this.log4allUrl = log4allUrl;
        this.application = application;
    }


    @Override
    public void run() {
        synchronized (lockFlusher){
            if (lockFlusher){
                System.out.println("thread already in executions");
                return;
            }else{
                lockFlusher=true;
            }
        }
        File cacheDir = basicCacheFile.getParentFile();
        File cacheFilesToFlush[] = cacheDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(basicCacheFile.getName()) && name.endsWith(".toFlush");
            }
        });
        Log4AllClient log4allClient = new Log4AllClient(log4allUrl,this.application);
        try {
            for (File cacheFile:cacheFilesToFlush) {
                List<String> lines = FileUtils.readLines(cacheFile);
                JSONArray logs=new JSONArray();
                for (String line:lines) {
                    logs.put(new JSONObject(line));
                }
                if (logs != null) {
                    if(log4allClient.log(logs)) {
                        cacheFile.delete();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Log4AllException e) {
            e.printStackTrace();
        }finally {
            synchronized (lockFlusher){
                lockFlusher=false;
            }
        }
    }

    public String getApplication() {
        return application;
    }
}
