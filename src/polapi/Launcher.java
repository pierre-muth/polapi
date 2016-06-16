package polapi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.text.WordUtils;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.serial.Serial;

public class Launcher {
	private ThermalPrinter printer;
	private GpioController gpio;
	private GpioPinDigitalInput printButton;
	private GpioPinDigitalInput printerMotor;
	private Camera picam;
	private MonochromImage monochromImage;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
	public static final String HEADERPATH = "/home/pi/polapi/header.txt";
	private static final String CONFPATH = "/home/pi/polapi/config.txt";
	private static final String HEADERKEY = "HEADER:";
	private static final String WELCOMEKEY = "WELCOME:";
	private static final String SERIALSPEEDKEY = "SERIALSPEED:";
	private static final String BUFFERKEY = "BUFFER:";
	private static final String DELAYKEY = "DELAY:";
	private static final String HEATINGMAXDOTKEY = "HEATINGMAXDOT:";
	private static final String HEATTIMEKEY = "HEATTIME:";
	private static final String HEATINTERVALKEY = "HEATINTERVAL:";
	private static final String DEBUGKEY = "DEBUG:";
	private static final String CONTINUOUSDELAYKEY = "CONTINUOUSDELAY:";
	private static final String CAMERAPARAMKEY = "RASPIVID:";
	private static final String BUTTONPINKEY = "BUTTONPIN:";
	private static final String MOTORPINKEY = "MOTORPIN:";
	private static final String SERIALDEVICEKEY = "SERIALPORT:";
	private static final String SMALLFONTKEY = "SMALLFONT:";
	private static final String LINECHARKEY = "LINECHAR:";
	public static final String DATEKEY = "#date";
	public static final String DEFAULTKEY = "#default";
	public static String header = "";
	public static String welcome = "";
	public static int serialSpeed = 0;
	public static int printerBuffer = 80;
	public static int dataDelay = 2;
	public static int heatingMaxDot = 11;
	public static int heatTime = 70;
	public static int heatInterval = 250;
	public static boolean debugOutput = false;
	public static int continuousDelay = 15;
	public static String raspbivid_param = "-ex night -fps 0 -ev +0.5 -cfx 128:128"; //  ev +0.5, monochom effect, ...
	public static String button_pin_name = "GPIO 4";
	public static String motor_pin_name = "GPIO 3";
	public static String serial_device_name = Serial.DEFAULT_COM_PORT;
	public static boolean smallFont = false;
	public static int lineChar = 30;
	public static boolean headerFromFile = false;
	
	public Launcher () {
		//get some config
		String line;
		try (BufferedReader br = new BufferedReader( new FileReader(CONFPATH))){
			
			line = br.readLine();
			if (line != null && line.contains(WELCOMEKEY)) {
				welcome = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(HEADERKEY)) {
				header = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(SERIALSPEEDKEY)) {
				serialSpeed = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(BUFFERKEY)) {
				printerBuffer = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(DELAYKEY)) {
				dataDelay = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(HEATINGMAXDOTKEY)) {
				heatingMaxDot = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(HEATTIMEKEY)) {
				heatTime = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(HEATINTERVALKEY)) {
				heatInterval = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(CONTINUOUSDELAYKEY)) {
				continuousDelay = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(CAMERAPARAMKEY)) {
				raspbivid_param = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(BUTTONPINKEY)) {
				button_pin_name = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(MOTORPINKEY)) {
				motor_pin_name = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(SERIALDEVICEKEY)) {
				serial_device_name = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(SMALLFONTKEY)) {
				smallFont = Integer.parseInt( br.readLine() ) == 1;
			}
			line = br.readLine();
			if (line != null && line.contains(LINECHARKEY)) {
				lineChar = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(DEBUGKEY)) {
				debugOutput = Integer.parseInt( br.readLine() ) == 1; 
			}
			
		} catch (IOException e) {
			System.out.println("Error in config.txt");
		};
		
		//init header text
		try (BufferedReader br = new BufferedReader( new FileReader(HEADERPATH))){

			line = br.readLine();
			String singleLine = new String();
			
			while (line != null) {
				singleLine = singleLine +" "+line;
				line = br.readLine();
			}
			 
			header = WordUtils.wrap(singleLine, lineChar, ""+(char) 0x0A, true);
			headerFromFile = true;
			if (debugOutput) {
				System.out.println("header.txt found : \n"+header);
			}
			
		} catch (IOException e) {
			System.out.println(HEADERPATH+" not found, using config.txt for header text");
		};
		
		header.concat(" ");
		welcome.concat(" ");
		
		//init button and Printer motor input
		gpio = GpioFactory.getInstance();
		
		printButton = gpio.provisionDigitalInputPin(RaspiPin.getPinByName(button_pin_name), PinPullResistance.PULL_DOWN);
		printButton.addListener(new ButtonListener());
		
		printerMotor = gpio.provisionDigitalInputPin(RaspiPin.getPinByName(motor_pin_name), PinPullResistance.OFF);
		printerMotor.addListener(new MotorListener());

		PrinterConfig pc = new PrinterConfig();
		pc.heatingMaxDot = (byte) heatingMaxDot;
		pc.heatTime = (byte) heatTime;
		pc.heatInterval = (byte) heatInterval;
		pc.printDensity = 0;
		pc.printBreakTime = 0;

		printer = new ThermalPrinter();
		printer.configPrinter(pc);
		
		picam = new Camera();
		new Thread(picam).start();
		
		monochromImage = new MonochromImage();
	}
	
	private void print(){
		if (printer != null && !printer.isPrinting() && monochromImage != null ) { // reject if yet printing
			monochromImage.setPixels(picam.getAFrame());
			printer.printImage(monochromImage.getDitheredMonochrom(), Camera.IMG_HEIGHT, Camera.IMG_WIDTH);
			monochromImage.writeToFile();
		}
	}

	public static void main(String[] args) {
		System.out.println("PolaPi starting...");
		new Launcher();
	}

	private class PrintManager extends Thread {
		
		@Override
		public void run () {

			while (printButton.isHigh()) {
				
				print();
				
				try {
					sleep(continuousDelay *1000);		
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
			
		}
	}

	class ButtonListener implements GpioPinListenerDigital {
		long lastPrintTime;
		
		public ButtonListener() {
			lastPrintTime = System.currentTimeMillis();
		}
		
		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			if (event.getState().isLow()) return;  // reject the return to default state event
			System.out.println("Print button pressed !");
			
			long currentTime = System.currentTimeMillis();
			if ( currentTime - lastPrintTime < 500 ) return; // reject if less than 500 ms
			
			lastPrintTime = currentTime;
			PrintManager pm = new PrintManager();
			pm.start();
		}
	}
	
	class MotorListener implements GpioPinListenerDigital {
		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			//got a new printed line
			printer.motorStep();
		}
	}

}
