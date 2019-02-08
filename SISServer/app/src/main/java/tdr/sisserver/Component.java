package tdr.sisserver;

/**
 * Created by zhenjiangfan on 2016/12/10.
 */
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Component {
    //Basic, Controller, Monitor, Advertiser, Debugger
    public static final String BASIC = "Basic";
    public static final String CONTROLLER = "Controller";
    public static final String MONITOR = "Monitor";
    public static final String ADVERTISER = "Advertiser";
    public static final String DEBUGGER = "Debugger";

    String scope;
    String componentType;
    String name;
    String incomingMessages;
    String outgoingMessages;

    // message writer for a component
    MsgEncoder encoder;
    // message reader for a component
    MsgDecoder decoder;

    ConcurrentLinkedQueue messageQueue;

    public Component(String s, String t, String n) {
        scope = s;
        componentType = t;
        name = n;
    }
    public String getIncomingMessages() {
        return incomingMessages;
    }

    public ConcurrentLinkedQueue getMessageQueue() {
        return messageQueue;
    }

    public void setIncomingMessages(String incomingMessages) {
        this.incomingMessages = incomingMessages;
    }

    public void setMessageQueue(ConcurrentLinkedQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public String getOutgoingMessages() {
        return outgoingMessages;
    }

    public void setOutgoingMessages(String outgoingMessages) {
        this.outgoingMessages = outgoingMessages;
    }

    @Override
    public boolean equals(Object obj) {
        Component info = (Component) obj;
        return info.name.equals(name) && info.scope.equals(scope);
    }

    public void setDecoder(MsgDecoder decoder) {
        this.decoder = decoder;
    }

    public void setEncoder(MsgEncoder encoder) {
        this.encoder = encoder;
    }

    public MsgDecoder getDecoder() {
        return decoder;
    }

    public MsgEncoder getEncoder() {
        return encoder;
    }

    @Override
    public int hashCode() {
        int result = HashCodeUtil.hash(HashCodeUtil.SEED, scope);
        result = HashCodeUtil.hash(result, name);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\nScope: " + scope + "\n");
        builder.append("Component Type: " + componentType + "\n");
        builder.append("Name: " + name + "\n");
        return builder.toString();
    }
}

class Components{
    //static Map<String, Component> componentMap = new ConcurrentHashMap<>();
    static ArrayList< Component> componentList = new ArrayList<Component>();
}
