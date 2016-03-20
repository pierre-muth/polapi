package polapi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class LauncherFX extends Application{
	ThermalPrinter printer;
	String fileToPrint;
	GpioController gpio;
	GpioPinDigitalInput printButton;
	GpioPinDigitalInput printerMotor;
	Camera picam;
	MonochromImage monochromImage;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
	
	private static final String CONFPATH = "/home/pi/polapi/config.txt";
	private static final String HEADERKEY = "HEADER:";
	private static final String WELCOMEKEY = "WELCOME:";
	private static final String SERIALKEY = "SERIAL:";
	public static final String DATEKEY = "#date";
	public static String header = "";
	public static String welcome = "";
	public static int serialSpeed = 0;

	public LauncherFX () {
		//get some config
		String line;
		try (BufferedReader br = new BufferedReader( new FileReader(CONFPATH))){
			line = br.readLine();
			if (line.contains(WELCOMEKEY)) {
				welcome = br.readLine();
			}
			line = br.readLine();
			if (line.contains(HEADERKEY)) {
				header = br.readLine();
			}
			line = br.readLine();
			if (line.contains(SERIALKEY)) {
				serialSpeed = Integer.parseInt( br.readLine() );
			}
		} catch (IOException e) {
			System.out.println("Error in config.txt");
		};
		header.concat(" ");
		welcome.concat(" ");
		
		//init button and Printer motor input
		gpio = GpioFactory.getInstance();
		
		printButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN);
		printButton.addListener(new ButtonListener());
		
		printerMotor = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.OFF);
		printerMotor.addListener(new MotorListener());

		PrinterConfig pc = new PrinterConfig();
		pc.heatingMaxDot = 11;
		pc.heatTime = (byte) 70;
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
		System.out.println("PolaPi starting...");
		launch();
		new LauncherFX();
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
			if (printer != null && !printer.isPrinting() && monochromImage != null ) { // reject if yet printing
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

	@Override
	public void start(Stage primaryStage) throws Exception {
		VBox vbox = new VBox();
		vbox.setStyle("-fx-background-color: black");
		Scene scene = new Scene(vbox, 320, 240);
		primaryStage.setScene(scene);
		primaryStage.show();	
	}

}
