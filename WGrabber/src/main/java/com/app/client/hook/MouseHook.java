package com.app.client.hook;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jnativehook.GlobalScreen;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;
import org.jnativehook.mouse.NativeMouseWheelEvent;
import org.jnativehook.mouse.NativeMouseWheelListener;

import com.wt.shared.util.Utils;
 
public class MouseHook implements NativeMouseListener, NativeMouseWheelListener {

	private static final Log log = LogFactory.getLog(MouseHook.class);
	
	private boolean STORE = false;
	 
	private Map<Long, Integer> ACTIVITY = new HashMap<Long, Integer>();
	
	private final HookListener listener;
	
	public MouseHook(HookListener listener) {
		this.listener = listener;
	}
 
	public void resetMouseHook() {
		log.info("Clear mouse activity");
		ACTIVITY.clear();
	}
	
	public Map<Long, Integer> getMouseActivity() {
		log.info("Get mouse activity");
		return new HashMap<Long, Integer>(ACTIVITY);
	}
	
	public void setMouseHook() {
		STORE = true;
		
		GlobalScreen.getInstance().addNativeMouseListener(this);
		GlobalScreen.getInstance().addNativeMouseWheelListener(this);
		
		log.info("Mouse hook setted");
 	}
 
	public void unsetMouseHook() {
		STORE = false;
		
		GlobalScreen.getInstance().removeNativeMouseListener(this);
		GlobalScreen.getInstance().removeNativeMouseWheelListener(this);
		log.info("Mouse hook unsetted");
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
		log.debug("Add mouse action at " + now.getTime() 
				+ ", saved as: " + (newAct ? "new" : "old"));
		listener.onMouseActionPerformed();
	}

	@Override
	public void nativeMouseWheelMoved(NativeMouseWheelEvent arg0) {
		if (STORE) {
			addAction();
		}		
	}

	@Override
	public void nativeMouseClicked(NativeMouseEvent arg0) {
		if (STORE) {
			addAction();
		}		
	}

	@Override
	public void nativeMousePressed(NativeMouseEvent arg0) {
		if (STORE) {
			addAction();
		}		
	}

	@Override
	public void nativeMouseReleased(NativeMouseEvent arg0) {
	}
}