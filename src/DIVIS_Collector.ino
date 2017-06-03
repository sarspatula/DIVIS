//========================================
// DIVIS VERSION NOTES
//========================================

// v 0082
// add nighttime scheduling and deep sleep per cycle functionality
// fixed bug where string length was causing the API to be truncated on the thingspeak json. converted measurement values to integers before adding to the json string.
// added two particle.publish events to be able to view the currentHour and runtime

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

Adafruit_TCS34725 tcs = Adafruit_TCS34725(TCS34725_INTEGRATIONTIME_50MS, TCS34725_GAIN_1X);

// integration time options: 2? or 4?, 24, 50, 101, 154, 700
// gain options; 1, 4, 16, 60

//*****************Configuration Variables***********************
#define g_numDevice 4 // set number of TCS devices
#define g_sample 150 // set number of samples per cycle
#define sleepHour 21 // set 24 hour time to start night sleep
#define wakeHour 6 // set 24 hour time to wake
#define restTime 240 // set time to rest between cycles in seconds
//***************************************************************

uint16_t clear, red, green, blue;
String sensorReadings[g_numDevice];
float r_coef[g_numDevice];
float g_coef[g_numDevice];
float b_coef[g_numDevice];


//Variables for writing to Thingspeak
String TSjson;
String api_key = "T40C52G76D4ZB47D"; // Replace this string with a valid ThingSpeak Write API Key.
String field1 = "50ms/1x";
String field2 = "";  // i.e. field2 is null
String field3 = "";
String field4 = "";
String field5 = "";
String field6 = "";
String field7 = "";
String field8 = "";
String lat = "";
String lon = "";
String el = "";
String status = "";

//************************************************
//************************************************
//************************************************

void setup() {
    delay(100);
    Serial.begin(9600);
    Serial.println("Color View Test!");
    mux.begin();
    initializeMultiTCS(g_numDevice);
}

void loop() {

		sampleMulti(g_numDevice,g_sample,0,0); //take samples from all devices
		createTSjson(TSjson,g_numDevice); // creates the json string to pass to thingspeak
		publishToThingSpeak(); // publishes data to thingspeak
		delay(restTime * 1000); // simulating being in sleep mode
		nightTimeSchedule(); // turns off at nighttime
		//System.sleep(SLEEP_MODE_DEEP, restTime);
    }

//************************************************
//************************************************
//************************************************


void initializeTCS(int device) {

   mux.setChannel(device);
       if (tcs.begin()) {
        Serial.println("Found sensor");
    } else {
        Serial.println("No TCS34725 found ... check your connections");
        while (1); // halt!
    }

    mux.setChannel(device);
    tcs.setInterrupt(true);
		delay(60);
}

// initializes all devices
void initializeMultiTCS(int numDevice){

	for(int i=0;i<numDevice;i++){
		initializeTCS(i);
	}
}

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
	unsigned long start = millis();
	 float runtime = 0;
	 float r [numDevice];
	 float g [numDevice];
	 float b [numDevice];
	 float c [numDevice];
	 int r_int [numDevice];
	 int g_int [numDevice];
	 int b_int [numDevice];
	 int c_int [numDevice];

		for (int i=0; i<samples; i++){
				for (int i=0; i<numDevice; i++) {
					collect(i, printbool);
					r[i] = r[i] + red;
					b[i] = b[i] + blue;
					g[i] = g[i] + green;
					c[i] = c[i] + clear;
				}
			}

for (int i=0; i<numDevice; i++){
		r_int[i] = (int)round(r[i] / samples);
		b_int[i] = (int)round(b[i] / samples);
		g_int[i] = (int)round(g[i] / samples);
		c_int[i] = (int)round(c[i] / samples);

	}

//convert numbers to string and save in global variable to prepare to send to
	for (int i=0; i<numDevice; i++){
			sensorReadings[i] = String(r_int[i]) + ";" + String(g_int[i]) + ";" +  String(b_int[i]) + ";" +  String(c_int[i]);
		}



		runtime = (millis() - start)/1000;
		Particle.publish("runtime",runtime,60,PRIVATE);

if (multiprintbool == 1) {
		for (int i=0; i<numDevice; i++){
			Serial.print("device:");
			Serial.print(i);
			Serial.print("::");
			Serial.print(r[i]);
			Serial.print("|");
			Serial.print(b[i]);
			Serial.print("|");
			Serial.print(g[i]);
			Serial.print("|");
			Serial.print(c[i]);
			Serial.print("   ||   ");
			Serial.println(runtime);
		}
	}
}

void publishToThingSpeak(){
	Particle.publish("divisWrite_", TSjson, 60, PRIVATE);
	Serial.print("publishing to Thingspeak...");
	delay(20000); //ensures minimum time between writes to thingspeak.
	//Make sure to delete this delay once the device is put into deep sleep between readings
}


void nightTimeSchedule(){
	int currentHour = Time.hour();
	Particle.publish("hour", currentHour, 60, PRIVATE);
	int sleepTime = (24 - sleepHour + wakeHour + 1) * 60 * 60; // converts sleep and wake hours to a sleep time in seconds
	if (currentHour >= sleepHour){
		System.sleep(SLEEP_MODE_DEEP, sleepTime);
	}
}


//****************************WebHook Notes and Functions**************************************************
//This code was modified from the following sources
//Source1: https://www.hackster.io/15223/thingspeak-particle-photon-using-webhooks-dbd96c
//Source2: https://github.com/mawrob/thingspeak-particle-webhookwrite/blob/master/source.ino


// Demo program that uses a Particle Webhook to POST data to ThingSpeak.
// For details on the ThingSpeak Channel Feed api see: https://www.mathworks.com/help/thingspeak/update-channel-feed.html

void createTSjson(String &dest, int numDevice) {


		dest = "{";


		if(field1.length()>0){
				dest = dest + "\"1\":\""+ field1 +"\",";
		}

		for (int i=0; i<numDevice; i++){
			int field=i+2;
			if(sensorReadings[i].length()>0){
					dest = dest + "\""+ field +"\":\""+ sensorReadings[i] +"\",";
			}
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

}
