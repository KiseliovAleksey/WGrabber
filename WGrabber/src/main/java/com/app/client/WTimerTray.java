package com.app.client;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wt.shared.data.StatisticsData;
import com.wt.shared.util.Utils;

public class WTimerTray implements ContextListener {

	private static final Log log = LogFactory.getLog(WTimerTray.class);
	
	private TrayIcon icon = null;
	private PopupMenu menu = null;
	
	protected WTimer context;
	private TrayActions actions;
	
	private boolean started = false;
	private MenuItem startStopItem;
	private MenuItem meterItem;
	
	public WTimerTray(final WTimer context, 
			final TrayActions actions) throws Exception {
		log.info("Start WTimerTray");
		this.actions = actions;
		this.context = context;
		context.addListener(this);
		menu = new PopupMenu();
		menu.add(getStartStopCommand());
		menu.add(getMemoCommand());
		menu.add(getSettingsCommand());
		menu.add(getMeterCommand());
		menu.add(getExitCommand());
		icon = new TrayIcon(Utils.loadImage("/offline.png"), formatStatistic(null),
				menu);
		icon.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if (arg0.getClickCount() == 2) {
					actions.onSettings();
				}
			}
		});
		SystemTray.getSystemTray().add(icon);
	}

	private MenuItem getMeterCommand() {
		meterItem = new MenuItem(
				WTimer.getString("METER"));
		meterItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				actions.onMeter();
			}
		});
		return meterItem;
	}

	public void removeIcon() {
		SystemTray.getSystemTray().remove(icon);
		context.removeListener(this);
	}
	
	private MenuItem getStartStopCommand() {
		startStopItem = new MenuItem(WTimer.getString("START_TIMER"));
		startStopItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				onStartStop();
			}
		});
		return startStopItem;
	}
	
	public void onStartStop() {
		started = !started;
		if (started) {
			processStart();
		}
		else {
			actions.onStop();
			icon.setImage(Utils.loadImage("/offline.png"));
			startStopItem.setLabel(WTimer.getString("START_TIMER"));
		}
	}
	
	public void processStart() {
		actions.onStart();
		icon.setImage(Utils.loadImage("/online.png"));
		startStopItem.setLabel(WTimer.getString("STOP_TIMER"));
	}
	
	private MenuItem getMemoCommand() {
		final MenuItem item = new MenuItem(
				WTimer.getString("CHANGE_MEMO"));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				actions.onMemo();
			}
		});
		return item;
	}
	
	private MenuItem getSettingsCommand() {
		final MenuItem item = new MenuItem(
				WTimer.getString("SETTINGS_TITLE"));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				actions.onSettings();
			}
		});
		return item;
	}
	
	private MenuItem getExitCommand() throws Exception {
		MenuItem item = new MenuItem(WTimer.getString("EXIT"));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				actions.onExit();
			}
		});
		return item;
	}

	public void setIcon(Image loadImage) {
		icon.setImage(loadImage);
	}

	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch(evt) {
			case STATISTIC_UPDATED:
			case MEMO_STATE_CHANGED:
			case SCREENSHOT_CREATED:
				if (param instanceof StatisticsData) {
					icon.setToolTip(formatStatistic((StatisticsData)param));
				}
				else {
					icon.setToolTip(formatStatistic(context.getStatistic()));
				}
				break;
			default:
				break;
		}
	}

	private String formatStatistic(StatisticsData s) {
		String res = "W-Timer - ";
		String memoText = context.getSettings().getMemoText();
		int count = memoText.split("\n").length;
		memoText = memoText.split("\n")[0];
		if (memoText.length() > 30) {
			memoText = memoText.substring(0, 27) + "...";
		}
		else {
			if (count > 1) {
				memoText += "...";
			}
		}
		if (memoText == null || memoText.length() == 0) {
			memoText = "No memo";
		}
		res += memoText;
		if (s != null) {
			res += "\n";
			String week = s.getThisWeekTimeString();
			
			week += " [" + s.getThisWeekEarningsString() + "]";
			
			res += week;
		}
		if (context.getLastShot() != null) {
			res += "\n";
			res += WTimer.getString("LAST_SNAPSHOT");
			res += " " + new SimpleDateFormat("HH:mm:ss").format(
					context.getLastShot());
		}
		return res;
	}
}
