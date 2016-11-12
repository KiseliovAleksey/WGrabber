package com.app.client.hook;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import com.wt.shared.util.Utils;;

public class KeyboardHook implements NativeKeyListener {

	private static final Log log = LogFactory.getLog(KeyboardHook.class);
	
	private boolean STORE = false;
	private Map<Long, Integer> ACTIVITY = new HashMap<Long, Integer>();
	
	private final HookListener listener;
	
	public KeyboardHook(HookListener listener) {
		this.listener = listener;
	}

	public void nativeKeyPressed(NativeKeyEvent e) {
        if (STORE) {
  		  addAction();
  	  	}
	}
	
	public void nativeKeyReleased(NativeKeyEvent e) {
	}
	
	public void nativeKeyTyped(NativeKeyEvent e) {
	}
	
	public void resetKeyboardHook() {
		log.info("Clear keyboard activity");
		ACTIVITY.clear();
	}
	
	public Map<Long, Integer> getKeyboardActivity() {
		log.info("Get keyboard activity");
		return new HashMap<Long, Integer>(ACTIVITY);
	}
	
	public void setKeyboardHook() {
		STORE = true;
		GlobalScreen.getInstance().addNativeKeyListener(this);
		log.info("Keyboard hook setted");
 	}

	public void unsetKeyboardHook() {
		STORE = false;
		GlobalScreen.getInstance().removeNativeKeyListener(this);
		log.info("Keyboard hook unsetted");
	}
	
	private void addAction() {
		Date now = new Date(Utils.getMinuteStart(
				Utils.now().getTime()));
		boolean newAct = false;
		if (ACTIVITY.get(now.getTime()) == null) {
			ACTIVITY.put(now.getTime(), 1);
			newAct = true;
		}
		else {
			ACTIVITY.put(now.getTime(),
					ACTIVITY.get(now.getTime()) + 1);
		}
		log.debug("Add keyboard action at " + now.getTime() 
				+ ", saved as: " + (newAct ? "new" : "old"));
		listener.onKeyboardActionPerformed();
	}
}
