package com.app.client.shot;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.util.DialogUtil;
import com.wt.shared.data.RequestStatus;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ScreenshotFailedDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(
			ScreenshotFailedDialog.class);
	
	private WTimer context;
	
	public ScreenshotFailedDialog(final WTimer context,
			byte[] result) {
		super(context.getMainFrame());
		this.context = context;
		setLayout(new FormLayout("p", 
				"p,p"));
		JPanel messages = new JPanel();
		BoxLayout layout = new BoxLayout(messages, BoxLayout.Y_AXIS);
		messages.setLayout(layout);
		add(messages, new CellConstraints().xy(1, 1));
		messages.add(new JLabel(WTimer.getString("SERVER_BACKUP_FAILS")));
		
		Map<RequestStatus, Integer> bugs = new HashMap<RequestStatus, Integer>();
		for (byte res: result) {
			RequestStatus status = RequestStatus.lookup(res);
			if (status != null) {
				if (bugs.get(status) == null) {
					bugs.put(status, 0);
				}
				bugs.put(status, bugs.get(status) + 1);
			}
		}
		bugs.remove(RequestStatus.SUCCESS);
		for (Map.Entry<RequestStatus, Integer> s: bugs.entrySet()) {
			messages.add(new JLabel(s.getKey().getMessage() + 
					" (total " + s.getValue() + ")"));
		}
		
		JPanel buttons = new JPanel(new FlowLayout());
		add(buttons, new CellConstraints().xy(1, 2));
		
		final JCheckBox neverShow = new JCheckBox(
				WTimer.getString("NEVER_SHOW_FAILED_SCREENSHOTS"));
		neverShow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				context.getSettings().setShowScreenshotFails(
						!neverShow.isSelected());
				try {
					context.getSettings().update();
				} catch (Throwable t) {
					log.error(t, t);
				}
			}
		});
		buttons.add(neverShow);
		
		JButton settings = new JButton(
				WTimer.getString("CHECK_SETTINGS"));
		settings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				onClose();
				context.onSettings();
			}
		});
		buttons.add(settings);
		
		JButton close = new JButton(WTimer.getString("CLOSE"));
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				onClose();
			}
		});
		buttons.add(close);
		
		setModal(false);
		setResizable(false);
		pack();
		DialogUtil.centerWindowInScreen(this);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				onClose();
			}
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ScreenshotFailedDialog.this.toFront();
			}
		});
	}

	protected void onClose() {
		setVisible(false);
		if (context != null) {
			context.onScreenshotFailClosed();
		}
		context = null;
	}

	public static void debug(byte[] result) {
		Map<RequestStatus, Integer> bugs = new HashMap<RequestStatus, Integer>();
		for (byte res: result) {
			RequestStatus status = RequestStatus.lookup(res);
			if (status != null) {
				if (bugs.get(status) == null) {
					bugs.put(status, 0);
				}
				bugs.put(status, bugs.get(status) + 1);
			}
		}
		bugs.remove(RequestStatus.SUCCESS);
		for (Map.Entry<RequestStatus, Integer> s: bugs.entrySet()) {
			log.error(s.getKey().getMessage() + 
					" (total " + s.getValue() + ")");
		}		
	}
}
