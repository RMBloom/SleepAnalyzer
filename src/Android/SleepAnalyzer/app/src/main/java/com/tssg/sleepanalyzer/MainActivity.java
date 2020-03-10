/*
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2016
 */

package com.tssg.sleepanalyzer;



import java.io.FileWriter;
import java.io.PrintWriter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

// kt:
import java.util.Calendar;
// kt:

import java.io.File;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import com.choosemuse.example.libmuse.R;
import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.AnnotationData;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.MessageType;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConfiguration;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseFileFactory;
import com.choosemuse.libmuse.MuseFileReader;
import com.choosemuse.libmuse.MuseFileWriter;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;
import com.choosemuse.libmuse.Result;
import com.choosemuse.libmuse.ResultLevel;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

//kt:
import android.media.ToneGenerator;
import android.media.AudioManager;

import static java.util.Locale.*;
// kt:

// kt:
/* new stuff
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
// kt:


/**
 * This example will illustrate how to connect to a Muse headband,
 * register for and receive EEG data and disconnect from the headband.
 * Saving EEG data to a .muse file is also covered.
 * <p>
 * For instructions on how to pair your headband with your Android device
 * please see:
 * http://developer.choosemuse.com/hardware-firmware/bluetooth-connectivity/developer-sdk-bluetooth-connectivity-2
 * <p>
 * Usage instructions:
 * 1. Pair your headband if necessary.
 * 2. Run this project.
 * 3. Turn on the Muse headband.
 * 4. Press "Refresh". It should display all paired Muses in the Spinner drop down at the
 * top of the screen.  It may take a few seconds for the headband to be detected.
 * 5. Select the headband you want to connect to and press "Connect".
 * 6. You should see EEG and accelerometer data as well as connection status,
 * version information and relative alpha values appear on the screen.
 * 7. You can pause/resume data transmission with the button at the bottom of the screen.
 * 8. To disconnect from the headband, press "Disconnect"
 */
public class MainActivity extends Activity implements OnClickListener {

	/**
	 * Tag used for logging purposes.
	 */
	protected final String TAG = getClass().getSimpleName();

	// smf
	boolean fromFile = false;

	/**
	 * The MuseManager is how you detect Muse headbands and receive notifications
	 * when the list of available headbands changes.
	 */
	private MuseManagerAndroid manager;

	/**
	 * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
	 * headband, register listeners to receive EEG data and get headband
	 * configuration and version information.
	 */
	private Muse muse;

	/**
	 * The ConnectionListener will be notified whenever there is a change in
	 * the connection state of a headband, for example when the headband connects
	 * or disconnects.
	 * <p>
	 * Note that ConnectionListener is an inner class at the bottom of this file
	 * that extends MuseConnectionListener.
	 */
	private ConnectionListener connectionListener;

	/**
	 * The DataListener is how you will receive EEG (and other) data from the
	 * headband.
	 * <p>
	 * Note that DataListener is an inner class at the bottom of this file
	 * that extends MuseDataListener.
	 */
	private DataListener dataListener;

	/**
	 * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
	 * depending on the type of headband and the preset configuration.  We buffer the
	 * data that is read until we can update the UI.
	 * <p>
	 * The stale flags indicate whether or not new data has been received and the buffers
	 * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
	 * and ACCELEROMETER values in this example.
	 * <p>
	 * Note: the array lengths of the buffers are taken from the comments in
	 * MuseDataPacketType, which specify 3 values for accelerometer and 6
	 * values for EEG and EEG-derived packets.
	 */
	private final double[] eegBuffer = new double[6];
	private boolean eegStale;
	private final double[] alphaBuffer = new double[6];
	private boolean alphaStale;
	private final double[] accelBuffer = new double[3];
	private boolean accelStale;

	/**
	 * We will be updating the UI using a handler instead of in packet handlers because
	 * packets come in at a very high frequency and it only makes sense to update the UI
	 * at about 60fps. The update functions do some string allocation, so this reduces our memory
	 * footprint and makes GC pauses less frequent/noticeable.
	 */
	private final Handler handler = new Handler();

	/**
	 * In the UI, the list of Muses you can connect to is displayed in a Spinner object for this example.
	 * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
	 */
	private ArrayAdapter<String> spinnerAdapter;

	/**
	 * It is possible to pause the data transmission from the headband.  This boolean tracks whether
	 * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
	 */
	private boolean dataTransmission = true;

	/**
	 * To save data to a file, you should use a MuseFileWriter.  The MuseFileWriter knows how to
	 * serialize the data packets received from the headband into a compact binary format.
	 * To read the file back, you would use a MuseFileReader.
	 */
	private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();

	// kt:
	private FileWriter writeData = null;
	private PrintWriter print_line = null;
	// kt:

	/**
	 * We don't want file operations to slow down the UI, so we will defer those file operations
	 * to a handler on a separate thread.
	 */
	private final AtomicReference<Handler> fileHandler = new AtomicReference<>();

	// kt:
	int waive_pkt_cnt = 0;
	int data_set_cnt = 0;
	long data_time_stamp_ref = 0;
	long pkt_timestamp = 0;
	long pkt_timestamp_ref = 0;

	double[] eegElem = {0, 0, 0, 0};
	double[] alphaAbsoluteElem = {0, 0, 0, 0};
	double[] betaAbsoluteElem = {0, 0, 0, 0};
	double[] deltaAbsoluteElem = {0, 0, 0, 0};
	double[] gammaAbsoluteElem = {0, 0, 0, 0};
	double[] thetaAbsoluteElem = {0, 0, 0, 0};

