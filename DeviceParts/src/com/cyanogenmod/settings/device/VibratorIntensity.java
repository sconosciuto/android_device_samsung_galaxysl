package com.cyanogenmod.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.os.Vibrator;

public class VibratorIntensity extends DialogPreference implements OnClickListener {

    private static final String FILE_PATH = "/sys/vibrator/pwmvalue";
    private static final int DEFAULT_VALUE = 127;
    private static final int MAX_VALUE = 127;
    private static final int MIN_VALUE = 0;
    private static final String SETTING_KEY = DeviceSettings.KEY_VIBRATOR_INTENSITY;
    private static final int TITLE = R.string.vibrator_intensity_seekbar_title;

    private ScreenSeekBar mSeekBar;

    // Track instances to know when to restore original value
    // (when the orientation changes, a new dialog is created before the old one is destroyed)
    private static int sInstances = 0;

    public VibratorIntensity(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_dialog_seekbar);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        sInstances++;

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar_seekbar);
        TextView valueDisplay = (TextView) view.findViewById(R.id.seekbar_value);
        mSeekBar = new ScreenSeekBar(seekBar, valueDisplay);
        TextView title = (TextView) view.findViewById(R.id.seekbar_title);
        title.setText(TITLE);
        SetupButtonClickListeners(view);
    }

    private void SetupButtonClickListeners(View view) {
        Button mTestButton = (Button)view.findViewById(R.id.seekbar_btn_default);
        mTestButton.setOnClickListener(this);
	mTestButton.setText("Test");
        TextView mMinusButton = (TextView)view.findViewById(R.id.seekbar_btn_plus);
        mMinusButton.setOnClickListener(this);
        TextView mPlusButton = (TextView)view.findViewById(R.id.seekbar_btn_minus);
        mPlusButton.setOnClickListener(this);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        sInstances--;

        if (positiveResult)
            mSeekBar.save();
        else if (sInstances == 0)
            mSeekBar.reset();
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = String.format("%d", sharedPrefs.getInt(SETTING_KEY, DEFAULT_VALUE));
        Utils.writeValue(FILE_PATH, value);
    }

    public static boolean isSupported() {
        if (Utils.fileExists(FILE_PATH)) {
            return true;
        }

        return false;
    }

    class ScreenSeekBar implements SeekBar.OnSeekBarChangeListener {
        protected int mOriginal;
        protected SeekBar mSeekBar;
        protected TextView mValueDisplay;

        public ScreenSeekBar(SeekBar seekBar, TextView valueDisplay) {
            mSeekBar = seekBar;
            mValueDisplay = valueDisplay;

            // Get value from the settings
            SharedPreferences sharedPreferences = getSharedPreferences();
            mOriginal = sharedPreferences.getInt(SETTING_KEY, DEFAULT_VALUE);

            // Set seekbar range
            mSeekBar.setMax(MAX_VALUE - MIN_VALUE);
            reset();
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        // For inheriting class
        protected ScreenSeekBar() {
        }

        public void reset() {
            mSeekBar.setProgress(mOriginal - MIN_VALUE); // The saved value is the real value
            updateValue(mOriginal - MIN_VALUE);
        }

        public void save() {
            Editor editor = getEditor();
            int value = mSeekBar.getProgress() + MIN_VALUE; // Progress starts from 0, add MIN_VALUE to get the real value
            editor.putInt(SETTING_KEY, value);
            editor.commit();
            writeConfig(value);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateValue(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        protected void updateValue(int progress) {
            mValueDisplay.setText(String.format("%d", progress));
        }

        public void setValue(int value) {
            mOriginal = value;
            reset();
        }

        public void moveSeekBar(int value) {
            int progress = (mSeekBar.getProgress() + MIN_VALUE) + value;
            if (progress >= MIN_VALUE && progress <= MAX_VALUE) {
                mOriginal = progress;
            }
            reset();
        }

        private void writeConfig(int value) {
            Utils.writeValue(FILE_PATH, String.format("%d", value));
        }

        private void testVibration() {
            writeConfig(mSeekBar.getProgress() + MIN_VALUE);
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(500);
            writeConfig(mOriginal);
        }
    }

    public void onClick(View v) {
        switch(v.getId()){
            case R.id.seekbar_btn_default:
                mSeekBar.testVibration();
                break;
            case R.id.seekbar_btn_minus:
                mSeekBar.moveSeekBar(-1);
                break;
            case R.id.seekbar_btn_plus:
                mSeekBar.moveSeekBar(1);
                break;
        }
    }
}
