package org.knopflerfish.android.basicapp;

import org.knopflerfish.android.service.Knopflerfish;
import org.knopflerfish.android.service.Knopflerfish.Msg;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BasicStartStopActivity extends Activity {

  private BroadcastReceiver receiver;
  private boolean registered;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // broadcast receiver that gets messages from Knopflerfish service
    if (receiver == null) {
      receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
          Bundle extras = intent.getExtras();
          Msg msg = (Msg) extras.getSerializable(Knopflerfish.MSG);
          String s = null;
          switch (msg) {
          case STARTING: s = "Framework is starting"; break;
          case STARTED: s = "Framework was started"; break;
          case NOP: s = "Framework is already started"; break;
          case NOT_STARTED: s = "Framework did not start!"; break;
          case STOPPED: s = "Framework was stopped"; break;
          case NOT_STOPPED: s = "Framework did not stop!"; break;
          }
          showToast(s);
        }
      };
    }

    setContentView(R.layout.activity_main);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!registered) {
      registerReceiver(receiver, new IntentFilter(Knopflerfish.class.getName()));
      registered = true;
    }
  }
  
  @Override
  public void onPause() {
    super.onPause();
    if (registered) {
      unregisterReceiver(receiver);
      registered = false;
    }
  }
  
  public void startKf(View view) {
    // start Knopflerfish service
    Intent intent = new Intent(this, Knopflerfish.class);
    startService(intent);   

    updateLinks(true);
  }
  
  public void startKfInit(View view) {
    // start Knopflerfish service with init action
    Intent intent = new Intent(this, Knopflerfish.class);
    intent.setAction(Knopflerfish.ACTION_INIT);
    startService(intent);
    
    updateLinks(true);
  }
	
  public void shutdownKf(View view) {
    // shutdown Knopflerfish
    Intent intent = new Intent(this, Knopflerfish.class);
    if (!stopService(intent)) {
      showToast("Framework is not running.");
    }
    
    updateLinks(false);
  }
  private void showToast(String msg) {
    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
  }

  private void updateLinks(boolean show)
  {
    String link1 = show ? getString(R.string.link1) : "";
    String link2 = show ? getString(R.string.link2) : "";
    
    TextView tw = (TextView) findViewById(R.id.textViewLink1);
    tw.setText(link1);
    tw = (TextView) findViewById(R.id.textViewLink2);
    tw.setText(link2);    
  }

}
