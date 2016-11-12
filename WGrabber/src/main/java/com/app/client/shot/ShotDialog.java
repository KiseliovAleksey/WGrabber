package com.app.client.shot;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.WTimer;
import com.app.client.AppEvent;
import com.app.client.ContextListener;
import com.wt.shared.util.Utils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ShotDialog extends JDialog implements ContextListener, Closeable {
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(ShotDialog.class);
	private static final int BORDER = 5;

	private ShotDialogListener listener;
	private WindowAdapter windowListener;
	private WTimer context;
	private Timer labelTimer;
	private Timer windowTimer;
	
	public ShotDialog(WTimer context, final Screenshot screenshot, 
			final ShotDialogListener ls) {
		this.listener = ls;
		this.context = context;
		setTitle(WTimer.getString("SCREEN_CONTROL_TITLE"));
		log.info("Start Shot Dialog");
		
		context.addListener(this);
		
		setFocusable(false);
		setFocusableWindowState(false);
		setResizable(false);
		setIconImage(Utils.loadImage("/online.png"));
		setAlwaysOnTop(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		windowListener =  new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				submit();
				close();
			}
		};
		
		addWindowListener(windowListener);
		windowTimer = new Timer();
		windowTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				submit();
				close();
			}
		}, context.getSettings().getHideImage().getValue() * 1000);
		
		setLayout(new FormLayout("p", "p"));
		JPanel content = new JPanel(new FormLayout("p", 
				"p,2dlu,p,2dlu,p,2dlu,p"));
		content.setBorder(BorderFactory.createEmptyBorder(
				BORDER, BORDER, BORDER, BORDER));
		
		CellConstraints cc = new CellConstraints();
		content.add(new JLabel(trimCaption(
				screenshot.getCaption())), cc.xy(1, 1));
		
		content.add(new JLabel(Utils.format(screenshot.getTime())), 
				cc.xy(1, 3));

		final BufferedImage image = screenshot.getImage();
		final int targetHeight = 100;
		final int targetWidth = (int)Math.round(
				(targetHeight * 1.0 / image.getHeight()) * image.getWidth());
		JPanel result = new JPanel(){
			private static final long serialVersionUID = 1L;
			
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(image, 0, 0, targetWidth, targetHeight, 0, 0, 
						image.getWidth(), image.getHeight(), null);
			}
			
		};
		result.setPreferredSize(new Dimension(
				targetWidth, targetHeight));
		content.add(result, cc.xy(1, 5));
		
		JPanel panel = new JPanel(new FormLayout("1dlu:grow,2dlu,p", "p"));

		final JProgressBar progress = new JProgressBar(
				0, context.getSettings().getHideImage().getValue() + 1);
		progress.setValue(progress.getMaximum());
		content.add(panel, cc.xy(1, 7));

		JButton close = new JButton(WTimer.getString("CANCEL"));
		
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (listener != null) {
					listener.onCancel();
				}
				listener = null;
				log.info("Dispose shot dialog. Cancelling...");
				close();
			}
		});
		
		panel.add(progress, cc.xy(1, 1, "f,f"));
		
		panel.add(close, cc.xy(3, 1));
		
		labelTimer = new Timer();
		labelTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				final int val = progress.getValue();
				if (val > 0) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							progress.setValue(val - 1);							
						}
					});
				}
				else {
					labelTimer.cancel();
					labelTimer = null;
				}
			}
		}, 0, 1000);
		
		add(content, cc.xy(1, 1));

		int top = context.getDialogTop();
		int left = context.getDialogLeft();
		pack();
		setLocation(left - getWidth(), top - getHeight());
		context.addDialog(this);
	}

	private String trimCaption(String caption) {
		return (caption != null && caption.length() > 25) ? 
				caption.substring(0, 25) + "..." : caption;
	}

	private void submit() {
		if (listener != null) {
			listener.onSubmit();
		}
	}
	
	public interface ShotDialogListener {
		
		public void onCancel();
		
		public void onSubmit();
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
		removeWindowListener(windowListener);
		windowListener = null;
		if (windowTimer != null) {
			windowTimer.cancel();
			windowTimer = null;
		}
		if (context != null) {
			context.removeDialog(this);
			context.removeListener(this);
			context = null;
		}
		removeAll();
		listener = null;
		if (labelTimer != null) {
			labelTimer.cancel();
			labelTimer = null;
		}
		getRootPane().removeAll();
		setRootPane(null);
		removeNotify();
		dispose();
	}
}
