package vn.ptit.p2p.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * JSON serialization and deserialization utilities
 */
public class Json {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final Gson compactGson = new Gson();
    
    /**
     * Serialize object to JSON string
     */
    public static String toJson(Object obj) {
        return compactGson.toJson(obj);
    }
    
    /**
     * Serialize object to pretty-printed JSON string
     */
    public static String toPrettyJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * Deserialize JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return gson.fromJson(json, classOfT);
    }
    
    /**
     * Validate if a string is valid JSON
     */
    public static boolean isValidJson(String json) {
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
}

