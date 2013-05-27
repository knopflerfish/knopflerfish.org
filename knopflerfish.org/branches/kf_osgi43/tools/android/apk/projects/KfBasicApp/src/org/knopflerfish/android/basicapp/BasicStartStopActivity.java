package org.knopflerfish.android.basicapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.knopflerfish.android.service.Knopflerfish;
import org.knopflerfish.android.service.Knopflerfish.Msg;

public class BasicStartStopActivity extends Activity {

  private Messenger messenger;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // messenger for communication Knopflerfish -> Activity
    messenger = new Messenger(new Handler() {
      public void handleMessage(Message m) {
        Bundle reply = m.getData();
        Msg msg = (Msg) reply.getSerializable(Knopflerfish.MSG);
        String s = "";
        switch (msg) {
        case STARTING: s = "Framework is starting"; break;
        case NOP : s = "Framework is already running"; break;
        case STARTED:
          s = "Framework was started"; break;
        case NOT_STARTED: s = "Framework did not start!"; break;
        case STOPPED: s = "Framework was stopped"; break;
        case NOT_STOPPED: s = "Framework did not stop!"; break;
        }
        showToast(s);
      }
    });
    
    setContentView(R.layout.activity_main);
  }

  public void startKf(View view) {
    // start Knopflerfish service
    Intent intent = new Intent(this, Knopflerfish.class);
    intent.putExtra("messenger", messenger);
    startService(intent);   

    updateLinks(true);
  }
  
  public void startKfInit(View view) {
    // start Knopflerfish service with init action
    Intent intent = new Intent(this, Knopflerfish.class);
    intent.putExtra("messenger", messenger);
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
