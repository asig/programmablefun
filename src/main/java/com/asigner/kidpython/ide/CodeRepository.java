package com.asigner.kidpython.ide;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CodeRepository {

    private static final String KEY_PERSISTENCE_STRATEGY = "CodeRepository.PersistenceStrategy";

//    private final Settings settings;

    interface Listener {
        void strategyChanged(PersistenceStrategy newStrategy);
    }

    private static class Content {
        @JsonProperty("selected_stylesheet")
        private int selectedStylesheet = 0;

        @JsonProperty("selected_source")
        private int selectedSource = 0;

        @JsonProperty("sources")
        private List<Source> sources;

        Content() {
            sources = Lists.newArrayList();
            for (int i = 0; i < SOURCES; i++) {
                sources.add(new Source(String.format("%d", i+1), ""));
            }
        }
    }

    private final static int SOURCES = 10;

    private final List<Listener> listeners = Lists.newArrayList();
    private final BlockingQueue<byte[]> outstanding = new LinkedBlockingQueue<>();
    private final Thread syncerThread;

    private Content content = new Content();
    private volatile PersistenceStrategy persistenceStrategy;

    public CodeRepository(PersistenceStrategy persistenceStrategy) {
//        settings = Settings.getInstance();
//        try {
//            String className = settings.get(KEY_PERSISTENCE_STRATEGY, LocalPersistenceStrategy.class.getName());
//            Class<?> clazz = Class.forName(className);
//            Constructor<?> ctor = clazz.getConstructor();
//            this.persistenceStrategy = (PersistenceStrategy) ctor.newInstance();
//        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
//            this.persistenceStrategy = new LocalPersistenceStrategy();
//        }
        this.persistenceStrategy = persistenceStrategy;
        syncerThread = new Thread(() -> {
            for (;;) {
                List<byte[]> buf = Lists.newArrayListWithCapacity(1024);
                outstanding.drainTo(buf);
                if (buf.size() > 0) {
                    byte[] lastData = buf.get(buf.size() - 1);
                    try {
                        this.persistenceStrategy.save(lastData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        syncerThread.setDaemon(true);
        syncerThread.start();
    }

    public void load() {
        loadFrom(persistenceStrategy);
    }

    public void save() {
        ObjectMapper m = new ObjectMapper();
        JsonFactory factory = m.getFactory();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            factory.createGenerator(bos, JsonEncoding.UTF8).writeObject(content);
            outstanding.offer(bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFrom(PersistenceStrategy strategy) {
        try {
            byte[] data = strategy.load();
            ObjectMapper m = new ObjectMapper();
            JsonFactory factory = m.getFactory();
            content = factory.createParser(data).readValueAs(Content.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void switchStrategy(PersistenceStrategy strategy) {
        save();
        loadFrom(strategy);
        persistenceStrategy = strategy;
        listeners.forEach(l -> l.strategyChanged(strategy));
//        settings.set(KEY_PERSISTENCE_STRATEGY, strategy.getClass().getName());
//        settings.save();
    }

    @JsonIgnore
    public int getNofSources() {
        return SOURCES;
    }

    public Source getSource(int idx) {
        return content.sources.get(idx);
    }

    public int getSelectedSource() {
        return content.selectedSource;
    }

    public void setSelectedSource(int selectedSource) {
        content.selectedSource = selectedSource;
    }

    public int getSelectedStylesheet() {
        return content.selectedStylesheet;
    }

    public void setSelectedStylesheet(int selectedStylesheet) {
        content.selectedStylesheet = selectedStylesheet;
        save();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

}
