package com.itmo.ktelnoy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@Data
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public final class User {
    @BsonId
    @BsonProperty("_id")
    private ObjectId id;
    private String name;
    private String passHash;
    private Currency preferredCurrency;

    public User(String name, String passHash, Currency preferredCurrency) {
        this.id = ObjectId.get();
        this.name = name;
        this.passHash = passHash;
        this.preferredCurrency = preferredCurrency;
    }

}
