package tests;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SlitTest01 extends JPanel {
	private static final long serialVersionUID = 8189796520825155250L;
	public static final int HEIGHT = 240;
	public static final int WIDTH  = 320;
	public static final int FPS = 25;

	JLabel labelOrigin;
	JLabel labelComputed;

	Robot robot;
	private Timer timer;

	int[] pixList = new int[HEIGHT * WIDTH];
	int[] verticalLineRGBList = new int[HEIGHT*3];
	int[] verticalLineGrayList = new int[HEIGHT];
	int[] verticalLineErrorList = new int[HEIGHT +2];
	

	public SlitTest01(){

		labelOrigin = new JLabel();
		labelComputed = new JLabel();

		labelComputed.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		labelComputed.setOpaque(true);
		add(labelComputed, BorderLayout.CENTER);

		labelOrigin.setPreferredSize(new Dimension(5, HEIGHT));
		labelOrigin.setBackground(Color.black);
		labelOrigin.setHorizontalAlignment(JLabel.CENTER);
		labelOrigin.setOpaque(true);
		add(labelOrigin, BorderLayout.EAST);

		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		CaptureLineTask task = new CaptureLineTask();

		timer = new Timer();
		timer.schedule(task, 500, (1000/FPS));

	}

	private static void createAndShowGUI() {
		//Create and set up the window.
		JFrame frame = new JFrame("SliT");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new SlitTest01(), BorderLayout.CENTER );
		//Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
	
	private class CaptureLineTask extends TimerTask {
		private int errorPointer = 0;
		@Override
		public void run() {
			Rectangle screenRectangle = new Rectangle(
					MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y - (HEIGHT/2),
					1, HEIGHT);
			final BufferedImage sourceImage = robot.createScreenCapture(screenRectangle);
			final BufferedImage stackImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY); 
			
			sourceImage.getData().getPixels(0, 0, 1, HEIGHT, verticalLineRGBList);
			
			// RGB to gray
			for (int i = 0; i < HEIGHT ; i++) {
				verticalLineGrayList[i] = (verticalLineRGBList[i*3] + verticalLineRGBList[(i*3)+1] + verticalLineRGBList[(i*3)+2])/3;
			}
			
			// Dithering
			int oldpixel, error;
			for (int i = 0; i < HEIGHT ; i++) {
				
				oldpixel = verticalLineGrayList[i] + verticalLineErrorList[errorPointer];
				verticalLineGrayList[i] = oldpixel  < 128 ? 0 : 255;
	        	error = oldpixel - verticalLineGrayList[i];
	        	if (error < -127) error = -127;
	        	if (error > 127) error = 127;
				
	        	if (i != 0) {
	        		verticalLineErrorList[(errorPointer+HEIGHT-1)%(verticalLineErrorList.length)] += 3*error/16;
	        	} 
	        	if (i != HEIGHT-1){
	        		verticalLineErrorList[(errorPointer+1)%(verticalLineErrorList.length)] += 7*error/16;
	        		verticalLineErrorList[(errorPointer+HEIGHT+1)%(verticalLineErrorList.length)] = error/16;
	        	} 
	        	
	        	verticalLineErrorList[(errorPointer+HEIGHT)%(verticalLineErrorList.length)] += 5*error/16;
	        	
	        	errorPointer++;
	        	errorPointer %= verticalLineErrorList.length;
			}
			
			// shift by one column to left
			int x, y;
			for (int i = 0; i < pixList.length; i++) {
				x = i%WIDTH;
				y = i/WIDTH;
				if (x != WIDTH-1){
					pixList[x+(y*WIDTH)] = pixList[x+1+(y*WIDTH)];
				}
			}
			
			// add column
			for (int i = 0; i < HEIGHT ; i++) {
				pixList[(WIDTH-1) + ((i)*WIDTH)] = verticalLineGrayList[i];
			}
	        
			WritableRaster wr = stackImage.getData().createCompatibleWritableRaster();
			wr.setPixels(0, 0, WIDTH, HEIGHT, pixList);

			stackImage.setData(wr);

			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					labelComputed.setIcon(new ImageIcon(stackImage));
					labelOrigin.setIcon(new ImageIcon(sourceImage));
				}
			});
			
		}
		
	}
	
}
