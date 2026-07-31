package com.cloudimny.server

import android.content.Context
import android.net.Uri
import com.cloudimny.models.meta.Artist
import com.cloudimny.models.meta.Track
import com.cloudimny.server.security.ServerCertificateStore
import com.cloudimny.util.displayName
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.UUID

interface TrackApi {
    @GET("api/v1/track")
    suspend fun getAllTracks(): List<Track>

    @Multipart
    @POST("api/v1/upload")
    suspend fun uploadTrack(
        @Part("track") track: RequestBody,
        @Part file: MultipartBody.Part
    ): Track
}

object ServerRepository {
    suspend fun loadAllTracks(context: Context): List<Track> =
        RetrofitClient.trackApi(context).getAllTracks()

    suspend fun uploadTrack(context: Context, fileUri: Uri, title: String, artist: String): Track {
        val track = Track(null, title, Artist(null, artist)) // using null, bc objects (may) not exist
        val trackPart = RetrofitClient.gson.toJson(track)
            .toRequestBody("application/json".toMediaType())

        val filePart = MultipartBody.Part.createFormData(
            "file",
            displayName(context, fileUri),
            uriRequestBody(context, fileUri)
        )

        return RetrofitClient.trackApi(context).uploadTrack(trackPart, filePart)
    }

    private fun uriRequestBody(context: Context, uri: Uri): RequestBody =
        object : RequestBody() {
            override fun contentType() = context.contentResolver.getType(uri)?.toMediaType()

            override fun contentLength(): Long =
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1

            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open selected file")
                input.use { sink.writeAll(it.source()) }
            }
        }
}

private object RetrofitClient {
    private var trackApi: TrackApi? = null
    private var cachedHost: String? = null

    val gson = GsonBuilder()
        .registerTypeAdapter(UUID::class.java, object : TypeAdapter<UUID>() {
            override fun write(out: JsonWriter, value: UUID?) {
                out.value(value?.toString())
            }

            override fun read(input: JsonReader): UUID = UUID.fromString(input.nextString())
        })
        .create()

    fun trackApi(context: Context): TrackApi {
        val host = ServerCertificateStore.host(context)
            ?: error("Server host is not configured")

        var api = trackApi
        if (api == null || cachedHost != host) {
            val client = OkHttpClient.Builder()
                .sslSocketFactory(
                    ServerCertificateStore.sslContext(context).socketFactory,
                    ServerCertificateStore.trustManager(context)
                )
                .build()

            api = Retrofit.Builder()
                .baseUrl("https://$host/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(TrackApi::class.java)

            trackApi = api
            cachedHost = host
        }

        return api
    }
}
