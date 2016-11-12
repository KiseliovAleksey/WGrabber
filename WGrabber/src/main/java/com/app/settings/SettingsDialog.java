package com.app.settings;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.app.client.TimeInterval;
import com.app.client.util.DialogUtil;
import com.app.client.util.NumberDoc;
import com.wt.shared.util.Utils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.l2fprod.common.swing.JDirectoryChooser;

public class SettingsDialog extends JDialog implements ContextListener {
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(SettingsDialog.class);
	
	public static boolean isVisible = false;
	
	private JTextField server;
	private JTextField port;
	private JTextField username;
	private JPasswordField password;
	private JTextField workdir;
	private JComboBox<TimeInterval> memo;
	private JComboBox<TimeInterval> hideimage;
	private JComboBox<TimeInterval> reminder;
	private JButton workdirPath;
	
	private JButton save;
	private JButton cancel;
	
	private WTimer context;
	
	public SettingsDialog(WTimer context) {
		super();
		this.context = context;
		
		setTitle(WTimer.getString("SETTINGS_TITLE"));
		if (this.context.isTimerActive()) {
			setIconImage(Utils.loadImage("/online.png"));
		}
		else {
			setIconImage(Utils.loadImage("/offline.png"));
		}
		setMinimumSize(new Dimension(450, 250));
		createControls();
		initialize();
		initializeGui();
		DialogUtil.centerDialogInParent(this);
		DialogUtil.closeOnEscape(this);
		context.addListener(this);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setModal(false);
	}

	private void initialize() {
		server.setText(context.getSettings().getServer());
		port.setText(context.getSettings().getPort() + "");
		username.setText(context.getSettings().getLogin());
		password.setText(context.getSettings().getPassword());
		workdir.setText(context.getSettings().getWorkdir());
		
		memo.setSelectedItem(context.getSettings().getMemo());
		hideimage.setSelectedItem(context.getSettings().getHideImage());
		reminder.setSelectedItem(context.getSettings().getReminder());
	}

	private void initializeGui() {
		JTabbedPane tabs = new JTabbedPane();
		CellConstraints cc = new CellConstraints();
		setLayout(new FormLayout("10:grow", "10:grow"));
		
		JPanel container = new JPanel();
		container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		container.setLayout(new FormLayout("10:grow", "10:grow,2dlu,p"));
		
		add(container, cc.xy(1, 1, "f,f"));
		container.add(tabs, cc.xy(1, 1, "f,f"));
		
		JPanel connection = new JPanel();
		connection.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		connection.setLayout(new FormLayout("p,2dlu,10dlu:grow", 
				"p,2dlu,p,2dlu,p,2dlu,p,2dlu"));
		connection.add(new JLabel(WTimer.getString("SERVER_SETTING")), 
				cc.xy(1, 1));
		connection.add(server, cc.xy(3, 1, "f,f"));
		
		connection.add(new JLabel(WTimer.getString("PORT_SETTING")), 
				cc.xy(1, 3));
		connection.add(port, cc.xy(3, 3, "f,f"));
		
		connection.add(new JLabel(WTimer.getString("USER_SETTING")), 
				cc.xy(1, 5));
		connection.add(username, cc.xy(3, 5, "f,f"));
		
		connection.add(new JLabel(WTimer.getString("PASS_SETTING")), 
				cc.xy(1, 7));
		connection.add(password, cc.xy(3, 7, "f,f"));
		
		tabs.addTab(WTimer.getString("CONNECTION_SETTING"), connection);
		
		JPanel prefs = new JPanel();
		prefs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		prefs.setLayout(new FormLayout("p,2dlu,10dlu:grow", 
			"p,2dlu,p,2dlu,p,2dlu,p,2dlu,p,2dlu"));
		
		prefs.add(new JLabel(WTimer.getString("MEMO_SETTING")), 
				cc.xy(1, 3));
		prefs.add(memo, cc.xy(3, 3, "f,f"));
		
		prefs.add(new JLabel(WTimer.getString("SCREENSHOT_SETTING")), 
				cc.xy(1, 5));
		prefs.add(hideimage, cc.xy(3, 5, "f,f"));
		
		prefs.add(new JLabel(WTimer.getString("REMINDER_SETTING")), 
				cc.xy(1, 7));
		prefs.add(reminder, cc.xy(3, 7, "f,f"));
		
		prefs.add(new JLabel(WTimer.getString("FOLDER_SETTING")), 
				cc.xy(1, 9));
		prefs.add(createPath(), cc.xy(3, 9, "f,f"));
		
		tabs.addTab(WTimer.getString("PREFS_SETTING"), prefs);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new FormLayout("p,2dlu,p", "p"));
		bottom.add(save, cc.xy(1, 1));
		bottom.add(cancel, cc.xy(3, 1));
		
		container.add(bottom, cc.xy(1, 3, "r,f"));
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		isVisible = visible;
	}
	
	private JPanel createPath() {
		JPanel panel = new JPanel(
				new FormLayout("10dlu:grow,2dlu,p", "p"));
		CellConstraints cc = new CellConstraints();
		panel.add(workdir, cc.xy(1, 1, "f,f"));
		panel.add(workdirPath, cc.xy(3, 1, "f,f"));
		return panel;
	}

	private void createControls() {
		save = new JButton(WTimer.getString("SAVE_SETTINGS"));
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onSave();
			}
		});
		cancel = new JButton(WTimer.getString("CANCEL"));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});
		
		server = new JTextField();
		port = new JTextField();
		port.setDocument(new NumberDoc(5));
		username = new JTextField();
		password = new JPasswordField();
		workdir = new JTextField();
		workdir.setEditable(false);
		
		TimeInterval[] minutes = new TimeInterval[] {
				TimeInterval.I5MIN,
				TimeInterval.I10MIN,
				TimeInterval.I30MIN,
				TimeInterval.NEVER
		};
		TimeInterval[] seconds = new TimeInterval[] {
				TimeInterval.I4SEC,
				TimeInterval.I10SEC,
				TimeInterval.I16SEC,
				TimeInterval.NEVER
		};
		memo = new JComboBox<>(minutes);
		hideimage = new JComboBox<>(seconds);
		reminder = new JComboBox<>(minutes);
		
		workdirPath = new JButton("...");
		final SettingsDialog app = this;
		workdirPath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JDirectoryChooser();
				if (chooser.showOpenDialog(app) == JFileChooser.APPROVE_OPTION) {
					workdir.setText(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
	}

	protected void onCancel() {
		setVisible(false);
	}

	protected void onSave() {
		context.getSettings().setServer(server.getText());
		context.getSettings().setPort(
				Integer.parseInt(port.getText()));
		context.getSettings().setLogin(username.getText());
		context.getSettings().setPassword(
				new String(password.getPassword()));
		context.getSettings().setWorkdir(workdir.getText());
		
		context.getSettings().setMemo(
				(TimeInterval) memo.getSelectedItem());
		context.getSettings().setHideImage(
				(TimeInterval) hideimage.getSelectedItem());
		context.getSettings().setReminder(
				((TimeInterval)reminder.getSelectedItem()));
		
		try {
			context.getSettings().update();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					context.notify(AppEvent.SETTINGS_UPDATED);					
				}
			});
			onCancel();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, 
					WTimer.getString("CANNOT_SAVE_SETTINGS"), 
					WTimer.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
			log.error(e, e);
		}
	}
	
	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		if (evt.equals(AppEvent.ONLINE_CHANGED)) {
			if ((Boolean)param) {
				setIconImage(Utils.loadImage("/online.png"));
			}
			else {
				setIconImage(Utils.loadImage("/offline.png"));
			}
		}
	}
}
