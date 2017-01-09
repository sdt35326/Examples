package ucxpresso.net.homesense.Sensor;


/**
 * Copyright 2017, Embeda Technology Inc.
 * All rights reserved.
 * <p>
 * Contact us: service+java@embeda.com.tw
 * Created by jason on 2017/1/7.
 * License: CC By NC.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import net.ucxpresso.java.duino.SwiftDuino;
import net.ucxpresso.java.duino.libs.Serial;
import net.ucxpresso.java.utilies.Timeout;

public class PMS5003 extends Object {

    public enum FIELD {
        PM10_STD(0),
        PM25_STD(1),
        PM100_STD(2),
        PM10_AIR(3),
        PM25_AIR(4),
        PM100_AIR(5),
        UM03(6),
        UM05(7),
        UM10(8),
        UM25(9),
        UM50(10),
        UM100(11),
        RESERVE(12);

        public int rawValue;
        FIELD(int value) {
            this.rawValue = value;
        }
    }

    private Serial mSerial;
    private static final int MAX_FIELDS = 13;
    private short mChecksum;
    private Runnable mHandle = null;
    private short[] mFields = new short[MAX_FIELDS];

    public PMS5003(Serial serial) {
        super();
        mSerial = serial;
        mSerial.event(new Runnable() {
            @Override
            public void run() {
                if ( mSerial.available() >= 32 ) {
                    if ( mSerial.read() == 0x42 && mSerial.read() == 0x4d ) {
                        reset_checksum();
                        final short length = read(true);
                        if ( length == 28 ) {
                            // update fields
                            for (int i=0; i<MAX_FIELDS; i++) {
                                mFields[i] = read(true);
                            }
                            // checksum check
                            final short chk = (short) (read(false) & 0x00FF);
                            mChecksum &= 0x00FF;
                            if ( mChecksum == chk && mHandle != null ) {
                                mHandle.run();
                            }
                        }
                    }
                }
            }
        });
    }

    public void handle(Runnable runnable) {
        mHandle = runnable;
    }

    public short field(FIELD index) {
        return mFields[index.rawValue];
    }

    private void reset_checksum() {
        mChecksum = 143;
    }

    private short read(boolean withChecksum) {
        byte[] bytes = mSerial.read(2);
        if (bytes != null) {
            if (withChecksum) {
                mChecksum += (bytes[0] + bytes[1]);
            }
            return (short) ((bytes[0] << 8) + bytes[1]);    // MSB First
        }
        return 0;
    }
}
