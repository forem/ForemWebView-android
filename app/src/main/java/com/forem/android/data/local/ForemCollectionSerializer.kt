package com.forem.android.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.forem.android.app.model.ForemCollection
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/** Serializes [ForemCollection] for data-store proto. */
object ForemCollectionSerializer : Serializer<ForemCollection> {
    override val defaultValue: ForemCollection = ForemCollection.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): ForemCollection {
        try {
            return ForemCollection.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: ForemCollection, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.myForemCollectionDataStore: DataStore<ForemCollection> by dataStore(
    fileName = "myForemCollection.pb",
    serializer = ForemCollectionSerializer
)

val Context.discoverForemCollectionDataStore: DataStore<ForemCollection> by dataStore(
    fileName = "discoverForemCollection.pb",
    serializer = ForemCollectionSerializer
)
