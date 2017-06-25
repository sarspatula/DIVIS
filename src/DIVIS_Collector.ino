//========================================
// DIVIS VERSION NOTES
//========================================

// v 010
// Reworked the process for defining the variables that are used in the thingspeak json. 
// Modified the code for use with two sensors, the MVP version of the DIVIS sensor
// removed functions that are obsolete

//========================================
// Test Status: Not tested
//========================================


//========================================
// Resources
//========================================
//Guide to creating webhooks to thingspeak:
//https://docs.particle.io/guide/tools-and-features/webhooks/
//https://www.hackster.io/15223/thingspeak-particle-photon-using-webhooks-dbd96c

//Thingspeak channel ID: 271987
//thingspeak API key: T40C52G76D4ZB47D
//========================================

// This #include statement was automatically added by the Particle IDE.
#include <Adafruit_TCS34725.h>


//***************************************************************
//**************************TCA9548A Library .h *****************
//source: // https://github.com/rickkas7/TCA9548A-RK

#ifndef __TCA9548A_RK_H
#define __TCA9548A_RK_H


class TCA9548A {
public:
	/**
	 * You typically create one of these objects on the stack.
	 *
	 * The first argument is typically Wire (pins D0 and D1). On the Electron it can also be Wire1 (pins C4 and C5)
	 *
	 * The addr argument is the address 0-7 based on the setting of A0, A1 and A2.
	 */
	TCA9548A(TwoWire &wire, int addr = 0);
	virtual ~TCA9548A();

	/**
	 * Typically called during setup() to start the Wire interface.
	 */
	void begin();

	/**
	 * Sets the I2C channel 0-7 to use
	 */
	void setChannel(uint8_t channel);

	/**
	 * Deselect all channels, so none is selected
	 */
	void setNoChannel();


	bool writeControl(uint8_t value);

	static const uint8_t DEVICE_ADDR = 0x70; // 1110 in the fixed bits + 3 adjustable bits

protected:
	TwoWire &wire;
	int addr; // This is just 0-7, the (0x70 of the 7-bit address is ORed in later)

};

#endif /* __TCA9548A_RK_H */

//***************************************************************
//**************************TCA9548A Library .cpp *****************
//source: // https://github.com/rickkas7/TCA9548A-RK


#include "Particle.h"



TCA9548A::TCA9548A(TwoWire &wire, int addr) :
	wire(wire), addr(addr) {
}

TCA9548A::~TCA9548A() {
}

void TCA9548A::begin() {
	wire.begin();
}

void TCA9548A::setChannel(uint8_t channel) {
	writeControl(1 << channel);
}

void TCA9548A::setNoChannel() {
	writeControl(0);
}

bool TCA9548A::writeControl(uint8_t value) {
	wire.beginTransmission(addr | DEVICE_ADDR);
	wire.write(value);

	int stat = wire.endTransmission(true);

	// Serial.printlnf("writeControl addr=0x%x value=0x%x stat=%d", addr | DEVICE_ADDR, value, stat);

	return (stat == 0);
}

//*******************************************************************
//*********************Start Primary Code****************************

// This #include statement was automatically added by the Spark IDE.
#include "Adafruit_TCS34725/Adafruit_TCS34725.h"
#include <Wire.h>
#include <math.h>
//#include "src/statistic.h"
// https://github.com/rickkas7/TCA9548A-RK

TCA9548A mux(Wire, 0);

boolean commonAnode = false;
char szInfo[128];

Adafruit_TCS34725 tcs = Adafruit_TCS34725(TCS34725_INTEGRATIONTIME_24MS, TCS34725_GAIN_1X);

// integration time options: 2? or 4?, 24, 50, 101, 154, 700
// gain options; 1, 4, 16, 60

