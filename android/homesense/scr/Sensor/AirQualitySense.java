package ucxpresso.net.homesense.Sensor;

/**
 * Copyright 2017, Embeda Technology Inc.
 * All rights reserved.
 * <p>
 * Contact us: service+java@embeda.com.tw
 * Created by jason on 2017/1/8.
 * License: CC By NC.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import android.util.Log;

import net.ucxpresso.java.duino.SwiftDuino;
import net.ucxpresso.java.duino.libs.I2Cdev;
import net.ucxpresso.java.duino.libs.Serial;
import net.ucxpresso.java.utilies.Timeout;

import ucxpresso.net.homesense.MQTT.JavaMqttClient;

public class AirQualitySense extends SenseBase {
    private static final String TAG = "AirQualitySense";

    private final int   LED3_PIN = 20;      // nano51822-udk LED3
    private final int   LED4_PIN = 21;      // nano51822-udk LED4

    // PMS5003 Sensor
    private PMS5003 G5;
    private final int G5_SET_PIN = 17;
    private final int G5_RXD_PIN = 16;
    private final int G5_TXD_PIN = 15;
    private final int G5_RST_PIN = 14;

    private final AverageFilter mFilter1 = new AverageFilter(10);
    private final AverageFilter mFilter2 = new AverageFilter(10);
    private final AverageFilter mFilter3 = new AverageFilter(10);

    // Temperature & Humidity
    private final int   SDA_PIN = 29;
    private final int   SCL_PIN = 28;
    private DHT12       mDHT12;

    // MOTT Server
    private final String         mServer = "tcp://mqtt.thingspeak.com:1883"; 
    private final String         mChannel = "YOUR-CHANNEL-ID";
    private final String         mApiKey = "YOUR-CHANNEL-WRITE-KEY";

    private final JavaMqttClient mMqtt;
    private final Timeout        mPublishInterval = new Timeout();
    private final int PUBLISH_INTERVAL = 60 * 1000;  // publish @ every minute

    /**
     * Constructor
     */
    public AirQualitySense(String address) {
        super(address); // bluetooth MAC address
        mMqtt = new JavaMqttClient(mServer); // with SSL
    }

    /**
     * ArduinoLike Setup()
     */
    @Override
    public void setup() {
        super.setup();

        // Setup Sensor Channels
        SensorContent ch0 = new SensorContent("Standard PM1.0", "PM1.0", "ug/m3");
        SensorContent ch1 = new SensorContent("Standard PM2.5", "PM2.5", "ug/m3");
        SensorContent ch2 = new SensorContent("Standard PM10", "PM10", "ug/m3");
        SensorContent ch3 = new SensorContent("Temperature", "Temp", "°C");
        SensorContent ch4 = new SensorContent("Humidity", "Humi", "%");
        mSensors.clear();
        mSensors.add(ch0);
        mSensors.add(ch1);
        mSensors.add(ch2);
        mSensors.add(ch3);
        mSensors.add(ch4);

        final Serial serial = new Serial(mDuino);
        serial.begin(SwiftDuino.UART_BAUDRATE.B9600, G5_RXD_PIN, G5_TXD_PIN);   // setup serial port


        // initialize G5
        G5 = new PMS5003(serial);
        mDuino.digitalWrite(G5_RST_PIN, SwiftDuino.PinLevel.HIGH);
        mDuino.digitalWrite(G5_SET_PIN, SwiftDuino.PinLevel.HIGH);
        G5.handle(new Runnable() {
            @Override
            public void run() {
                mDuino.digitalWrite(LED4_PIN, SwiftDuino.PinLevel.HIGH);

                final int PM10 = G5.field(PMS5003.FIELD.PM10_STD);
                final int PM25 = G5.field(PMS5003.FIELD.PM25_STD);
                final int PM100 = G5.field(PMS5003.FIELD.PM100_STD);

                mFilter1.update(PM10);
                mFilter2.update(PM25);
                if (mFilter3.update(PM100)) {
                    mSensors.get(0).update(mFilter1.result(), timeToDuration());
                    mSensors.get(1).update(mFilter2.result(), timeToDuration());
                    mSensors.get(2).update(mFilter3.result(), timeToDuration());
                }
                mDuino.digitalWrite(LED4_PIN, SwiftDuino.PinLevel.LOW);
            }
        });

        // Setup DHT12
        final I2Cdev i2c = new I2Cdev(mDuino);
        i2c.setup(SDA_PIN, SCL_PIN);
        mDHT12 = new DHT12(i2c);
        notifyDataSetChanged(); // update UI

        // Mqtt
        mMqtt.connect();
        mPublishInterval.reset();
    }

    /**
     * ArduinoLike Loop()
     */
    @Override
    public void loop() {
        super.loop();

        // read Temperature & Humidity
        if ( mDHT12.read() ) {
            mSensors.get(3).update(mDHT12.temperature(), timeToDuration());
            mSensors.get(4).update(mDHT12.humidity(), timeToDuration());
        }
        notifyDataSetChanged(); // update UI

        // Send to ThingSpeak
        if ( mMqtt.isConnected() ) {
            if ( mPublishInterval.isExpired(PUBLISH_INTERVAL) ) {
                mPublishInterval.reset();
                String payload =
                        "field1=" + mSensors.get(1).current() +
                        "&field2=" + mSensors.get(3).current() +
                        "&field3=" + mSensors.get(4).current() +
                        "&status=MQTTPUBLISH";

                mMqtt.publish(mChannel, mApiKey, payload);
                Log.d(TAG, "Mqtt published:" + payload);
            }
        }
        mDuino.delay(5000);
    }

	/**
	 * Bluetooth Disconnect
	 */
    @Override
    public void disconnected() {
        super.disconnected();
        if ( mMqtt.isConnected() ) {
            mMqtt.disconnect();
        }
    }
}
