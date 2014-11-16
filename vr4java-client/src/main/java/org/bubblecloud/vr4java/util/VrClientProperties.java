package org.bubblecloud.vr4java.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * Created by tlaukkan on 11/16/2014.
 */
public class VrClientProperties {
    private static final Logger LOGGER = Logger.getLogger(VrClientProperties.class.getName());

    public static void save(String key, String value) {
        final Properties properties = new Properties();
        try {
            final File file = new File("vr4java-client-ext.properties");
            if (file.exists()) {
                properties.load(new FileInputStream("vr4java-client-ext.properties"));
            } else {
                //properties.load((InputStream) null);
            }
            properties.put(key, value);
            properties.store(new FileOutputStream("vr4java-client-ext.properties"), null);
        } catch (IOException e) {
            LOGGER.error("Error setting " + key + " to properties.", e);
        }
    }
}
