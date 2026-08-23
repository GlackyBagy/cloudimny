package com.cloudimny.server

import android.content.Context
import android.net.Uri
import com.cloudimny.models.meta.Artist
import com.cloudimny.models.meta.Playlist
import com.cloudimny.models.meta.Track
import com.cloudimny.server.security.ServerCertificateStore
import com.cloudimny.util.displayName
import com.google.gson.Gson
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
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @PUT("api/v1/track/{id}")
    suspend fun editTrack(@Path("id") id: UUID, @Body track: RequestBody): Track

    @PUT("api/v1/artist/{id}")
    suspend fun editArtist(@Path("id") id: UUID, @Body artist: RequestBody): Artist

    @DELETE("api/v1/track/{id}")
    suspend fun deleteTrack(@Path("id") id: UUID)

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
        val trackPart = toJson(track)

        val filePart = MultipartBody.Part.createFormData(
            "file",
            displayName(context, fileUri),
            uriRequestBody(context, fileUri)
        )

        return RetrofitClient.trackApi(context).uploadTrack(trackPart, filePart)
    }

    suspend fun deleteTrack(context: Context, id: UUID) =
        RetrofitClient.trackApi(context).deleteTrack(id)

    suspend fun editTrack(context: Context, track: Track) =
         RetrofitClient.trackApi(context).editTrack(track.id!!, toJson(track))

    suspend fun editArtist(context: Context, artist: Artist) =
        RetrofitClient.trackApi(context).editArtist(artist.id!!, toJson(artist))

    private fun toJson(obj: Any) =
        RetrofitClient.gson.toJson(obj)
            .toRequestBody("application/json".toMediaType())

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

    fun coverUrl(context: Context, trackId: UUID): String =
        "${RetrofitClient.baseUrl(context)}api/v1/cover?trackId=$trackId"

    fun httpClient(context: Context): OkHttpClient = RetrofitClient.httpClient(context)

    suspend fun loadAllPlaylists(context: Context): List<Playlist> =
        RetrofitClient.trackApi(context).getAllPlaylists()

    suspend fun loadPlaylist(context: Context, id: UUID): Playlist =
        RetrofitClient.trackApi(context).getPlaylist(id)

    suspend fun createPlaylist(context: Context, name: String, trackIds: List<UUID>): Playlist =
        RetrofitClient.trackApi(context).createPlaylist(CreatePlaylistRequest(name, trackIds))
}

private object RetrofitClient {
    private var trackApi: TrackApi? = null
    private var cachedHttpClient: OkHttpClient? = null
    private var cachedBaseUrl: String? = null

    val gson: Gson = GsonBuilder()
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

    fun httpClient(context: Context): OkHttpClient {
        val baseUrl = baseUrl(context)

        var client = cachedHttpClient
        if (client == null || cachedBaseUrl != baseUrl) {
            client = OkHttpClient.Builder()
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

            cachedHttpClient = client
            cachedBaseUrl = baseUrl
            trackApi = null
        }

        return client
    }

    fun trackApi(context: Context): TrackApi {
        val client = httpClient(context)

        var api = trackApi
        if (api == null) {
            api = Retrofit.Builder()
                .baseUrl(baseUrl(context))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(TrackApi::class.java)

            trackApi = api
        }

        return api
    }
}
