package polapi;

import java.io.BufferedInputStream;
import java.io.IOException;

public class Camera implements Runnable {

	public static final int IMG_HEIGHT = 384;						//Printer width = 384 pixels
	public static final int IMG_WIDTH = (int) (IMG_HEIGHT * 1.5);	// 2/3 ratio
	public static final int FPS = 12;	
	public static final String RASPIVID = 
			"/opt/vc/bin/raspividyuv"+	//frame rate
			" -w "+IMG_WIDTH+" -h "+IMG_HEIGHT+			//image dimension
			" -ex night -fps 0 -ev +0.5 -t 0 -cfx 128:128 -o -";				//no timeout, monochom effect   -ev +0.5


	private int[] pixBuf = new int[IMG_HEIGHT * IMG_WIDTH ];
	private int[] pixList = new int[IMG_HEIGHT * IMG_WIDTH ];

	@Override
	public void run() {

		try {
			// launch video process
			Process p = Runtime.getRuntime().exec(RASPIVID);
			BufferedInputStream bis = new BufferedInputStream(p.getInputStream());

			System.out.println("start camera");

			int pixRead = bis.read();
			int pixCount = 1; // we just read the first pixel yet

			while (pixRead != -1) {
				// after skipping chroma data, end of a frame
				if (pixCount > (IMG_WIDTH * IMG_HEIGHT) + ((IMG_WIDTH * IMG_HEIGHT)/2) -1) {
					pixCount = 0;
				}
				pixRead = bis.read();
				// first are only luminance pixel info
				if (pixCount < (IMG_WIDTH * IMG_HEIGHT)) {
					pixBuf[pixCount] = pixRead;
				}
				pixCount++;
				// a luminance frame arrived
				if (pixCount == (IMG_WIDTH * IMG_HEIGHT)) {
					pixList = pixBuf.clone();
				}
			}

			System.out.println("end camera");
			p.destroy();
			bis.close();
			System.exit(0);

		} catch (IOException ieo) {
			ieo.printStackTrace();
		}
	}
	
	public int[] getAFrame() {
		return pixList.clone();
	}

}
