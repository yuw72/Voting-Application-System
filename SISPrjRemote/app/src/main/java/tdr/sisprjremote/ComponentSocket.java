package tdr.sisprjremote;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;

/*
   This class is a Thread object that is used to control the connection with the SIS server.
 */
class ComponentSocket extends Thread {
    //private static final String TAG = "ComponentSocket";
    private static String serverAddress;
    private static int serverPort;
    private static MsgEncoder msgEncoder;
    private static MsgDecoder msgDecoder;

    Socket socket;
    KeyValueList message;

    //Callback object that is used to update UI(Activity) as long as there is something
    // happen to the connection between the component and SIS server.
    Handler callback;

    //Control flag variable that determines whether the thread should be alive
    boolean killThread = false;

    ComponentSocket() {
    }
    //TODO Socket communication always requires the receiver replies to the sender back.
    ComponentSocket(String addr, int port, Handler callbacks) {
        serverAddress = addr;
        serverPort = port;
        Log.d(MainActivity.TAG, "Server Address: " + serverAddress);
        Log.d(MainActivity.TAG, "Server Port: " + serverPort);
        callback = callbacks;
    }

    @Override
    public void run() {
        super.run();
        //Keep listening if there is any incoming messages
        while(!killThread){
            try {
                //Build a new socket
                socket = new Socket(serverAddress, serverPort);
                socket.setKeepAlive(true);
                msgDecoder = new MsgDecoder(socket.getInputStream());
                msgEncoder = new MsgEncoder(socket.getOutputStream());
                //Tell the activity that a new socket has been built.
                Message message = callback.obtainMessage(MainActivity.CONNECTED);
                callback.sendMessage(message);
                killThread = false;
                while(true){
                   /* //Check if there is an incoming message.
                    KeyValueList kvList = msgDecoder.getMsg();
                    if (kvList.size() > 1) {
                        Log.e(MainActivity.TAG, "Received raw: <" + kvList.encodedString() + ">");
                        //Tell the activity that a new message has been received.
                        Message msg = callback.obtainMessage(MainActivity.MESSAGE_RECEIVED);
                        msg.obj = kvList.toString();
                        callback.sendMessage(msg);
                    }*/
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
        Log.e(MainActivity.TAG, "Sock thread killed." );
    }
    //The function is called by the activity and used to set the output message
    void setMessage(KeyValueList kvList) {
        if(msgEncoder!=null){
            try {
                Log.e(MainActivity.TAG, "Sending messsage." );
                msgEncoder.sendMsg(kvList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    boolean isSocketAlive(){
        if(socket==null)
            return false;
        return socket.isConnected() ;

    }

    //Just send an acknowledgement if there is nothing to be sent at the moment
    void ack(){
        KeyValueList reply = new KeyValueList();
        reply.putPair("ack","ack");
        try {
            msgEncoder.sendMsg(reply);
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "IOException: " + e.toString());
        }
    }
}