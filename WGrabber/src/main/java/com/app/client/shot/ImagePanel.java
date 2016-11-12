package com.app.client.shot;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private BufferedImage image;
	
	public ImagePanel(BufferedImage image) {
		this.image = image;
	}

	@Override 
    public void paintComponent(Graphics g) {
		int dx;
		int dy;
		if (getHeight() < getWidth() - 110) {
			dy = this.getHeight();
			dx = (int)((dy * image.getWidth() * 1.0 / (image.getHeight())));
		}
		else {
			dx = this.getWidth();
			dy = (int)((dx * image.getHeight() * 1.0 / (image.getWidth())));
		}
		
        g.drawImage(image, (this.getWidth() - dx) / 2, 
        		(this.getHeight() - dy) / 2, 
        		dx, dy, this); 
    } 
}
