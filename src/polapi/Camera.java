package polapi;

import java.io.BufferedInputStream;
import java.io.IOException;

public class Camera implements Runnable {

	public static final int S_HEIGHT = 384;						//Printer width = 384 pixels
	public static final int S_WIDTH = (int) (S_HEIGHT * 1.5);	// 2/3 ratio
	public static final int FPS = 12;	
	public static final String RASPIVID = 
			"/opt/vc/bin/raspividyuv -fps "+FPS+	//frame rate
			" -w "+S_WIDTH+" -h "+S_HEIGHT+			//image dimension
			" -ev +1 -t 0 -cfx 128:128 -o -";				//no timeout, monochom effect


	private int[] pixBuf = new int[S_HEIGHT * S_WIDTH ];
	private int[] pixList = new int[S_HEIGHT * S_WIDTH ];

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
				if (pixCount > (S_WIDTH * S_HEIGHT) + ((S_WIDTH * S_HEIGHT)/2) -1) {
					pixCount = 0;
				}
				pixRead = bis.read();
				// first are only luminance pixel info
				if (pixCount < (S_WIDTH * S_HEIGHT)) {
					pixBuf[pixCount] = pixRead;
				}
				pixCount++;
				// a luminance frame arrived
				if (pixCount == (S_WIDTH * S_HEIGHT)) {
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

	public byte[] getAFrame() {

		int pixelWithError, pixelDithered, error;
		boolean notLeft, notRight, notBottom;
		int[] pixDest = new int[pixList.length];

		//dithering
		for (int pixCount = 0; pixCount < pixList.length; pixCount++) {

			notLeft = pixCount%S_WIDTH!=0;
			notBottom = pixCount < S_WIDTH*(S_HEIGHT-1);
			notRight = (pixCount+1)%S_WIDTH!=0;

			pixelWithError = pixDest[pixCount] + pixList[pixCount];

			if (pixelWithError < 128) pixelDithered = 0;
			else pixelDithered = 255;

			pixDest[pixCount] = pixelDithered;

			error = pixelWithError - pixelDithered;

			if (notRight) pixDest[pixCount+1] += 7*error/16;
			if (notLeft && notBottom) pixDest[pixCount+(S_WIDTH-1)] += 3*error/16;
			if (notBottom) pixDest[pixCount+(S_WIDTH)] += 5*error/16;
			if (notRight && notBottom) pixDest[pixCount+(S_WIDTH+1)] += 3*error/16;
		}

		//generate image with pixel bit in bytes
		byte[] pixBytes = new byte[(S_HEIGHT/8) * S_WIDTH ];
		
		int mask = 0x01;
		int x, y;
		for (int i = 0; i < pixBytes.length; i++) {
			for (int j = 0; j < 8; j++) {
				mask = 0b10000000 >>> j;
				x = i / (S_HEIGHT/8);
				y = (S_HEIGHT-1) - ((i%(S_HEIGHT/8)*8 ) +j)  ;
				if ( pixDest[x+(y*S_WIDTH)] == 0 ) {
					pixBytes[i] = (byte) (pixBytes[i] | mask);
				}
			}
		}

		return pixBytes;
	}


}
