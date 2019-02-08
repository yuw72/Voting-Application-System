package tdr.sisserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientInfo {

    /**
     * An array of sample (dummy) items.
     */
    public static final List<ClientItem> CLIENT_LIST = new ArrayList<ClientItem>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, ClientItem> CLIENT_LIST_MAP = new HashMap<String, ClientItem>();
    public static void addItem(ClientItem item) {
        item.status=1;
        CLIENT_LIST.add(item);
        CLIENT_LIST_MAP.put(item.id, item);
    }
    public static void updateItem(String itemID,String content) {
        for(int i =0;i<CLIENT_LIST.size();i++){
            if(CLIENT_LIST.get(i).id.equals(itemID)){
                CLIENT_LIST.get(i).content = content;
            }
        }
    }
    public static void updateItemStatus(String itemID,String content,int status) {
        for(int i =0;i<CLIENT_LIST.size();i++){
            if(CLIENT_LIST.get(i).id.equals(itemID) && CLIENT_LIST.get(i).content.equals(content)){
                CLIENT_LIST.get(i).status = status;
            }
        }
    }
    public static void removeItem(String itemID) {
        for(int i =0;i<CLIENT_LIST.size();i++){
            if(CLIENT_LIST.get(i).id.equals(itemID)){
                CLIENT_LIST_MAP.remove(itemID);
                CLIENT_LIST.remove(i);
            }
        }
    }
    /**
     * A dummy item representing a piece of content.
     */
    public static class ClientItem {
        public final String id;
        public String content;
        public String details;
        public int status = 0;//0:default. 1:regsitered. 2:connected

        public ClientItem(String id, String content, String details) {
            this.id = id;
            this.content = content;
            this.details = details;
            addItem(this);
        }
        @Override
        public String toString() {
            return content;
        }
    }
}
