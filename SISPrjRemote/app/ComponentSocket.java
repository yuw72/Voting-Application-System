package tdr.sisprjremote;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;


class ComponentSocket extends Thread {
    private static final String TAG = "ComponentSocket";
    private static String serverAddress;
    private static int serverPort;
    private static MsgEncoder msgEncoder;
    private static MsgDecoder msgDecoder;

    Socket socket;
    KeyValueList message;

    Handler callback;

    boolean killThread = false;

    ComponentSocket() {
    }
    //TODO Socket communication always requires the receiver replies to the sender back.
    ComponentSocket(String addr, int port, Handler callbacks) {
        serverAddress = addr;
        serverPort = port;
        Log.d(TAG, "Server Address: " + serverAddress);
        Log.d(TAG, "Server Port: " + serverPort);
        callback = callbacks;
    }

    @Override
    public void run() {
        super.run();
        //Keep listening if there is any incoming messages
        while(!killThread){
            try {
                if(socket !=null && socket.isConnected()){
                    if(message!=null){
                        KeyValueList tmpmessage = message;
                        message = null;
                        msgEncoder.sendMsg(tmpmessage);
                        KeyValueList kvList = msgDecoder.getMsg();
                        if (kvList.size() > 0) {
                            Log.e(TAG, "Received raw: <" + kvList.encodedString() + ">");
                            //Tell the activity that a new message has been received.
                            Message msg = callback.obtainMessage(MainActivity.MESSAGE_RECEIVED);
                            msg.obj = kvList.toString();
                            callback.sendMessage(msg);
                        }
                    }
                }else{
                    //Build a new socket
                    socket = new Socket(serverAddress, serverPort);
                    socket.setKeepAlive(true);
                    msgDecoder = new MsgDecoder(socket.getInputStream());
                    msgEncoder = new MsgEncoder(socket.getOutputStream());
                    //Tell the activity that a new socket has been built.
                    Message message = callback.obtainMessage(MainActivity.CONNECTED);
                    callback.sendMessage(message);
                    killThread = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message message = callback.obtainMessage(MainActivity.DISCONNECTED);
                callback.sendMessage(message);
            }
            try {
                Thread.sleep(100);
            }  catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    //Kill the socket listening thread by setting the alive flag to true
    void killThread() {
        killThread = true;
        Message message = callback.obtainMessage(MainActivity.DISCONNECTED);
        callback.sendMessage(message);
        Log.e(TAG, "Sock thread killed." );
    }
    //The function is called by the activity and used to set the output message
    void setMessage(KeyValueList kvList) {
        message = kvList;
    }
    boolean isSocketAlive(){
        return !killThread ;

    }
}