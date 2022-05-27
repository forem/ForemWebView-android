package com.forem.android.data.remote.discover

import com.forem.android.app.model.Forem
import com.forem.android.app.model.ForemCollection
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

/** Converts response received from [DiscoverForemService] to [ForemCollection] proto. */
class DiscoverForemDeserializer : JsonDeserializer<ForemCollection?> {

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): ForemCollection {
        val foremJsonArray: JsonArray = json.asJsonObject.getAsJsonArray("forems")
        val foremCollectionBuilder = ForemCollection.newBuilder()
        for (forem in foremJsonArray) {
            val newForem = Forem.newBuilder()
                .setName(forem.asJsonObject.get("name").asString)
                .setLogo(forem.asJsonObject.get("logo").asString)
                .setHomePageUrl(forem.asJsonObject.get("homePageUrl").asString)
                .build()
            foremCollectionBuilder.putForem(newForem.homePageUrl, newForem)
        }
        return foremCollectionBuilder.build()
    }
}
