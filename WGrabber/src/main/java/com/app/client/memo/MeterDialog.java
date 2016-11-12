package com.app.client.memo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
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
import com.wt.shared.AppRegistry;
import com.wt.shared.data.StatisticsData;
import com.wt.shared.util.Utils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MeterDialog extends JDialog implements ContextListener, Closeable {
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(MeterDialog.class);
	private static final int BORDER = 5;

	private WTimer context;
	private Timer windowTimer;
	
	public MeterDialog(WTimer context, 
			StatisticsData data) {
		this.context = context;
		setTitle(WTimer.getString("METER_TITLE"));
		log.info("Start Meter Dialog");
		
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
				"55dlu,2dlu,30dlu,2dlu,30dlu", 
				"p,2dlu,p,2dlu,p,2dlu,p,2dlu,p,2dlu,p,2dlu,p"));
		content.setBorder(BorderFactory.createEmptyBorder(
				BORDER, BORDER, BORDER, BORDER));
		
		CellConstraints cc = new CellConstraints();
		if (data != null) {
			int y = 1;
			
			content.add(new JLabel(
					WTimer.getString("TODAY")), cc.xy(1, y));
			content.add(new JLabel(data.getTodayTimeString()),
					cc.xy(3, y));
			
			content.add(new JLabel(data.getTodayEarningsString()),
					cc.xy(5, y));
			y += 2;
				
			content.add(new JLabel(
				WTimer.getString("THIS_WEEK")), cc.xy(1, y));
			content.add(new JLabel(data.getThisWeekTimeString()),
					cc.xy(3, y));
			
			content.add(new JLabel(data.getThisWeekEarningsString()),
					cc.xy(5, y));
			y += 2;
			
			content.add(new JLabel(
					WTimer.getString("THIS_MONTH")), cc.xy(1, y));
			content.add(new JLabel(data.getThisMonthTimeString()),
					cc.xy(3, y));
			
			content.add(new JLabel(data.getThisMonthEarningsString()),
					cc.xy(5, y));
			y += 2;
			
			content.add(new JLabel(
					WTimer.getString("LAST_WEEK")), cc.xy(1, y));
			content.add(new JLabel(data.getLastWeekTimeString()),
						cc.xy(3, y));
				
			content.add(new JLabel(data.getLastWeekEarningsString()),
						cc.xy(5, y));
			y += 2;
				
			content.add(new JLabel(
					WTimer.getString("LAST_MONTH")), cc.xy(1, y));
			content.add(new JLabel(data.getLastMonthTimeString()),
					cc.xy(3, y));
			
			content.add(new JLabel(data.getLastMonthEarningsString()),
					cc.xy(5, y));
			y += 2;
			
			content.add(getChart(data), cc.xyw(1, y, 5, "f,f"));
			y += 2;
			
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent arg0) {
					onCancel();
				}
			});
		}
		else {
			content.add(new JLabel(WTimer.getString(
					"NO_DATA")), cc.xywh(1, 1, 5, 5, "c,c"));
		}
		
		add(content, cc.xy(1, 1));
		
		pack();
		int top = context.getDialogTop();
		int left = context.getDialogLeft();
		setLocation(left - getWidth(), top - getHeight());
		context.addDialog(this);
	}

	private JPanel getChart(StatisticsData data) {
		int height = AppRegistry.INTERVAL + 1;
		final BufferedImage image = new BufferedImage(
				data.getActivityChart().length, height, 
				BufferedImage.TYPE_INT_ARGB);
		int[] sy = data.getActivityChart();
		int[] y = new int[sy.length];
		int[] x = new int[y.length];
		for (int i = 0; i < x.length; i++) {
			x[i] = i;
			y[i] = AppRegistry.INTERVAL - sy[i];
		}
		
		GeneralPath path = new GeneralPath();
		path.moveTo(x[0], y[0]);
		for (int i = 2; i < x.length; i++) {
			path.curveTo(x[i - 2], y[i - 2], 
					x[i - 1], y[i - 1], 
					x[i], y[i]);
		}
		
		Graphics2D ig = ((Graphics2D)image.getGraphics());
		ig.setRenderingHint(
			    RenderingHints.KEY_ANTIALIASING,
			    RenderingHints.VALUE_ANTIALIAS_ON);
		ig.setStroke(new BasicStroke(
				.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		ig.setColor(Color.BLACK);
		
//		image.getGraphics().setColor(Color.GREEN);
		ig.draw(path);
//		image.getGraphics().drawPolygon(x, y, x.length);
		JPanel panel = new JPanel(){
			private static final long serialVersionUID = 1L;
			
			@Override
			public void paint(Graphics g) {
				g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			}
			
		};
		panel.setPreferredSize(new Dimension(1, 50));
		return panel;
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
			context.meterClosed();
			context.removeDialog(this);
			context = null;
		}
		removeAll();
	}
}
