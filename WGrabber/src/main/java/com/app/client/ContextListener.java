package com.app.client;

public interface ContextListener {
	
	public void onNotifyReceived(AppEvent evt, Object param);
}
