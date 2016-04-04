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


public class ScreenCapture4 extends JPanel{
	public static final int HEIGHT = 240;
	public static final int WIDTH  = 240;

	JLabel labelOrigin;
	JLabel labelComputed;
	
	Robot robot;
	private Timer timer;

	static int[] pixList = new int[HEIGHT * WIDTH ];

	public ScreenCapture4() {
		initGUI();
	}

	private void initGUI() {
		labelOrigin = new JLabel();
		labelComputed = new JLabel();
		
		labelComputed.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		labelComputed.setOpaque(true);
		add(labelComputed, BorderLayout.CENTER);

		labelOrigin.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		labelOrigin.setOpaque(true);
		add(labelOrigin, BorderLayout.EAST);
		
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
			final BufferedImage monoImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY); 
			
			Graphics2D g2d = monoImage.createGraphics();
			g2d.drawImage(sourceImage,0, 0, WIDTH, HEIGHT, null); 
			g2d.dispose();
			
			monoImage.getData().getPixels(0, 0, WIDTH, HEIGHT, pixList);
			
			int[] errorList = new int[WIDTH+2];
			int errorPointer = 0;
	        int oldpixel, error;
	        boolean bottom, left, right;
	        
	        for (int i = 0; i < pixList.length; i++) {
	        	left = (i%WIDTH) == 0;
	        	right = (i%WIDTH) == WIDTH-1;
	        	bottom = i > WIDTH*(HEIGHT-1);
	        	
	        	oldpixel = pixList[i] + errorList[errorPointer];
	        	pixList[i] = oldpixel  < 128 ? 0 : 255;
	        	error = oldpixel - pixList[i];
	        	
	        	if (!right) {
	        		errorList[(errorPointer+1)%(errorList.length)] += 7*error/16;
	        	}
                if ( !left && !bottom) {
                	errorList[(errorPointer+WIDTH-1)%(errorList.length)] += 3*error/16;
                }
                if (!bottom) {
                	errorList[(errorPointer+WIDTH)%(errorList.length)] += 5*error/16;
                }
                if ( !right && !bottom ) {
                	errorList[(errorPointer+WIDTH+1)%(errorList.length)] = error/16;
                } else {
                	errorList[(errorPointer+WIDTH+1)%(errorList.length)] = 0;
                }
                
                errorPointer++;
	        	errorPointer %= errorList.length;
			}
	        
			WritableRaster wr = monoImage.getData().createCompatibleWritableRaster();
			wr.setPixels(0, 0, WIDTH, HEIGHT, pixList);

			monoImage.setData(wr);

			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					labelComputed.setIcon(new ImageIcon(monoImage));
					labelOrigin.setIcon(new ImageIcon(sourceImage));
				}
			});
		}
	}

	private static void createAndShowGUI() {
		//Create and set up the window.
		JFrame frame = new JFrame("FrameDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new ScreenCapture4(), BorderLayout.CENTER );
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
