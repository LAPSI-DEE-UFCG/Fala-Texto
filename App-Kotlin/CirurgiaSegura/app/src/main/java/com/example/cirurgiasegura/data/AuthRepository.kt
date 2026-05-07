package com.example.cirurgiasegura.data

import com.example.cirurgiasegura.network.ApiClient
import com.example.cirurgiasegura.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthRepository {

    // 1. Interface de Callback
    // A Activity vai implementar isso para "ouvir" o resultado do login
    interface AuthCallback {
        fun onSuccess(token: String)
        fun onError(message: String)
    }

    fun login(callback: AuthCallback) {
        val api = ApiClient.apiService

        val loginData = HashMap<String, String>()
        loginData["username"] = "Fala-texto"
        loginData["password"] = "Transcrição_de_fala_em_texto_api"

        api.login(loginData).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    try {
                        val token = response.body()?.access_token
                        if (token != null) {
                            // 2. Sucesso: retorna o token formatado
                            callback.onSuccess("Bearer $token")
                        } else {
                            // 3. Erro: retorna a mensagem de erro
                            callback.onError("Token não encontrado na resposta")
                        }
                    } catch (e: Exception) {
                        callback.onError("Erro ao interpretar token: ${e.message}")
                    }
                } else {
                    callback.onError("Erro no login: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                callback.onError("Falha na conexão: ${t.message}")
            }
        })
    }
}