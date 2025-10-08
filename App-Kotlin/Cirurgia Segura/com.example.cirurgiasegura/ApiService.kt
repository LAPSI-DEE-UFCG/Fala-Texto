package com.example.uploadcirurgiasegura2

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part



interface ApiService {
    @Multipart
    @POST("preencher-pdf")
    fun uploadArquivos(
        @Part files: List<MultipartBody.Part>
    ): Call<ResponseBody>
}
