package com.app.client.memo;

import java.io.Closeable;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.app.client.TimeInterval;
import com.wt.shared.util.Utils;

public class ReminderProcessor implements ContextListener, Closeable {

	private static final Log log = LogFactory.getLog(ReminderProcessor.class);
	
	private WTimer context;
	private Timer timer;
	private long pauseEntered;
	
	public ReminderProcessor(final WTimer context) {
		this.context = context;
		context.addListener(this);
	}
	
	public void updateReminder() {
		pauseEntered = Utils.now().getTime();
	}
	
	protected void showReminder() {
		context.showReminder();
	}
	
	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch(evt) {
			case REMINDER_STATE_CHANGED:
				if (context.isTimerActive()) {
					break;
				}
			case ONLINE_CHANGED:
				if (!(Boolean)param) {
					pauseEntered = Utils.now().getTime();
					timer = new Timer();
					timer.schedule(new ReminderTask(), 0, 1000);
					log.info("Reminder timer started");
				}
				else {
					if (timer != null) {
						timer.cancel();
						timer = null;
						log.info("Reminder timer cancelled");
					}
				}
				break;
			case EXIT:
				close();
				break;
			default:
				break;
		}	
	}

	private class ReminderTask extends TimerTask {

		@Override
		public void run() {
			if (!context.isTimerActive() &&
					context.getSettings().getReminder() != TimeInterval.NEVER) {
				Date dt = Utils.now();
				long elapsed = dt.getTime() - 
					context.getSettings().getReminder().getValue() * 60 * 1000;
				if ((elapsed - pauseEntered) % 30 == 0) {
					//debug every 30 sec
					log.info("Start reminder after " + 
							(int)((elapsed - pauseEntered) / -1000) + "sec");
				}
				if (elapsed >= pauseEntered) {
					showReminder();
				}
			}		
		}
		
	}

	@Override
	public void close() {
		context.removeListener(this);
		context = null;
		pauseEntered = 0;
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
	}
}
