package com.noddy.breathingled;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;

import java.io.IOException;

public class PWMActivity extends Activity
{
    private static final String TAG = PWMActivity.class.getSimpleName();

    private Handler mHandler = new Handler();
    private Pwm mPwm;

    private boolean mIsPulseIncreasing = true;

    private static final double MAX_DUTY_CYCLE = 100; // 最大占空比100%
    private static final double MIN_DUTY_CYCLE = 0; // 最小占空比0%
    private double mDutyCycleCurrent; // 当前占空比
    private static final double DUTY_CYCLE_STEP = 1; // 占空比步进

    private static final int INTERVAL_BETWEEN_STEPS_MS = 20; // led亮度每次刷新间隔（毫秒）

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "PWMActivity onCreate()");

        PeripheralManagerService service = new PeripheralManagerService();

        try
        {
            String pinName = BoardDefaults.getPWMPort();

            // 打开PWM
            mPwm = service.openPwm(pinName);

            // 初始化PWM
            initializePwm(mPwm);

            mHandler.post(mChangePWMRunnable);
        }
        catch (IOException ex)
        {
            Log.e(TAG, "Unable to access PWM", ex);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        Log.i(TAG, "Closing PWM");

        if (null != mChangePWMRunnable)
        {
            mHandler.removeCallbacks(mChangePWMRunnable);
        }

        if (null != mPwm)
        {
            try
            {
                mPwm.close();
                mPwm = null;
            }
            catch (IOException e)
            {
                Log.e(TAG, "Unable to close PWM", e);
            }
        }
    }

    public void initializePwm(Pwm pwm) throws IOException
    {
        pwm.setPwmFrequencyHz(100); // Hz：周期/每秒，100表示100次/s，即一个脉冲周期需要10ms（1000ms/100 = 10ms）
        pwm.setPwmDutyCycle(mDutyCycleCurrent); // 默认占空比为0，如果设置为75，树莓派PWM输出3.3v电压，假设电路中不存在电阻，则一个脉冲周期内，高电平电压为3.3v * 75% = 2.475v
        pwm.setEnabled(true);
    }


    private Runnable mChangePWMRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mPwm == null)
            {
                Log.w(TAG, "Stopping runnable since mPwm is null");
                return;
            }

            // 改变占空比，让led由亮到暗，再由暗到亮，不断循环
            if (mIsPulseIncreasing)
                mDutyCycleCurrent += DUTY_CYCLE_STEP;
            else
                mDutyCycleCurrent -= DUTY_CYCLE_STEP;


            if (mDutyCycleCurrent >= MAX_DUTY_CYCLE)
            {
                mIsPulseIncreasing = !mIsPulseIncreasing;
                mDutyCycleCurrent = MAX_DUTY_CYCLE;
            }
            else if ((mDutyCycleCurrent <= MIN_DUTY_CYCLE))
            {
                mIsPulseIncreasing = !mIsPulseIncreasing;
                mDutyCycleCurrent = MIN_DUTY_CYCLE;
            }


            Log.i(TAG, "Changing PWM Duty Cycle:" + mDutyCycleCurrent);

            try
            {
                mPwm.setPwmDutyCycle(mDutyCycleCurrent);

                mHandler.postDelayed(this, INTERVAL_BETWEEN_STEPS_MS);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };
}