	public int[] horseshoeElem = {0, 0, 0, 0};

	// bit masks for packet types:
	public static final int AlphaAbsolute = 1;
	public static final int BetaAbsolute = 2;
	public static final int DeltaAbsolute = 4;
	public static final int GammaAbsolute = 8;
	public static final int ThetaAbsolute = 0x10;
	public static final int EegAbsolute = 0x20;
	public static final int Horseshoe = 0x80;

	public static final int AllDataMask = AlphaAbsolute |
			BetaAbsolute |
			DeltaAbsolute |
			GammaAbsolute |
			ThetaAbsolute |
			EegAbsolute |
			Horseshoe;

	int got_data_mask = 0;

	// sound stuff
	public int tone_on_duration = 4000;
	public int tone_off_duration = 3000;
	public static final int HorseshoeElemeToneMax = 4; // since there 4 sensors here, start with "completed playing all sounds" value
	public static final boolean[] validSensor = {false, true, true, false};
	public static final int[] HorseshoeTones =
			{
					ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE,
					ToneGenerator.TONE_PROP_BEEP2,
					ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE,   //ToneGenerator.TONE_PROP_BEEP,
					ToneGenerator.TONE_CDMA_ABBR_INTERCEPT,
			};
	public int horseshoeElemeTone = 0;
	public int stopSounds = 0;
	// kt:

	// smf:
	public static final Locale locale = ENGLISH;

	//--------------------------------------
	// Lifecycle / Connection code


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String method = "onCreate()";

		Log.i(TAG, method);

		// We need to set the context on MuseManagerAndroid before we can do anything.
		// This must come before other LibMuse API calls as it also loads the library.
		manager = MuseManagerAndroid.getInstance();
		manager.setContext(this);

		Log.d(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

		WeakReference<MainActivity> weakActivity = new WeakReference<>(this);

		// Register a listener to receive connection state changes.
		connectionListener = new ConnectionListener(weakActivity);
		// Register a listener to receive data from a Muse.
		dataListener = new DataListener(weakActivity);
		// Register a listener to receive notifications of what Muse headbands
		// we can connect to.
		manager.setMuseListener(new MuseL(weakActivity));

		// Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
		// simplify the connection process.  This requires access to the COARSE_LOCATION
		// or FINE_LOCATION permissions.  Make sure we have these permissions before
		// proceeding.
		ensurePermissions();

		// Load and initialize our UI.
		initUI();

		// Start up a thread for asynchronous file operations.
		// This is only needed if you want to do File I/O.
		fileThread.start();

		// Start our asynchronous updates of the UI.
		handler.post(tickUi);

		// RB
		// Get the external storage state
		String state = Environment.getExternalStorageState();

		// Storage Directory
		File fileDir;

		// External vs. Local storage
		if (Environment.MEDIA_MOUNTED.equals(state))
			// Get the external storage "/Download" directory path
			fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		else
			// Get the local storage directory path
			fileDir = Environment.getDataDirectory();

		// Construct the test file path
		final File sourceFile = new File(fileDir, "test_muse_file.muse");

		// Check if the file exists
		if (sourceFile.exists()) {

			// Get the test file name
			String sourceFileName = sourceFile.getName();

			Log.w(method, "playing test file: " + sourceFileName);

			// Set the flag
			fromFile = true;

			TextView statusText = (TextView) findViewById(R.id.con_status);
			statusText.setText("Reading from test file — "
								+ sourceFileName +
								"\nMuse headband connections are disabled!\n");
			statusText.setTypeface(null, Typeface.BOLD_ITALIC);

			// Disable the buttons and spinner when reading from a file
			findViewById(R.id.refresh).setEnabled(false);
			findViewById(R.id.connect).setEnabled(false);
			findViewById(R.id.disconnect).setEnabled(false);
			findViewById(R.id.pause).setEnabled(false);
			findViewById(R.id.muses_spinner).setEnabled(false);

			// file can be big, read it in a separate thread
			Thread playFileThread = new Thread(new Runnable() {
				public void run() {
					playMuseFile(sourceFile);
				}
			});
			playFileThread.setName("File Player");
			playFileThread.start();
		} else {
			Log.w(method, "test file doesn't exist");

			// Check for faulty API
			//	API 25 (7.1.1)
			// has problem with this audioFeedbackThread
			if (!android.os.Build.VERSION.RELEASE.startsWith("7.1.")) {
				//kt:
				// start the audio feedback thread
				Thread audioFeedbackThread = new Thread(new Runnable() {
					public void run() {
						stopSounds = 0;
						playAudioFeedback(0);
					}
				});
				audioFeedbackThread.setName("Audio Feedback");
				audioFeedbackThread.start();
				// kt:
			}

		}
		// RB

		// Check for faulty APIs
		//	API 17 (4.2.2) &
		//	API 18 (4.3.1) &
		//	API 25 (7.1.1)
		//	have faulty Tone Generator support
		if (!android.os.Build.VERSION.RELEASE.startsWith("4.2.") &&
			!android.os.Build.VERSION.RELEASE.startsWith("4.3.") &&
			!android.os.Build.VERSION.RELEASE.startsWith("7.1.")) {
			// kt: initial audio test
			Log.d("Muse Headband", "sound test start");
			stopSounds = 0;
			playAudioFeedback(1);
			Log.d("Muse Headband", "sound test done");
			// kt: end audio test
		}

	}    // end - onCreate()

