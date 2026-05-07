package com.example.cirurgiasegura.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Usamos um 'object' para criar um Singleton (uma única instância)
object ApiClient {

    private const val BASE_URL = "https://processarpdffalatex.zapto.org"

    // 'lazy' garante que o Retrofit só seja criado quando for usado pela 1ª vez
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Exposição pública do serviço já criado
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}