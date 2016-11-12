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

public class MemoProcessor implements ContextListener, Closeable {
	private static final Log log = LogFactory.getLog(MemoProcessor.class);
	
	private Timer timer;
	private long memoEntered;
	private WTimer context;
	
	public MemoProcessor(final WTimer context) {
		this.context = context;
		context.addListener(this);
	}
	
	public void updateMemo() {
		memoEntered = Utils.now().getTime();
	}
	
	protected void showMemo() {
		context.showMemo();
	}

	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch(evt) {
		case MEMO_STATE_CHANGED:
				if (!context.isTimerActive()) {
					break;
				}
				param = !((Boolean)param);
			case ONLINE_CHANGED:
				if ((Boolean)param) {
					memoEntered = Utils.now().getTime();
					timer = new Timer();
					timer.schedule(new MemoTask(), 0, 1000);
					log.info("Memo timer started");
				}
				else {
					if (timer != null) {
						timer.cancel();
						timer = null;
						log.info("Memo timer cancelled");
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

	private class MemoTask extends TimerTask {
	
		@Override
		public void run() {
			if (context.isTimerActive() && 
					context.getSettings().getMemo() != TimeInterval.NEVER) {
				Date dt = Utils.now();
				long elapsed = dt.getTime() - 
					context.getSettings().getMemo().getValue() * 60 * 1000;
				if (((elapsed - memoEntered) / 1000) % 30 == 0) {
					//debug every 30 sec
					log.info("Start memo after " + 
							(int)((elapsed - memoEntered) / -1000) + "sec");
				}
				if (elapsed >= memoEntered) {
					showMemo();
				}
			}		
		}
	}
	
	@Override
	public void close() {
		context.removeListener(this);
		context = null;
		memoEntered = 0;
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
	}

}
