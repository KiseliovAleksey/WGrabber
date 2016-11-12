package com.app.client.backup;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.app.client.settings.Settings;
import com.app.client.shot.Screenshot;
import com.wt.shared.AppRegistry;
import com.wt.shared.data.RequestStatus;
import com.wt.shared.data.ScreenshotCryptor;
import com.wt.shared.data.StatisticsData;
import com.wt.shared.util.FileUtil;
import com.wt.shared.util.ObjectUtil;
import com.wt.shared.util.Utils;

public class BackupProcessor implements Runnable, ContextListener, Closeable {

	private static final Log log = LogFactory.getLog(BackupProcessor.class);
	
	private static final int CHECK_TIME = 60 * 1000;
	
	private Settings config;
	private Timer timer;
	
	private WTimer context;
	private boolean inProcess = false;
	
	public BackupProcessor(WTimer context) {
		this.context = context;
		context.addListener(this);
	}

	private Settings loadConfig() {
		return new Settings();
	}

	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch(evt) {
			case SETTINGS_UPDATED:
				context.clearStatistics();
			case SCREENSHOT_CREATED:
				try {
					go();
				}
				catch(Throwable t) {
					log.error(t, t);
				}
				break;
			case EXIT:
				close();
				break;
			default:
				break;
		}
	}
	
	
	@Override
	public void run() {
		try {
			log.info("Starting Backup Processor...");
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						go();
					}
					catch(Throwable t) {
						log.error(t, t);
					}
				}
			}, 0, CHECK_TIME);
		} catch (Throwable e) {
			log.error("Start backup service", e);
		}
	}

	private void go() {
		if (inProcess) {
			return;
		}
		inProcess = true;
		try {
			config = loadConfig();
			String path = FileUtil.mergePath(config.getWorkdir(), 
					Screenshot.FOLDER);
			log.info("Searching for screenshots in path: " + path);
			File[] files = new File(path).listFiles();
			if (files == null) {
				log.info("Backup: no files found. Returning...");
				files = new File[0];
//				return;
			}
			log.info("Total files founded: " + files.length);
			List<File> names = new ArrayList<File>();
			for (File f: files) {
				try {
					BufferedImage image = ImageIO.read(f);
					if (ScreenshotCryptor.decrypt(image) != null) {
						names.add(f);
					}
				}
				catch(Throwable t) {
					log.info("File skipped: " + f.getAbsolutePath());
					continue;
				}
			}
			log.info("Left unique files: " + names.size());

			byte[] results = new byte[names.size()];
			int index = 0;
			boolean failed = false;
			for (File f: names) {
				byte result = backupFile(f);
				if (result == RequestStatus.SUCCESS.getCode()) {
					if (f.exists()) {
						log.info("Delete file: " + f.getName());
						f.delete();
					}
				}
				else {
					failed = true;
				}
				results[index++] = result;
			}
			if (context.getStatistic() == null) {
				backupFile(null);
			}
			if (failed) {
				context.notify(AppEvent.SCREENSHOT_FAILS, results);
			}
		}
		finally {
			inProcess = false;
		}
	}

	private byte backupFile(File f) {
		if (f == null || !f.exists()) {
			log.info("Nothing to backup!");
			//continue to get statistics
		}
		Socket s = null;
		try {
			URL url = new URL(config.getServer());
			s = new Socket(url.getHost(), config.getPort());
        }
        catch(Throwable t) {
        	log.error(t, t);
        	context.notify(AppEvent.SERVER_FAILS);
        	return RequestStatus.SOCKET_ERROR.getCode();
        }
		byte result = RequestStatus.SUCCESS.getCode();
        try {
			OutputStream outStream = s.getOutputStream();
            DataOutputStream os = new DataOutputStream(outStream);
            os.writeLong(Utils.now().getTime());
            os.writeUTF(context.getSettings().getLogin());
            os.writeUTF(context.getSettings().getPassword());

            Long fileSize = 0l;
            if (f != null) {
	            log.info("Sending: " + f.getName());
	            fileSize = f.length();
            }
            
            os.writeLong(fileSize);
            if (f != null) {
	            FileInputStream is = new FileInputStream(f);
	            byte[] buf = new byte[AppRegistry.BLOCK_SIZE];
	            int count = 1;
	            
	            MessageDigest hashAlg = MessageDigest.getInstance("MD5");
	            while((count = is.read(buf, 0, AppRegistry.BLOCK_SIZE)) > 0)
	            {
	            	hashAlg.update(buf, 0, count);
	                outStream.write(buf, 0, count);
	            }
	            byte hash[] = hashAlg.digest();
	            outStream.write(hash);
	            is.close();
	            log.info("File sent: " + f.getName());
            }
            DataInputStream stream = new DataInputStream(
            		s.getInputStream());
            result = stream.readByte();
            int statLength = stream.readInt();
            if (statLength > 0) {
            	byte[] buf = new byte[AppRegistry.BLOCK_SIZE];
            	
            	log.info("Receiving statistcs, length: " + statLength);
            	
            	int totalRead = 0;
            	int count = 0;
            	ByteArrayOutputStream ostr = 
            			new ByteArrayOutputStream(statLength);
            	while(totalRead < statLength && count != -1) {
            		count = stream.read(buf, 0, AppRegistry.BLOCK_SIZE);
            		
            		int actual = count;
            		if (totalRead + count > statLength) {
            			actual = (int) (statLength - totalRead);
            		}
            		
            		ostr.write(buf, 0, actual);
            		totalRead += actual;
            	}
            	
				StatisticsData stat = (StatisticsData) 
					ObjectUtil.fromBytes(ostr.toByteArray());
				if (stat != null) {
					if (context.getSettings().getLogin() != null &&
							context.getSettings().getLogin().equals(
									stat.getUsername())) {
						context.notify(AppEvent.STATISTIC_UPDATED, stat);
						log.info(stat);
					}
					if (stat.getUsername() == null) {
						context.notify(AppEvent.STATISTIC_UPDATED, null);
					}
				}
            }
            else {
            	log.info("Statistcs error, length is less than zero: " 
            			+ statLength);
            }
        }
        catch(Throwable t) {
        	log.error("An error occured during sending!", t);
        	return RequestStatus.SERVER_ERROR.getCode();
        }
        finally {
            try {
				s.close();
			} catch (IOException e) {
				log.error(e, e);
			}
        }
        return result;
    }
	
	public void close() {
		if (context != null) {
			context.removeListener(this);
			context = null;
		}
		try {
			if (timer != null) {
				timer.cancel();
			}
		} catch(Exception e) {
			log.error("Stop backup service", e);
		}
	}
}