//*****************Configuration Variables***********************
#define g_numDevice 2 // set number of TCS devices. Max = 7.
#define g_sample 150 // set number of samples per cycle
#define sleepHour 21 // set 24 hour time to start night sleep
#define wakeHour 6 // set 24 hour time to wake
#define restTime 60 // set time to rest between cycles in seconds
#define sat_max 10240 // set the maximum value returned by the sensor for saturation calculations
String DIVIS_ID = "001";
//*********************Sensor Corretion Coefficients*************
// must adjust depeding on number of sensors
float r_coef[] = {1.0,0.97096181};
float g_coef[] = {1.0,0.989730857};
float b_coef[] = {1.0,1.001603333};
float c_coef[] = {1.0,0.984265143};


//***************************************************************

uint16_t clear, red, green, blue;
String sensor_raw[g_numDevice];
String sensor_diff[g_numDevice];
String TSjson;
String api_key = "CCDMJAB229VPBB5O"; // api key for red and green data

//variables to count saturation for each color reading

//Variables for writing to raw channel

String field1 = "1"; // DIVIS ID
String field2 = "";  // Upper sensor R,B,G,C
String field3 = "";	 // Lower sensor R,B,G,C
String field4 = "";	 // Saturation ratio R,G,B,C
String field5 = "";  // r difference ratio
String field6 = "";	 // g difference ratio
String field7 = "";	 // b difference ratio
String field8 = "";  // c difference ratio
String lat = "";
String lon = "";
String el = "";
String status = "";


//Variables for writing to raw channel

//************************************************
//************* M A I N   L O O P ****************
//************************************************

void setup() {
    delay(100);
    Serial.begin(9600);
    Serial.println("Color View Test!");
    mux.begin();
    initializeMultiTCS(g_numDevice);
}

void loop() {

		nightTime(); // turns off at nighttime
		sampleMulti(g_numDevice,g_sample,1,1); //take samples from all devices saves the to an array which is concatenated into a string
		createTSjson(TSjson); // creates the json string to pass to thingspeak
		publishToThingSpeak(TSjson); // publishes raw data to thingspeak
		delay(restTime * 1000); // simulating being in sleep mode
		//System.sleep(SLEEP_MODE_DEEP, restTime);
    }

//************************************************
//************************************************
//************************************************

// INITIALIZATION
void initializeTCS(int device) {

   mux.setChannel(device);
       if (tcs.begin()) {
        Serial.print("Found sensor:");
				Serial.println(device);
    } else {
        Serial.println("No TCS34725 found ... check your connections");
				Particle.publish("Initialization error",NULL,60,PRIVATE);
        while (1); // halt!
    }

    mux.setChannel(device);
    tcs.setInterrupt(true);
		delay(100);
}

// initializes all devices
void initializeMultiTCS(int numDevice){

	for(int i=0;i<numDevice;i++){
		initializeTCS(i);
	}
	Particle.publish("All TCS34725 Found!",NULL,60,PRIVATE);
}
//======================================================

//DATA COLLECTION
// collects a single data point from one TCS34725 sensor and saves to global red/green/blue/clear variables
void collect(int device, bool printbool){
	mux.setChannel(device);
	tcs.getRawData(&red, &green, &blue, &clear);
	delay(50);  // must be set at or above the "integration time" as set when the sensor is declared.
	// integration time is essentially the exposure time of the sensor.
	//The higher the number, the higher the raw output number will be.

if(printbool==1){
	Serial.print("device:");
	Serial.print(device);
	Serial.print("::");
	Serial.print(red);
	Serial.print("|");
	Serial.print(blue);
	Serial.print("|");
	Serial.print(green);
	Serial.print("|");
	Serial.print(clear);
	Serial.println("   ||   ");
	}
}

//takes a given number of samples from one device and averages them together
void sample(int device, int samples, bool printbool) {
unsigned long start = millis();
float runtime = 0;
 float c, r, g, b;
	for (int i=0; i<samples; i++){
		collect(device,printbool);
		r = r + red;
		b = b + blue;
		g = g + green;
		c = c + clear;
	}
	r = r / samples;
	b = b / samples;
	g = g / samples;
	c = c / samples;

	runtime = (millis() - start)/1000;

if(printbool==1){
	Serial.print("deviceAvg:");
	Serial.print(device);
	Serial.print("|");
	Serial.print(r);
	Serial.print("|");
	Serial.print(g);
	Serial.print("|");
	Serial.print(b);
	Serial.print("|");
	Serial.print(c);
	Serial.print("|||");
	Serial.println(runtime);
 }
}

