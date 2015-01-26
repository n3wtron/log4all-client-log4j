package net.log4all.log4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.log4all.client.Log4AllClient;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Created by igor on 05/06/14.
 */
public class Log4AllAppender extends AppenderSkeleton {
	private final Object cacheLock = new Object();
	private String cacheFile;
	private String log4allUrl;
	private String application;
	private String token;
	private long maxSize = 5242880;
	private int maxOldSec = 60;
	private static Date lastFlusherStarted = null;

	@Override
	protected void append(LoggingEvent event) {
		Map<String, String> additionalTags = new HashMap<String, String>();
		sendLog(event, additionalTags);
		if (lastFlusherStarted == null || lastFlusherStarted.before(new Date(new Date().getTime() - maxOldSec * 1000))) {
			startFlusher();
		}
	}

	private void sendLog(LoggingEvent event, Map<String, String> additionalTags) {
		cacheLog(event, additionalTags);
	}

	private void flushCache() {
		String nwCacheFile = null;
		synchronized (cacheLock) {
			File cacheFl = new File(getCacheFile());
			if (cacheFl.exists()) {
				// rename cacheFile for the thread to prevent multi flush
				nwCacheFile = getCacheFile() + "-" + UUID.randomUUID().toString() + ".toFlush";
				try {
					FileUtils.moveFile(cacheFl, new File(nwCacheFile));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		startFlusher();
	}

	private void startFlusher() {
		lastFlusherStarted = new Date();
		System.out.println("Starting flusher");
		CacheFlusher flusher = new CacheFlusher(getLog4allUrl(), getApplication(), getToken(),getCacheFile());
		Thread flusherTh = new Thread(flusher);
		flusherTh.start();
	}

	private void cacheLog(LoggingEvent event, Map<String, String> additionalTags) {
		if (getCacheFile() != null && !getCacheFile().isEmpty()) {
			File outFile = new File(getCacheFile());
			if (outFile.length() > maxSize || outFile.lastModified() < (new Date().getTime() - (maxOldSec * 1000))) {
				flushCache();
			}
			synchronized (cacheLock) {
				FileWriter fwriter = null;
				try {
					String message = event.getMessage().toString();
					for (Map.Entry<String, String> hashEntry : additionalTags.entrySet()) {
						message += " #" + hashEntry.getKey() + ":" + hashEntry.getValue();
					}
					JSONObject jsonLog;
					if (event.getThrowableInformation() != null) {
						StringBuffer stackBuff = new StringBuffer();
						for (String snglStack : event.getThrowableInformation().getThrowableStrRep()) {
							stackBuff.append(snglStack + "\n");
						}
						jsonLog = Log4AllClient.toJSON(message, event.getLevel().toString().replace('\n', ' '), stackBuff.toString(), new Date());
					} else {
						jsonLog = Log4AllClient.toJSON(message, event.getLevel().toString().replace('\n', ' '), new Date());
					}
					fwriter = new FileWriter(outFile, true);
					jsonLog.write(fwriter);
					fwriter.write('\n');
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

	public long getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(long maxSize) {
		this.maxSize = maxSize;
	}

	public void setMaxOldSec(int maxOldSec) {
		this.maxOldSec = maxOldSec;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
