package com.cloudimny.server

import android.content.Context
import android.net.Uri
import com.cloudimny.models.meta.Artist
import com.cloudimny.models.meta.Playlist
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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.UUID

data class CreatePlaylistRequest(val name: String, val trackIds: List<UUID>)

interface TrackApi {
    @GET("api/v1/track")
    suspend fun getAllTracks(): List<Track>

    @Multipart
    @POST("api/v1/upload")
    suspend fun uploadTrack(
        @Part("meta") track: RequestBody,
        @Part file: MultipartBody.Part
    ): Track

    @GET("api/v1/playlist")
    suspend fun getAllPlaylists(): List<Playlist>

    @GET("api/v1/playlist/{id}")
    suspend fun getPlaylist(@Path("id") id: UUID): Playlist

    @POST("api/v1/playlist")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Playlist
}

object ServerRepository {
    suspend fun loadAllTracks(context: Context): List<Track> =
        RetrofitClient.trackApi(context).getAllTracks()

    suspend fun uploadTrack(context: Context, fileUri: Uri, title: String, artist: String): Track {
        val track =
            Track(null, title, Artist(null, artist)) // using null, bc objects (may) not exist
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

    fun streamingUrl(context: Context, trackId: UUID): String =
        "${RetrofitClient.baseUrl(context)}api/v1/streaming/$trackId"

    suspend fun loadAllPlaylists(context: Context): List<Playlist> =
        RetrofitClient.trackApi(context).getAllPlaylists()

    suspend fun loadPlaylist(context: Context, id: UUID): Playlist =
        RetrofitClient.trackApi(context).getPlaylist(id)

    suspend fun createPlaylist(context: Context, name: String, trackIds: List<UUID>): Playlist =
        RetrofitClient.trackApi(context).createPlaylist(CreatePlaylistRequest(name, trackIds))
}

private object RetrofitClient {
    private var trackApi: TrackApi? = null
    private var cachedBaseUrl: String? = null

    val gson = GsonBuilder()
        .registerTypeAdapter(UUID::class.java, object : TypeAdapter<UUID>() {
            override fun write(out: JsonWriter, value: UUID?) {
                out.value(value?.toString())
            }

            override fun read(input: JsonReader): UUID = UUID.fromString(input.nextString())
        })
        .create()

    fun baseUrl(context: Context): String {
        val host = ServerCertificateStore.host(context)
            ?: error("Server host is not configured")
        return "https://$host/"
    }

    fun trackApi(context: Context): TrackApi {
        val baseUrl = baseUrl(context)

        var api = trackApi
        if (api == null || cachedBaseUrl != baseUrl) {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val secret = ServerCertificateStore.authSecret(context)
                    val request = if (secret != null) {
                        chain.request().newBuilder()
                            .addHeader("Authorization", secret)
                            .build()
                    } else {
                        chain.request()
                    }
                    chain.proceed(request)
                }
                .sslSocketFactory(
                    ServerCertificateStore.sslContext(context).socketFactory,
                    ServerCertificateStore.trustManager(context)
                )
                .build()

            api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(TrackApi::class.java)

            trackApi = api
            cachedBaseUrl = baseUrl
        }

        return api
    }
}
