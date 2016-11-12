package com.app.client.memo;

import java.awt.Dimension;
import java.io.Closeable;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.wt.shared.util.Utils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OverlimitDialog extends JDialog implements ContextListener, Closeable {
	
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(OverlimitDialog.class);
	private static final int BORDER = 5;

	private WTimer context;
	private Timer windowTimer;
	
	public OverlimitDialog(WTimer context) {
		this.context = context;
		setTitle(WTimer.getString("OVERLIMIT_TITLE"));
		log.info("Start Overlimit Dialog");
		
		setFocusable(false);
		setFocusableWindowState(false);
		setIconImage(Utils.loadImage("/online.png"));
		setAlwaysOnTop(true);
		setResizable(false);
		setModal(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setMinimumSize(new Dimension(100, 100));
		
		windowTimer = new Timer();
		windowTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				onCancel();
			}
		}, 10000);
		
		setLayout(new FormLayout("p", "p"));
		JPanel content = new JPanel(new FormLayout(
				"120dlu:grow", "p"));
		content.setBorder(BorderFactory.createEmptyBorder(
				BORDER, BORDER, BORDER, BORDER));
		
		CellConstraints cc = new CellConstraints();
		content.add(new JLabel(WTimer.getString(
				"LIMIT_INFO")), cc.xy(1, 1, "f,f"));
		
		add(content, cc.xy(1, 1));
		
		pack();
		int top = context.getDialogTop();
		int left = context.getDialogLeft();
		setLocation(left - getWidth(), top - getHeight());
		context.addDialog(this);
	}

	protected void onCancel() {
		setVisible(false);
		close();
	}

	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		switch(evt) {
			case EXIT:
				close();
			default:
				break;
		}
	}

	@Override
	public void close() {
		setVisible(false);
		if (windowTimer != null) {
			windowTimer.cancel();
			windowTimer = null;
		}
		if (context != null) {
			context.overlimitClosed();
			context.removeDialog(this);
			context = null;
		}
		removeAll();
	}
}
