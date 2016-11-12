package com.app.client.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

public class DialogUtil {

	public static void centerDialogInContainer(JDialog dialog, Container owner) {
		int x;
		int y;
		Point topLeft;
		Dimension parentSize = owner.getSize();
		Dimension ownSize = dialog.getSize();

		if (owner.isVisible()) {
			topLeft = owner.getLocationOnScreen();
		} else {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			topLeft = new Point((screenSize.width - ownSize.width) / 2,
					(screenSize.height - ownSize.height) / 2);
		}

		if (parentSize.width > ownSize.width) {
			x = ((parentSize.width - ownSize.width) / 2) + topLeft.x;
		} else {
			x = topLeft.x;
		}
		if (parentSize.height > ownSize.height) {
			y = ((parentSize.height - ownSize.height) / 2) + topLeft.y;
		} else {
			y = topLeft.y;
		}
		dialog.setLocation(x, y);
	}

	public static void centerDialogInParent(JDialog dialog) {
		if (dialog.getParent() != null && dialog.getParent().isVisible()
				&& dialog.getParent().getWidth() != 0 
				&& dialog.getParent().getHeight() != 0) {
			centerDialogInContainer(dialog, dialog.getParent());
		} else {
			centerWindowInScreen(dialog);
		}
	}

	public static void centerWindowInScreen(Window window) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension mfSize = window.getSize();
		window.setLocation((screenSize.width - mfSize.width) / 2,
				(screenSize.height - mfSize.height) / 2);
	}

	public static JTextField selectableLabel(String text) {
		JTextField result = new JTextField(text);
		result.setForeground(UIManager.getColor("Label.foreground"));
		result.setBackground(UIManager.getColor("Label.background"));
		result.setFont(UIManager.getFont("Label.font"));
		result.setEditable(false);
		result.setBorder(null);
		result.setMargin(new Insets(2, 6, 2, 2));
		return result;
	}

	public static void scrollTextArea(JTextArea control) {
		if (control != null) {
			int count = control.getLineCount();
			if (count > 0) {
				int end;
				try {
					end = control.getLineStartOffset(count - 1);
					control.setCaretPosition(end);
					control.repaint();
				} catch (BadLocationException e) {
				}
			}
		}
	}

	public static void showModalResizableDialog(JDialog dialog, JFrame mainFrame) {
		DialogUtil.centerDialogInContainer(dialog, mainFrame);
		dialog.setModal(true);
		dialog.setResizable(true);
		dialog.setVisible(true);
	}

	public static void showModal(JDialog dialog) {
		DialogUtil.centerDialogInParent(dialog);
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public static void closeOnEscape(final JDialog dialog) {
		dialog.getRootPane().registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dialog.setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
			JComponent.WHEN_IN_FOCUSED_WINDOW);
	}
	
	public static void addEnterAction(JComponent field, Action action) {
		InputMap inputMap = field.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");

		ActionMap actionMap = field.getActionMap();
		actionMap.put("enter", action);
	}

	public static void setEnabled(Container component, boolean enabled) {
		component.setEnabled(enabled);
		for (Component child : component.getComponents()) {
			if (child instanceof Container) {
				setEnabled((Container) child, enabled);
			}
		}
	}
}
