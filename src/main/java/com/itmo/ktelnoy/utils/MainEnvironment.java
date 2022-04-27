package com.itmo.ktelnoy.utils;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class MainEnvironment {
    //TODO: refactor

    private static final ConcurrentHashMap<Class, MainEnvironment> envs = new ConcurrentHashMap<>();
    private static final String DATABASE_NAME = "my_database";

    private volatile SocketAddress serverAddress;
    @Getter
    private final MongoDriver mongoClient;
    @Getter
    private final Logger logger;
    private Class<?> exampleClass;

    public MainEnvironment(Class<?> exampleClass, MongoDriver mongoClient, Logger logger) {
        this.exampleClass = exampleClass;
        this.mongoClient = mongoClient;
        this.logger = logger;
    }

    private MainEnvironment(Class<?> exampleClass, MongoDriver mongoClient) {
        this(exampleClass, mongoClient, LoggerFactory.getLogger(exampleClass));
    }

    public static MainEnvironment newEnvironment(Class<?> exampleClass) {
        MainEnvironment env = new MainEnvironment(exampleClass, new MongoDriver(DATABASE_NAME));
        envs.put(exampleClass, env);
        return env;
    }

    public static MainEnvironment getRegisteredEnvironment(Class<?> exampleClass) {
        return envs.get(exampleClass);
    }

    public static void invokeExample(Class<?> exampleClass, String[] params) {
        try {
            Method method = exampleClass.getMethod("main", String[].class);
            method.invoke(null, (Object) params);
        } catch (IllegalAccessException e) {
            System.err.println("Failed to invoke main method on the example.");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("Failed to get the main method on the example. ");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Failed to invoke main method on the example. ");
            e.printStackTrace();
        }
    }

    public void registerServerAddress(SocketAddress server) {
        serverAddress = server;
    }

    public SocketAddress getServerAddress(Class<?> serverClass, String[] args) {
        String host = "127.0.0.1";

        if(null != args && args.length > 0) {
            String portAsStr = args[0];
            try {
                int port = Integer.parseInt(portAsStr);
                if (args.length > 1) {
                    host = args[1];
                }

                return new InetSocketAddress(host, port);
            } catch (NumberFormatException e) {
                printUsageAndExit();
            }
        }

        invokeExample(serverClass, new String[] {"false"});

        MainEnvironment serverEnv = getRegisteredEnvironment(serverClass);

        if (serverEnv == null) {
            throw new RuntimeException("No environment registered for the server: " + serverClass.getName());
        } else if (serverEnv.serverAddress == null) {
            throw new RuntimeException("Failed to start the server: " + serverClass.getName());
        }

        return serverEnv.serverAddress;
    }

    public boolean shouldWaitForShutdown(String[] args) {
        return args.length == 0;
    }

    private void printUsageAndExit() {
        System.out.println("Usage: java " + exampleClass.getName() + " [<server port> [<server host>]]");
        System.exit(-1);
    }
}
