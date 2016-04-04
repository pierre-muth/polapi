package tests;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class ScreenCapture5 extends JPanel{
	public static final int HEIGHT = 384;
	public static final int WIDTH  = 384;

	JLabel labelComputed;
	
	Robot robot;
	private Timer timer;

	static int[] pixList = new int[HEIGHT * WIDTH *3];

	public ScreenCapture5() {
		initGUI();
	}

	private void initGUI() {
		labelComputed = new JLabel();
		labelComputed.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		labelComputed.setOpaque(true);
		add(labelComputed, BorderLayout.CENTER);

		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		CaptureTaskDither task = new CaptureTaskDither();

		timer = new Timer();
		timer.schedule(task, 500, 50);
	}

	private class CaptureTaskDither extends TimerTask {

		@Override
		public void run() {
			Rectangle screenRectangle = new Rectangle(
					MouseInfo.getPointerInfo().getLocation().x - (WIDTH/2)
					, MouseInfo.getPointerInfo().getLocation().y - (HEIGHT/2),
					WIDTH, HEIGHT);
			final BufferedImage sourceImage = robot.createScreenCapture(screenRectangle);
			
			sourceImage.getData().getPixels(0, 0, WIDTH, HEIGHT, pixList);
			
			int[] errorListR = new int[WIDTH+2];
			int[] errorListG = new int[WIDTH+2];
			int[] errorListB = new int[WIDTH+2];
			int errorPointer = 0;
	        int oldpixelR,  oldpixelG, oldpixelB;
	        int errorR, errorG, errorB;
	        boolean bottom, left, right;
	        int index;
	        
	        for (int i = 0; i < (WIDTH*HEIGHT); i++) {
	        	index = i*3;
	        	
	        	left = (i%WIDTH) == 0;
	        	right = (i%WIDTH) == WIDTH-1;
	        	bottom = i > WIDTH*(HEIGHT-1);
	        	
	        	oldpixelR = pixList[index] + errorListR[errorPointer];
	        	oldpixelG = pixList[index+1] + errorListG[errorPointer];
	        	oldpixelB = pixList[index+2] + errorListB[errorPointer];
	        	
	        	pixList[index] = oldpixelR  < 128 ? 0 : 255;
	        	pixList[index+1] = oldpixelG  < 128 ? 0 : 255;
	        	pixList[index+2] = oldpixelB  < 128 ? 0 : 255;
	        	
	        	errorR = oldpixelR - pixList[index];
	        	errorG = oldpixelG - pixList[index+1];
	        	errorB = oldpixelB - pixList[index+2];
	        	
	        	if (!right) {
	        		errorListR[(errorPointer+1)%(errorListR.length)] += 7*errorR/16;
	        		errorListG[(errorPointer+1)%(errorListG.length)] += 7*errorG/16;
	        		errorListB[(errorPointer+1)%(errorListB.length)] += 7*errorB/16;
	        	}
                if (!left && !bottom) {
                	errorListR[(errorPointer+WIDTH-1)%(errorListR.length)] += 3*errorR/16;
                	errorListG[(errorPointer+WIDTH-1)%(errorListG.length)] += 3*errorG/16;
                	errorListB[(errorPointer+WIDTH-1)%(errorListB.length)] += 3*errorB/16;
                }
                if (!bottom) {
                	errorListR[(errorPointer+WIDTH)%(errorListR.length)] += 5*errorR/16;
                	errorListG[(errorPointer+WIDTH)%(errorListG.length)] += 5*errorG/16;
                	errorListB[(errorPointer+WIDTH)%(errorListB.length)] += 5*errorB/16;
                }
                if (!right && !bottom) {
                	errorListR[(errorPointer+WIDTH+1)%(errorListR.length)] =   errorR/16;
                	errorListG[(errorPointer+WIDTH+1)%(errorListG.length)] =   errorG/16;
                	errorListB[(errorPointer+WIDTH+1)%(errorListB.length)] =   errorB/16;
                } else {
                	errorListR[(errorPointer+WIDTH+1)%(errorListR.length)] = 0;
                	errorListG[(errorPointer+WIDTH+1)%(errorListG.length)] = 0;
                	errorListB[(errorPointer+WIDTH+1)%(errorListB.length)] = 0;
                }
                
                errorPointer++;
	        	errorPointer %= (errorListR.length);
			}
	        
			WritableRaster wr = sourceImage.getData().createCompatibleWritableRaster();
			wr.setPixels(0, 0, WIDTH, HEIGHT, pixList);

			sourceImage.setData(wr);

			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					labelComputed.setIcon(new ImageIcon(sourceImage));
				}
			});
		}
	}

	private static void createAndShowGUI() {
		//Create and set up the window.
		JFrame frame = new JFrame("FrameDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new ScreenCapture5(), BorderLayout.CENTER );
		//Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
