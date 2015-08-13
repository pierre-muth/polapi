package polapi;

public class PrinterConfig {
	
	public byte heatingMaxDot;			// Heating dots (20=balance of darkness vs no jams)
	public byte heatTime;		// Library default = 255 (max)
	public byte heatInterval;  // Heat interval (500 uS = slower, but darker) Unit (10us), Default: 2 (20us)
	public byte printDensity;			// 50% + 5% * n(D4-D0) printing density
	public byte printBreakTime;		// D7..D5 of n is used to set the printing break time.  Break time
	  										// is n(D7-D5)*250us.
	public PrinterConfig() {
		//default values
		heatingMaxDot = 20;			// Heating dots (20=balance of darkness vs no jams)
		heatTime = (byte) 200;		// Library default = 255 (max)
		heatInterval = (byte) 250;  // Heat interval (500 uS = slower, but darker) Unit (10us), Default: 2 (20us)
		printDensity = 14;			// 50% + 5% * n(D4-D0) printing density
		printBreakTime  = 4;		// D7..D5 of n is used to set the printing break time.  Break time
		  										// is n(D7-D5)*250us.
		
	}
	
}
