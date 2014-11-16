package org.bubblecloud.vr4java.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tlaukkan on 11/16/2014.
 */
public class Genderize {
    private static final Logger LOGGER = Logger.getLogger(VrClientProperties.class.getName());

    public static boolean isMale(final String name) {
        try {
            URL obj = new URL("http://api.genderize.io/?name=" + name);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setInstanceFollowRedirects(true);
            int responseCode = con.getResponseCode();
            ObjectMapper objectMapper = new ObjectMapper();
            final Map<String, Object> result = objectMapper.readValue(con.getInputStream(), HashMap.class);
            return result.get("gender").equals("male");
        } catch (final Exception e) {
            LOGGER.error("Error deducing gender from name: " + name, e);
            return true;
        }
    }

    public static void main(final String[] args) {
        System.out.println(isMale("Jaana"));
    }
}