// samples from multile devices, one sample each in turn, in a loop
// creates aggregate data in an array then overrights that data with the mean
void sampleMulti(int numDevice, int samples, bool printbool, bool multiprintbool){
	 Particle.publish("Start Data Collection",NULL,60,PRIVATE);

	 // delcare variables for storing sample data and making calucations
	 unsigned long start = millis();
	 float runtime = 0;
	 //variables for initial readings
	 float r [numDevice];
	 float g [numDevice];
	 float b [numDevice];
	 float c [numDevice];
	 //variables for raw readings
	 int r_int [numDevice];
	 int g_int [numDevice];
	 int b_int [numDevice];
	 int c_int [numDevice];
	 //variables for calculated differences
	 float r_diff [numDevice];
	 float g_diff [numDevice];
	 float b_diff [numDevice];
	 float c_diff [numDevice];
	 //variables for saturation
	 int r_sat_counter = 0;
	 int g_sat_counter = 0;
	 int b_sat_counter = 0;
	 int c_sat_counter = 0;

	 int r_sat_ratio = 0;
	 int g_sat_ratio = 0;
	 int b_sat_ratio = 0;
	 int c_sat_ratio = 0;



// Take N samples from each device and create a running total. each device is polled in tern, in order to make observations between devices as close together in time as possible
		for (int i=0; i<samples; i++){
				for (int i=0; i<numDevice; i++) {
					collect(i, printbool);
					r[i] = r[i] + (red * r_coef[i]);
					b[i] = b[i] + (blue * b_coef[i]);
					g[i] = g[i] + (green * g_coef[i]);
					c[i] = c[i] + (clear * c_coef[i]);

					// count readings with saturation on the upper sensor
					if ((i==0) && (red > (sat_max * 0.7))){
						r_sat_counter = r_sat_counter + 1;
					}
					if ((i==0) && (green > (sat_max * 0.7))){
						g_sat_counter = g_sat_counter + 1;
					}
					if ((i==0) && (blue > (sat_max * 0.7))){
						b_sat_counter = b_sat_counter + 1;
					}
					if ((i==0) && (clear > (sat_max * 0.7))){
						c_sat_counter = c_sat_counter + 1;
					}

				}
			}


for(int i=0; i<(numDevice-1); i++){
	r_diff[i] = (r[i] - r[i+1])/r[i];
	g_diff[i] = (g[i] - g[i+1])/g[i];
	b_diff[i] = (b[i] - b[i+1])/b[i];
	c_diff[i] = (c[i] - c[i+1])/c[i];
}


// Calculate final values for raw data
for (int i=0; i<numDevice; i++){
		r_int[i] = (int)round(r[i] / samples);
		b_int[i] = (int)round(b[i] / samples);
		g_int[i] = (int)round(g[i] / samples);
		c_int[i] = (int)round(c[i] / samples);
	}

// saturation of upper sensor
		r_sat_ratio = (r_sat_counter/samples)*100;
		g_sat_ratio = (g_sat_counter/samples)*100;
		b_sat_ratio = (b_sat_counter/samples)*100;
		c_sat_ratio = (c_sat_counter/samples)*100;

// Map readings to sensor data

		field1 = DIVIS_ID; // DIVIS ID
		field2 = String(r_int[0]) + "," + String(g_int[0]) + "," + String(b_int[0]) + "," + String(c_int[0]);  // Upper sensor R,B,G,C
		field3 = String(r_int[1]) + "," + String(g_int[1]) + "," + String(b_int[1]) + "," + String(c_int[1]);	 // Lower sensor R,B,G,C
		field4 = String(r_sat_ratio) + "," + String(g_sat_ratio) + "," + String(b_sat_ratio) + "," + String(c_sat_ratio);  // Saturation ratio R,G,B,C
		field5 = String(r_diff[0]);	 // r difference ratio
		field6 = String(g_diff[0]);  // g difference ratio
		field7 = String(b_diff[0]);	 // b difference ratio
		field8 = String(c_diff[0]);	 // c difference ratio
		lat = "";
		lon = "";
		el = "";
		status = "";


		runtime = (millis() - start)/1000;
		Particle.publish("Runtime",String(runtime),60,PRIVATE);
		Serial.println(runtime);

if (multiprintbool == 1) {
		for (int i=0; i<numDevice; i++){
			Serial.print("device:");
			Serial.print(i);
			Serial.print("::");
			Serial.print(r_int[i]);
			Serial.print("|");
			Serial.print(b_int[i]);
			Serial.print("|");
			Serial.print(g_int[i]);
			Serial.print("|");
			Serial.println(c_int[i]);
		}
	}
}
//======================================================


