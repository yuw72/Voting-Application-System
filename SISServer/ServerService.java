package tdr.sisserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
/*
    The Service class that used to keep listening to incoming messages from SIS components and forward Reading messages to some components in the system.
 */
public class ServerService extends Service {

    private static final String TAG = "ServerService";

    public ServerService() {
    }
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        ServerService getService() {
            return ServerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(ServerService.this, "SISServer service created.", Toast.LENGTH_SHORT).show();
    }

    Handler activityCallbacks;
    String port;
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
       return mBinder;
    }

    public void setActivityCallbacks(Handler callbacks){
        activityCallbacks = callbacks;
    }

    private ServerSocket universal;
    private ExecutorService service;

    void startServer(final String p) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                port = p;
                Log.e(TAG, "Starting server.");
                try {
                    universal = new ServerSocket(Integer.parseInt(p));
                    Log.e(TAG, "Server started. Port Number: " + universal.getLocalPort());

                    Message message = activityCallbacks.obtainMessage(SISClientListActivity.DISPLAY_INFO);
                    message.obj = "Listen on Port: " + universal.getLocalPort();
                    activityCallbacks.sendMessage(message);
                    service = Executors.newCachedThreadPool();

                    while (true) {
                        Log.e(TAG, "Prepare to accept new connection.");
                        Socket socket = universal.accept();
                        service.execute(new SocketsThread(socket));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (universal != null) {
                        try {
                            universal.close();
                            Log.d(TAG, "Server Shut down.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public void closeSocket(){
        if (universal != null) {
            try {
                universal.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static int counter = 1;
    class SocketsThread implements Runnable {
        String myName = "SISComponent";
        private int connectionNumber;
        private MsgDecoder msgDecoder;
        private MsgEncoder msgEncoder;
        private ConcurrentLinkedQueue messageQueue = new ConcurrentLinkedQueue<KeyValueList>();
        private String ip;
        private int port;

        boolean flag = true;

        //TODO Socket communication always requires the receiver replies to the sender back.
        SocketsThread(final Socket socket) {
            Log.e(TAG, "Received Connection #" + counter + ". Port Number: " + socket.getPort());
            connectionNumber = counter;
            counter++;
            try {
                msgDecoder = new MsgDecoder(socket.getInputStream());
                msgEncoder = new MsgEncoder(socket.getOutputStream());
                ip = socket.getInetAddress().toString();
                port = socket.getLocalPort();
            } catch (IOException e) {
                Log.e(TAG, "Initialize Failed.");
            }
        }

        @Override
        public void run() {
            try {
                KeyValueList kvList;
                while (flag) {
                    kvList = msgDecoder.getMsg();
                    if (kvList.size()<2) {
                        if(messageQueue.size()>0){
                            KeyValueList tmpkvList = (KeyValueList)messageQueue.remove();
                            msgEncoder.sendMsg(tmpkvList);
                        }else{
                            ack();
                        }
                    }else{
                        Log.e(TAG, "Received raw: <" + kvList.encodedString() + ">");
                        //process the message
                        processMessage(kvList);
                        reply();
                    }
                    try {
                        Thread.sleep(100);
                    }  catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.e(TAG, "Connection #" + connectionNumber + " closed.");
                Message message = activityCallbacks.obtainMessage(SISClientListActivity.CLIENT_DISCONNECTED);
                message.obj = connectionNumber + ": disconnected.";
                activityCallbacks.sendMessage(message);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void processMessage(KeyValueList kvList) {
            String scope = kvList.getValue(SISMessage.SCOPE);
            String messageType = kvList.getValue(SISMessage.TYPE);
            String sender = kvList.getValue(SISMessage.SENDER);
            myName = sender;
            String receiver = kvList.getValue(SISMessage.RECEIVER);
            String role = kvList.getValue(SISMessage.ROLE);
            Log.d(TAG, "Message Received: " + kvList.toString());
            switch (messageType) {
                case SISMessage.READING:
                    for(Component component:Components.componentList){
                        Log.e(TAG, Components.componentList.size()+" component: " +component.name);
                        if(!scope.equalsIgnoreCase(SISMessage.DEFAULT_SCOPE)){
                            //scopeValue = scope.substring(scope.indexOf(".")+1, scope.length());
                            if(!scope.equalsIgnoreCase(component.scope)){
                                continue;
                            }
                        }
                        //role or name
                        if(receiver!=null && !receiver.equals("")){
                            if(!receiver.equalsIgnoreCase(component.name)){
                                continue;
                            }
                        }
                        //do not forward the message to itself
                        if(component.name.equalsIgnoreCase(myName)){
                            continue;
                        }
                        //forward the message
                        component.messageQueue.add(kvList);
                    }
                    break;
                case SISMessage.REGISER:
                    Component cmpt = new Component(scope,role,sender);
                    cmpt.setDecoder(this.msgDecoder);
                    cmpt.setEncoder(this.msgEncoder);
                    cmpt.setMessageQueue(this.messageQueue);
                    Components.componentList.add(cmpt);
                    Message message = activityCallbacks.obtainMessage(SISClientListActivity.NEW_COMPONENT_CONNECTED);
                    message.obj = connectionNumber + ":" +sender+":"+ ip + ":" + port;
                    activityCallbacks.sendMessage(message);

                    break;
                case SISMessage.EMERGENCY:
                    for(Component component:Components.componentList){
                        if(!scope.equalsIgnoreCase(SISMessage.DEFAULT_SCOPE)){
                            //scopeValue = scope.substring(scope.indexOf(".")+1, scope.length());
                            if(!scope.equalsIgnoreCase(component.scope)){
                                continue;
                            }
                        }
                        //the receivers must be controllers
                        if(role!=null && !role.equals("")){
                            if(!role.equalsIgnoreCase(Component.CONTROLLER)){
                                continue;
                            }
                        }
                        //role or name
                        if(receiver!=null && !receiver.equals("")){
                            if(!receiver.equalsIgnoreCase(component.name)){
                                continue;
                            }
                        }
                        //do not forward the message to itself
                        if(component.name.equalsIgnoreCase(myName)){
                            continue;
                        }
                        ////forward the message
                        component.messageQueue.add(kvList);
                    }
                    break;
                case SISMessage.ALERT:
                    for(Component component:Components.componentList){
                        if(!scope.equalsIgnoreCase(SISMessage.DEFAULT_SCOPE)){
                            //scopeValue = scope.substring(scope.indexOf(".")+1, scope.length());
                            if(!scope.equalsIgnoreCase(component.scope)){
                                continue;
                            }
                        }
                        //the receivers must be monitors
                        if(role!=null && !role.equals("")){
                            if(!role.equalsIgnoreCase(Component.MONITOR)){
                                continue;
                            }
                        }
                        //role or name
                        if(receiver!=null && !receiver.equals("")){
                            if(!receiver.equalsIgnoreCase(component.name)){
                                continue;
                            }
                        }
                        //do not forward the message to itself
                        if(component.name.equalsIgnoreCase(myName)){
                            continue;
                        }
                        //forward the message
                        component.messageQueue.add(kvList);
                    }
                    break;
                case SISMessage.SETTING:
                    break;
            }
        }
        //Send a confirm message back after receiving a Register or Reading message
        void reply(){
            KeyValueList reply = new KeyValueList();
            reply.putPair(SISMessage.TYPE, SISMessage.CONFIRM);
            reply.putPair(SISMessage.SENDER, myName);

            try {
                msgEncoder.sendMsg(reply);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.toString());
            }
        }
        //Just send an acknowledgement when there is nothing to send.
        void ack(){
            KeyValueList reply = new KeyValueList();
            reply.putPair("ack","ack");
            try {
                msgEncoder.sendMsg(reply);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.toString());
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "Service onUnbind.");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "Service onDestroy.");
        super.onDestroy();
        closeSocket();
    }
}
