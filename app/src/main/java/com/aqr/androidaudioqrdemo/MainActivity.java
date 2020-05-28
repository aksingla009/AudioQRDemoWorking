package com.aqr.androidaudioqrdemo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.LinearGradient;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import at.ac.fhstp.sonitalk.SoniTalkConfig;
import at.ac.fhstp.sonitalk.SoniTalkContext;
import at.ac.fhstp.sonitalk.SoniTalkDecoder;
import at.ac.fhstp.sonitalk.SoniTalkEncoder;
import at.ac.fhstp.sonitalk.SoniTalkMessage;
import at.ac.fhstp.sonitalk.SoniTalkPermissionsResultReceiver;
import at.ac.fhstp.sonitalk.SoniTalkSender;
import at.ac.fhstp.sonitalk.exceptions.ConfigException;
import at.ac.fhstp.sonitalk.exceptions.DecoderStateException;
import at.ac.fhstp.sonitalk.utils.ConfigFactory;
import at.ac.fhstp.sonitalk.utils.DecoderUtils;
import at.ac.fhstp.sonitalk.utils.EncoderUtils;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;


public class MainActivity extends AppCompatActivity implements SoniTalkDecoder.MessageListener, SoniTalkPermissionsResultReceiver.Receiver {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 42;
    private String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO};

    // These request codes are used to know which action was accepted / denied by the user.
    public static final int SENDING_REQUEST_CODE = 2001;
    public static final int RECEIVING_REQUEST_CODE = 2002;

    private SoniTalkPermissionsResultReceiver soniTalkPermissionsResultReceiver;
    private SoniTalkContext soniTalkContext;
    private SoniTalkDecoder soniTalkDecoder;
    private SoniTalkEncoder soniTalkEncoder;
    private SoniTalkMessage currentMessage;
    private SoniTalkSender soniTalkSender;

    private ImageButton btnPlay;
    private EditText edtSignalText;


    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(NUMBER_OF_CORES + 1);

    private AudioTrack playerFrequency;

    private ViewGroup rootViewGroup;

    private ImageButton btnListen;
    private TextView txtDecodedText;

    boolean silentMode;
    private Toast currentToast;

    //Message will not be allowed to be send until the previous msg was completely Sent or cancelled
    private boolean isSendingMsgAllowed = true;
    private boolean isListeningMsgAllowed = true;

    TriStateToggleButton triStateToggleButton;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.setClassLoader(SoniTalkPermissionsResultReceiver.class.getClassLoader());
        outState.putParcelable(SoniTalkPermissionsResultReceiver.soniTalkPermissionsResultReceiverTag, soniTalkPermissionsResultReceiver);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        triStateToggleButton  = findViewById(R.id.tstb_1);

        triStateToggleButton.setOnToggleChanged(new TriStateToggleButton.OnToggleChanged() {
            @Override
            public void onToggle(TriStateToggleButton.ToggleStatus toggleStatus, boolean booleanToggleStatus, int toggleIntValue) {
                switch (toggleStatus) {
                    case off:
                        Log.d(TAG,"TOGGLE STATUS OFF PAY CALLED");
                        //user tapped on extreme left that is Pay side
                        //So User will Firstly listen to Receiver's details that to whom would he pay
                        stopSending();
                        listenToMsg();
                        break;
                    case mid:
                        Log.d(TAG,"TOGGLE STATUS MID CALLED");
                        stopDecoder();
                        stopSending();
                        break;
                    case on:
                        Log.d(TAG,"TOGGLE STATUS ON RECEIVE CALLED");
                        //user tapped on extreme right that is receive side
                        //Here initially user will send a msg over speaker that these are my details PAYER will then send money accordingly
                        //after listening to this sent message and fetching details out of it
                        sendMsgToEncode();
                        stopDecoder();
                        break;
                }
            }
        });

        rootViewGroup = (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);

        soniTalkPermissionsResultReceiver = new SoniTalkPermissionsResultReceiver(new Handler());

        soniTalkPermissionsResultReceiver.setReceiver(this);


        if (soniTalkContext == null) {
            soniTalkContext = SoniTalkContext.getInstance(this, soniTalkPermissionsResultReceiver);
        }

        btnPlay = findViewById(R.id.btnPlay);
        edtSignalText = findViewById(R.id.edtSignalText);

        btnListen = findViewById(R.id.btnListen);

        txtDecodedText = findViewById(R.id.txtDecodedText);
    }

    private void sendMsgToEncode() {
        Log.e(TAG,"IN SEND MSG To ENCODE Block ");

        currentMessage = null;
        if (isSendingMsgAllowed) {
            String textToSend = edtSignalText.getText().toString();
            generateMessage("TYpe Here");
            isSendingMsgAllowed = false;
        } else {
            stopSending();
            isSendingMsgAllowed = true;
        }
    }

    private void listenToMsg() {
        Log.e(TAG,"isListeningMsgAllowed : "+isListeningMsgAllowed);
        if (isListeningMsgAllowed) {
            onStartListeningMsg();
            isListeningMsgAllowed = false;
        } else {
            stopDecoder();
            isListeningMsgAllowed = true;
        }
    }

    public void stopSending() {
        if (soniTalkSender != null) {
            soniTalkSender.cancel();
        }
        isSendingMsgAllowed = true;
        btnPlay.setImageResource(R.drawable.ic_volume_up_grey_48dp);
    }

    public void sendMessageOverSound() {
        if (currentMessage != null) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int volume = ConfigConstants.SETTING_LOUDNESS_DEFAULT;
            audioManager.setStreamVolume(3, (int) Math.round((audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * volume / 100.0D)), 0);

            if (soniTalkContext == null) {
                soniTalkContext = SoniTalkContext.getInstance(MainActivity.this, soniTalkPermissionsResultReceiver);
            }
            soniTalkSender = soniTalkContext.getSender();
            Log.e(TAG,"JUST abut to SEND MSG");
            soniTalkSender.send(currentMessage, SENDING_REQUEST_CODE);


        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.signal_generator_not_created), Toast.LENGTH_LONG).show();
        }
    }

    public void generateMessage(String messageString) {
        if (playerFrequency != null) {
            playerFrequency.stop();
            playerFrequency.flush();
            playerFrequency.release();
            playerFrequency = null;
        }

        SoniTalkConfig config = null;
        try {
            config = ConfigFactory.getDefaultConfig(this.getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigException e) {
            e.printStackTrace();
        }

        if (soniTalkContext == null) {
            soniTalkContext = SoniTalkContext.getInstance(MainActivity.this, soniTalkPermissionsResultReceiver);
        }
        soniTalkEncoder = soniTalkContext.getEncoder(config);

        final byte[] bytes = messageString.getBytes(StandardCharsets.UTF_8);

        if (!EncoderUtils.isAllowedByteArraySize(bytes, config)) {
            Toast.makeText(getApplicationContext(), getString(R.string.encoder_exception_text_too_long), Toast.LENGTH_LONG).show();
        } else {
            // Move the background execution handling away from the Activity (in Encoder or Service or AsyncTask). Creating Runnables here may leak the Activity
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    currentMessage = soniTalkEncoder.generateMessage(bytes);
                    sendMessageOverSound();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        soniTalkPermissionsResultReceiver.setReceiver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG,"onPause() called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG,"onResume() called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopDecoder();
        stopSending();
        setReceivedText("");

        if (currentToast != null) {
            currentToast.cancel();
        }
        soniTalkPermissionsResultReceiver.setReceiver(null);
    }

    private void onStartListeningMsg() {
        Log.e(TAG,"onStartListeningMsg");
        if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
            Log.e(TAG, "asking for audio permission to listen msg");
            requestAudioPermission();
        } else {
            Log.e(TAG, "audio permission already granted to listen msg");
            startReceivingMessagesToDecode();
        }
    }

    private void startReceivingMessagesToDecode() {
        Log.e(TAG,"startReceivingMessagesToDecode");
        try {
            SoniTalkConfig config = ConfigFactory.getDefaultConfig(this.getApplicationContext());

            if (soniTalkContext == null) {
                soniTalkContext = SoniTalkContext.getInstance(this, soniTalkPermissionsResultReceiver);
            }
            int samplingRate = 44100;
            soniTalkDecoder = soniTalkContext.getDecoder(samplingRate, config); //, stepFactor, frequencyOffsetForSpectrogram, silentMode);
            soniTalkDecoder.addMessageListener(this); // MainActivity will be notified of messages received (calls onMessageReceived)
            //soniTalkDecoder.addSpectrumListener(this); // Can be used to receive the spectrum when a message is decoded.

            // Should not throw the DecoderStateException as we just initialized the Decoder
            soniTalkDecoder.receiveBackground(RECEIVING_REQUEST_CODE);

        } catch (DecoderStateException e) {
            setReceivedText(getString(R.string.decoder_exception_state) + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, getString(R.string.decoder_exception_io) + e.getMessage());
        } catch (ConfigException e) {
            Log.e(TAG, getString(R.string.decoder_exception_config) + e.getMessage());
        }

    }

    private void stopDecoder() {
        isListeningMsgAllowed = true;
        btnListen.setImageResource(R.drawable.baseline_hearing_grey_48);
        setReceivedText("");

        if (soniTalkDecoder != null) {
            soniTalkDecoder.stopReceiving();
        }
        soniTalkDecoder = null;
    }

    public void setReceivedText(String decodedText) {
        txtDecodedText.setText(decodedText);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            //we will show an explanation next time the user click on start
            showRequestPermissionExplanation(R.string.permissionRequestExplanation);
        }
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startReceivingMessagesToDecode();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showRequestPermissionExplanation(R.string.permissionRequestExplanation);
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        Log.e(TAG,"Checking custom permissions");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void showRequestPermissionExplanation(int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.permission_request_explanation_positive, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
        );
        builder.setNegativeButton(R.string.permission_request_explanation_negative, null);
        builder.show();
    }

    public void requestAudioPermission() {
        Log.i(TAG, "Audio permission has NOT been granted. Requesting permission.");
        // If an explanation is needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)) {
            Log.i(TAG, "Displaying audio permission rationale to provide additional context.");
            Snackbar.make(rootViewGroup, R.string.permissionRequestExplanation,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    REQUEST_RECORD_AUDIO_PERMISSION);
                        }
                    })
                    .show();
        } else {
            // First time, no explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onMessageReceived(final SoniTalkMessage receivedMessage) {
        if (receivedMessage.isCrcCorrect()) {
            //Log.d("ParityCheck", "The message was correctly received");
            final String textReceived = DecoderUtils.byteToUTF8(receivedMessage.getMessage());
            Log.e("Received message", textReceived);


            // We stop when CRC is correct and we are not in silent mode
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Update text displayed
                    Toast.makeText(MainActivity.this,"Received message"+textReceived,Toast.LENGTH_SHORT).show();
                    setReceivedText(textReceived + " (" + String.valueOf(receivedMessage.getDecodingTimeNanosecond() / 1000000) + "ms)");

                    if (currentToast != null) {
                        currentToast.cancel(); // NOTE: Cancel so fast that only the last one in a series really is displayed.
                    }
                    // Stops recording if needed and shows a Toast
                    if (!silentMode) {
                        // STOP everything.
                        stopDecoder();
//                        currentToast = Toast.makeText(MainActivity.this, "Correctly received a message. Stopped.", Toast.LENGTH_SHORT);
//                        currentToast.show();
                    }
                }
            });
        } else {
            Log.e("ParityCheck", "The message was NOT correctly received");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //main.setReceivedText("Please try again, could not detect or decode the message!");

                    if (currentToast != null) {
                        currentToast.cancel(); // NOTE: Cancel so fast that only the last one in a series really is displayed.
                    }
                    if (!silentMode) {
                        setReceivedText(getString(R.string.detection_crc_incorrect));
                    } else {
                        setReceivedText(getString(R.string.detection_crc_incorrect_keep_listening));
                    }
                }
            });
        }
    }

    @Override
    public void onDecoderError(final String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopDecoder();
                setReceivedText(errorMessage);
            }
        });
    }

    @Override
    public void onSoniTalkPermissionResult(int resultCode, Bundle resultData) {
        int actionCode = 0;
        if (resultData != null) {
            actionCode = resultData.getInt(getString(R.string.bundleRequestCode_key));
        }
        switch (resultCode) {
            case SoniTalkContext.ON_PERMISSION_LEVEL_DECLINED:
                Log.e(TAG,"ON PERMISSION LEVEL DECLINED");
                if (currentToast != null) {
                    currentToast.cancel();
                }
                switch (actionCode) {
                    case RECEIVING_REQUEST_CODE:
                        currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_receiving_listening_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();

                        triStateToggleButton.setToggleMid(true);
                        //onStopListeningMsg();

                        // Set buttons in the state NOT RECEIVING
                        isListeningMsgAllowed = true;
                        btnListen.setImageResource(R.drawable.baseline_hearing_grey_48);

                        break;
                    case SENDING_REQUEST_CODE:
                        Log.e(TAG,"ON PERMISSION LEVEL DECLINED in SENDING REQUEST CODE");
                        currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_sending_sending_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();

                        triStateToggleButton.setToggleMid(true);

                        // Set buttons in the state NOT RECEIVING
                        isSendingMsgAllowed = true;
                        btnPlay.setImageResource(R.drawable.ic_volume_up_grey_48dp);

                        break;
                }


                break;
            case SoniTalkContext.ON_REQUEST_GRANTED:

                Log.e(TAG,"ON_REQUEST GRANTED");
                //Log.d(TAG, "ON_REQUEST_GRANTED");
                //Log.d(TAG, String.valueOf(resultData.getInt(getString(R.string.bundleRequestCode_key), 0)));

                switch (actionCode) {
                    case RECEIVING_REQUEST_CODE:
                        // Set buttons in the state RECEIVING
                        isListeningMsgAllowed = false;
                        btnListen.setImageResource(R.drawable.baseline_hearing_orange_48);

                        currentToast = Toast.makeText(MainActivity.this, "Clicked on Pay Listening msg permission granted", Toast.LENGTH_SHORT);
                        currentToast.show();

                        setReceivedText(getString(R.string.decoder_start_text));
                        break;

                    case SENDING_REQUEST_CODE:
                        //This is Where Actual Sending should take place

                        currentToast = Toast.makeText(MainActivity.this, "Clicked on Receive Sending msg permission granted", Toast.LENGTH_SHORT);
                        currentToast.show();

                        isSendingMsgAllowed = false;
                        btnPlay.setImageResource(R.drawable.ic_volume_up_orange_48dp);

                        break;
                }

                break;
            case SoniTalkContext.ON_REQUEST_DENIED:
                Log.e(TAG,"REQUEST DENIED");
                if (currentToast != null) {
                    currentToast.cancel();
                }

                // Checks the requestCode to adapt the UI depending on the action type (receiving or sending)
                switch (actionCode) {
                    case RECEIVING_REQUEST_CODE:
                        //showRequestPermissionExplanation(R.string.on_receiving_listening_permission_required);
                        // Set buttons in the state NOT RECEIVING
                        /*currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_receiving_listening_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();*/
                        isListeningMsgAllowed = true;

                        triStateToggleButton.setToggleMid(true);
                        currentToast = Toast.makeText(MainActivity.this, "Clicked on Pay Listening msg denied", Toast.LENGTH_SHORT);
                        currentToast.show();

                        btnListen.setImageResource(R.drawable.baseline_hearing_grey_48);

                        break;
                    case SENDING_REQUEST_CODE:
                        //showRequestPermissionExplanation(R.string.on_sending_sending_permission_required);
                        /*currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_sending_sending_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();*/

                        triStateToggleButton.setToggleMid(true);
                        currentToast = Toast.makeText(MainActivity.this, "Clicked on receive Sending Msg denied", Toast.LENGTH_SHORT);
                        currentToast.show();

                        isSendingMsgAllowed = true;

                        btnPlay.setImageResource(R.drawable.ic_volume_up_grey_48dp);

                }
                break;

            case SoniTalkContext.ON_SEND_JOB_FINISHED:
                //Message successfuly transmitted
                //USe this code block to resend the same msg

                Log.e(TAG,"ON SEND JOB FINISHED");
                isSendingMsgAllowed = true;
                btnPlay.setImageResource(R.drawable.ic_volume_up_grey_48dp);

                triStateToggleButton.setToggleMid(true);


                currentToast = Toast.makeText(MainActivity.this, "Message sent successfully", Toast.LENGTH_SHORT);
                currentToast.show();

                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int volume = ConfigConstants.SETTING_LOUDNESS_DEFAULT;
                //audioManager.setStreamVolume(3, volume, 0);
                audioManager.setStreamVolume(3, (int) Math.round((audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * volume / 100.0D)), 0);

                break;

            case SoniTalkContext.ON_SHOULD_SHOW_RATIONALE_FOR_ALLOW_ALWAYS:
                break;

            case SoniTalkContext.ON_REQUEST_L0_DENIED:
                Log.e(TAG, "ON_REQUEST_L0_DENIED");
                switch (actionCode) {
                    case RECEIVING_REQUEST_CODE:
                        showRequestPermissionExplanation(R.string.on_receiving_listening_permission_required);
                        isListeningMsgAllowed = true;
                        btnListen.setImageResource(R.drawable.baseline_hearing_grey_48);

                        break;
                    case SENDING_REQUEST_CODE:
                        showRequestPermissionExplanation(R.string.on_sending_sending_permission_required);
                        isSendingMsgAllowed = true;

                        btnPlay.setImageResource(R.drawable.ic_volume_up_grey_48dp);

                }
                break;

            default:

                Log.e(TAG, "onSoniTalkPermissionResult unknown resultCode: " + resultCode);
                break;

        }
    }
}


