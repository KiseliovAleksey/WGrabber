package com.app.client.memo;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Closeable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.app.client.TimeInterval;
import com.wt.shared.util.Utils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MemoDialog extends JDialog implements ContextListener, Closeable {
	private static final long serialVersionUID = 1L;

	private WTimer context;
	private JTextArea memo;
	private JComboBox<TimeInterval> memoSetting;
	
	public MemoDialog(final WTimer context, String memoText) {
		setTitle(WTimer.getString("CURRENT_TASK"));
		setIconImage(Utils.loadImage("/online.png"));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModal(false);

		this.context = context;
		
		memoSetting = new JComboBox<>(new TimeInterval[] {
				TimeInterval.I5MIN,
				TimeInterval.I10MIN,
				TimeInterval.I30MIN,
				TimeInterval.NEVER
		});
		memoSetting.setSelectedItem(
				context.getSettings().getMemo());
		memoSetting.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				context.getSettings().setMemo(
					(TimeInterval) memoSetting.getSelectedItem());
			}
		});
		
		memo = new JTextArea();
		memo.setText(memoText);
		memo.setOpaque(true);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				onCancel();
			}
		});
		
		JButton ok = new JButton(WTimer.getString("CHANGE_MEMO"));
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onOk();
			}
		});
		
		JButton cancel = new JButton(WTimer.getString("CANCEL"));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});
		
		JPanel container = new JPanel();
		container.setBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5));
		container.setLayout(new FormLayout("p:grow", 
				"p,2dlu,10dlu:grow,2dlu,p"));
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FormLayout("p,70dlu,10dlu:grow,p,2dlu,p", "p"));
		CellConstraints cc = new CellConstraints();
		buttons.add(ok, cc.xy(4, 1));
		buttons.add(cancel, cc.xy(6, 1));
		buttons.add(new JLabel(WTimer.getString("MEMO_SETTING")),
				cc.xy(1, 1));
		buttons.add(memoSetting, cc.xy(2, 1));
		
		container.add(new JLabel(WTimer.getString("TASK_TITLE")), 
				cc.xy(1, 1));
		container.add(memo, cc.xy(1, 3, "f,f"));
		container.add(buttons, cc.xy(1, 5, "f,f"));
		
		setLayout(new FormLayout("300dlu:grow", "170dlu:grow"));
		add(container, cc.xy(1, 1, "f,f"));
		
		context.addListener(this);
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screen.width / 2 - this.getWidth() / 2, 
				screen.height / 2 - this.getHeight() / 2);
	}
	
	private void onOk() {
		context.setMemo(memo.getText());
		onCancel();
	}
	
	private void onCancel() {
		if (context != null) {
			context.getMemoProcessor().updateMemo();
			setVisible(false);
			close();
		}
	}
	
	@Override
	public void onNotifyReceived(AppEvent evt, Object param) {
		if (evt.equals(AppEvent.ONLINE_CHANGED)) {
			onCancel();
		}
		if (evt.equals(AppEvent.EXIT)) {
			onCancel();
		}
	}

	@Override
	public void close(){
		context.memoClosed();
		context.removeListener(this);
		context = null;
		removeAll();
		memo = null;
	}
}
