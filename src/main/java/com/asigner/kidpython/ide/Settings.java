package com.asigner.kidpython.ide;

import com.asigner.kidpython.ide.util.OS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {
    private static final String KEY_SELECTEDSTYLESHEET = "SelectedStylesheet";
    private static final String KEY_SELECTEDSOURCE = "SelectedSource";

    private static Settings instance = null;

    private String fileName = null;
    private Properties properties = null;
//    private List<SettingsChangedListener> listeners = new LinkedList<SettingsChangedListener>();

    public static synchronized Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public int getSelectedStylesheetIndex() {
        return getInt(KEY_SELECTEDSTYLESHEET, 0);
    }

    public void setSelectedstylesheetIndex(int idx) {
        set(KEY_SELECTEDSTYLESHEET, idx);
    }

    public int getSelectedSourceIndex() {
        return getInt(KEY_SELECTEDSOURCE, 0);
    }

    public void setSelectedSourceIndex(int idx) {
        set(KEY_SELECTEDSOURCE, idx);
    }

    public static String getSettingsDirectory() {
        File dir = new File(OS.getAppDataDirectory() + "/ProgrammableFun/");
        dir.mkdirs();
        return dir.getAbsolutePath();
    }

    private Settings() {
        fileName = getSettingsDirectory() + "/settings.properties";
        properties = new Properties(getDefaultProperties());
        FileInputStream is = null;
        try {
            is = new FileInputStream(fileName);
            properties.load(is);
        } catch (FileNotFoundException e) {
            // File not here... save default properties
            save(getDefaultProperties()); // make sure properties are written if
            // the file didn't exist
        } catch (IOException e) {
            // do nothing
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public void save() {
        save(properties);
    }

    private void save(Properties props) {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fileName);
            props.store(os, "Settings for Programmable Fun");
        } catch (IOException e) {
            // do nothing
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private Properties getDefaultProperties() {

        Properties props = new Properties();
        InputStream is = getClass().getResourceAsStream("/default-settings.properties");
        try {
            props.load(is);
        } catch (Exception e) {
            // do nothing
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
        return props;
    }
//
//    public void addListener(SettingsChangedListener listener) {
//        listeners.add(listener);
//    }
//
//    public void removeListener(SettingsChangedListener listener) {
//        listeners.remove(listener);
//    }
//

    private void fireSettingsChanged(String property) {
//        for (SettingsChangedListener l : listeners) {
//            l.settingsChanged(property);
//        }
    }

    public Settings remove(String key) {
        properties.remove(key);
        return this;
    }

    public Settings set(String key, String newVal) {
        String oldVal = properties.getProperty(key);
        if (!(oldVal != null && oldVal.equals(newVal))) {
            properties.setProperty(key, newVal);
            fireSettingsChanged(key);
        }
        return this;
    }

    private Settings set(String key, boolean newVal) {
        this.set(key, Boolean.toString(newVal) );
        return this;
    }

    private Settings set(String key, int newVal) {
        this.set(key, Integer.toString(newVal) );
        return this;
    }

    private String get(String key) {
        return properties.getProperty(key);
    }

    private int getInt(String key, int dflt) {
        String res = properties.getProperty(key);
        return res == null ? dflt : Integer.parseInt(res);
    }

    public boolean getBoolean(String key, boolean dflt) {
        String res = properties.getProperty(key);
        return res == null ? dflt : Boolean.parseBoolean(res);
    }

    public String get(String key, String dflt) {
        String res = properties.getProperty(key);
        return res == null ? dflt : res;
    }
}
