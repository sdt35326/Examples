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

import net.ucxpresso.java.duino.libs.I2Cdev;
import net.ucxpresso.java.duino.libs.Serial;
import net.ucxpresso.java.utilies.Timeout;

import java.text.SimpleDateFormat;
import java.util.Date;

import ucxpresso.net.homesense.MQTT.JavaMqttClient;
import ucxpresso.net.homesense.SenseApplication;

public class AirQualitySense extends SenseBase {
    private static final String TAG = "AirQualitySense";

    private final int LED3_PIN = 20;      // nano51822-udk LED3
    private final int LED4_PIN = 21;      // nano51822-udk LED4

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
    private final int SDA_PIN = 29;
    private final int SCL_PIN = 28;
    private DHT12 mDHT12;

    // ThingSpeak MOTT Server
    private final JavaMqttClient mMqttThingSpeak = new JavaMqttClient("tcp://mqtt.thingspeak.com:1883");
    private String mThingSpeakChannelId;
    private String mThingSpeakApiKey;   // for write

    // LASS Server
    private final JavaMqttClient    mMqttLass = new JavaMqttClient("tcp://gpssensor.ddns.net:1883");

    private final Timeout mPublishInterval = new Timeout();
    private final int PUBLISH_INTERVAL = 60 * 1000;  // publish @ every minute

    // SQLite Sense Information
    private SenseApplication.SenseInfo mSenseInfo;

    /**
     * Constructor
     */
    public AirQualitySense(String address) {
        super(address); // bluetooth MAC address
    }

    /**
     * ArduinoLike Setup()
     */
    @Override
    public void setup() {
        super.setup();
        mSensors.clear();

        // Setup Sensor base fields
        SensorContent field1 = new SensorContent("Temperature", "Temp", "°C");
        SensorContent field2 = new SensorContent("Humidity", "Humi", "%");
        mSensors.add(field1);
        mSensors.add(field2);

        final Serial serial = new Serial(this);
        serial.begin(UART_BAUDRATE.B9600, G5_RXD_PIN, G5_TXD_PIN);  // Enable Serial port

        // initialize G5
        G5 = new PMS5003(serial);
        digitalWrite(G5_RST_PIN, PinLevel.HIGH);
        digitalWrite(G5_SET_PIN, PinLevel.HIGH);
        G5.handle(new Runnable() {
            @Override
            public void run() {
                // if g5 exist, add new fields in mSenses.
                if (mSensors.size() < 5) {
                    SensorContent field3 = new SensorContent("Standard PM1.0", "PM1.0", "ug/m3");
                    SensorContent field4 = new SensorContent("Standard PM2.5", "PM2.5", "ug/m3");
                    SensorContent field5 = new SensorContent("Standard PM10", "PM10", "ug/m3");
                    mSensors.add(field3);
                    mSensors.add(field4);
                    mSensors.add(field5);
                }

                digitalWrite(LED4_PIN, PinLevel.HIGH);

                int PM10 = G5.field(PMS5003.FIELD.PM10_STD);
                int PM25 = G5.field(PMS5003.FIELD.PM25_STD);
                int PM100 = G5.field(PMS5003.FIELD.PM100_STD);

                mFilter1.update(PM10);
                mFilter2.update(PM25);
                if (mFilter3.update(PM100)) {
                    mSensors.get(2).update(mFilter1.result(), timeToDuration());
                    mSensors.get(3).update(mFilter2.result(), timeToDuration());
                    mSensors.get(4).update(mFilter3.result(), timeToDuration());
                }
                digitalWrite(LED4_PIN, PinLevel.LOW);
            }
        });

        // Setup DHT12
        final I2Cdev i2c = new I2Cdev(this);
        i2c.setup(SDA_PIN, SCL_PIN);
        mDHT12 = new DHT12(i2c);
        notifyDataSetChanged(); // update UI

        // publish interval
        mPublishInterval.reset();

        // load device info from SQLite
        mSenseInfo = SenseApplication.mInstance.sense_query(mAddress);
        if (mSenseInfo != null) {
            mThingSpeakChannelId = mSenseInfo.mThingSpeakId;
            mThingSpeakApiKey = mSenseInfo.mThingSpeakWriteKey;
        }
        // update device name, if need.
        if (mSenseInfo != null && mSenseInfo.mLocation.length() > 0) {
            if (mSenseInfo.mLocation.equals(ble().peripheral().getName()) == false) {
                setDeviceName(mSenseInfo.mLocation);
            }
        }
    }

