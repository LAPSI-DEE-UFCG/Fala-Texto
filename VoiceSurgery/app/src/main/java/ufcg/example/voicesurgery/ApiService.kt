package ufcg.example.voicesurgery

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("login")
    fun login(@Body loginData: Map<String, String>): Call<MainActivity.LoginResponse>

    @Multipart
    @POST("preencher-pdf")
    fun uploadArquivos(
        @Header("Authorization") authHeader: String,

        @Part files: List<MultipartBody.Part>
    ): Call<ResponseBody>
}