package com.app.client.shot;

import java.io.Closeable;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.app.client.TimeInterval;
import com.wt.shared.AppRegistry;
import com.wt.shared.util.DateUtil;
import com.wt.shared.util.Utils;

public class ShotProcessor implements ContextListener, Closeable {
	public static final int MINIMUM_SECONDS_FOR_INTERVAL = TimeInterval.I16SEC.getValue();
	public static final int MINIMUM_SECONDS_FOR_START = TimeInterval.I4SEC.getValue();
	
	private static final Log log = LogFactory.getLog(ShotProcessor.class);
	
	private WTimer context;
	private IntervalProperties interval;
	private Timer timer;
	
	public ShotProcessor(final WTimer context) {
		this.context = context;
		context.addListener(this);
		interval = new IntervalProperties();
	}
	
	private void initializeInterval() {
		long ms = Utils.now().getTime();
		
		interval.reset();
		interval.setStart(Utils.getIntervalStart(ms));
		interval.setEnd(interval.getStart() + AppRegistry.INTERVAL * 60 * 1000);
		log.info("Interval: " + Utils.format(new Date(interval.getStart()))
			+ " - " + Utils.format(new Date(interval.getEnd())));
		
		int timeForThisInterval = new Long(interval.getEnd() - ms).intValue();
		int start = MINIMUM_SECONDS_FOR_START * 1000;
		int end = MINIMUM_SECONDS_FOR_INTERVAL * 1000;
		if (timeForThisInterval >= start + end) {
			timeForThisInterval -= start + end;
			Random r = new Random(timeForThisInterval);
			interval.setScreen(new Date(ms
//					 + start + 2000)); r.nextInt();
					+ r.nextInt(timeForThisInterval) + start));
//			log.info("Scheduled screen: " + Utils.format(interval.getScreen()));
		}
		else {
			log.info("Less than "
					+ (MINIMUM_SECONDS_FOR_INTERVAL + MINIMUM_SECONDS_FOR_START)
					+ " seconds for the next interval, ignoring...");
		}
	}
	
	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch(evt) {
			case ONLINE_CHANGED:
				if ((Boolean)param) {
					timer = new Timer();
					timer.schedule(new ScreenshotTask(), 0, 1000);
					log.info("Start - Starting with a new interval");
				}
				else {
					timer.cancel();
					timer = null;
					interval.reset();
				}
				break;
			case EXIT:
				timer.cancel();
				timer = null;
				close();
			default:
				break;
		}
	}
	
	public class IntervalProperties implements Closeable {
		private long start = 0;
		private long end = 0;
		private Date scheduledScreen = null;
		
		public void setScreen(Date screen) {
			scheduledScreen = screen;
		}
		
		public Date getScreen() {
			return scheduledScreen;
		}
		
		public void setStart(long start) {
			this.start = start;
		}
		public long getStart() {
			return start;
		}
		public void setEnd(long end) {
			this.end = end;
		}
		public long getEnd() {
			return end;
		}
		
		public boolean isCurrent() {
			Date dt = Utils.now();
			return dt.getTime() > start && dt.getTime() < end;
		}

		public void reset() {
			scheduledScreen = null;
			start = 0;
			end = 0;
		}
		
		@Override
		public void close() {
			scheduledScreen = null;
		}
	}

	private class ScreenshotTask extends TimerTask {
		@Override
		public void run() {
			if (!context.isTimerActive()) {
				return;
			}
			if (!interval.isCurrent()) {
				log.info("Interval Timer Working - Create new Interval");
				initializeInterval();
			}
			if (interval.getScreen() != null) {
				long target = DateUtil.trimMills(interval.getScreen()).getTime();
				long current = Utils.now().getTime();
				if (target <= current) {
					Thread screenThread = new Thread(new Runnable() {
						@Override
						public void run() {
							new Screenshot(context).makeScreenShort(); 
						}
					}, "Screenshot-Thread");
					screenThread.start();
					interval.setScreen(null);
				}
			}
		}
	}

	@Override
	public void close() {
		context.removeListener(this);
		context = null;
		interval.close();
		interval = null;
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
	}
}
