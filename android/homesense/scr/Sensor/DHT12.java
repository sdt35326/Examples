package ucxpresso.net.homesense.Sensor;

/**
 * Copyright 2017, Embeda Technology Inc.
 * All rights reserved.
 * <p>
 * Contact us: service+java@embeda.com.tw
 * Created by jason on 2017/1/9.
 * License: CC By NC.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import net.ucxpresso.java.duino.libs.I2Cdev;


public class DHT12 extends Object {
    public static byte I2C_ADDRESS = (0xB8 >> 1);

    private I2Cdev  mI2C;
    private float   mTemperature;
    private float   mHumidity;

    public DHT12(I2Cdev i2c) {
        super();
        mI2C = i2c;
    }

    public boolean read() {
        byte[] bytes = mI2C.readBytes(I2C_ADDRESS, (byte) 0, 4, true);
        if ( bytes != null ) {
            mHumidity = (float)bytes[0] + scale(bytes[1]);
            mTemperature = (float)bytes[2] + scale(bytes[3]);
            return true;
        }
        return false;
    }

    public float temperature() {
        return mTemperature;
    }

    public float humidity() {
        return mHumidity;
    }

    private float scale(byte value) {
        if ( value >= 100 ) {
            return (value / 1000);
        } else if ( value >= 10 ) {
            return (value / 100);
        }
        return (value / 10);
    }
}
