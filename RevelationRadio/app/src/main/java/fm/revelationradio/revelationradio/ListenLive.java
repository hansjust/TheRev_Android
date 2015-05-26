package fm.revelationradio.revelationradio;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class ListenLive extends ActionBarActivity {
    private StreamPlayerService musicService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen_live);

        StreamPlayerService.setSong(getString(R.string.media_URL), getString(R.string.title_default), getString(R.string.artist_default), "");
        musicService = StreamPlayerService.getInstance();

        Intent serviceIntent = new Intent(getApplicationContext(), StreamPlayerService.class);
        serviceIntent.setAction("fm.revelationradio.revelationradio.StreamPlayerService.PLAY");

        startService(serviceIntent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_listen_live, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void playStream(View view) {
        StreamPlayerService svc = StreamPlayerService.getInstance();
        if (svc == null) {
            Log.v("RevelationRadio", "MusicPlayer is null");
            return;
        }
        if (svc.isPlaying())
            svc.pauseMusic();
        else
            svc.startMusic();

    }
}
