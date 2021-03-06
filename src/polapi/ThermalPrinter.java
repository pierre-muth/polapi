package polapi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;

public class ThermalPrinter {
	static final byte ESC = 27;
	static final byte _7 = 55;
	static final byte DC2 = 18;
	static final byte DIEZE = 35;
	private final Serial serial;
	private PrintImageThread pit;
	private PrinterConfig printerConfig;

	public ThermalPrinter() {
		serial = SerialFactory.createInstance();
		
		String comPort;
		if (Launcher.serial_device_name.contains(Launcher.DEFAULTKEY)) {
			comPort = Serial.DEFAULT_COM_PORT;
		} else {
			comPort = Launcher.serial_device_name;
		}
		
		System.out.println("Opening "+ comPort+" at "+Launcher.serialSpeed+" bauds.");
		
		serial.open(comPort, Launcher.serialSpeed);
		
	}

	public void configPrinterWithDefault() {
		printerConfig = new PrinterConfig();
		configPrinter(printerConfig);
	}

	public void configPrinter(PrinterConfig config) {
		printerConfig = config;
		PrinterConfigThread pct = new PrinterConfigThread(printerConfig);
		pct.start();
	}

	public void printImage(byte[] img, int width, int length) {
		if (isPrinting()) return;
		pit = new PrintImageThread(img, width, length);
		pit.start();
	}

	public boolean isPrinting() {
		return pit != null && pit.isAlive();
	}

	public void motorStep() {
		if (isPrinting()) {
			pit.addOnePrintedLine();
		}
	}

	private class PrinterConfigThread extends Thread {
		private PrinterConfig config;
		private byte[] configSequence;
		private byte[] smallFontSelectSequence;

		public PrinterConfigThread(PrinterConfig config) {
			this.config = config;

			configSequence = new byte[] {
					ESC, _7, 
					config.heatingMaxDot, config.heatTime, config.heatInterval, 
					DC2, DIEZE,
					(byte) ((config.printBreakTime << 5) | config.printDensity),
					0x1D, 0x61, (byte) 0xFF // auto report ?
			};
			
			smallFontSelectSequence = new byte[] {
					0x1B,  0x74, 0x01
			};
			
		}

		@Override
		public void run () {

			System.out.println("Printer: start config");
			
			try {
				sleep(1000);		// some time serial not yet configured
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			serial.write((char) 0x0A);
			
			try {
				sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			for (int i = 0; i < configSequence.length; i++) {
				serial.write(configSequence[i]);
			}
			
			try {
				sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if ( Launcher.smallFont ) {
				for (int i = 0; i < smallFontSelectSequence.length; i++) {
					serial.write(smallFontSelectSequence[i]);
				}
				try {
					sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			serial.write((char) 0x0A);
			serial.write(Launcher.welcome);
			serial.write((char) 0x0A);
			serial.write((char) 0x0A);
			serial.write((char) 0x0A);
			serial.write((char) 0x0A);
			System.out.println("Printer: end config");
		}
	}

	private class PrintImageThread extends Thread {
		private byte[] imageBytes;
		private int width, length;
		private AtomicBoolean hold = new AtomicBoolean();
		private AtomicInteger linePrinted = new AtomicInteger(0);

		public PrintImageThread(byte[] img, int width, int length) {
			this.imageBytes = img;
			this.length = length;
			this.width = width;
			hold.set(false);
		}

		public void addOnePrintedLine() {
			linePrinted.incrementAndGet();
		}

		@Override
		public void run () {
			byte[] printLineCommand ;

			System.out.println("Printer: start print bitmap");

			try {
				sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			for (int lineSent = 0; lineSent < length; lineSent++) {

				if (lineSent%255 == 0) {
					int remainingLines = length-lineSent;
					if (remainingLines > 255) remainingLines = 255;
					printLineCommand = new byte[] {0x12, 0x2A, (byte) remainingLines, 48};

					for (int j = 0; j < printLineCommand.length; j++) {
						serial.write(printLineCommand[j]);
					}
				}

				for (int j = 0; j < 48; j++) {
					byte imageByte = imageBytes[(lineSent*48)+j];
					serial.write(imageByte);
				}

				if (Launcher.debugOutput)
					System.out.println("Line sent: "+lineSent+", linePrinted: "+linePrinted.get());

				if (lineSent > 50) {
					
					int i=0;
					while ( (lineSent - linePrinted.get()) > Launcher.printerBuffer && i < 20) {
						try { 
							sleep(1);	
							i++;
//							System.out.print(".");
						} catch (InterruptedException e) {	}
					}
					
				}

			}

			try {
				sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
			String date = sdf.format(new Date());
			
			if (Launcher.header != " ") {
				serial.write(Launcher.header.replace(Launcher.DATEKEY, date)+"\n");
			} 
			
			serial.write((char) 0x0A);
			serial.write((char) 0x0A);

			System.out.println("Printer: end bitmap\n");
		}


	}

}
