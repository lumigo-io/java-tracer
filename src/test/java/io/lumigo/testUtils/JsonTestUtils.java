package io.lumigo.testUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonTestUtils {

    public static boolean compareJsonStrings(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }

        try {
            JSONObject json1 = new JSONObject((String) o1);
            JSONObject json2 = new JSONObject((String) o2);
            return json1.similar(json2);
        } catch (Exception e1) {
            try {
                JSONArray json1 = new JSONArray((String) o1);
                JSONArray json2 = new JSONArray((String) o2);
                return json1.similar(json2);
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
