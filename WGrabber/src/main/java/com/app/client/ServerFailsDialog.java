package com.app.client;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.util.DialogUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ServerFailsDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(
			ServerFailsDialog.class);
	
	private WTimer context;
	
	public ServerFailsDialog(final WTimer context) {
		super(context.getMainFrame());
		this.context = context;
		setLayout(new FormLayout("p", 
				"p,p"));
		add(new JLabel(WTimer.getString("SERVER_FAILS")), 
				new CellConstraints().xy(1, 1));
		JPanel buttons = new JPanel(new FlowLayout());
		add(buttons, new CellConstraints().xy(1, 2));
		
		final JCheckBox neverShow = new JCheckBox(
				WTimer.getString("NEVER_SHOW_SERVER"));
		neverShow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				context.getSettings().setShowServerFails(
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
				context.onSettings();
				onClose();
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
				ServerFailsDialog.this.toFront();
			}
		});
	}

	protected void onClose() {
		setVisible(false);
		if (context != null) {
			context.onServerFailClosed();
		}
		context = null;		
	}
}