//PUBLISH

//publish to thingspeak
void publishToThingSpeak(String json){
	Particle.publish("divisWrite_", json, 60, PRIVATE);
	Particle.publish("divisString",String(json.length()),60,PRIVATE);
	Serial.println("publishing to Thingspeak...");
  delay(15000); //ensures minimum time between writes to thingspeak.
	//Make sure to delete this delay once the device is put into deep sleep between readings
}

//Create Thingspeak Json
//This code was modified from the following sources
//Source1: https://www.hackster.io/15223/thingspeak-particle-photon-using-webhooks-dbd96c
//Source2: https://github.com/mawrob/thingspeak-particle-webhookwrite/blob/master/source.ino


void createTSjson(String &dest) {


		dest = "{";


		if(field1.length()>0){
					dest = dest + "\"1\":\""+ field1 +"\",";
		}

		if(field2.length()>0){
					dest = dest + "\"2\":\""+ field2 +"\",";
		}

		if(field3.length()>0){
					dest = dest + "\"3\":\""+ field3 +"\",";
		}

		if(field4.length()>0){
					dest = dest + "\"4\":\""+ field4 +"\",";
		}

		if(field5.length()>0){
					dest = dest + "\"5\":\""+ field5 +"\",";
		}

		if(field6.length()>0){
					dest = dest + "\"6\":\""+ field6 +"\",";
		}

		if(field7.length()>0){
					dest = dest + "\"7\":\""+ field7 +"\",";
		}

		if(field8.length()>0){
					dest = dest + "\"8\":\""+ field8 +"\",";
		}

    if(lat.length()>0){
        dest = dest + "\"a\":\""+ lat +"\",";
    }

    if(lon.length()>0){
        dest = dest + "\"o\":\""+ lon +"\",";
    }

    if(el.length()>0){
        dest = dest + "\"e\":\""+ el +"\",";
    }

    if(status.length()>0){
        dest = dest + "\"s\":\""+ status +"\",";
    }

    dest = dest + "\"k\":\"" + api_key + "\"}";

		Serial.println(TSjson);

}
//=========================================================

//SLEEP CONTROLL
void nightTime(){
// checks for day light savings time and sets the time zone offset from UTC
	if(Time.isDST() == 1){

		Time.zone(-8); // sets timezone to PST w/out daylight savings
	}
	else{
		Time.zone(-7); // sets timezone to PST w/ daylight savings
	}
	int currentHour = Time.hour();
	Particle.publish("hour_", "CurrentHour: " + String(currentHour) + " / " + "IsDST: " + String(Time.isDST()), 60, PRIVATE);

	if (currentHour >= sleepHour){
		int sleepHours = (24 - sleepHour + wakeHour); // calculates sleep time from sleephour and wake hour
		int sleepSeconds = sleepHours * 60 * 60; // converts sleep and wake hours to a sleep time in seconds
		Particle.publish("going to sleep",String(sleepSeconds),60,PRIVATE);
		delay(5000);
		System.sleep(SLEEP_MODE_DEEP, sleepSeconds);
	}
}
//===============================================================
