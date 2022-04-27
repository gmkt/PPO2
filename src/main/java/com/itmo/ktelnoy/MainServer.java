package com.itmo.ktelnoy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.ktelnoy.utils.MainEnvironment;
import com.itmo.ktelnoy.model.User;
import com.mongodb.rx.client.Success;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.ResponseContentWriter;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static rx.Observable.*;

public final class MainServer {

    public static final String USER_PATH = "/user/";
    public static final String GOOD_PATH = "/good/";
    public static final String GOODS_PATH = "/goods";

    public static final String USER_ID_PARAM = "userId";

    public static final ObjectMapper mapper = new ObjectMapper();

    public static void main(final String[] args) {

        MainEnvironment env = MainEnvironment.newEnvironment(MainServer.class);

        HttpServer<ByteBuf, ByteBuf> server;

        // TODO: support three operations: add good, register user, get goods catalog for user

        server = HttpServer.newServer(8080)
                .start((req, resp) -> {
                    String path = req.getDecodedPath();
                    if (path.startsWith(USER_PATH)) {
                        path = path.substring(USER_PATH.length());
                        if (req.getHttpMethod().equals(HttpMethod.POST)) {
                            return resp.writeString(onPostResult(req, result -> env.getMongoClient().insertUser(result), User.class, resp));
                        } else if (req.getHttpMethod().equals(HttpMethod.DELETE)) {
                            return resp.writeString(env.getMongoClient().deleteUserById(path).map(el -> "Successfully deleted user " + el.toString() + "\n").reduce(String::concat));
                        } else if (req.getHttpMethod().equals(HttpMethod.GET)) {
                            return resp.writeString(env.getMongoClient().getAllUsers().map(el -> "Result: " + el.toString() + "\n").reduce(String::concat));
                        }
                    } else if (path.startsWith(GOOD_PATH)) {
                        path = path.substring(GOOD_PATH.length());
                        if (req.getHttpMethod().equals(HttpMethod.POST)) {
                            return resp.writeString(onPostResult(req, result -> env.getMongoClient().insertGood(result), Map.class, resp));
                        } else if (req.getHttpMethod().equals(HttpMethod.DELETE)) {
                            return resp.writeString(env.getMongoClient().deleteGoodById(path).map(el -> "Successfully deleted good " + el.toString() + "\n").reduce(String::concat));
                        }
                    } else if (path.startsWith(GOODS_PATH) && req.getQueryParameters() != null) {
                        List<String> userIds = req.getQueryParameters().get(USER_ID_PARAM);
                        if (userIds != null && !userIds.isEmpty()) {
                            String userId = userIds.get(0);
                            return resp.writeString(env.getMongoClient().getUserPreferredCurrencyById(userId)
                                    .switchMap(user -> env.getMongoClient().getAllGoodsWithCurrency(user.getPreferredCurrency()))
                                    .map(el -> "Result: " + el.toString() + "\n").reduce(String::concat));
                        }
                    }
                    return resp.writeString(just("Failed processing request!"));
                });
        if (env.shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        env.registerServerAddress(server.getServerAddress());
    }

    private static <T> Observable<String> onPostResult(HttpServerRequest<ByteBuf> request,
                                                       Function<T, Observable<Success>> mongoRequestFunction,
                                                       Class<T> documentClass, ResponseContentWriter<ByteBuf> responseWriter) {
        Observable<String> result = request.getContent().map(bb -> bb.toString(Charset.defaultCharset()));
        return result.reduce(String::concat).switchMap(response -> {
            try {
                return mongoRequestFunction.apply(mapper.readValue(response, documentClass))
                        .map(el -> "Successfully created " + el.toString() + "\n").reduce(String::concat);
            } catch (JsonProcessingException e) {
                return just(e.getMessage());
            }
        });
    }
}