    /**
     * ArduinoLike Loop()
     */
    @Override
    public void loop() {
        super.loop();

        if (isConnected()) {
            // read Temperature & Humidity
            if (mDHT12.read()) {
                digitalWrite(LED3_PIN, PinLevel.HIGH);
                mSensors.get(0).update(mDHT12.temperature(), timeToDuration());
                mSensors.get(1).update(mDHT12.humidity(), timeToDuration());
                digitalWrite(LED3_PIN, PinLevel.LOW);

                // update UI
                notifyDataSetChanged();

                // publish
                if (mPublishInterval.isExpired(PUBLISH_INTERVAL)) {
                    mPublishInterval.reset();
                    //
                    // Send to ThingSpeak Mqtt Server
                    //
                    if (mThingSpeakChannelId != null && mMqttThingSpeak.connectIfNecessary()) {
                        String payload = makeThingSpeakPaylod();
                        mMqttThingSpeak.publish(mThingSpeakChannelId, mThingSpeakApiKey, payload);
                        Log.d(TAG, "Publish to ThingSpeak:" + payload);
                    }
                    //
                    // Send to LASS Mqtt Server
                    //
                    if ( mSenseInfo.mSendToLASS && mMqttLass.connectIfNecessary() ) {
                        String payload = makeLassPayload();
                        mMqttLass.publish(mSenseInfo.mLassTopic, payload, false);
                        Log.d(TAG, "Publish to LASS:" + payload);
                    }
                }
            }
        }
        delay(5000);
    }

    /**
     * Make ThingSpeak Mqtt Payload
     * @return ThingSpeak Payload
     */
    private String makeThingSpeakPaylod() {
        String payload = "field1=" + mSensors.get(0).current() +    // Temperature (default)
                "&field2=" + mSensors.get(1).current();             // Humidity (default)
        // Include G5 ?
        if (mSensors.size() > 2) {
            for (int i = 3; i <= mSensors.size(); i++) {
                payload += ("&field" + i + "=") + mSensors.get(i - 1).current();
            }
        }
        payload += "&status=Publish by " + mSenseInfo.mLocation;
        return payload;
    }

    /**
     * Make LASS Mqtt Payload
     * @return Lass Payload
     */
    private String makeLassPayload() {

        final SimpleDateFormat dateFm = new SimpleDateFormat("YYYY-MM-dd");
        final SimpleDateFormat timeFm = new SimpleDateFormat("HH:mm:ss");

        String payload = "|ver_format=3|fmt_opt=0" +
                "|app=" + mSenseInfo.mLassAppId +
                "|ver_app=0.7.3+" +
                "|device_id=" + mSenseInfo.mLassDeviceId +
                "|tick=" + millis() +
                "|date=" + dateFm.format(new Date(millis())) +
                "|time=" + timeFm.format(new Date(millis())) +
                "|device=HomeSense" +
                "|s_0=2234.00" +
                //"|s_1=100.00" +   // battery level
                //"|s_2=1.00" +     // battery mode
                //"|s_3=0.00" +     // motion speed
                "|s_d0=" + mSensors.get(3).current() +  // PM2.5
                "|s_d1=" + mSensors.get(4).current() +  // PM10
                "!s_d2=" + mSensors.get(2).current() +  // PM1.0
                "|s_t0=" + mSensors.get(0).current() +  // Temperature
                "|s_h0=" + mSensors.get(1).current() +  // Humidity
                "|gps_lat=" + SenseApplication.mInstance.mLocation.getLatitude() +
                "|gps_lon=" + SenseApplication.mInstance.mLocation.getLongitude() +
                "|gps_fix=1|gps_num=9|gps_alt=2";

        return payload;
    }

}
