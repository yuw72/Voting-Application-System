package tdr.myComponent;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
/*
    A demo Activity, which shows how to build a connection with SIS server, register itself to the server,
    and send a message to the server as well.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    static Button connectButton;
    static Button resetButton;

    static ComponentSocket client;
    static TextView messageToSendList,messageReceivedList;

    public static final String TAG = "myComponent";
    //public static ArrayList<Integer> list = new ArrayList<Integer>();

    public static int MAX=10;
    public static int[] list= new int[MAX+1];
    public  ArrayList<String> arr=new ArrayList<String>();
    //static Toolbar toolbar;
    public static boolean open=false;
    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 2;
    public static final int MESSAGE_RECEIVED = 3;
    public static final int OPEN_POOL=4;
    public static final int CLOSE_POOL=5;
    static Handler callbacks = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String str;
            String[] strs;
            switch (msg.what) {
                case CONNECTED:
                    connectButton.setText(R.string.disconnect);
                    break;
                case DISCONNECTED:
                    //Toast.makeText(MainActivity.this,"disconnect disconnect.",Toast.LENGTH_LONG).show();
                    //Log.d("", "===============================================================:2 " );
                    connectButton.setText(R.string.connect);
                    break;
                case MESSAGE_RECEIVED:
                    str = (String)msg.obj;
                    messageReceivedList.append(str+"********************\n");
                    final int scrollAmount = messageReceivedList.getLayout().getLineTop(messageReceivedList.getLineCount()) - messageReceivedList.getHeight();
                    if (scrollAmount > 0)
                        messageReceivedList.scrollTo(0, scrollAmount);
                    else
                        messageReceivedList.scrollTo(0, 0);
                    break;
                case OPEN_POOL:
                    open=true;
                    break;
                case CLOSE_POOL:
                    open=false;
                    int i=0;
                    int j=0;
                    int pointer=0;
                    while(i<MAX+1)
                    {
                        for ( j = i+1; j < MAX+1; j++)
                        {
                           if(list[i]<list[j])
                           {
                               pointer=j;
                               break;
                           }
                        }
                        i=j;
                    }
                    for(int k=0;k<MAX+1;k++)
                        Log.i(TAG, "vote is"+ list[k]+"\n");
                    messageReceivedList.append("number "+pointer+" has highest vote ");
                    final int scrollAmount2 = messageReceivedList.getLayout().getLineTop(messageReceivedList.getLineCount()) - messageReceivedList.getHeight();
                    if (scrollAmount2 > 0)
                        messageReceivedList.scrollTo(0, scrollAmount2);
                    else
                        messageReceivedList.scrollTo(0, 0);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        SmsReceiver.bindListener(new SmsListener() {
            @Override
            public void messageReceived(String messageText,String sender) {
                if(open) {
                    if(!arr.contains(sender)) {
                        arr.add(sender);
                        Log.i("Text", messageText + "\t " + arr.get(0));
                        Toast.makeText(MainActivity.this, "Message: " + messageText + "sender" + sender, Toast.LENGTH_LONG).show();
                        if (isInteger(messageText)) {
                        Toast.makeText(MainActivity.this, "Message: is integer", Toast.LENGTH_LONG).show();
                        int result = Integer.parseInt(messageText);
                        if (result > 0 && result < MAX + 1) {
                            list[result] = list[result] + 1;
                        }

                        }
                    }
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }

    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        EditText serverIp,serverPort,refreshRate,connectcope;

        EditText messageScope,messageType,roleType,messageName,receiver
                ,messageContent,addMessageAttrKey,addMessageAttrValue;//sender

        View rootView1,rootView2,rootView3;

        Button msgSendButton,xmlLoadButton,msgClearButton,addMessageAttrButton ;
        KeyValueList messageInfo = new KeyValueList();

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int num = getArguments().getInt(ARG_SECTION_NUMBER);

            if(num==1){
                if(rootView1==null){
                    rootView1 = inflater.inflate(R.layout.connectionconfig, container, false);
                    serverIp = (EditText) rootView1.findViewById(R.id.serverIp);
                    serverPort = (EditText) rootView1.findViewById(R.id.serverPort);
                    refreshRate = (EditText) rootView1.findViewById(R.id.refreshRate);
                    connectcope = (EditText) rootView1.findViewById(R.id.scope);
                    connectButton = (Button) rootView1.findViewById(R.id.connectButton);

                    connectButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(connectButton.getText().toString().equalsIgnoreCase(getResources().getString(R.string.disconnect))){
                                client.killThread();
                            }else{
                                client = new ComponentSocket(serverIp.getText().toString(), Integer.parseInt(serverPort.getText().toString()),callbacks);
                                client.start();
                            }
                        }
                    });
                    resetButton = (Button) rootView1.findViewById(R.id.resetButton);
                    resetButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            serverIp.setText("10.0.2.15");
                            serverPort.setText(R.string.server_port_value);
                            refreshRate.setText(R.string.fresh_rate_value);
                            connectcope.setText(R.string.scope_value);
                            if(client!=null){
                                client.killThread();
                            }
                        }
                    });
                }
                return rootView1;
            }else if(num==2 ){
                if(rootView2==null){
                    rootView2 = inflater.inflate(R.layout.messageinput, container, false);
                    messageScope = (EditText) rootView2.findViewById(R.id.messageScope);
                    messageType = (EditText) rootView2.findViewById(R.id.messageType);
                    roleType = (EditText) rootView2.findViewById(R.id.roleType);
                    messageName = (EditText) rootView2.findViewById(R.id.messageName);
                    //sender = (EditText) rootView2.findViewById(R.id.sender);
                    receiver = (EditText) rootView2.findViewById(R.id.receiver);
                    messageContent = (EditText) rootView2.findViewById(R.id.messageContent);
                    addMessageAttrKey = (EditText) rootView2.findViewById(R.id.addMessageAttrKey);
                    addMessageAttrValue = (EditText) rootView2.findViewById(R.id.addMessageAttrValue);
                    msgSendButton = (Button) rootView2.findViewById(R.id.msgSendButton);
                    xmlLoadButton = (Button) rootView2.findViewById(R.id.xmlLoadButton);
                    msgClearButton = (Button) rootView2.findViewById(R.id.msgClearButton);
                    addMessageAttrButton = (Button) rootView2.findViewById(R.id.addMessageAttrButton);
                    msgSendButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //if(connectButton.getText().toString().equalsIgnoreCase(getResources().getString(R.string.disconnect))){
                            if(client!=null && client.isSocketAlive()){
                                connectButton.setText(R.string.disconnect);
                                sendData();
                            }else{
                                connectButton.setText(R.string.connect);
                                Toast.makeText(PlaceholderFragment.this.getContext(), "Please connect to the server first.",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    xmlLoadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //if(connectButton.getText().toString().equalsIgnoreCase(getResources().getString(R.string.disconnect))){
                            if(client!=null && client.isSocketAlive()){
                                connectButton.setText(R.string.disconnect);
                                openFolder();
                            }else{
                                connectButton.setText(R.string.connect);
                                Toast.makeText(PlaceholderFragment.this.getContext(), "Please connect to the server first.",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    msgClearButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            clearInput();
                        }
                    });
                    addMessageAttrButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String key = addMessageAttrKey.getText().toString();
                            if(key==null || key.equals("")){
                                Toast.makeText(PlaceholderFragment.this.getContext(), "Please enter a key.",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String value = addMessageAttrValue.getText().toString();
                            if(value==null || value.equals("")){
                                Toast.makeText(PlaceholderFragment.this.getContext(), "Please enter a value.",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            messageInfo.putPair(key,value);
                        }
                    });
                }
                return rootView2;
            }else if(num==3 ){
                if(rootView3==null){
                    rootView3 = inflater.inflate(R.layout.messagedisplay, container, false);
                    messageToSendList  = (TextView) rootView3.findViewById(R.id.messageToSendList);
                    messageToSendList.setMovementMethod(ScrollingMovementMethod.getInstance());
                    messageReceivedList  = (TextView) rootView3.findViewById(R.id.messageReceivedList);
                    messageReceivedList.setMovementMethod(ScrollingMovementMethod.getInstance());
                }
                return rootView3;
            }
            return rootView1;
        }
        public void clearInput(){
            messageScope.setText("");
            messageType.setText("");
            roleType.setText("");
            messageName.setText("");
            messageContent.setText("");
            //sender.setText("");
            receiver.setText("");
            addMessageAttrKey.setText("");
            addMessageAttrValue.setText("");
        }
        public void sendData(){
            if(client==null){
                Toast.makeText(PlaceholderFragment.this.getContext(), "Please connect to the server first.",Toast.LENGTH_SHORT).show();
                return;
            }

            String scope = messageScope.getText().toString();
            if(scope==null || scope.equals("")){
                Toast.makeText(PlaceholderFragment.this.getContext(), "Please enter a scope.",Toast.LENGTH_SHORT).show();
                return;
            }
            messageInfo.putPair("Scope",scope);

            String type = messageType.getText().toString();
            if(type==null || type.equals("")){
                Toast.makeText(PlaceholderFragment.this.getContext(), "Please enter a message type.",Toast.LENGTH_SHORT).show();
                return;
            }

            messageInfo.putPair("MessageType", type);

            String name = messageName.getText().toString();
            if(name==null || name.equals("") || name.equals("Name")){
                Toast.makeText(PlaceholderFragment.this.getContext(), "Please enter a sender name.",Toast.LENGTH_SHORT).show();
                return;
            }
            messageInfo.putPair("Sender", name);

            String role = roleType.getText().toString();
            if(role!=null && !role.equals("") && !role.equals("Role")){
                messageInfo.putPair("Role",role );
            }

            String content = messageContent.getText().toString();
            if(content!=null && !content.equals("")  && !content.equals("Message")){
                messageInfo.putPair("Message", content);
            }


            String recStr = receiver.getText().toString();
            if(recStr!=null && !recStr.equals("") && !recStr.equals("Receiver")){
                messageInfo.putPair("Receiver", recStr);
            }
            KeyValueList tmp = messageInfo;
            sentMessage(tmp);
            messageInfo = new KeyValueList();
        }
        //Call a system service that enables users to choose a XML file.
        public void openFolder() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/");
            intent.setDataAndType(uri, "*/*");
            try {
                startActivityForResult(Intent.createChooser(intent, "Open XML configuration file"), 1);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(PlaceholderFragment.this.getContext(), "Please install a File Manager.",Toast.LENGTH_LONG).show();
            }
        }
        //Called after the user chooses a XML file.
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case 1:
                    if (resultCode == RESULT_OK) {
                        // Get the Uri of the selected file
                        Uri uri = data.getData();
                        String path = uri.toString();
                        Log.e("", "kvList================uri=========="+ path);
                        path = path.replace("file:///","/");
                        File myFile = new File(path);
                        //file:///storage/emulated/0/AlertMonitor.xml

                        KeyValueList kvList = XMLParser.getMessagesFromXML(myFile);
                        Log.e("", "kvList================xml=========="+kvList );
                        sentMessage(kvList);
                    }
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    //Pass a message to the socket thread and update the sent-text view.
    static void sentMessage(final KeyValueList messageInfo){
        if(client!=null){
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    client.setMessage(messageInfo);
                }
            }, 100);

            messageToSendList.append(messageInfo.toString()+"********************\n");
            final int scrollAmount = messageToSendList.getLayout().getLineTop(messageToSendList.getLineCount()) - messageToSendList.getHeight();
            if (scrollAmount > 0)
                messageToSendList.scrollTo(0, scrollAmount);
            else
                messageToSendList.scrollTo(0, 0);
        }
    }
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Connnection Configuration";
                case 1:
                    return "Message(s) Setting";//xml
                case 2:
                    return "Results";

            }
            return null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(client!=null){
            client.killThread();
        }
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
}
