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
import net.ucxpresso.java.duino.SwiftDuino;
import java.util.ArrayList;

public class SenseBase extends SwiftDuino implements SwiftDuino.arduinoLike {
    protected final String mAddress;

    // Sensor Data
    protected ArrayList<SensorContent> mSensors;

    // UI
    private SenseUI mSenseUI;
    public interface SenseUI {
        public void onNotifyDataSetChanged();
    }

    // Start Time
    private long mStartTime;
    private long mStopTime;

    public SenseBase(String address) {
        super();
        delegate(this);
        mAddress = address;
        mSensors = new ArrayList<SensorContent>();
        mStartTime = 0;
        mSenseUI = null;
    }

    public boolean equals(String address) {
        return mAddress.equals(address);
    }

    public ArrayList<SensorContent> sensors() {
        return mSensors;
    }

    public void attachUiHandle(SenseUI ui) {
        mSenseUI = ui;
    }

    public void detachUiHandle() {
        mSenseUI = null;
    }

    protected synchronized void notifyDataSetChanged() {
        if ( mSenseUI != null ) {
            mSenseUI.onNotifyDataSetChanged();
        }
    }

    public long startTime() {
        return mStartTime;
    }

    public long timeToDuration() {
        return (mStopTime - mStartTime);
    }

    @Override
    public void setup() {
        mStartTime = SwiftDuino.millis() / 1000; // Time Unit by Second
        mStopTime = mStartTime;
    }

    @Override
    public void loop() {
        mStopTime = SwiftDuino.millis() / 1000;  // Time Unit by Second
    }

    public void willDisconnect() {
    }

    @Override
    public void disconnected() {
        close(); // exit duino's loop
    }
}
