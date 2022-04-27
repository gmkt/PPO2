package com.itmo.ktelnoy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.Map;

@Data
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public final class Good {
    @BsonId
    @BsonProperty("_id")
    private ObjectId id;
    private String name;
    private Map<Currency, Double> currencies;

    public Good(String name, Map<Currency, Double> currencies) {
        this.id = ObjectId.get();
        this.name = name;
        this.currencies = currencies;
    }
}
