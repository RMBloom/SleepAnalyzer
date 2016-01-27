/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2015
 */

package com.interaxon.test.libmuse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
//kt:
import android.media.ToneGenerator;
import android.media.AudioManager;
    
import com.interaxon.libmuse.Accelerometer;
import com.interaxon.libmuse.AnnotationData;
import com.interaxon.libmuse.ConnectionState;
import com.interaxon.libmuse.Eeg;
import com.interaxon.libmuse.LibMuseVersion;
import com.interaxon.libmuse.MessageType;
import com.interaxon.libmuse.Muse;
import com.interaxon.libmuse.MuseArtifactPacket;
import com.interaxon.libmuse.MuseConfiguration;
import com.interaxon.libmuse.MuseConnectionListener;
import com.interaxon.libmuse.MuseConnectionPacket;
import com.interaxon.libmuse.MuseDataListener;
import com.interaxon.libmuse.MuseDataPacket;
import com.interaxon.libmuse.MuseDataPacketType;
import com.interaxon.libmuse.MuseFileFactory;
import com.interaxon.libmuse.MuseFileReader;
import com.interaxon.libmuse.MuseFileWriter;
import com.interaxon.libmuse.MuseManager;
import com.interaxon.libmuse.MusePreset;
import com.interaxon.libmuse.MuseVersion;

/*kt: new stuff
  HORSESHOE
  ALPHA_ABSOLUTE);
  ALPHA_SCORE);
  BETA_ABSOLUTE);
  BETA_RELATIVE);
  BETA_SCORE);
  DELTA_ABSOLUTE);
  DELTA_RELATIVE);
  DELTA_SCORE);
  GAMMA_ABSOLUTE);
  GAMMA_RELATIVE);
  GAMMA_SCORE);
  THETA_ABSOLUTE);
  THETA_RELATIVE);
  THETA_SCORE);
*/
// does not exist : import java.awt.Toolkit;



/**
 * In this simple example MainActivity implements 2 MuseHeadband listeners
 * and updates UI when data from Muse is received. Similarly you can implement
 * listers for other data or register same listener to listen for different type
 * of data.
 * For simplicity we create Listeners as inner classes of MainActivity. We pass
 * reference to MainActivity as we want listeners to update UI thread in this
 * example app.
 * You can also connect multiple muses to the same phone and register same
 * listener to listen for data from different muses. In this case you will
 * have to provide synchronization for data members you are using inside
 * your listener.
 *
 * Usage instructions:
 * 1. Enable bluetooth on your device
 * 2. Pair your device with muse
 * 3. Run this project
 * 4. Press Refresh. It should display all paired Muses in Spinner
 * 5. Make sure Muse headband is waiting for connection and press connect.
 * It may take up to 10 sec in some cases.
 * 6. You should see EEG and accelerometer data as well as connection status,
 * Version information and MuseElements (alpha, beta, theta, delta, gamma waves)
 * on the screen.
 */
public class MainActivity extends Activity implements OnClickListener {
    int waive_pkt_cnt = 0;
    int data_set_cnt = 0;
    long data_time_stamp = 0;
    long data_time_stamp_ref = 0;
    long pkt_timestamp  = 0;
    long pkt_timestamp_ref = 0;
    
    double[] eegElem = {0,0,0,0};
    double[] alphaAbsoluteElem = {0,0,0,0};
    double[] betaAbsoluteElem = {0,0,0,0};
    double[] deltaAbsoluteElem = {0,0,0,0};
    double[] gammaAbsoluteElem = {0,0,0,0};
    double[] thetaAbsoluteElem = {0,0,0,0};
    public int[]    horseshoeElem     = {0,0,0,0};
    //int artifactsElem = 0;
    
    // bit masks for packet types:
    public static final int AlphaAbsolute=1;
    public static final int BetaAbsolute  = 2;
    public static final int DeltaAbsolute = 4;
    public static final int GammaAbsolute = 8;
    public static final int ThetaAbsolute = 0x10;
    public static final int EegAbsolute   = 0x20;
    public static final int Horseshoe     = 0x80;
    //public static final int Artifacts    = 0x40;
   
