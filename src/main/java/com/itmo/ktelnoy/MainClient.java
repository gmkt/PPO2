package com.itmo.ktelnoy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.ktelnoy.utils.MainEnvironment;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.client.HttpClient;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;

import static rx.Observable.just;

public class MainClient {
    public static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        MainEnvironment env = MainEnvironment.newEnvironment(MainClient.class);
        Logger logger = env.getLogger();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            /*
            mapper.writeValue(baos, new User("user", "228", Currency.RUBBLE));
            makeRequestWithLogging(env, logger, "/user/", HttpMethod.POST, baos.toByteArray());
             */
            //makeRequestWithLogging(env, logger, "/user/", HttpMethod.GET, new byte[]{});
            //makeRequestWithLogging(env, logger, "/user/60738b3e48437659509461b4", HttpMethod.DELETE, new byte[]{});
            /*
            HashMap<Currency, Double> currencies = new HashMap<>();
            currencies.put(Currency.RUBBLE, 100.0);
            currencies.put(Currency.DOLLAR, 1.0);
            mapper.writeValue(baos, new Good("good", currencies));
            makeRequestWithLogging(env, logger, "/good/", HttpMethod.POST, baos.toByteArray());
             */
            makeRequestWithLogging(env, logger, "/goods?userId=60738f414843765ce440f04b", HttpMethod.GET, new byte[]{});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void makeRequestWithLogging(MainEnvironment env, Logger logger, String path, HttpMethod method, byte[] requestPayload) {
        SocketAddress serverAddress = env.getServerAddress(MainServer.class, new String[]{});
        HttpClient.newClient(serverAddress)
                .enableWireLogging("hello-client", LogLevel.ERROR)
                .createRequest(method, path)
                .writeBytesContent(just(requestPayload))
                .doOnNext(resp -> logger.info(resp.toString()))
                .flatMap(resp -> resp.getContent()
                        .map(bb -> bb.toString(Charset.defaultCharset())))
                .toBlocking()
                .forEach(logger::info);
    }
}
