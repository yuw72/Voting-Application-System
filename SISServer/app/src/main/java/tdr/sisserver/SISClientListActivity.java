package tdr.sisserver;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import android.util.Log;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;


/**
 * An activity representing a list of SISClients. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class SISClientListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    RecyclerView recyclerView;

    private static final String TAG = "SISClientListActivity";
    EditText portText;
    Button startButton;
    TextView infoDisplay;
    public static final int DISPLAY_INFO = 1;
    public static final int NEW_COMPONENT_REGISTERED = 2;
    public static final int NEW_COMPONENT_CONNECTED = 3;
    public static final int UPDATE_CLIENT_NAME = 4;
    public static final int CLIENT_DISCONNECTED = 5;
    public static final int UNREGISTERED_COMPONENT = 6;

    public static final String STOP_SERVER = "STOP SERVER";
    public static final String START_SERVER = "START SERVER";
    public static final String LISTENING_PORT = "8000";

    //The service object that bound to this activity, it is used for exchanging data between the activity and server.
    private ServerService serverService;
    private ServiceConnection mConnection = null;
    //Callback object that passed to server service, it is called by the server service to send data back to the activity.
    Handler callbacks = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str;
            String[] strs;
            SimpleItemRecyclerViewAdapter adapter = null;
            ClientInfo.ClientItem item = null;
            switch (msg.what) {
                case DISPLAY_INFO:
                    str = (String) msg.obj;
                    infoDisplay.append(str);
                    //button
                    startButton.setText(STOP_SERVER);
                    break;
                case NEW_COMPONENT_REGISTERED:
                    str = (String) msg.obj;
                    Toast.makeText(SISClientListActivity.this,"R:"+ str, Toast.LENGTH_SHORT).show();
                    adapter = (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
                    strs = str.split(":");
                    item = new ClientInfo.ClientItem(strs[0],strs[1], str);
                    adapter.notifyDataSetChanged();
                    break;
                case NEW_COMPONENT_CONNECTED:
                    str = (String) msg.obj;
                    Toast.makeText(SISClientListActivity.this, "C:"+str, Toast.LENGTH_SHORT).show();
                    adapter = (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
                    strs = str.split(":");
                    ClientInfo.updateItemStatus(strs[0],strs[1],2);
                    //item = new ClientInfo.ClientItem(strs[0],strs[1], str);
                    adapter.notifyDataSetChanged();
                    break;
                case UPDATE_CLIENT_NAME:
                    adapter = (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
                    str = (String) msg.obj;
                    int cid = msg.arg1;
                    ClientInfo.updateItem(cid + "", str);
                    adapter.notifyDataSetChanged();
                    break;
                case CLIENT_DISCONNECTED:
                    str = (String) msg.obj;
                    strs = str.split(":");
                    ClientInfo.removeItem(strs[0]);
                    adapter = (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(SISClientListActivity.this, str, Toast.LENGTH_SHORT).show();
                    break;
                case UNREGISTERED_COMPONENT:
                    str = (String) msg.obj;
                    Toast.makeText(SISClientListActivity.this, "Unregistered component->"+str, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    void doBindService() {
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  Because we have bound to a explicit
                // service that we know is running in our own process, we can
                // cast its IBinder to a concrete class and directly access it.
                serverService = ((ServerService.LocalBinder) service).getService();
                serverService.setActivityCallbacks(callbacks);
                String port = portText.getText().toString();
                if(!port.equals(LISTENING_PORT)){
                    portText.setText(LISTENING_PORT);
                    port = LISTENING_PORT;
                }
                serverService.startServer(port);
                //Display the listening IP and port
                String ip = NetTool.getIpAddress();
                portText.setText(ip + ":" + port);
                Toast.makeText(SISClientListActivity.this, "SISServer connected.", Toast.LENGTH_SHORT).show();
            }
            //Callled when the server service is disconnected.
            public void onServiceDisconnected(ComponentName className) {
                serverService = null;
                Toast.makeText(SISClientListActivity.this, "SISServer disconnected.", Toast.LENGTH_SHORT).show();
            }
        };
        bindService(new Intent(SISClientListActivity.this,
                ServerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void stopService() {
        //stopService(new Intent(SISClientListActivity.this, ServerService.class));
        if(serverService!=null){
            Log.e(TAG, "Stopping server.");
            this.unbindService(mConnection);
            serverService.stopSelf();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sisclient_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "SIS Server", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        if (recyclerView == null) {
            recyclerView = (RecyclerView) findViewById(R.id.sisclient_list);
            assert recyclerView != null;
            setupRecyclerView((RecyclerView) recyclerView);
        }


        if (findViewById(R.id.sisclient_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
        if (portText == null) {
            portText = (EditText) findViewById(R.id.portText);
        }
        if (startButton == null) {
            startButton = (Button) findViewById(R.id.startButton);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (startButton.getText().equals(START_SERVER)) {
                        //bind the server service, start it if it has not started yet.
                        doBindService();
                    } else if (startButton.getText().equals(STOP_SERVER)) {
                        startButton.setText(START_SERVER);
                        stopService();
                    }
                }
            });
        }
        if (infoDisplay == null) {
            infoDisplay = (TextView) findViewById(R.id.infoDisplay);
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(ClientInfo.CLIENT_LIST));
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("SISClientList Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<ClientInfo.ClientItem> mValues;

        public SimpleItemRecyclerViewAdapter(List<ClientInfo.ClientItem> items) {
            mValues = items;
        }

        public void addItem(ClientInfo.ClientItem item) {
            mValues.add(item);
        }

        public List<ClientInfo.ClientItem> getList() {
            return mValues;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sisclient_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            String displayText = "";
            String[] strs = mValues.get(position).details.split(":");
            if(mValues.get(position).status==1){
                displayText = "C#:"+mValues.get(position).id+" Registered:"+strs[1]+" IP:"+strs[2];
            }else if(mValues.get(position).status==2){
                displayText = "C#:"+mValues.get(position).id+" Connected:"+strs[1]+" IP:"+strs[2];
            }else{
                displayText = "C#:"+mValues.get(position).id+" Unknown:"+strs[1]+" IP:"+strs[2];
            }
            holder.mIdView.setText(mValues.get(position).id);
            final String content = mValues.get(position).content;
            final String detail = mValues.get(position).details;
            holder.mContentView.setText(displayText);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(SISClientDetailFragment.ARG_ITEM_ID, holder.mItem.id);
                        SISClientDetailFragment fragment = new SISClientDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.sisclient_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
//                        Intent intent = new Intent(context, SISClientDetailActivity.class);
//                        intent.putExtra(SISClientDetailFragment.ARG_ITEM_ID, holder.mItem.id);
//                        context.startActivity(intent);
                        String[] strs = detail.split(":");
                        String content = "Connection #: "+strs[0]+"\n"
                                +"Name: "+strs[1]+"\n"
                                +"Address: "+strs[2]+"\n"+" Port: "+strs[3];
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Component Detail")
                                .setMessage(content)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                })
                                .create().show();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public ClientInfo.ClientItem mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