    public static final int AllDataMask = AlphaAbsolute | BetaAbsolute |
	DeltaAbsolute | GammaAbsolute | ThetaAbsolute | EegAbsolute | Horseshoe; // | Artifacts;
   
    int got_data_mask=0;
    
    // kt: sound stuff
    public int tone_on_duration = 4000;
    public int tone_off_duration = 3000;
    public  static final int HorseshoeElemeToneMax = 4; // since there 4 sensors here, start with "completed playing all sounds" value
    public  static final int[] validSensor = {false, true, true, false };
    public  static final int[] HorseshoeTones =
    {
	ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE,
	ToneGenerator.TONE_PROP_BEEP2,
	ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE,   //ToneGenerator.TONE_PROP_BEEP,
	ToneGenerator.TONE_CDMA_ABBR_INTERCEPT,	
    };
    public int horseshoeElemeTone = 0;
    public int stopSounds = 0;
    /**
     * Connection listener updates UI with new connection status and logs it.
     */
    class ConnectionListener extends MuseConnectionListener {

        final WeakReference<Activity> activityRef;
	
        ConnectionListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(MuseConnectionPacket p) {
            final ConnectionState current = p.getCurrentConnectionState();
            final String status = p.getPreviousConnectionState().toString() +
                         " -> " + current;
            final String full = "Muse " + p.getSource().getMacAddress() +
                                " " + status;
            Log.i("Muse Headband kt", full);
            Activity activity = activityRef.get();
            // UI thread is used here only because we need to update
            // TextView values. You don't have to use another thread, unless
            // you want to run disconnect() or connect() from connection packet
            // handler. In this case creating another thread is required.
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView statusText =
                                (TextView) findViewById(R.id.con_status);
                        statusText.setText(status);
                        TextView museVersionText =
                                (TextView) findViewById(R.id.version);
                        if (current == ConnectionState.CONNECTED) {
                            MuseVersion museVersion = muse.getMuseVersion();
                            String version = museVersion.getFirmwareType() +
                                 " - " + museVersion.getFirmwareVersion() +
                                 " - " + Integer.toString(
                                    museVersion.getProtocolVersion());
                            museVersionText.setText(version);
                        } else {
                            museVersionText.setText(R.string.undefined);
                        }
                    }
		});
            }
        }
    }

    
    /**
     * Data listener will be registered to listen for: Accelerometer,
     * Eeg and Relative Alpha bandpower packets. In all cases we will
     * update UI with new values.
     * We also will log message if Artifact packets contains "blink" flag.
     * DataListener methods will be called from execution thread. If you are
     * implementing "serious" processing algorithms inside those listeners,
     * consider to create another thread.
     */
    class DataListener extends MuseDataListener {

        final WeakReference<Activity> activityRef;
        private MuseFileWriter fileWriter;

        DataListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(MuseDataPacket p) {
            switch (p.getPacketType()) {
		//	    case EEG:
		//		updateEeg(p.getValues());
		//		break;
	    case ACCELEROMETER:
		updateAccelerometer(p.getValues());
		break;
	    case ALPHA_RELATIVE:
		updateAlphaRelative(p.getValues());
		break;
	    case BATTERY:
		fileWriter.addDataPacket(1, p);
		// It's library client responsibility to flush the buffer,
		// otherwise you may get memory overflow. 
		if (fileWriter.getBufferedMessagesSize() > 8096)
		    fileWriter.flush();
		break;
	    case HORSESHOE:
	    case EEG:
	    case ALPHA_ABSOLUTE:
		//case ALPHA_SCORE:
	    case BETA_ABSOLUTE:
		// case BETA_RELATIVE:
		//case BETA_SCORE:
	    case DELTA_ABSOLUTE:
		//case DELTA_RELATIVE:
		//case DELTA_SCORE:
	    case GAMMA_ABSOLUTE:
		//case GAMMA_RELATIVE:
		//case GAMMA_SCORE:
	    case THETA_ABSOLUTE:
		//case THETA_RELATIVE:
		//case THETA_SCORE:
		handleWaivePacket(p, p.getValues());
                default:
                    break;
            }
        }

        @Override
        public void receiveMuseArtifactPacket(MuseArtifactPacket p) {
            if (p.getHeadbandOn() && p.getBlink()) {
                Log.i("Artifacts", "blink");
            }
        }
	private void handleWaivePacket(MuseDataPacket p, final ArrayList<Double> data) //(MuseDataPacket p)
	{
	    double elem1 = 0, elem2 = 0, elem3 = 0, elem4 = 0;
	    
	    //final ArrayList<Double> data = p.getValues();
	    boolean got_data = false;
	    if((pkt_timestamp == 0) || ( pkt_timestamp == -1))
	    {
		pkt_timestamp = p.getTimestamp();
		if(pkt_timestamp_ref == 0)
		{
		    pkt_timestamp_ref = pkt_timestamp;
		}
	    }
	    long tstamp = Calendar.getInstance().getTimeInMillis();
	    if(data_time_stamp_ref == 0)
		data_time_stamp_ref = tstamp; //save start time
	    
	    switch (p.getPacketType())
	    {
	    case EEG:
		eegElem[0] = data.get(Eeg.TP9.ordinal())/1000; // divide by 1000 to scale with alpha absolute, beta etc. signals
		eegElem[1] = data.get(Eeg.FP1.ordinal())/1000;
		eegElem[2] = data.get(Eeg.FP2.ordinal())/1000;
		eegElem[3] = data.get(Eeg.TP10.ordinal())/1000;
		got_data_mask |= EegAbsolute;
		break;
	    case ALPHA_ABSOLUTE:
		alphaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
		alphaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
		alphaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
		alphaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
		got_data_mask |= AlphaAbsolute;
		break;

	    case BETA_ABSOLUTE:
		betaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
		betaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
		betaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
		betaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
		got_data_mask |= BetaAbsolute;
		break;
		
	    case DELTA_ABSOLUTE:
		deltaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
		deltaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
		deltaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
		deltaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
		
		got_data_mask |= DeltaAbsolute;
		break;
		
	    case GAMMA_ABSOLUTE:
		gammaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
		gammaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
		gammaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
		gammaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());	
		got_data_mask  |= GammaAbsolute;

	    case THETA_ABSOLUTE:
		thetaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
		thetaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
		thetaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
		thetaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
		got_data_mask |= ThetaAbsolute;
		break;
		
	    case HORSESHOE:
		//int helem1 = 0, helem2 = 0, helem3 = 0, helem4 = 0;
		updateHorseshoe(data);
		elem1 = data.get(Eeg.TP9.ordinal());
		elem2 = data.get(Eeg.FP1.ordinal());
		elem3 = data.get(Eeg.FP2.ordinal());
		elem4 = data.get(Eeg.TP10.ordinal());
	
		horseshoeElem[0] = (int)elem1;
		horseshoeElem[1] = (int)elem2;
		horseshoeElem[2] = (int)elem3;
		horseshoeElem[3] = (int)elem4;
		got_data_mask   |= Horseshoe;
		//int tone_on_duration = 400;
		//int tone_off_duration = 400;
		/*
		try
		{
		if(elem1 > 2)
		{
		    ToneGenerator toneG1 = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
		    toneG1.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 100);
		    Thread.sleep(tone_on_duration);
		    toneG1.stopTone();
		    Thread.sleep(tone_off_duration);
		}
		if(elem2 > 2)
		{
		    ToneGenerator toneG2 = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
		    toneG2.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 100);
		    Thread.sleep(tone_on_duration);
		    toneG2.stopTone();
		    Thread.sleep(tone_off_duration);
		}
		if(elem3 > 2)
		{
		    ToneGenerator toneG3 = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
		    toneG3.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
		    Thread.sleep(tone_on_duration);
		    toneG3.stopTone();
		    Thread.sleep(tone_off_duration);
		}
		if(elem4 > 2)
		{
		    ToneGenerator toneG4 = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
		    toneG4.startTone(ToneGenerator.TONE_PROP_BEEP2, 50);
		    Thread.sleep(tone_on_duration);
		    toneG4.stopTone();
		    Thread.sleep(tone_off_duration);
		}
		} // end try
		catch(Exception e){}
		*/
		/*
		String strData = tstamp-data_time_stamp_ref + ",,,,," +
		    Integer.toHexString(helem1).toUpperCase() + "," +
		    Integer.toHexString(helem2).toUpperCase() + "," +
		    Integer.toHexString(helem3).toUpperCase() + "," +
		    Integer.toHexString(helem4).toUpperCase() + "\r\n";
		print_line.printf(strData);
		*/
		break;

	    }
	    // write/append data to file	    
	    if(got_data_mask == AllDataMask )
	    {
		long cur_tstamp = tstamp-data_time_stamp_ref;
		long cur_pkt_tstamp = pkt_timestamp - pkt_timestamp_ref;
		String strData = cur_pkt_tstamp + "," + cur_tstamp + "," +
		    eegElem          [0] + "," + eegElem          [3] + "," + eegElem          [1] + "," + eegElem          [2] + "," +
		    alphaAbsoluteElem[0] + "," + alphaAbsoluteElem[3] + "," + alphaAbsoluteElem[1] + "," + alphaAbsoluteElem[2] + "," +
		    betaAbsoluteElem [0] + "," + betaAbsoluteElem [3] + "," + betaAbsoluteElem [1] + "," + betaAbsoluteElem [2] + "," +
		    deltaAbsoluteElem[0] + "," + deltaAbsoluteElem[3] + "," + deltaAbsoluteElem[1] + "," + deltaAbsoluteElem[2] + "," +
		    gammaAbsoluteElem[0] + "," + gammaAbsoluteElem[3] + "," + gammaAbsoluteElem[1] + "," + gammaAbsoluteElem[2] + "," +
		    thetaAbsoluteElem[0] + "," + thetaAbsoluteElem[3] + "," + thetaAbsoluteElem[1] + "," + thetaAbsoluteElem[2] + "," +
		    horseshoeElem    [0] + "," + horseshoeElem    [3] + "," + horseshoeElem    [1] + "," + horseshoeElem    [2] + "\r\n";
		
		pkt_timestamp = 0; // for the next time
		waive_pkt_cnt++; //for debug
		//if(waive_pkt_cnt == 5) //for debug
		//    waive_pkt_cnt = 0;
		//if(elem1!= 0 && elem2!=0 && elem3!=0 && elem4!=0)
		{
		    print_line.printf(strData);
		    got_data_mask = 0;
		    data_set_cnt++;
		    //if (writeData.getBufferedMessagesSize() > 8096)
		    if(data_set_cnt == 5) // tune the final number
		    {
			//Log.i("Muse packet timestamp=",  String.valueOf(muse_tstamp));
			data_set_cnt = 0;
			try {
			    writeData.flush();
			}
			catch (Exception e) {
			    Log.e("Muse Headband exception", e.toString());
			}
		    }
		}
	    }
	}


        private void updateAccelerometer(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView acc_x = (TextView) findViewById(R.id.acc_x);
                        TextView acc_y = (TextView) findViewById(R.id.acc_y);
                        TextView acc_z = (TextView) findViewById(R.id.acc_z);
                        acc_x.setText(String.format(
                            "%6.2f", data.get(Accelerometer.FORWARD_BACKWARD.ordinal())));
                        acc_y.setText(String.format(
                            "%6.2f", data.get(Accelerometer.UP_DOWN.ordinal())));
                        acc_z.setText(String.format(
                            "%6.2f", data.get(Accelerometer.LEFT_RIGHT.ordinal())));
                    }
                });
            }
        }

        private void updateEeg(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                         TextView tp9 = (TextView) findViewById(R.id.eeg_tp9);
                         TextView fp1 = (TextView) findViewById(R.id.eeg_fp1);
                         TextView fp2 = (TextView) findViewById(R.id.eeg_fp2);
                         TextView tp10 = (TextView) findViewById(R.id.eeg_tp10);
			 
                         tp9.setText(String.format(
                            "%6.2f", data.get(Eeg.TP9.ordinal())));
                         fp1.setText(String.format(
                            "%6.2f", data.get(Eeg.FP1.ordinal())));
                         fp2.setText(String.format(
                            "%6.2f", data.get(Eeg.FP2.ordinal())));
                         tp10.setText(String.format(
                            "%6.2f", data.get(Eeg.TP10.ordinal())));
                    }
                });
            }
        }

        private void updateAlphaRelative(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                         TextView elem1 = (TextView) findViewById(R.id.elem1);
                         TextView elem2 = (TextView) findViewById(R.id.elem2);
                         TextView elem3 = (TextView) findViewById(R.id.elem3);
                         TextView elem4 = (TextView) findViewById(R.id.elem4);
                         elem1.setText(String.format(
                            "%6.2f", data.get(Eeg.TP9.ordinal())));
                         elem2.setText(String.format(
                            "%6.2f", data.get(Eeg.FP1.ordinal())));
                         elem3.setText(String.format(
                            "%6.2f", data.get(Eeg.FP2.ordinal())));
                         elem4.setText(String.format(
                            "%6.2f", data.get(Eeg.TP10.ordinal())));
                    }
                });
            }
        }

       private void updateHorseshoe(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                         TextView tp9 = (TextView) findViewById(R.id.eeg_tp9);
                         TextView fp1 = (TextView) findViewById(R.id.eeg_fp1);
                         TextView fp2 = (TextView) findViewById(R.id.eeg_fp2);
                         TextView tp10 = (TextView) findViewById(R.id.eeg_tp10);
                         tp9.setText(String.format(
                            "%6.2f", data.get(Eeg.TP9.ordinal())));
                         fp1.setText(String.format(
                            "%6.2f", data.get(Eeg.FP1.ordinal())));
                         fp2.setText(String.format(
                            "%6.2f", data.get(Eeg.FP2.ordinal())));
                         tp10.setText(String.format(
                            "%6.2f", data.get(Eeg.TP10.ordinal())));
                    }
                });
            }
        }

        public void setFileWriter(MuseFileWriter fileWriter) {
            this.fileWriter  = fileWriter;
        }
				 /*kt: not needed
	public void setFileWriterData(MuseFileWriter fileWriterData) {
            this.fileWriterData  = fileWriterData;
        }
	import java.time.LocalDateTime;			 */


    }

    private Muse muse = null;
    private ConnectionListener connectionListener = null;
    private DataListener dataListener = null;
    private boolean dataTransmission = true;
    private MuseFileWriter fileWriter = null;
    // private File fileData = null;
    private FileWriter writeData = null;
    private PrintWriter print_line = null;
    public MainActivity() {
        // Create listeners and pass reference to activity to them
        WeakReference<Activity> weakActivity =
                                new WeakReference<Activity>(this);

        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);
	//kt: temporary sound test
	Log.w("Muse Headband", "sound test start");
	stopSounds = 0;
	playAudioFeedback(1);
	Log.w("Muse Headband", "sound test done");
	// end audio test
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        Button pauseButton = (Button) findViewById(R.id.pause);
        pauseButton.setOnClickListener(this);
        // // Uncommet to test Muse File Reader
        //
        // // file can be big, read it in a separate thread
        // Thread thread = new Thread(new Runnable() {
        //     public void run() {
        //         playMuseFile("testfile.muse");
        //     }
        // });
        // thread.start();

	//kt: start the audio feedback thread
        Thread thread = new Thread(new Runnable() {
             public void run() {
		 stopSounds = 0;
                 playAudioFeedback(0);
             }
         });
         thread.start();

	
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        fileWriter = MuseFileFactory.getMuseFileWriter(
                     new File(dir, "new_muse_file.muse"));
        Log.i("Muse Headband", "libmuse version=" + LibMuseVersion.SDK_VERSION);
        fileWriter.addAnnotationString(1, "MainActivity onCreate");
        dataListener.setFileWriter(fileWriter);

	boolean append_to_file = false;
	//GettingCurrentDate gcd = new GettingCurrentDate();
	DateFormat df = new SimpleDateFormat("dd-MM-yy_HH:mm:ss");
	Date dateobj = new Date();
	//System.out.println(df.format(dateobj));
	String ldt = df.format( dateobj);
	//String ldt = gcd.getCurrentDateTime();
	try {
	    String fdname = dir+"/muse_data_file"+ ldt + ".csv";
	    writeData = new FileWriter( fdname, append_to_file);
	} catch (Exception e)
	{
	    Log.e("Muse Headband", e.toString());
	}
	print_line = new PrintWriter( writeData );
	//fileWriterData.addAnnotationString(1, "alpha, beta, delta, gamma, theta,");

	//String strData = "Time ms,AlphaAbs0,AlphaAbs1,AlphaAbs2,AlphaAbs3, BetaAbs0, BetaAbs1, BetaAbs2, BetaAbs3, DeltaAbs0, DeltaAbs1, DeltaAbs2, DeltaAbs3, GammaAbs0, GammaAbs1, GammaAbs2, GammaAbs3, ThetaAbs0, ThetaAbs1, ThetaAbs2, ThetaAbs3, Horseshoe0, Horseshoe1, Horseshoe2, Horseshoe4 " + "\r\n";
