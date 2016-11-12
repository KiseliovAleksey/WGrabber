package com.app.client.shot;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.TimeInterval;
import com.app.client.User32;
import com.app.client.shot.ShotDialog.ShotDialogListener;
import com.wt.shared.data.ScreenshotData;
import com.wt.shared.util.FileUtil;
import com.wt.shared.util.OSUtil;
import com.wt.shared.util.Utils;
import com.sun.jna.Native;
import com.sun.jna.PointerType;

public class Screenshot {
	private static final Log log = LogFactory.getLog(Screenshot.class);
	public static final String FOLDER = "screenshots";
	
	public static final String WIN_TITLE_MAC = "global frontApp, frontAppName, windowTitle\n" +
		"set windowTitle to \"\"\n" +
		"tell application \"System Events\"\n" +
		"    set frontApp to first application process whose frontmost is true\n" +
		"    set frontAppName to name of frontApp\n" +
		"    tell process frontAppName\n" +
		"        tell (1st window whose value of attribute \"AXMain\" is true)\n" +
		"            set windowTitle to value of attribute \"AXTitle\"\n" +
		"        end tell\n" +
		"    end tell\n" +
		"end tell\n" +
		"return {frontAppName, windowTitle}\n";
	
	private WTimer context;
	private BufferedImage screenShot;
	private Date time;
	private String caption;
	
	public Screenshot(WTimer context) {
		this.context = context;
	}
	
	public void makeScreenShort() {
		if (context.getLastShot() != null &&
				((Utils.now().getTime() - context.getLastShot().getTime()) <= 
				TimeInterval.I16SEC.getValue() * 1000)) {
			log.error("There were Screenshot last 16 sec. " +
					"THIS SHOULD NOT HAPPENS! Returning...");
//			return;
		}
		context.setLastShot(Utils.now());
		log.info("Making Screenshot!");
		try {
			Robot r = new Robot();
			log.info("Robot instance created");
			screenShot = r.createScreenCapture(
				new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			log.info("Screen capture completed");
			//normally this should take some time
			Utils.synchronizeTime();
			byte[] data = serializeImageInfo();
			encrypt(data);
			
			//
//			byte[] dec = decrypt(screenShot);
//			System.out.println("---");
//			debug(dec);
//			ScreenshotData dc = deserializeImageInfo(dec);
//			System.out.println(dc.getCaption());
			//
			
			if (context != null) {
				new ShotDialog(context, this,
						new ShotDialogListener() {
							@Override
							public void onSubmit() {
								processNext(context.getSettings().getWorkdir());
							}
							@Override
							public void onCancel() {
								//reset mouse and kb state
								context.flushKeyboard();
								context.flushMouse();
							}
						}).setVisible(true);
			}
			else {
				processNext(context.getSettings().getWorkdir());
			}
		}
		catch(Exception e) {
			log.error(e, e);
		}
	}
	
	private void processNext(final String workdir) {
		Thread processThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String path = FileUtil.mergePath(workdir, FOLDER);
					new File(path).mkdirs();
					ImageIO.write(screenShot, "PNG", 
							new File(FileUtil.mergePath(workdir, FOLDER,
									Utils.now().getTime() + ".png")));
					context.onScreenshotCreated();
				} catch (Exception e) {
					if (context != null) {
						JOptionPane.showMessageDialog(context.getMainFrame(), 
							"Cannot save image! " +
								"Check permissions on temp folder!");
					}
					log.error(e, e);
				}				
			}
		});
		processThread.start();
	}
	
	private void encrypt(byte[] sData) {
		//4192 bit + 512 bytes + 4 bytes
		int index = 0;
		List<Byte> target = new ArrayList<Byte>();
		byte[] length = ByteBuffer.allocate(4).
				putInt(sData.length).array();
		for (byte b: length) {
			target.add(b);
		}
		for (byte b: sData) {
			target.add(b);
		}
		Byte[] data = target.toArray(new Byte[0]);
		for (int i = 0; i < screenShot.getWidth(); i++) {
			for (int j = 0; j < screenShot.getHeight(); j++) {
				int rgb = screenShot.getRGB(i, j);
				rgb &= 0xFFFFFFFE;
				if (((data[index / 8] >> (7 - index % 8)) & 0x1) == 1) {
					rgb++;
				}
				screenShot.setRGB(i, j, rgb);
				if (index == data.length * 8 - 1) {
					break;
				}
				index++;
			}
		}
	}

	private byte[] serializeImageInfo() {
		this.time = context.getLastShot();
		caption = "N/A";
		if (OSUtil.isWin() || OSUtil.isWinVista()) {
			try {
				caption = getWinTitle();
			}
			catch(Throwable t) {
				log.error("Cannot get active window text for Win", t);
			}
		}
		if (OSUtil.isMacos()) {
			try {
				caption = getMacTitle();
			}
			catch(Throwable t) {
				log.error("Cannot get active window text for Mac", t);
			}
		}
		if (OSUtil.isLinux()) {
			log.error("Window Title not implemented for Linux");
		}
		
		log.debug("Caption: " + caption);
		
		Map<Long, Integer> keys = context == null ?
				null : context.flushKeyboard();
		Map<Long, Integer> mouse = context == null ?
				null : context.flushMouse();
		
		ScreenshotData data = new ScreenshotData();
		data.setCaption(caption);
		data.setDate(time);
		data.setImage(screenShot);
		data.setKeys(keys);
		data.setMouse(mouse);
		data.setUsername(context.getSettings().getLogin());
		data.setPassword(context.getSettings().getPassword());
		data.setServer(context.getSettings().getServer());
		data.setMemo(context.getSettings().getMemoText());
		data.setTimeSynchronized(Utils.TIME_SYNCHRONIZED);
		
		return data.getBytes();
	}

	private String getWinTitle() throws Throwable {
		byte[] windowText = new byte[512];
		PointerType hwnd = User32.INSTANCE.GetForegroundWindow(); 
		User32.INSTANCE.GetWindowTextA(hwnd, windowText, 512); 
		
		return Native.toString(windowText);
	}
	
	private String getMacTitle() throws Throwable {
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(
				new String[] {"osascript",  "-e", WIN_TITLE_MAC});
		BufferedReader buf = new BufferedReader(
			new InputStreamReader(process.getInputStream()));
		String line = "";
		String output = "";
		while ((line = buf.readLine()) != null) {
			output = line;
		}
		return output;
	}
	
	public String getCaption() {
		return caption;
	}
	
	public Date getTime() {
		return time;
	}

	public BufferedImage getImage() {
		return screenShot;
	}
}
