package com.itmo.ktelnoy.utils;

import com.itmo.ktelnoy.model.Currency;
import com.itmo.ktelnoy.model.Good;
import com.itmo.ktelnoy.model.User;
import com.mongodb.annotations.ThreadSafe;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.Success;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import rx.Observable;

import java.util.Map;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static org.bson.codecs.configuration.CodecRegistries.*;

@ThreadSafe
public class MongoDriver {

    public static final String USERS_COLLECTION = "users";
    public static final String GOODS_COLLECTION = "goods";

    private MongoClient mongoClient;
    private CodecRegistry codecRegistry;
    private final String database;

    public MongoDriver(String database) {
        this.database = database;
        initMongoClient();
    }

    public <T> Observable<Success> insertDocumentToCollection(String collectionName, T document, Class<T> documentClass) {
        return getCollection(collectionName, documentClass).insertOne(document);
    }

    public <T> Observable<DeleteResult> deleteDocumentFromCollection(String collectionName, Bson filter, Class<T> documentClass) {
        return getCollection(collectionName, documentClass).deleteOne(filter);
    }

    public Bson currencyFilter(Currency currency) {
        return exists(Good.Fields.currencies + "." + currency.name());
    }

    /** USERS **/

    public Observable<User> getAllUsers() {
        return getCollection(USERS_COLLECTION, User.class).find().toObservable();
    }

    public Observable<User> getUserPreferredCurrencyById(String userId) {
        return getDocumentProjectedByQuery(USERS_COLLECTION, eq(User.Fields.id, userId), include(User.Fields.preferredCurrency), User.class);
    }

    public Observable<Success> insertUser(User user) {
        return insertDocumentToCollection(USERS_COLLECTION, user, User.class);
    }


    public Observable<DeleteResult> deleteUserById(String userId) {
        return deleteDocumentFromCollection(USERS_COLLECTION, eq(User.Fields.id, userId), User.class);
    }

    /** GOODS **/

    public Observable<Map> getAllGoodsWithCurrency(Currency currency) {
        return getAllDocumentsProjectedByQuery(GOODS_COLLECTION, currencyFilter(currency),
                include(Good.Fields.name, Good.Fields.currencies + "." + currency.name()), Map.class);
    }

    public Observable<Success> insertGood(Map good) {
        return insertDocumentToCollection(GOODS_COLLECTION, good, Map.class);
    }

    public Observable<DeleteResult> deleteGoodById(String goodId) {
        return deleteDocumentFromCollection(GOODS_COLLECTION, eq(Good.Fields.id, goodId), Map.class);
    }


    private <T> MongoCollection<T> getCollection(String collectionName, Class<T> documentClass) {
        return mongoClient.getDatabase(database).withCodecRegistry(codecRegistry).getCollection(collectionName, documentClass);
    }

    private <T> Observable<T> getAllDocumentsProjectedByQuery(String collectionName, Bson filter, Bson projection, Class<T> documentClass) {
        return getCollection(collectionName, documentClass).find(filter, documentClass).projection(projection).toObservable();
    }

    private <T> Observable<T> getDocumentProjectedByQuery(String collectionName, Bson filter, Bson projection, Class<T> documentClass) {
        return getCollection(collectionName, documentClass).find(filter, documentClass).projection(projection).first();
    }

    private void initMongoClient() {
        mongoClient = MongoClients.create();
        codecRegistry = fromRegistries(MongoClients.getDefaultCodecRegistry(), fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    }

}
