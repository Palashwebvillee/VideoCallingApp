package prabhat.x.spiker.videocallingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import java.sql.Date;

public abstract class IncomingCallScreenActivity extends BroadcastReceiver {
    /*

        static final String TAG = IncomingCallScreenActivity.class.getSimpleName();
        private String mCallId;
        private AudioPlayer mAudioPlayer;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.incoming);

            Button answer = (Button) findViewById(R.id.answerButton);
            answer.setOnClickListener(mClickListener);
            Button decline = (Button) findViewById(R.id.declineButton);
            decline.setOnClickListener(mClickListener);

            mAudioPlayer = new AudioPlayer(this);
            mAudioPlayer.playRingtone();
            mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
        }

        @Override
        protected void onServiceConnected() {
            Call call = getSinchServiceInterface().getCall(mCallId);
            if (call != null) {
                call.addCallListener(new SinchCallListener());
                TextView remoteUser = (TextView) findViewById(R.id.remoteUser);
                remoteUser.setText(call.getRemoteUserId());

            } else {
                Log.e(TAG, "Started with invalid callId, aborting");
                finish();
            }
        }

        private void answerClicked() {
            mAudioPlayer.stopRingtone();
            Call call = getSinchServiceInterface().getCall(mCallId);
            if (call != null) {
                call.answer();
                Intent intent = new Intent(this, CallScreenActivity.class);
                intent.putExtra(SinchService.CALL_ID, mCallId);
                startActivity(intent);
            } else {
                finish();
            }
        }

        private void declineClicked() {
            mAudioPlayer.stopRingtone();
            Call call = getSinchServiceInterface().getCall(mCallId);
            if (call != null) {
                call.hangup();
            }
            finish();
        }

        private class SinchCallListener implements VideoCallListener {

            @Override
            public void onCallEnded(Call call) {
                CallEndCause cause = call.getDetails().getEndCause();
                Log.d(TAG, "Call ended, cause: " + cause.toString());
                mAudioPlayer.stopRingtone();
                finish();
            }

            @Override
            public void onCallEstablished(Call call) {
                Log.d(TAG, "Call established");
            }

            @Override
            public void onCallProgressing(Call call) {
                Log.d(TAG, "Call progressing");
            }

            @Override
            public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
                // Send a push through your push provider here, e.g. GCM
            }

            @Override
            public void onVideoTrackAdded(Call call) {
                // Display some kind of icon showing it's a video call
            }
        }

        private OnClickListener mClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.answerButton:
                        answerClicked();
                        break;
                    case R.id.declineButton:
                        declineClicked();
                        break;
                }
            }
        };
    */

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing

    static final String TAG = IncomingCallScreenActivity.class.getSimpleName();
    private String mCallId;
    private AudioPlayer mAudioPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
        } else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }


            onCallStateChanged(context, state, number);
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected abstract void onIncomingCallReceived(Context ctx, String number, Date start);

    protected abstract void onIncomingCallAnswered(Context ctx, String number, Date start);

    protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

    protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);

    protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

    protected abstract void onMissedCall(Context ctx, String number, Date start);


    public void onCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                // callStartTime = new Date();
                savedNumber = number;
                onIncomingCallReceived(context, number, callStartTime);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    //     callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber, callStartTime);
                } else {
                    isIncoming = true;
                    ///   callStartTime = new Date();
                    onIncomingCallAnswered(context, savedNumber, callStartTime);
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime);
                } else if (isIncoming) {
                    //   onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                } else {
                    // onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                break;
        }
        lastState = state;
    }

}
