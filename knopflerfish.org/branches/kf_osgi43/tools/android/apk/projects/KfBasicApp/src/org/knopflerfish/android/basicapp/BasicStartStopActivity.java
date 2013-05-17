package org.knopflerfish.android.basicapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.knopflerfish.android.service.Knopflerfish;

public class BasicStartStopActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void startKfInit(View view) {
    // start Knopflerfish service with init action
    Intent intent = new Intent(this, Knopflerfish.class);
    intent.setAction(Knopflerfish.ACTION_INIT);
    startService(intent);
    
    updateLinks(true);
  }
	
  public void startKf(View view) {
    // start Knopflerfish service
    Intent intent = new Intent(this, Knopflerfish.class);
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
