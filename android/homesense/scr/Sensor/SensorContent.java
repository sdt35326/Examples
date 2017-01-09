package ucxpresso.net.homesense.Sensor;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

import ucxpresso.net.homesense.SensorAdapter;

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

public class SensorContent extends Object {
    static final int DEF_MAX_RECORD_COUNT = 1440;
    public String mDescription;
    public String mTag;
    public String mUnit;


    // Value
    private float mCurrent  = 0;
    private float mMin = 0;
    private float mMax = 0;
    private float mAvg = 0;
    public final ArrayList<Entry> mValues = new ArrayList<Entry>();;

    public SensorContent(final String description, final String tag, final String unit) {
        super();
        mDescription = description;
        mTag = tag;
        mUnit = unit;
    }

    public void clear() {
        mMax = mCurrent;
        mMin = mCurrent;
        mAvg = 0;
        mValues.clear();
    }

    public void update(float value, long timeInterval) {
        mCurrent = value;
        if ( mValues.size() == 0) {
            mMax = value;
            mMin = value;
        } else {
            if (value > mMax) mMax = value;
            if (value < mMin) mMin = value;
        }
        mAvg = (mMax + mMin) / 2;

        if ( mValues.size() > DEF_MAX_RECORD_COUNT ) {
            mValues.remove(0);
        }
        mValues.add(new Entry(timeInterval, value));
    }

    public String current() {
        return String.format("%.1f", mCurrent);
    }

    public String avg() {
        return String.format("%.2f", mAvg);
    }

    public String min() {
        return String.format("%.2f", mMin);
    }

    public String max() {
        return String.format("%.2f", mMax);
    }
}