//Sensor1, Sensor2, Sensor3, Sensor4 " + "\r\n";
	//old setup: all sensors
	//String strData = "Packet time,Time ms,Eeg TP9,Eeg TP10,Eeg FP1,Eeg FP2,AlphaAbs TP9,AlphaAbs TP10,AlphaAbs FP1,AlphaAbs FP2,BetaAbs TP9,BetaAbs TP10,BetaAbs FP1,BetaAbs FP2,Delta TP9,Delta TP10,Delta FP1,Delta FP2,GammaAbs TP9,GammaAbs TP10,GammaAbs FP1,GammaAbs FP2,ThetaAbs TP9,ThetaAbs TP10,ThetaAbs FP1,ThetaAbs FP2,Horseshoe tp9,Horseshoe TP10,Horseshoe FP1,Horseshoe FP2 " + "\r\n";

	//new setu[p : only fp1, fp2 eeg sensors
	String strData = "Packet time,Time ms,Eeg FP1,Eeg FP2,AlphaAbs FP1,AlphaAbs FP2,BetaAbs FP1,BetaAbs FP2,Delta FP1,Delta FP2,GammaAbs FP1,GammaAbs FP2,ThetaAbs FP1,ThetaAbs FP2,,Horseshoe FP1,Horseshoe FP2 " + "\r\n";
	
	print_line.printf(strData);
    }

    @Override
    public void onClick(View v) {
        Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        if (v.getId() == R.id.refresh) {
            MuseManager.refreshPairedMuses();
            List<Muse> pairedMuses = MuseManager.getPairedMuses();
            List<String> spinnerItems = new ArrayList<String>();
            for (Muse m: pairedMuses) {
                String dev_id = m.getName() + "-" + m.getMacAddress();
                Log.i("Muse Headband", dev_id);
                spinnerItems.add(dev_id);
            }
            ArrayAdapter<String> adapterArray = new ArrayAdapter<String> (
                    this, android.R.layout.simple_spinner_item, spinnerItems);
            musesSpinner.setAdapter(adapterArray);
        }
        else if (v.getId() == R.id.connect) {
            List<Muse> pairedMuses = MuseManager.getPairedMuses();
            if (pairedMuses.size() < 1 ||
                musesSpinner.getAdapter().getCount() < 1) {
                Log.w("Muse Headband", "There is nothing to connect to");
            }
            else {
                muse = pairedMuses.get(musesSpinner.getSelectedItemPosition());
                ConnectionState state = muse.getConnectionState();
                if (state == ConnectionState.CONNECTED ||
                    state == ConnectionState.CONNECTING) {
                    Log.w("Muse Headband",
                    "doesn't make sense to connect second time to the same muse");
                    return;
                }
                configureLibrary();
                fileWriter.open();
                fileWriter.addAnnotationString(1, "Connect clicked");
		stopSounds = 0;

                /**
                 * In most cases libmuse native library takes care about
                 * exceptions and recovery mechanism, but native code still
                 * may throw in some unexpected situations (like bad bluetooth
                 * connection). Print all exceptions here.
                 */
                try {
                    muse.runAsynchronously();
                } catch (Exception e)
		{
                    Log.e("Muse Headband", e.toString());
                }
            }
        }
        else if (v.getId() == R.id.disconnect) {
            if (muse != null) {
                /**
                 * true flag will force libmuse to unregister all listeners,
                 * BUT AFTER disconnecting and sending disconnection event.
                 * If you don't want to receive disconnection event (for ex.
                 * you call disconnect when application is closed), then
                 * unregister listeners first and then call disconnect:
                 * muse.unregisterAllListeners();
                 * muse.disconnect(false);
                 */
		stopSounds = 1;
                muse.disconnect(true);
                fileWriter.addAnnotationString(1, "Disconnect clicked");
                fileWriter.flush();
                fileWriter.close();
		try {
		    writeData.flush();
		    writeData.close();
		}
		catch (Exception e) {
		    Log.e("Muse Headband exception", e.toString());
		}
            }
        }
        else if (v.getId() == R.id.pause) {
            dataTransmission = !dataTransmission;
            if (muse != null) {
                muse.enableDataTransmission(dataTransmission);
            }
        }
    }
    // kt:
    private void f_not_used(ToneGenerator toneGen, int toneIdx)
    {
	if(validSensor[toneIdx] == false)
	{
	    return;
	}
	
	ToneGenerator toneG = null;
	if(toneGen == null)
	    toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
	else
	    toneG = toneGen;
	
	toneG.startTone( HorseshoeTones[toneIdx]);
	try
	{
	    Thread.sleep(tone_on_duration/8);
	}
	catch(Exception e) {}
	toneG.stopTone();
	try
	{
	    Thread.sleep(tone_off_duration/2);
	}
	catch(Exception e) {}
    }
    
    private void playAudioFeedback(int at_mode) {
	int tone;
	int play_tone = 0;
	int i = 0;
	int k;
	// create generator once
	//ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

	// tone test
	int j;
	if(at_mode == 1)
	{
	    for(j=0; j<HorseshoeElemeToneMax; j++)
	    {
		for(k=0; k<2; k++) //play twice
	        {
		    musePlayTone(null, j);
		}
	    } // horseshoe for loop
	    return; // test done
	}
	// end test
	

	// normal operation
	Log.i("Muse Headband kt:", "Starting ToneGenerator");
	ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);

	// infinite - user can connect/disconnect multiple times without exiting the app
	while(true)
	{
	    if(stopSounds != 0)
	    {
		toneG.stopTone();
		toneG = null;
		Log.i("Muse Headband kt:", "Stopping ToneGenerator");
		//stopSounds = 0;
		// put thread to sleep
		try
		{
		    Thread.sleep(1000); //let CPU rest
		}
		catch(Exception e) {}
		continue;
	    }
	    
	    if(horseshoeElemeTone != HorseshoeElemeToneMax)
	    {
		i = horseshoeElemeTone;
	    }
	    // do not play souns if horseshoe is less than 3
	    // look for horseshoe less than 3
	    while(horseshoeElem[i] < 3) 
	    {
		i++;
		if(i == HorseshoeElemeToneMax)
		    break;	
	    }
	    if(i != HorseshoeElemeToneMax) // if none bad, no sound
	    {
		// we had bad sensor, play its sound
		musePlayTone(toneG, i);
	    }
	    // put thread to sleep
	    try
	    {
		Thread.sleep(1000); //let CPU rest
	    }
	    catch(Exception e) {}
	} // infinite lop
    }

   private void playAudioFeedback_apk(int at_mode)
  {
    int i = 0;
    if (at_mode == 1)
    {
      at_mode = 0;
      while (at_mode < 4)
      {
        i = 0;
        while (i < 2)
        {
          musePlayTone(null, at_mode);
          i += 1;
        }
        at_mode += 1;
      }
    }
    Log.i("Muse Headband kt:", "Starting ToneGenerator");
    ToneGenerator localToneGenerator = new ToneGenerator(4, 100);
    at_mode = i;
    for (;;)
    {
      if (this.stopSounds != 0)
      {
        localToneGenerator.stopTone();
        localToneGenerator = null;
        Log.i("Muse Headband kt:", "Stopping ToneGenerator");
        try
        {
          Thread.sleep(1000L);
        }
        catch (Exception localException1) {}
      }
      else
      {
        if (this.horseshoeElemeTone != 4) {
          at_mode = this.horseshoeElemeTone;
        }
        do
        {
          i = at_mode;
          if (this.horseshoeElem[at_mode] >= 3) {
            break;
          }
          i = at_mode + 1;
          at_mode = i;
        } while (i != 4);
        if (i != 4) {
          musePlayTone(localToneGenerator, i);
        }
        try
        {
          Thread.sleep(1000L);
          at_mode = i;
        }
        catch (Exception localException2)
        {
          at_mode = i;
        }
      }
    }
  }
   
    /*
     * Simple example of getting data from the "*.muse" file
     */
    private void playMuseFile(String name) {
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(dir, name);
        final String tag = "Muse File Reader";
        if (!file.exists()) {
            Log.w(tag, "file doesn't exist");
            return;
        }
        MuseFileReader fileReader = MuseFileFactory.getMuseFileReader(file);
        while (fileReader.gotoNextMessage()) {
            MessageType type = fileReader.getMessageType();
            int id = fileReader.getMessageId();
            long timestamp = fileReader.getMessageTimestamp();
            Log.i(tag, "type: " + type.toString() +
                  " id: " + Integer.toString(id) +
                  " timestamp: " + String.valueOf(timestamp));
            switch(type) {
                case EEG: case BATTERY: case ACCELEROMETER: case QUANTIZATION:
                    MuseDataPacket packet = fileReader.getDataPacket();
                    Log.i(tag, "data packet: " + packet.getPacketType().toString());
                    break;
                case VERSION:
                    MuseVersion version = fileReader.getVersion();
                    Log.i(tag, "version" + version.getFirmwareType());
                    break;
                case CONFIGURATION:
                    MuseConfiguration config = fileReader.getConfiguration();
                    Log.i(tag, "config" + config.getBluetoothMac());
                    break;
                case ANNOTATION:
                    AnnotationData annotation = fileReader.getAnnotation();
                    Log.i(tag, "annotation" + annotation.getData());
                    break;
                default:
                    break;
            }
        }
    }


    private void configureLibrary() {
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ACCELEROMETER);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.EEG);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ALPHA_RELATIVE);
	
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ARTIFACTS);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.BATTERY);
	//kt start:
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ALPHA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.BETA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.DELTA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.GAMMA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.THETA_ABSOLUTE);
	
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.HORSESHOE);
	//kt end
	
        muse.setPreset(MusePreset.PRESET_14);
        muse.enableDataTransmission(dataTransmission);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // kt: new stuff
   
  private void musePlayTone(ToneGenerator paramToneGenerator, int toneIdx)
  {
      if(validSensor[toneIdx] == false)
      {
	  return;
      }

    if (paramToneGenerator == null) {
	paramToneGenerator = new ToneGenerator(4, 100);
    }
    for (;;)
    {
      paramToneGenerator.startTone(HorseshoeTones[toneIdx]);
      try
      {
        Thread.sleep(this.tone_on_duration / 8);
        paramToneGenerator.stopTone();
        try
        {
          Thread.sleep(this.tone_off_duration / 2);
          return;
        }
        catch (Exception e)
	{
	    Log.e("Muse Headband exception", e.toString());
	}
      }
      catch (Exception e)
      {
	  Log.e("Muse Headband exception", e.toString());
      }
    }
  }
    
    private void playAudioFeedback_apk(int paramInt)
  {
    int i = 0;
    if (paramInt == 1)
    {
      paramInt = 0;
      while (paramInt < 4)
      {
        i = 0;
        while (i < 2)
        {
          musePlayTone(null, paramInt);
          i += 1;
        }
        paramInt += 1;
      }
    }
    Log.i("Muse Headband kt:", "Starting ToneGenerator");
    ToneGenerator localToneGenerator = new ToneGenerator(4, 100);
    paramInt = i;
    for (;;)
    {
      if (this.stopSounds != 0)
      {
        localToneGenerator.stopTone();
        localToneGenerator = null;
        Log.i("Muse Headband kt:", "Stopping ToneGenerator");
        try
        {
          Thread.sleep(1000L);
        }
        catch (Exception localException1) {}
      }
      else
      {
        if (this.horseshoeElemeTone != 4) {
          paramInt = this.horseshoeElemeTone;
        }
        do
        {
          i = paramInt;
          if (this.horseshoeElem[paramInt] >= 3) {
            break;
          }
          i = paramInt + 1;
          paramInt = i;
        } while (i != 4);
        if (i != 4) {
          musePlayTone(localToneGenerator, i);
        }
        try
        {
          Thread.sleep(1000L);
          paramInt = i;
        }
        catch (Exception localException2)
        {
          paramInt = i;
        }
      }
    }
  }

}
