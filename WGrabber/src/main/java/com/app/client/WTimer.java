package com.app.client;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jnativehook.GlobalScreen;

import com.app.client.backup.BackupProcessor;
import com.app.client.hook.HookVitalityChecker;
import com.app.client.hook.KeyboardHook;
import com.app.client.hook.MouseHook;
import com.app.client.memo.MemoDialog;
import com.app.client.memo.MemoProcessor;
import com.app.client.memo.MeterDialog;
import com.app.client.memo.OverlimitDialog;
import com.app.client.memo.ReminderDialog;
import com.app.client.memo.ReminderProcessor;
import com.app.client.settings.Settings;
import com.app.client.settings.SettingsDialog;
import com.wt.shared.data.StatisticsData;
import com.wt.shared.util.Messages;
import com.wt.shared.util.Utils;
import com.app.client.shot.ScreenshotFailedDialog;
import com.app.client.shot.ShotProcessor;

public class WTimer implements ContextListener, TrayActions {
	private static final Log log = LogFactory.getLog(WTimerTray.class);
	
	private SettingsDialog settingsDlg = null;
	private Settings settings;
	
	private List<ContextListener> listeners = new ArrayList<ContextListener>();
	private WTimerTray tray;
	private FileLock lock;
	private ShotProcessor processor;
	private MemoProcessor memoProcessor;
	private ReminderProcessor reminderProcessor;
	
	private boolean timerActive = false;

	private ReminderDialog reminderDlg = null;
	private MemoDialog memoDlg = null;
	private OverlimitDialog overlimitDlg = null;
	private MeterDialog meterDlg = null;
	private ScreenshotFailedDialog scrFailedDlg = null;
	private ServerFailsDialog serverFailedDlg = null;
	
	private final KeyboardHook keyHook;
	private final MouseHook mouseHook;
	private final HookVitalityChecker hookChecker;

	private FileOutputStream lockFile;
	private Thread backupThread = null;
	
	private Date lastShot = null;
	
	private boolean serverFailShownOnce = false;
	private boolean screenshotFailShownOnce = false;
	private List<WeakReference<JDialog>> stack = 
			new ArrayList<WeakReference<JDialog>>();
	
	private StatisticsData stat;
	
	public WTimer() throws Exception {
		log.info("Staring W-Timer client");
		addListener(this);
		lock();
		
		hookChecker = new HookVitalityChecker(this);
		
		keyHook = new KeyboardHook(hookChecker);
		mouseHook = new MouseHook(hookChecker);

		hookChecker.start();
		
		settings = new Settings();
		initializeMessages();
		
		tray = new WTimerTray(this, this);
		processor = new ShotProcessor(this);
		memoProcessor = new MemoProcessor(this);
		reminderProcessor = new ReminderProcessor(this);
		
		backupThread = new Thread(new BackupProcessor(this));
		backupThread.setDaemon(true);
		backupThread.start();
	}
	
	private void initializeMessages() {
		Messages.initialize(settings.getLocale());
	}

	public static String getString(String key) {
		return Messages.getString(key);
	}
	
	public void setMemo(String memo) {
		settings.setMemoText(memo);
	}

	public String getMemo() {
		return settings.getMemoText();
	}

	public ShotProcessor getProcessor() {
		return processor;
	}

	public MemoProcessor getMemoProcessor() {
		return memoProcessor;
	}

	public ReminderProcessor getReminderProcessor() {
		return reminderProcessor;
	}
	
	private void lock() throws Exception {
		File file = new File("lock");
		file.createNewFile();
		try {
			lockFile = new FileOutputStream(file);
			lock = lockFile.getChannel().tryLock();
			if (lock == null) {
				throw new Exception("Lock = null");
			}
		} catch(Throwable e) {
			log.error("Another instance is already running!", e);
			throw new Exception("Another instance is already running!");
		}
	}

	public JDialog getMainFrame() {
		JDialog d = new JDialog();
		d.setIconImage(Utils.loadImage("/online.png"));
		return d;
	}
	
