package net.aotter.springbootwebmongo.controller;

import java.util.Calendar;
import java.util.Date;

import com.mongodb.client.model.*;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Controller {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/report")
    public String report() {
        // Note: remember to add unique index to "hour" field
        Document result = mongoTemplate.getCollection("SpringBootWebMongo")
                .findOneAndUpdate(
                        Filters.eq("hour", DateUtils.round(new Date(), Calendar.HOUR)),
                        Updates.inc("count", 1L),
                        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
                );
        return String.format("#%d", result.getLong("count"));
    }

}
