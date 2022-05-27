package com.forem.android.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.forem.android.app.model.CurrentForem
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/** Serializes [CurrentForem] for data-store proto. */
object CurrentForemSerializer : Serializer<CurrentForem> {
    override val defaultValue: CurrentForem = CurrentForem.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): CurrentForem {
        try {
            return CurrentForem.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: CurrentForem, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.currentForemDataStore: DataStore<CurrentForem> by dataStore(
    fileName = "currentForem.pb",
    serializer = CurrentForemSerializer
)