	protected void onPause() {
		super.onPause();
		// It is important to call stopListening when the Activity is paused
		// to avoid a resource leak from the LibMuse library.
		manager.stopListening();
	}

//	public boolean isBluetoothEnabled() {
//		return BluetoothAdapter.getDefaultAdapter().isEnabled();
//	}

	@Override
	public void onClick(View v) {

		Log.i(TAG, "onClick()");

		if (v.getId() == R.id.refresh) {
			// The user has pressed the "Refresh" button.
			// Start listening for nearby or paired Muse headbands. We call stopListening
			// first to make sure startListening will clear the list of headbands and start fresh.
			manager.stopListening();
			manager.startListening();

		} else if (v.getId() == R.id.connect) {

			// The user has pressed the "Connect" button to connect to
			// the headband in the spinner.

			// Listening is an expensive operation, so now that we know
			// which headband the user wants to connect to we can stop
			// listening for other headbands.
			manager.stopListening();

			List<Muse> availableMuses = manager.getMuses();
			Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

			// Check that we actually have something to connect to.
			if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
				Log.d(TAG, "There is nothing to connect to");
			} else {

				// Cache the Muse that the user has selected.
				muse = availableMuses.get(musesSpinner.getSelectedItemPosition());
				// Unregister all prior listeners and register our data listener to
				// receive the MuseDataPacketTypes we are interested in.  If you do
				// not register a listener for a particular data type, you will not
				// receive data packets of that type.
				muse.unregisterAllListeners();
				muse.registerConnectionListener(connectionListener);
				muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
				muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
				muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
				muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
				muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
				muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);

				// Initiate a connection to the headband and stream the data asynchronously.
				muse.runAsynchronously();
			}

		} else if (v.getId() == R.id.disconnect) {

			// The user has pressed the "Disconnect" button.
			// Disconnect from the selected Muse.
			if (muse != null) {
				muse.disconnect();
			}

		} else if (v.getId() == R.id.pause) {

			// The user has pressed the "Pause/Resume" button to either pause or
			// resume data transmission.  Toggle the state and pause or resume the
			// transmission on the headband.
			if (muse != null) {
				dataTransmission = !dataTransmission;
				muse.enableDataTransmission(dataTransmission);
			}
		}
	}

	//--------------------------------------
	// Permissions

	/**
	 * The ACCESS_COARSE_LOCATION permission is required to use the
	 * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
	 * On an Android 6.0 device, the following code will display 2 dialogs,
	 * one to provide context and the second to request the permission.
	 * On an Android device running an earlier version, nothing is displayed
	 * as the permission is granted from the manifest.
	 * <p>
	 * If the permission is not granted, then Muse 2016 (MU-02) headbands will
	 * not be discovered and a SecurityException will be thrown.
	 */
	private void ensurePermissions() {

		Log.i(TAG, "ensurePermissions()");

		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
			// the user to grant us the permission.

			DialogInterface.OnClickListener buttonListener =
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							ActivityCompat.requestPermissions(MainActivity.this,
									new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
									0);
						}
					};

			// This is the context dialog which explains to the user the reason we are requesting
			// this permission.  When the user presses the positive (I Understand) button, the
			// standard Android permission dialog will be displayed (as defined in the button
			// listener above).
			AlertDialog introDialog = new AlertDialog.Builder(this)
					.setTitle(R.string.permission_dialog_title)
					.setMessage(R.string.permission_dialog_description)
					.setPositiveButton(R.string.permission_dialog_understand, buttonListener)
					.create();
			introDialog.show();
		}
	}


	//--------------------------------------
	// Listeners

	/**
	 * You will receive a callback to this method each time a headband is discovered.
	 * In this example, we update the spinner with the MAC address of the headband.
	 */
	public void museListChanged() {
		final List<Muse> list = manager.getMuses();
		spinnerAdapter.clear();
		for (Muse m : list) {
			spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
		}
	}

	/**
	 * You will receive a callback to this method each time there is a change to the
	 * connection state of one of the headbands.
	 *
	 * @param p    A packet containing the current and prior connection states
	 * @param muse The headband whose state changed.
	 */
	public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

		Log.i(TAG, "receiveMuseConnectionPacket()");

		final ConnectionState current = p.getCurrentConnectionState();

		// Format a message to show the change of connection state in the UI.
		final String status = p.getPreviousConnectionState() + " -> " + current;
		Log.d(TAG, status);

		// kt:
		// 1.3.0 final String full = "Muse " + p.getSource().getMacAddress() +" " + status;
		final String full = "Muse " + muse.getMacAddress() + " " + status;

		Log.d("Muse Headband kt", full);
		// kt:

		// Update the UI with the change in connection state.
		handler.post(new Runnable() {
			@Override
			public void run() {

				final TextView statusText = (TextView) findViewById(R.id.con_status);
				statusText.setText(status);

				final MuseVersion museVersion = muse.getMuseVersion();
				final TextView museVersionText = (TextView) findViewById(R.id.version);
				// If we haven't yet connected to the headband, the version information
				// will be null.  You have to connect to the headband before either the
				// MuseVersion or MuseConfiguration information is known.
				if (museVersion != null) {
					final String version = museVersion.getFirmwareType() + " - "
							+ museVersion.getFirmwareVersion() + " - "
							+ museVersion.getProtocolVersion();
					museVersionText.setText(version);
				} else {
					museVersionText.setText(R.string.undefined);
				}
			}
		});

		if (current == ConnectionState.DISCONNECTED) {
			Log.d(TAG, "Muse disconnected:" + muse.getName());
			// Save the data file once streaming has stopped.
			saveFile();
			// We have disconnected from the headband, so set our cached copy to null.
			this.muse = null;
		}
	}

	/**
	 * You will receive a callback to this method each time the headband sends a MuseDataPacket
	 * that you have registered.  You can use different listeners for different packet types or
	 * a single listener for all packet types as we have done here.
	 *
	 * @param p    The data packet containing the data from the headband (eg. EEG data)
	 * @param muse
	 */
	public void receiveMuseDataPacket(final MuseDataPacket p, Muse muse) {
		writeDataPacketToFile(p);

		// valuesSize returns the number of data values contained in the packet.
		final long n = p.valuesSize();

		if (fromFile) {
			switch (p.packetType()) {
				case EEG:
					if ((eegBuffer.length < n)) throw new AssertionError();
					getEegChannelValues(eegBuffer, p);
					eegStale = true;
					break;
				case ACCELEROMETER:
					if ((accelBuffer.length < n)) throw new AssertionError();
					getAccelValues(p);
					accelStale = true;
					break;
				case ALPHA_RELATIVE:
					if ((alphaBuffer.length < n)) throw new AssertionError();
					getEegChannelValues(alphaBuffer, p);
					alphaStale = true;
					break;
				case BATTERY:
				case DRL_REF:
				case QUANTIZATION:
				default:
					break;
			}
		} else {
			switch (p.packetType()) {
				// kt:
				/*
				case EEG:
					assert(eegBuffer.length >= n);
					getEegChannelValues(eegBuffer,p);
					eegStale = true;
					break;
				*/
				// kt:
				case ACCELEROMETER:
					if ((accelBuffer.length >= n)) throw new AssertionError();
					getAccelValues(p);
					accelStale = true;
					break;
				case ALPHA_RELATIVE:
					if ((alphaBuffer.length >= n)) throw new AssertionError();
					getEegChannelValues(alphaBuffer, p);
					alphaStale = true;
					break;
				// kt:
				// 1.3.0 case HORSESHOE:
				case HSI:
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
					// 1.3.0 handleWaivePacket(p, p.getValues());
					handleWaivePacket(p, p.values());
					// kt:
				case BATTERY:
				case DRL_REF:
				case QUANTIZATION:
				default:
					break;
			}
		}
	}

	// kt:
	private void handleWaivePacket(MuseDataPacket p, final ArrayList<Double> data) //(MuseDataPacket p)
	{
		double elem1, elem2, elem3, elem4;

		//final ArrayList<Double> data = p.getValues();
		if ((pkt_timestamp == 0) || (pkt_timestamp == -1)) {
			// 1.3.0 pkt_timestamp = p.getTimestamp();
			pkt_timestamp = p.timestamp();
			if (pkt_timestamp_ref == 0) {
				pkt_timestamp_ref = pkt_timestamp;
			}
		}
		long tstamp = Calendar.getInstance().getTimeInMillis();
		if (data_time_stamp_ref == 0)
			data_time_stamp_ref = tstamp; //save start time

		// 1.3.0 switch (p.getPacketType())
		switch (p.packetType()) {
			case EEG:
				// 1.3.0 eegElem[0] = data.get(Eeg.TP9.ordinal())/1000; // divide by 1000 to scale with alpha absolute, beta etc. signals
				eegElem[0] = data.get(Eeg.EEG1.ordinal()) / 1000; // divide by 1000 to scale with alpha absolute, beta etc. signals
				// 1.3.0 eegElem[1] = data.get(Eeg.FP1.ordinal())/1000;
				eegElem[1] = data.get(Eeg.EEG2.ordinal()) / 1000;
				// 1.3.0 eegElem[2] = data.get(Eeg.FP2.ordinal())/1000;
				eegElem[2] = data.get(Eeg.EEG3.ordinal()) / 1000;
				// 1.3.0 eegElem[3] = data.get(Eeg.TP10.ordinal())/1000;
				eegElem[3] = data.get(Eeg.EEG4.ordinal()) / 1000;
				got_data_mask |= EegAbsolute;
				break;
			case ALPHA_ABSOLUTE:
				// 1.3.0 alphaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
				alphaAbsoluteElem[0] = data.get(Eeg.EEG1.ordinal());
				// 1.3.0 alphaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
				alphaAbsoluteElem[1] = data.get(Eeg.EEG2.ordinal());
				// 1.3.0 alphaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
				alphaAbsoluteElem[2] = data.get(Eeg.EEG3.ordinal());
				// 1.3.0 alphaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
				alphaAbsoluteElem[3] = data.get(Eeg.EEG4.ordinal());
				got_data_mask |= AlphaAbsolute;
				break;

			case BETA_ABSOLUTE:
				// 1.3.0 betaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
				betaAbsoluteElem[0] = data.get(Eeg.EEG1.ordinal());
				// 1.3.0 betaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
				betaAbsoluteElem[1] = data.get(Eeg.EEG2.ordinal());
				// 1.3.0 betaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
				betaAbsoluteElem[2] = data.get(Eeg.EEG3.ordinal());
				// 1.3.0 betaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
				betaAbsoluteElem[3] = data.get(Eeg.EEG4.ordinal());
				got_data_mask |= BetaAbsolute;
				break;

			case DELTA_ABSOLUTE:
				// 1.3.0 deltaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
				deltaAbsoluteElem[0] = data.get(Eeg.EEG1.ordinal());
				// 1.3.0 deltaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
				deltaAbsoluteElem[1] = data.get(Eeg.EEG2.ordinal());
				// 1.3.0 deltaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
				deltaAbsoluteElem[2] = data.get(Eeg.EEG3.ordinal());
				// 1.3.0 deltaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
				deltaAbsoluteElem[3] = data.get(Eeg.EEG4.ordinal());

				got_data_mask |= DeltaAbsolute;
				break;

			case GAMMA_ABSOLUTE:
				// 1.3.0 gammaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
				gammaAbsoluteElem[0] = data.get(Eeg.EEG1.ordinal());
				// 1.3.0 gammaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
				gammaAbsoluteElem[1] = data.get(Eeg.EEG2.ordinal());
				// 1.3.0 gammaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
				gammaAbsoluteElem[2] = data.get(Eeg.EEG3.ordinal());
				// 1.3.0 gammaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
				gammaAbsoluteElem[3] = data.get(Eeg.EEG4.ordinal());
				got_data_mask |= GammaAbsolute;

			case THETA_ABSOLUTE:
				// 1.3.0 thetaAbsoluteElem[0] = data.get(Eeg.TP9.ordinal());
				thetaAbsoluteElem[0] = data.get(Eeg.EEG1.ordinal());
				// 1.3.0 thetaAbsoluteElem[1] = data.get(Eeg.FP1.ordinal());
				thetaAbsoluteElem[1] = data.get(Eeg.EEG2.ordinal());
				// 1.3.0 thetaAbsoluteElem[2] = data.get(Eeg.FP2.ordinal());
				thetaAbsoluteElem[2] = data.get(Eeg.EEG3.ordinal());
				// 1.3.0 thetaAbsoluteElem[3] = data.get(Eeg.TP10.ordinal());
				thetaAbsoluteElem[3] = data.get(Eeg.EEG4.ordinal());
				got_data_mask |= ThetaAbsolute;
				break;

			// 1.3.0 case HORSESHOE:
			case HSI:
				//int helem1 = 0, helem2 = 0, helem3 = 0, helem4 = 0;
				updateHorseshoe(data);
				// 1.3.0 elem1 = data.get(Eeg.TP9.ordinal());
				elem1 = data.get(Eeg.EEG1.ordinal());
				// 1.3.0 elem2 = data.get(Eeg.FP1.ordinal());
				elem2 = data.get(Eeg.EEG2.ordinal());
				// 1.3.0 elem3 = data.get(Eeg.FP2.ordinal());
				elem3 = data.get(Eeg.EEG3.ordinal());
				// 1.3.0 elem4 = data.get(Eeg.TP10.ordinal());
				elem4 = data.get(Eeg.EEG4.ordinal());

				if (validSensor[0])
					horseshoeElem[0] = (int) elem1;
				if (validSensor[1])
					horseshoeElem[1] = (int) elem2;
				if (validSensor[2])
					horseshoeElem[2] = (int) elem3;
				if (validSensor[3])
					horseshoeElem[3] = (int) elem4;

				got_data_mask |= Horseshoe;
				Log.i("kt: hrs data ", Integer.toString(waive_pkt_cnt));

				break;

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

		}

		// write/append data to file
		if (got_data_mask == AllDataMask) {
			long cur_tstamp = tstamp - data_time_stamp_ref;
			long cur_pkt_tstamp = pkt_timestamp - pkt_timestamp_ref;
			String strData = cur_pkt_tstamp + "," + cur_tstamp + "," +
					eegElem[0]           + "," + eegElem[3]           + "," + eegElem[1]                 + "," + eegElem[2]     + "," +
					alphaAbsoluteElem[0] + "," + alphaAbsoluteElem[3] + "," + alphaAbsoluteElem[1] + "," + alphaAbsoluteElem[2] + "," +
					betaAbsoluteElem[0]  + "," + betaAbsoluteElem[3]  + "," + betaAbsoluteElem[1]  + "," + betaAbsoluteElem[2]  + "," +
					deltaAbsoluteElem[0] + "," + deltaAbsoluteElem[3] + "," + deltaAbsoluteElem[1] + "," + deltaAbsoluteElem[2] + "," +
					gammaAbsoluteElem[0] + "," + gammaAbsoluteElem[3] + "," + gammaAbsoluteElem[1] + "," + gammaAbsoluteElem[2] + "," +
					thetaAbsoluteElem[0] + "," + thetaAbsoluteElem[3] + "," + thetaAbsoluteElem[1] + "," + thetaAbsoluteElem[2] + "," +
					horseshoeElem[0]     + "," + horseshoeElem[3]     + "," + horseshoeElem[1]     + "," + horseshoeElem[2]     + "\r\n";

			pkt_timestamp = 0; // for the next time
			waive_pkt_cnt++; // for debug
			//if(waive_pkt_cnt == 5) //for debug
			//    waive_pkt_cnt = 0;
			//if(elem1!= 0 && elem2!=0 && elem3!=0 && elem4!=0)
			{
				print_line.printf(strData);
				got_data_mask = 0;
				data_set_cnt++;
				//if (writeData.getBufferedMessagesSize() > 8096)
				if (data_set_cnt == 5) // tune the final number
				{
					//Log.i("Muse packet timestamp=",  String.valueOf(muse_tstamp));
					data_set_cnt = 0;
					try {
						writeData.flush();
					} catch (Exception e) {
						Log.e("Muse Headband exception", e.toString());
					}
				}
			}
		}
	}
	// kt:

	// kt:
	private void updateHorseshoe(final ArrayList<Double> data) {

		//Activity activity = activityRef.get();
		Activity activity = (Activity) this.getApplicationContext();

		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					//TextView tp9 = (TextView) findViewById(R.id.eeg_tp9);
					// 1.3.0 TextView fp1 = (TextView) findViewById(R.id.eeg_fp1);
					TextView fp1 = (TextView) findViewById(R.id.eeg_af7);
					// 1.3.0 TextView fp2 = (TextView) findViewById(R.id.eeg_fp2);
					TextView fp2 = (TextView) findViewById(R.id.eeg_af8);
					//TextView tp10 = (TextView) findViewById(R.id.eeg_tp10);
					//tp9.setText(String.format("%6.2f", data.get(Eeg.TP9.ordinal())));
					// 1.3.0 fp1.setText(String.format("%6.2f", data.get(Eeg.FP1.ordinal())));
					fp1.setText(String.format(locale,"%6.2f", data.get(Eeg.EEG2.ordinal())));
					// 1.3.0 fp2.setText(String.format("%6.2f", data.get(Eeg.FP2.ordinal())));
					fp2.setText(String.format(locale,"%6.2f", data.get(Eeg.EEG3.ordinal())));
					//tp10.setText(String.format("%6.2f", data.get(Eeg.TP10.ordinal())));
				}
			});
		}
	}
	// kt:


	/**
	 * You will receive a callback to this method each time an artifact packet is generated if you
	 * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
	 * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
	 *
	 */
	public void receiveMuseArtifactPacket(MuseArtifactPacket p, final Muse muse) {
	}

	/**
	 * Helper methods to get different packet values.  These methods simply store the
	 * data in the buffers for later display in the UI.
	 * <p>
	 * getEegChannelValue can be used for any EEG or EEG derived data packet type
	 * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
	 * of MuseDataPacketType for all of the available values.
	 * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
	 * getValue methods.
	 */
	private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
		buffer[0] = p.getEegChannelValue(Eeg.EEG1);
		buffer[1] = p.getEegChannelValue(Eeg.EEG2);
		buffer[2] = p.getEegChannelValue(Eeg.EEG3);
		buffer[3] = p.getEegChannelValue(Eeg.EEG4);
		buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
		buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
	}

	private void getAccelValues(MuseDataPacket p) {
		accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
		accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
		accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
	}


	//--------------------------------------
	// UI Specific methods

	/**
	 * Initializes the UI of the example application.
	 */
	private void initUI() {
		setContentView(R.layout.activity_main);
		Button refreshButton = (Button) findViewById(R.id.refresh);
		refreshButton.setOnClickListener(this);
		Button connectButton = (Button) findViewById(R.id.connect);
		connectButton.setOnClickListener(this);
		Button disconnectButton = (Button) findViewById(R.id.disconnect);
		disconnectButton.setOnClickListener(this);
		Button pauseButton = (Button) findViewById(R.id.pause);
		pauseButton.setOnClickListener(this);

		spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
		Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
		musesSpinner.setAdapter(spinnerAdapter);
	}

	/**
	 * The runnable that is used to update the UI at 60Hz.
	 * <p>
	 * We update the UI from this Runnable instead of in packet handlers
	 * because packets come in at high frequency -- 220Hz or more for raw EEG
	 * -- and it only makes sense to update the UI at about 60fps. The update
	 * functions do some string allocation, so this reduces our memory
	 * footprint and makes GC pauses less frequent/noticeable.
	 */
	private final Runnable tickUi = new Runnable() {
		@Override
		public void run() {
			if (eegStale) {
				updateEeg();
			}
			if (accelStale) {
				updateAccel();
			}
			if (alphaStale) {
				updateAlpha();
			}
			handler.postDelayed(tickUi, 1000 / 60);
		}
	};

	/**
	 * The following methods update the TextViews in the UI with the data
	 * from the buffers.
	 */
	private void updateAccel() {
		TextView acc_x = (TextView) findViewById(R.id.acc_x);
		TextView acc_y = (TextView) findViewById(R.id.acc_y);
		TextView acc_z = (TextView) findViewById(R.id.acc_z);
		acc_x.setText(String.format(locale, "%6.2f", accelBuffer[0]));
		acc_y.setText(String.format(locale, "%6.2f", accelBuffer[1]));
		acc_z.setText(String.format(locale, "%6.2f", accelBuffer[2]));
	}

	private void updateEeg() {
		TextView tp9 = (TextView) findViewById(R.id.eeg_tp9);
		TextView fp1 = (TextView) findViewById(R.id.eeg_af7);
		TextView fp2 = (TextView) findViewById(R.id.eeg_af8);
		TextView tp10 = (TextView) findViewById(R.id.eeg_tp10);
		tp9.setText(String.format(locale, "%6.2f", eegBuffer[0]));
		fp1.setText(String.format(locale, "%6.2f", eegBuffer[1]));
		fp2.setText(String.format(locale, "%6.2f", eegBuffer[2]));
		tp10.setText(String.format(locale, "%6.2f", eegBuffer[3]));
	}

	private void updateAlpha() {
		TextView elem1 = (TextView) findViewById(R.id.elem1);
		elem1.setText(String.format(locale, "%6.2f", alphaBuffer[0]));
		TextView elem2 = (TextView) findViewById(R.id.elem2);
		elem2.setText(String.format(locale, "%6.2f", alphaBuffer[1]));
		TextView elem3 = (TextView) findViewById(R.id.elem3);
		elem3.setText(String.format(locale, "%6.2f", alphaBuffer[2]));
		TextView elem4 = (TextView) findViewById(R.id.elem4);
		elem4.setText(String.format(locale, "%6.2f", alphaBuffer[3]));
	}


	//--------------------------------------
	// File I/O

	/**
	 * We don't want to block the UI thread while we write to a file, so the file
	 * writing is moved to a separate thread.
	 */
	private final Thread fileThread = new Thread() {
		@Override
		public void run() {
			Looper.prepare();
			fileHandler.set(new Handler());
			final File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
			final File file = new File(dir, "new_muse_file.muse");
			// MuseFileWriter will append to an existing file.
			// In this case, we want to start fresh so the file
			// if it exists.
			if (file.exists()) if (file.delete()) {
				Log.d(TAG, "Deleted " + file.getAbsolutePath());
			}
			Log.d(TAG, "Writing data to: " + file.getAbsolutePath());
			fileWriter.set(MuseFileFactory.getMuseFileWriter(file));
			Looper.loop();
		}
	};

	/**
	 * Writes the provided MuseDataPacket to the file.  MuseFileWriter knows
	 * how to write all packet types generated from LibMuse.
	 *
	 * @param p The data packet to write.
	 */
	private void writeDataPacketToFile(final MuseDataPacket p) {
		Handler h = fileHandler.get();
		if (h != null) {
			h.post(new Runnable() {
				@Override
				public void run() {
					fileWriter.get().addDataPacket(0, p);
				}
			});
		}
	}

	/**
	 * Flushes all the data to the file and closes the file writer.
	 */
	private void saveFile() {
		Handler h = fileHandler.get();
		if (h != null) {
			h.post(new Runnable() {
				@Override
				public void run() {
					MuseFileWriter w = fileWriter.get();
					// Annotation strings can be added to the file to
					// give context as to what is happening at that point in
					// time.  An annotation can be an arbitrary string or
					// may include additional AnnotationData.
					w.addAnnotationString(0, "Disconnected");
					w.flush();
					w.close();
				}
			});
		}
	}

	// kt:
	private void playAudioFeedback(int at_mode) {
		//int tone;
		//int play_tone = 0;
		int i = 0;
		int k;

		// tone test
		int j;
		if (at_mode == 1) {
			for (j = 0; j < HorseshoeElemeToneMax; j++) {
				for (k = 0; k < 2; k++) //play twice
				{
					musePlayTone(null, j);
				}
			} // horseshoe for loop
			return; // test done
		}
		// end tone test


		// normal operation
		Log.i("Muse Headband kt:", "Starting ToneGenerator");
		ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

		// infinite - user can connect/disconnect multiple times without exiting the app
		while (true) {
			if (stopSounds != 0) {
				if (toneG != null) {
					toneG.stopTone();
					toneG = null;
					Log.i("Muse Headband kt:", "Stopping ToneGenerator");
				}
				// put thread to sleep
				try {
					Thread.sleep(1000); //let CPU rest
				} catch (Exception e) {
					Log.e(TAG, "Error stopping the Tone generator");
				}
				continue;
			}

			if (horseshoeElemeTone != HorseshoeElemeToneMax) {
				i = horseshoeElemeTone;
			}
			// do not play sounds if horseshoe is less than 3
			// look for horseshoe less than 3
			while (horseshoeElem[i] < 3) {
				i++;
				if (i == HorseshoeElemeToneMax)
					break;
			}
			// we are here either becasue we scanned to the end of the array
			// or we found a bad sensor
			if (i != HorseshoeElemeToneMax) // if none bad, no sound
			{
				// we had bad sensor, play its sound
				musePlayTone(toneG, i);
			} else {
				horseshoeElemeTone = 0; //restart scanning from the start of array
			}
			// put thread to sleep
			try {
				Thread.sleep(1000); //let CPU rest
			} catch (Exception e) {
				Log.e(TAG, "Unable to put thread to sleep");
			}
		} // infinite loop
	}
	// kt:


	// smf
	private void playMuseFile(File fileName) {

		final String tag = "playMuseFile()";

		if (!fileName.exists()) {
			Log.w(tag, "file doesn't exist");
			return;
		}

		// Get a file reader
		MuseFileReader fileReader = MuseFileFactory.getMuseFileReader(fileName);

		// Loop through the packages in the file
		Result res = fileReader.gotoNextMessage();
		while (res.getLevel() == ResultLevel.R_INFO && !res.getInfo().contains("EOF")) {
			MessageType type = fileReader.getMessageType();
			int id = fileReader.getMessageId();
			long timestamp = fileReader.getMessageTimestamp();
			Log.d(tag, "type: " + type.toString() +
					        " id: "  + id +
					        " timestamp: " + timestamp);
			switch (type) {
				case EEG:
				case BATTERY:
				case ACCELEROMETER:
				case QUANTIZATION:
					MuseDataPacket packet = fileReader.getDataPacket();
					Log.d(tag, "data packet: " + packet.packetType().toString());
					dataListener.receiveMuseDataPacket(packet, muse);
					break;
				case VERSION:
					MuseVersion museVersion = fileReader.getVersion();
					final String version = museVersion.getFirmwareType() +
							" - " + museVersion.getFirmwareVersion() +
							" - " + museVersion.getProtocolVersion();
					Log.d(tag, "version " + version);
					Activity activity = dataListener.activityRef.get();
					// UI thread is used here only because we need to update
					// TextView values. You don't have to use another thread, unless
					// you want to run disconnect() or connect() from connection packet
					// handler. In this case creating another thread is required.
					if (activity != null) {
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								TextView museVersionText =
										(TextView) findViewById(R.id.version);
								museVersionText.setText(version);
							}
						});
					}
					break;
				case CONFIGURATION:
					MuseConfiguration config = fileReader.getConfiguration();
					Log.d(tag, "config " + config.getBluetoothMac());
					break;
				case ANNOTATION:
					AnnotationData annotation = fileReader.getAnnotation();
					Log.d(tag, "annotationData " + annotation.getData());
					Log.d(tag, "annotationFormat " + annotation.getFormat().toString());
					Log.d(tag, "annotationEventType " + annotation.getEventType());
					Log.d(tag, "annotationEventId " + annotation.getEventId());
					Log.d(tag, "annotationParentId " + annotation.getParentId());
					break;
				default:
					break;
			}

			// Read the next message.
			res = fileReader.gotoNextMessage();
		}
	}
	// smf

	/**
	 * Reads the provided .muse file and prints the data to the logcat.
	 *
	 * @param name The name of the file to read.  The file in this example
	 *             is assumed to be in the Environment.DIRECTORY_DOWNLOADS
	 *             directory.
	 */
	private void playMuseFile(String name) {

		final String tag = "playMuseFile()";

		// Get the "/DOWNLOAD" directory path
		File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		File file = new File(dir, name);

		if (!file.exists()) {
			Log.w(tag, "file doesn't exist");
			return;
		}

		// Define a file reader
		MuseFileReader fileReader = MuseFileFactory.getMuseFileReader(file);

		// Loop through each message in the file.  gotoNextMessage will read the next message
		// and return the result of the read operation as a Result.
		Result res = fileReader.gotoNextMessage();
		while (res.getLevel() == ResultLevel.R_INFO && !res.getInfo().contains("EOF")) {

			MessageType type = fileReader.getMessageType();
			int id = fileReader.getMessageId();
			long timestamp = fileReader.getMessageTimestamp();

			Log.i(tag, "type: " + type.toString() +
					        " id: " + id +
					        " timestamp: " + timestamp);

			switch (type) {
				// EEG messages contain raw EEG data or DRL/REF data.
				// EEG derived packets like ALPHA_RELATIVE and artifact packets
				// are stored as MUSE_ELEMENTS messages.
				case EEG:
				case BATTERY:
				case ACCELEROMETER:
				case QUANTIZATION:
				case GYRO:
				case MUSE_ELEMENTS:
					MuseDataPacket packet = fileReader.getDataPacket();
					Log.i(tag, "data packet: " + packet.packetType().toString());
					break;
				case VERSION:
					MuseVersion version = fileReader.getVersion();
					Log.i(tag, "version: " + version.getFirmwareType());
					break;
				case CONFIGURATION:
					MuseConfiguration config = fileReader.getConfiguration();
					Log.i(tag, "config: " + config.getBluetoothMac());
					break;
				case ANNOTATION:
					AnnotationData annotation = fileReader.getAnnotation();
					Log.i(tag, "annotation: " + annotation.getData());
					break;
				default:
					break;
			}

			// Read the next message.
			res = fileReader.gotoNextMessage();
		}
	}

	//--------------------------------------
	// Listener translators
	//
	// Each of these classes extend from the appropriate listener and contain a weak reference
	// to the activity.  Each class simply forwards the messages it receives back to the Activity.
	static class MuseL extends MuseListener {
		final WeakReference<MainActivity> activityRef;

		MuseL(final WeakReference<MainActivity> activityRef) {
			this.activityRef = activityRef;
		}

		@Override
		public void museListChanged() {
			activityRef.get().museListChanged();
		}
	}

	static class ConnectionListener extends MuseConnectionListener {
		final WeakReference<MainActivity> activityRef;

		ConnectionListener(final WeakReference<MainActivity> activityRef) {
			this.activityRef = activityRef;
		}

		@Override
		public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
			activityRef.get().receiveMuseConnectionPacket(p, muse);
		}
	}

	static class DataListener extends MuseDataListener {
		final WeakReference<MainActivity> activityRef;

		DataListener(final WeakReference<MainActivity> activityRef) {
			this.activityRef = activityRef;
		}

		@Override
		public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
			activityRef.get().receiveMuseDataPacket(p, muse);
		}

		@Override
		public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
			activityRef.get().receiveMuseArtifactPacket(p, muse);
		}
	}

	// kt: new stuff
	private void musePlayTone(ToneGenerator paramToneGenerator, int toneIdx) {
		if (validSensor[toneIdx]) {
			if (paramToneGenerator == null) {
				paramToneGenerator = new ToneGenerator(4, 100);
			}
			for (; ; ) {
				paramToneGenerator.startTone(HorseshoeTones[toneIdx]);
				try {
					Thread.sleep(this.tone_on_duration / 8);
					paramToneGenerator.stopTone();
					try {
						Thread.sleep(this.tone_off_duration / 2);
						return;
					} catch (Exception e) {
						Log.e("Muse Headband exception", e.toString());
					}
				} catch (Exception e) {
					Log.e("Muse Headband exception", e.toString());
				}
			}
		}
	}
	// kt:

}
