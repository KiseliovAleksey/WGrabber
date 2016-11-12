package com.app.client.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Helper class to use with input fields that should accept only numbers.
 * 
 * @author Aleksey Prochukhan
 * @version 1.0
 */
public class NumberDoc extends PlainDocument {
	private static final long serialVersionUID = 1L;
	/** The maximum allowed text length. */
	private final int maxLen;
	private final int maxValue;

	/**
	 * Constructs document object
	 * 
	 * @param maxLen
	 *            the maximum allowed text length.
	 */
	public NumberDoc(int maxLen, int maxValue) {
		if ((maxLen < 1) || (maxLen > 8)) {
			maxLen = 8;
		}
		this.maxLen = maxLen;
		this.maxValue = maxValue;
	}

	/**
	 * Constructs document object
	 * 
	 * @param maxLen
	 *            the maximum allowed text length.
	 */
	public NumberDoc(int maxLen) {
		this(maxLen, 0);
	}

	@Override
	public void replace(int off, int len, String text, AttributeSet a)
			throws BadLocationException {
		if (super.getLength() - len + text.length() > maxLen) {
			text = text.substring(0, maxLen - (super.getLength() - len));
		}
		String before = getText(0, getLength());
		try {
			super.replace(off, len, text, a);
			if (getLength() > 0) {
				int value = Integer.valueOf(getText(0, getLength()));
				if (maxValue > 0 && value > maxValue) {
					value = maxValue;
				}
				super.replace(0, getLength(), String.valueOf(value), a);
			}
		} catch (NumberFormatException e) {
			super.replace(0, getLength(), before, a);
		}
	}

	@Override
	public void insertString(int offs, String str, AttributeSet a)
			throws BadLocationException {

		String fragment;
		if (super.getLength() + str.length() < maxLen) {
			fragment = str;
		} else {
			fragment = str.substring(0, maxLen - super.getLength());
		}
		if (fragment != null && fragment.length() > 0) {
			fragment = fragment.replaceFirst("-", "");
		}
		try {
			if (Integer.valueOf(fragment) != null) {
				super.insertString(offs, fragment, a);
			}
		} catch (Exception e) {
			// we are here if user tries to enter a non-numeric value.
		}
	}
}