	public void addListener(ContextListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(ContextListener listener) {
		listeners.remove(listener);
	}
	
	public void notify(AppEvent evt) {
		notify(evt, null);
	}
	
	public void notify(AppEvent evt, Object param) {
		List<ContextListener> copy = new ArrayList<ContextListener>(listeners);
		for (ContextListener l: copy) {
			if (l != null) {
				l.onNotifyReceived(evt, param);
			}
		}
	}
	
	public Settings getSettings() {
		return settings;
	}
	
	public static void main(String[] args) throws Exception {
		new WTimer();
	}

	public boolean isTimerActive() {
		return timerActive;
	}

	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch (evt) {
			case EXIT:
				try {
					backupThread.interrupt();
				}
				catch(Throwable t) {
					log.error("Backup interruption", t);
				}
				try {
					lock.release();
				} catch (IOException e) {
					log.error("Cannot close lock!", e);
				}
				finally {
					GlobalScreen.unregisterNativeHook();
					System.exit(0);
				}
				break;
			case SETTINGS_UPDATED:
				screenshotFailShownOnce = false;
				serverFailShownOnce = false;
				break;
			case SCREENSHOT_FAILS:
				if (settings.isShowScreenshotFails()) {
					if (isTimerActive() || !screenshotFailShownOnce) {
						if (scrFailedDlg == null) {
							scrFailedDlg = new ScreenshotFailedDialog(
											this, (byte[]) param);
							scrFailedDlg.setVisible(true);
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									scrFailedDlg.toFront();
								}
							});
						}
						screenshotFailShownOnce = true;
					}
				}
				ScreenshotFailedDialog.debug((byte[])param);
				break;
			case SERVER_FAILS:
				if (settings.isShowServerFails()) {
					if (isTimerActive() || !serverFailShownOnce) {
						if (serverFailedDlg == null) {
							serverFailedDlg = new ServerFailsDialog(this);
							serverFailedDlg.setVisible(true);
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									serverFailedDlg.toFront();
								}
							});
						}
						serverFailShownOnce = true;
					}
				}
				break;
			case STATISTIC_UPDATED:
				stat = (StatisticsData) param;
				if (stat != null) {
					if (stat.getLimitTime() * 60 <= stat.getThisWeekTime()) {
						showOverlimit();
					}
				}
				break;
			default:
				break;
		}
	}

	public Map<Long, Integer> flushKeyboard() {
		Map<Long, Integer> total = keyHook.getKeyboardActivity();
		keyHook.resetKeyboardHook();
		return total;
	}
	
	public Map<Long, Integer> flushMouse() {
		Map<Long, Integer> total = mouseHook.getMouseActivity();
		mouseHook.resetMouseHook();
		return total;
	}

	@Override
	public void onExit() {
		notify(AppEvent.EXIT);
		tray.removeIcon();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.exit(0);
			}
		});
	}

	@Override
	public void onSettings() {
		if (SettingsDialog.isVisible) {
			return;
		}
		settingsDlg = new SettingsDialog(this);
		settingsDlg.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				settingsDlg.toFront();
			}
		});
	}

	@Override
	public void onStart() {
		timerActive = true;
		notify(AppEvent.ONLINE_CHANGED, timerActive);
		if (keyHook != null) {
			keyHook.setKeyboardHook();
		}
		if (mouseHook != null) {
			mouseHook.setMouseHook();
		}
	}
	
	@Override
	public void onStop() {
		timerActive = false;
		notify(AppEvent.ONLINE_CHANGED, timerActive);
		keyHook.unsetKeyboardHook();
		mouseHook.unsetMouseHook();
	}

	public void showReminder() {
		if (reminderDlg != null) {
			log.info("Reminder is already running! Return...");
			return;
		}
		log.info("Launch reminder");
		notify(AppEvent.REMINDER_STATE_CHANGED, true);
		reminderDlg = new ReminderDialog(this);
		reminderDlg.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				reminderDlg.toFront();
			}
		});
	}

	public void reminderClosed() {
		reminderDlg = null;
		notify(AppEvent.REMINDER_STATE_CHANGED, false);		
	}
	
	public void showMemo() {
		if (memoDlg != null) {
			log.info("Memo is already running! Return...");
			return;
		}
		log.info("Launch memo");
		notify(AppEvent.MEMO_STATE_CHANGED, true);
		memoDlg = new MemoDialog(this, settings.getMemoText());
		memoDlg.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				memoDlg.toFront();
			}
		});
	}
	
	public void showMeter() {
		if (meterDlg != null) {
			log.info("Meter is already running! Return...");
			return;
		}
		log.info("Launch meter");
		notify(AppEvent.METER_STATE_CHANGED, true);
		meterDlg = new MeterDialog(this, stat);
		meterDlg.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				meterDlg.toFront();
			}
		});
	}
	
	public void showOverlimit() {
		if (overlimitDlg != null) {
			log.info("Overlimit is already running! Return...");
			return;
		}
		log.info("Launch overlimit");
		notify(AppEvent.OVERLIMIT_STATE_CHANGED, true);
		overlimitDlg = new OverlimitDialog(this);
		overlimitDlg.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				overlimitDlg.toFront();
			}
		});
	}
	
	public void overlimitClosed() {
		overlimitDlg = null;
		notify(AppEvent.OVERLIMIT_STATE_CHANGED, false);		
	}
	
	public void memoClosed() {
		memoDlg = null;
		notify(AppEvent.MEMO_STATE_CHANGED, false);		
	}
	
	public void meterClosed() {
		meterDlg = null;
		notify(AppEvent.METER_STATE_CHANGED, false);		
	}
	
	public boolean isReminderActive() {
		return reminderDlg != null;
	}

	@Override
	public void onMemo() {
		showMemo();
	}

	@Override
	public void onMeter() {
		showMeter();
	}
	
	public WTimerTray getTray() {
		return tray;
	}

	public void onScreenshotCreated() {
		notify(AppEvent.SCREENSHOT_CREATED);
	}

	public void addDialog(JDialog dlg) {
		stack.add(new WeakReference<JDialog>(dlg));
	}
	
	public void removeDialog(JDialog dlg) {
		List<WeakReference<JDialog>> toRemove = 
				new ArrayList<WeakReference<JDialog>>();
		for (WeakReference<JDialog> d: stack) {
			if (d.get() == null) {
				toRemove.add(d);
				log.info("REMOVE DIALOG");
				continue;
			}
			if (d.get() == dlg) {
				toRemove.add(d);
			}
		}
		stack.removeAll(toRemove);
	}
	
	public int getDialogTop() {
		List<WeakReference<JDialog>> toRemove = 
				new ArrayList<WeakReference<JDialog>>();
		Insets screen = Toolkit.getDefaultToolkit().
				getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration());
		Dimension display = Toolkit.getDefaultToolkit().getScreenSize();
		int top = (int)(display.getHeight() - screen.bottom);
		for (WeakReference<JDialog> dlg: stack) {
			if (dlg.get() == null) {
				toRemove.add(dlg);
				log.info("REMOVE DIALOG");
				continue;
			}
			top = dlg.get().getY();
		}
		stack.removeAll(toRemove);
		return top;
	}
	
	public int getDialogLeft() {
		Insets screen = Toolkit.getDefaultToolkit().
				getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration());
		Dimension display = Toolkit.getDefaultToolkit().getScreenSize();
		return (int)(display.getWidth() - screen.right);
	}

	public StatisticsData getStatistic() {
		return stat;
	}

	public void onScreenshotFailClosed() {
		scrFailedDlg = null;
	}
	
	public void onServerFailClosed() {
		serverFailedDlg = null;
	}

	public Date getLastShot() {
		return lastShot;
	}

	public void setLastShot(Date lastShot) {
		this.lastShot = lastShot;
	}

	public void clearStatistics() {
		stat = null;
	}
}
