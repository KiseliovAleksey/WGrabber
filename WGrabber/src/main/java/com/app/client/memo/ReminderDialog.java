package com.app.client.memo;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Closeable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.app.client.TimeInterval;
import com.wt.shared.util.Utils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ReminderDialog extends JDialog implements ContextListener, Closeable {
	private static final long serialVersionUID = 1L;

	private WTimer context;
	private JComboBox<TimeInterval> reminderSetting;
	
	public ReminderDialog(final WTimer context) {
		setTitle(WTimer.getString("REMINDER_TITLE"));
		setModal(false);
		this.context = context;
		
		setIconImage(Utils.loadImage("/online.png"));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent arg0) {
			}
			@Override
			public void windowIconified(WindowEvent arg0) {
			}
			@Override
			public void windowDeiconified(WindowEvent arg0) {
			}
			@Override
			public void windowDeactivated(WindowEvent arg0) {
			}
			@Override
			public void windowClosing(WindowEvent arg0) {
			}
			@Override
			public void windowClosed(WindowEvent arg0) {
				onCancel();
			}
			@Override
			public void windowActivated(WindowEvent arg0) {
			}
		});
		
		context.addListener(this);
		
		reminderSetting = new JComboBox<>(new TimeInterval[] {
				TimeInterval.I5MIN,
				TimeInterval.I10MIN,
				TimeInterval.I30MIN,
				TimeInterval.NEVER
		});
		reminderSetting.setSelectedItem(
				context.getSettings().getReminder());
		reminderSetting.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				context.getSettings().setReminder(
						(TimeInterval) reminderSetting.getSelectedItem());
			}
		});
		
		JButton ok = new JButton(WTimer.getString(
				"RESUME"));
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onOk();
			}
		});
		
		JButton cancel = new JButton(
				WTimer.getString("CANCEL"));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});
		
		JPanel container = new JPanel();
		container.setBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5));
		container.setLayout(new FormLayout("p:grow", "max(30dlu;p),2dlu,p"));
		JPanel buttons = new JPanel();
		buttons.setLayout(new FormLayout("p,70dlu,10dlu:grow,p,2dlu,p", "p"));
		CellConstraints cc = new CellConstraints();
		buttons.add(new JLabel(WTimer.getString("REMINDER_SETTING")),
				cc.xy(1, 1));
		buttons.add(reminderSetting, cc.xy(2, 1));
		buttons.add(ok, cc.xy(4, 1));
		buttons.add(cancel, cc.xy(6, 1));
		
		
		container.add(new JLabel(WTimer.getString("WORK_STOPPED_1") + " " 
				+ WTimer.getString(context.getSettings().getReminder().getReplace())
				+ " " + WTimer.getString("WORK_STOPPED_2")), cc.xy(1, 1));
		container.add(buttons, cc.xy(1, 3, "c,f"));
		
		setLayout(new FormLayout("p:grow", "p:grow"));
		add(container, cc.xy(1, 1, "f,f"));
		
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screen.width / 2 - this.getWidth() / 2, 
				screen.height / 2 - this.getHeight() / 2);
	}

	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		if (evt.equals(AppEvent.ONLINE_CHANGED)) {
			setVisible(false);
		}
		if (evt.equals(AppEvent.EXIT)) {
			close();
		}
	}

	private void onCancel() {
		context.getReminderProcessor().updateReminder();
		close();
	}
	
	private void onOk() {
		context.getTray().onStartStop();
		onCancel();
	}

	@Override
	public void close() {
		setVisible(false);
		context.reminderClosed();
		context.removeListener(this);
		context = null;
		removeAll();
		reminderSetting = null;
	}
}