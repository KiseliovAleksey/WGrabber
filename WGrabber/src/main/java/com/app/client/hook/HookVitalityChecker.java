package com.app.client.hook;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jnativehook.GlobalScreen;

import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.app.client.WTimer;
import com.wt.shared.util.Utils;

public class HookVitalityChecker implements ContextListener, HookListener {

	private static final Log log = LogFactory.getLog(HookVitalityChecker.class);
	
	private static final int CHECKER_INTERVAL = 10 * 1000;
	
	static {
		registerHook();
	}
	
	private static void registerHook() {
		try {
			long ms = System.currentTimeMillis();
			GlobalScreen.registerNativeHook();
			log.info("Hook registered, time: " 
					+ (System.currentTimeMillis() - ms));
		} catch (Throwable e) {
			log.error(e, e);
		}
	}
	
	private static void unregisterHook() {
		try {
			long ms = System.currentTimeMillis();
			GlobalScreen.unregisterNativeHook();
			log.info("Hook unregistered, time: " 
					+ (System.currentTimeMillis() - ms));
		} catch (Throwable e) {
			log.error(e, e);
		}
	}
	
	private WTimer context;
	private Timer checker;
	
	private long lastMouseAction = 0;
	private long lastKeyboardAction = 0;
	
	public HookVitalityChecker(WTimer context) {
		this.context = context;
		this.context.addListener(this);
	}

	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch(evt) {
		case EXIT:
			this.context.removeListener(this);
			this.context = null;
			this.checker.cancel();
			this.checker = null;
			break;
		default:
			break;
		}
	}

	public void start() {
		checker = new Timer();
		checker.schedule(new TimerTask() {
			@Override
			public void run() {
				checkAlive();
			}
		}, CHECKER_INTERVAL, CHECKER_INTERVAL);		
	}

	protected void checkAlive() {
		if (context.isTimerActive()) {
			long now = Utils.now().getTime();
			boolean keyboard = false;
			boolean mouse = false;
			if (now - lastMouseAction > CHECKER_INTERVAL) {
				log.info("No mouse activity over " 
						+ (CHECKER_INTERVAL / 1000) 
						+ " seconds. Trying to resurect it...");
				mouse = true;
				lastMouseAction = now;
			}
			if (now - lastKeyboardAction > CHECKER_INTERVAL) {
				log.info("No keyboard activity over " 
						+ (CHECKER_INTERVAL / 1000) 
						+ " seconds. Trying to resurect it...");
				keyboard = true;
				lastKeyboardAction = now;
			}
			if (mouse || keyboard) {
				unregisterHook();
				registerHook();
			}
		}
	}

	@Override
	public void onMouseActionPerformed() {
		lastMouseAction = Utils.now().getTime();
	}

	@Override
	public void onKeyboardActionPerformed() {
		lastKeyboardAction = Utils.now().getTime();		
	}
	
}
