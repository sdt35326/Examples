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

public class AverageFilter {
    private int mSampleSize;
    private float mMax, mMin, mAvg, mResult;
    private int  mCount;
    public AverageFilter(int size) {
        mSampleSize = size;
        mCount = 0;
    }

    public boolean update(float  value) {
        if ( mCount == 0) {
            mMin = value;
            mMax = value;
            mAvg = value;
        } else {
            if ( value > mMax ) mMax = value;
            if ( value < mMin ) mMin = value;
            mAvg += value;
        }
        mCount++;
        if ( mCount == mSampleSize ) {
            mResult = (mAvg - (mMax+mMin)) / (mSampleSize - 2);
            mCount = 0;
            return true;
        }
        return false;
    }

    public float result() {
        return mResult;
    }
}
