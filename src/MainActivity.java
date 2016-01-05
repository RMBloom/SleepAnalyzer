/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2015
 */

package com.interaxon.test.libmuse;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.interaxon.libmuse.Accelerometer;
import com.interaxon.libmuse.ConnectionState;
import com.interaxon.libmuse.Eeg;
import com.interaxon.libmuse.LibMuseVersion;
import com.interaxon.libmuse.Muse;
import com.interaxon.libmuse.MuseArtifactPacket;
import com.interaxon.libmuse.MuseConnectionListener;
import com.interaxon.libmuse.MuseConnectionPacket;
import com.interaxon.libmuse.MuseDataListener;
import com.interaxon.libmuse.MuseDataPacket;
import com.interaxon.libmuse.MuseDataPacketType;
import com.interaxon.libmuse.MuseManager;
import com.interaxon.libmuse.MusePreset;
import com.interaxon.libmuse.MuseVersion;

/*kt: new stuff kt
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
    /**
     * Connection listener updates UI with new connection status and logs it.
     */
    class ConnectionListener extends MuseConnectionListener {

        final WeakReference<Activity> activityRef;

        ConnectionListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

	// file stuff
	FileWriter fstream = new FileWriter("MuseSleep_dateout.txt", true); //true tells to append data.
	outFile = new BufferedWriter(fstream);
	outFile.write("\nSleepMuse test write to file ");
	finally
	    {
		if(outFile != null) {
		    outFile.close();
		}
	    }
	//end of file stuff

        @Override
        public void receiveMuseConnectionPacket(MuseConnectionPacket p) {
            final ConnectionState current = p.getCurrentConnectionState();
            final String status = p.getPreviousConnectionState().toString() +
                         " -> " + current;
            final String full = "Muse " + p.getSource().getMacAddress() +
                                " " + status;
            Log.i("Muse Headband", full);
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
                }
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

        DataListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(MuseDataPacket p) {
            switch (p.getPacketType()) {
	    case EEG:
		updateEeg(p.getValues());
		break;
	    case ACCELEROMETER:
		updateAccelerometer(p.getValues());
		break;
	    case ALPHA_RELATIVE:
		updateAlphaRelative(p.getValues());
		break;
	    case ALPHA_ABSOLUTE:
	    case ALPHA_SCORE:
	    case BETA_ABSOLUTE:
	    case BETA_RELATIVE:
	    case BETA_SCORE:
	    case DELTA_ABSOLUTE:
	    case DELTA_RELATIVE:
	    case DELTA_SCORE:
	    case GAMMA_ABSOLUTE:
	    case GAMMA_RELATIVE:
	    case GAMMA_SCORE:
	    case THETA_ABSOLUTE:
	    case THETA_RELATIVE:
	    case THETA_SCORE:
		hadleWaivePacket(p);
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
	private  hadleWaivePacket(MuseDataPacket p);
	{
	    switch (p.getPacketType()) {
	    case ALPHA_ABSOLUTE:
		break;
	    case ALPHA_SCORE:
		break;
	    case BETA_ABSOLUTE:
		break;
	    case BETA_RELATIVE:
		break;
	    case BETA_SCORE:
		break;
	    case DELTA_ABSOLUTE:
		break;
	    case DELTA_RELATIVE:
		break;
	    case DELTA_SCORE:
		break;
	    case GAMMA_ABSOLUTE:
		break;
	    case GAMMA_RELATIVE:
		break;
	    case GAMMA_SCORE:
		break;
	    case THETA_ABSOLUTE:
		break;
	    case THETA_RELATIVE:
		break;
	    case THETA_SCORE:
		break;
	    }
	    // write/append data to file
	    // file name : <date>_museSleep.txt 02_25_2015
	    


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
    }

    private Muse muse = null;
    private ConnectionListener connectionListener = null;
    private DataListener dataListener = null;
    private boolean dataTransmission = true;

    public MainActivity() {
        // Create listeners and pass reference to activity to them
        WeakReference<Activity> weakActivity =
                                new WeakReference<Activity>(this);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);
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
        Log.i("Muse Headband", "libmuse version=" + LibMuseVersion.SDK_VERSION);
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
                    Log.w("Muse Headband", "doesn't make sense to connect second time to the same muse");
                    return;
                }
                configure_library();
                /**
                 * In most cases libmuse native library takes care about
                 * exceptions and recovery mechanism, but native code still
                 * may throw in some unexpected situations (like bad bluetooth
                 * connection). Print all exceptions here.
                 */
                try {
                    muse.runAsynchronously();
                } catch (Exception e) {
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
                muse.disconnect(true);
            }
        }
        else if (v.getId() == R.id.pause) {
            dataTransmission = !dataTransmission;
            if (muse != null) {
                muse.enableDataTransmission(dataTransmission);
            }
        }
    }

    private void configure_library() {
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ACCELEROMETER);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.EEG);
	/* kt: moved to below
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ALPHA_RELATIVE);
	*/
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ARTIFACTS);

	//kt: new stuff kt
	muse.registerDataListener(dataListener,
				  MuseDataPacketType.ALPHA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ALPHA_RELATIVE);
	muse.registerDataListener(dataListener,
				  MuseDataPacketType.ALPHA_SCORE);

	muse.registerDataListener(dataListener,
				  MuseDataPacketType.BETA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.BETA_RELATIVE);
	muse.registerDataListener(dataListener,
				  MuseDataPacketType.BETA_SCORE);

	muse.registerDataListener(dataListener,
				  MuseDataPacketType.DELTA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.DELTA_RELATIVE);
	muse.registerDataListener(dataListener,
				  MuseDataPacketType.DELTA_SCORE);

	muse.registerDataListener(dataListener,
				  MuseDataPacketType.GAMMA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.GAMMA_RELATIVE);
	muse.registerDataListener(dataListener,
				  MuseDataPacketType.GAMMA_SCORE);

	muse.registerDataListener(dataListener,
				  MuseDataPacketType.THETA_ABSOLUTE);
	muse.registerDataListener(dataListener,
                                  MuseDataPacketType.THETA_RELATIVE);
	muse.registerDataListener(dataListener,
				  MuseDataPacketType.THETA_SCORE);



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
}
