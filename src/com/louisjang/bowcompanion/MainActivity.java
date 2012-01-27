package com.louisjang.bowcompanion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "BowCompanion/.MainActivity";
	
    private Button mStart;
    private Button mStop;
    private Button mCountDec;
    private Button mCountInc;
    private Button mIntervalDec;
    private Button mIntervalInc;
    private Button mRepeatDec;
    private Button mRepeatInc;
    
    private EditText mCountEdit;
    private EditText mIntervalEdit;
    private EditText mRepeatEdit;
    
    private TextView mStatus;
    
    private int mTargetCount;
    private int mInterval;
    private int mTargetRepeat;
    
    private int mCurCount;
    private int mCurRepeat;
    
    static final private int STOPPED = 0;
    static final private int WAITING = 1;
    static final private int STARTED = 2;
    static final private int PAUSED = 3;

    
    private int mState = STOPPED;
    private boolean mStarted = false;

    private SoundPool mRings = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    private int mNotifyRingId;
    private int mWaitStart = 5;
    
    private int mWaitingTimeout;
    
    private Handler mH = new Handler();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mStart = (Button)findViewById(R.id.start);
        mStart.setOnClickListener(this);
        
        mStop = (Button)findViewById(R.id.stop);
        mStop.setOnClickListener(this);

        
        mCountDec = (Button)findViewById(R.id.count_dec);
        mCountDec.setOnClickListener(this);
        mCountInc = (Button)findViewById(R.id.count_inc);
        mCountInc.setOnClickListener(this);

        mIntervalDec = (Button)findViewById(R.id.interval_dec);
        mIntervalDec.setOnClickListener(this);
        mIntervalInc = (Button)findViewById(R.id.interval_inc);
        mIntervalInc.setOnClickListener(this);
        
        mRepeatDec = (Button)findViewById(R.id.repeat_dec);
        mRepeatDec.setOnClickListener(this);
        mRepeatInc = (Button)findViewById(R.id.repeat_inc);
        mRepeatInc.setOnClickListener(this);
        
        mCountEdit = (EditText)findViewById(R.id.count);
        mCountEdit.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) {}
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {
				try {
					mTargetCount = Integer.parseInt(arg0.toString());
				} catch (NumberFormatException e) {
					mCountEdit.setText("1");
				}
			}
        });
        mIntervalEdit = (EditText)findViewById(R.id.interval);
        mIntervalEdit.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) {}
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {
				try {
					mInterval = Integer.parseInt(arg0.toString());
				} catch (NumberFormatException e) {
					mIntervalEdit.setText("1");
				}
			}
        });

        mRepeatEdit = (EditText)findViewById(R.id.repeat);
        mRepeatEdit.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) {}
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {
				try {
					mTargetRepeat = Integer.parseInt(arg0.toString());
				} catch (NumberFormatException e) {
					mRepeatEdit.setText("1");
				}
			}
        });
        
        mStatus = (TextView)findViewById(R.id.status);
        
        if (savedInstanceState != null) {
        	mTargetCount = (Integer) savedInstanceState.get("count");
        	mInterval = (Integer) savedInstanceState.get("interval");
        	mTargetRepeat = (Integer) savedInstanceState.get("repeat");
        } else {
        	mTargetCount = 108;
        	mInterval = 10;
        	mTargetRepeat = 1;
        }
        updateEditText();
        updateViewState();
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mNotifyRingId = mRings.load(this, R.raw.notify, 0);
    }
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("count", mTargetCount);
		outState.putInt("interval", mInterval);
		outState.putInt("repeat", mTargetRepeat);
	}

	@Override
	protected void onStop() {
    	stop();
		super.onStop();
	}

	public void onClick(View v) {
    	if (v == mIntervalDec) {
    		mInterval -= 1;
    	} else if (v == mIntervalInc) {
    		mInterval += 1;
    	} else if (v == mCountDec) {
    		mTargetCount -= 1;
    	} else if (v == mCountInc) {
    		mTargetCount += 1;
    	} else if (v == mRepeatDec) {
    		mTargetRepeat -= 1;
    	} else if (v == mRepeatInc) {
    		mTargetRepeat += 1;
    	} else if (v == mStart) {
    		start();
    		return;
    	} else if (v == mStop) {
    		stop();
    	}
    	updateEditText();
    	updateViewState();
    }

    private Runnable mWorker = new Runnable() {
		public void run() {
			assert (mState != STOPPED);
			
			if (mState == WAITING) {
				mStatus.setText(Integer.toString(mWaitingTimeout));
				mWaitingTimeout -= 1;
				if (mWaitingTimeout > 0) {
					mH.postDelayed(this, 1000);
					return;
				}
				mState = STARTED;
				mCurCount = 0;
				mCurRepeat = 0;
				updateViewState();
			}
			
			if (mState == STARTED) {
				mCurCount += 1;
				if (mCurCount < mTargetCount) {
					mH.postDelayed(this, mInterval*1000);
					mRings.play(mNotifyRingId, 1.0f, 1.0f, 0, 0, 1.0f);
					mStatus.setText(String.format("%d: %d/%d", 1, mCurCount, mTargetCount));
					return;
				} 
				
				mCurRepeat += 1;
				if (mCurRepeat < mTargetRepeat) {
					// play repeat sound
					mCurCount = 0;
					mH.postDelayed(this, mInterval*1000);
					mStatus.setText(String.format("%d: %d/%d", mCurRepeat, mCurCount, mTargetCount));
					return;
				}

				// play end sound
				stop();
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				pm.userActivity(SystemClock.uptimeMillis(), false);
			}
		}
    };
    
    private void realStart() {
    	mState = WAITING;
    	mWaitingTimeout = mWaitStart;
		mH.post(mWorker);
    }

    private void stop() {
    	mState = STOPPED;
    	mH.removeCallbacks(mWorker);
		mStatus.setText(R.string.comleted);
    	updateViewState();
    }

	private void start() {
		if (mState == STOPPED) {
	    	AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	    	if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setMessage(R.string.zero_volume_alert)
	    		       .setCancelable(false)
	    		       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) {
	    		        	   realStart();
	    		           }
	    		       })
	    		       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) {
	    		           }
	    		       });
	    		AlertDialog alert = builder.create();
	    		alert.show();
	    	} else {
	    		realStart();
	    	}
		} else if (mState == WAITING) {
			stop();
		} else if (mState == STARTED) {
			mStatus.setText(String.format("Paused at %d: %d/%d", mCurRepeat, mCurCount, mTargetCount));
			mH.removeCallbacks(mWorker);
			mState = PAUSED;
		} else if (mState == PAUSED) {
			mState = STARTED;
			mStatus.setText(String.format("%d: %d/%d", 1, mCurCount, mTargetCount));
			mH.postDelayed(mWorker, mInterval*1000);
		}
		updateViewState();
	}

	private void updateEditText() {
        mCountEdit.setText(Integer.toString(mTargetCount));
        mIntervalEdit.setText(Integer.toString(mInterval));
        mRepeatEdit.setText(Integer.toString(mTargetRepeat));
	}
	
	private void updateViewState() {
		final boolean started = (mState != STOPPED);
		mCountEdit.setEnabled(!started);
		mIntervalEdit.setEnabled(!started);
		mRepeatEdit.setEnabled(!started);

		mCountInc.setEnabled(!started);
		mIntervalInc.setEnabled(!started);
		mRepeatInc.setEnabled(!started);
		
		if (started) {
			mCountDec.setEnabled(false);
			mIntervalDec.setEnabled(false);
			mRepeatDec.setEnabled(false);
		} else {
			mCountDec.setEnabled((mTargetCount > 1));
			mIntervalDec.setEnabled((mInterval > 1));
			mRepeatDec.setEnabled((mTargetRepeat > 1));
		}
		
		if (mState == WAITING)
			mStart.setText(R.string.cancel);
		else if (mState == STARTED)
			mStart.setText(R.string.pause);
		else if (mState == PAUSED)
			mStart.setText(R.string.resume);
		else
			mStart.setText(R.string.start);
		
		if (mState == STARTED || mState == PAUSED) {
			mStop.setVisibility(View.VISIBLE);
		} else {
			mStop.setVisibility(View.GONE);
		}
	}
}