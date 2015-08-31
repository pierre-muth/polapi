package polapi;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class Launcher {
	ThermalPrinter printer;
	String fileToPrint;
	GpioController gpio;
	GpioPinDigitalInput printButton;
	GpioPinDigitalInput printerMotor;
	Camera picam;
	MonochromImage monochromImage;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	public Launcher () {
		//init button and Printer motor input
		gpio = GpioFactory.getInstance();
		
		printButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN);
		printButton.addListener(new ButtonListener());
		
		printerMotor = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.OFF);
		printerMotor.addListener(new MotorListener());

		PrinterConfig pc = new PrinterConfig();
		pc.heatingMaxDot = 11;
		pc.heatTime = (byte) 60;
		pc.heatInterval = (byte) 250;
		pc.printDensity = 0;
		pc.printBreakTime = 0;

		printer = new ThermalPrinter();
		printer.configPrinter(pc);
		
		picam = new Camera();
		new Thread(picam).start();
		
		monochromImage = new MonochromImage();
	}

	public static void main(String[] args) {
		System.out.println("Pi-Print Test");
		new Launcher();
	}


	class ButtonListener implements GpioPinListenerDigital {
		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			if (event.getState().isLow()) return;
			System.out.println("Print button pressed !");
			
			if (printer != null && !printer.isPrinting() && monochromImage != null ) {
				monochromImage.setPixels(picam.getAFrame());
				printer.printImage(monochromImage.getDitheredMonochrom(), Camera.IMG_HEIGHT, Camera.IMG_WIDTH);
				monochromImage.writeToFile();
			}
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
