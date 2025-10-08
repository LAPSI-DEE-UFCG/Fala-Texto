Fechou! Agora que tenho todas as partes da sua API Flask, aqui está o guia completo com **requisições Kotlin para interagir com cada rota** — com destaque para autenticação, envio de arquivos, análise de resposta, e tratamento de token JWT.

---

## 🔐 1. Login – `POST /login`

**Objetivo**: obter o token JWT.

### Kotlin (com `OkHttp` e `Gson`)
```kotlin
val gson = Gson()
val json = gson.toJson(mapOf("username" to "Fala-texto", "password" to "Transcrição_de_fala_em_texto_api"))

val body = json.toRequestBody("application/json".toMediaType())
val request = Request.Builder()
    .url("http://<seu_host>:5000/login")
    .post(body)
    .build()

val response = client.newCall(request).execute()
val token = gson.fromJson(response.body?.string(), Map::class.java)["access_token"] as String
```

---

## 📄 2. Listar Campos do PDF – `POST /listar-campos`

**Protegida com JWT + upload de arquivo PDF**

```kotlin
val file = File("caminho/do/arquivo.pdf")
val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
    .addFormDataPart("file", file.name, file.asRequestBody("application/pdf".toMediaType()))
    .build()

val request = Request.Builder()
    .url("http://<seu_host>:5000/listar-campos")
    .addHeader("Authorization", "Bearer $token")
    .post(requestBody)
    .build()

val response = client.newCall(request).execute()
val json = response.body?.string()

val typeToken = object : TypeToken<MutableMap<String, Any?>>() {}.type
val campos: MutableMap<String, Any?> = gson.fromJson(json, typeToken)
val camposConvertidos: MutableMap<Pair<String, Int>, Any?> = mutableMapOf()

for ((chaveComposta, valor) in campos) {
    val partes = chaveComposta.split("|")
    if (partes.size == 2) {
        val nomeCampo = partes[0]
        val tipoCampo = partes[1].toIntOrNull()
        if (tipoCampo != null) {
            camposConvertidos[Pair(nomeCampo, tipoCampo)] = valor
        }
    }
}
```

---

## ✍️ 3. Preencher Campos do PDF – `POST /preencher-campos`

**PDF + campos no formato multipart/form-data**

```kotlin
import java.io.File

val file = File("documento.pdf")
val formBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
formBodyBuilder.addFormDataPart("file", file.name, file.asRequestBody("application/pdf".toMediaType()))

// Adicionar os campos com chaves no formato chave|tipo
for ((chave, valor) in dados) {
    val nomeCampo = "${chave.first}|${chave.second}"
    formBodyBuilder.addFormDataPart(nomeCampo, valor.toString())
}

val formBody = formBodyBuilder.build()

val request = Request.Builder()
    .url("http://<seu_host>:5000/preencher-campos")
    .addHeader("Authorization", "Bearer $token")
    .post(formBody)
    .build()

import java.util.UUID

val response = client.newCall(request).execute()
if (response.isSuccessful) {
    val pdfBytes = response.body?.bytes()
    // Salvar o PDF retornado:
    val nomeArquivo = "preenchido_${UUID.randomUUID()}.pdf"
    // Caminho para a pasta "Formulários" dentro de Downloads
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val pastaFormulario = File(downloadsDir, "Formulários")
    // Cria a pasta se não existir
    if (!pastaFormulario.exists()) {
        pastaFormulario.mkdirs()
    }
    // Cria o arquivo dentro da pasta
    val arquivoDestino = File(pastaFormulario, nomeArquivo)
    arquivoDestino.writeBytes(pdfBytes!!)
} else {
    println("Algo deu errado: código ${response.code}")
}

```

---

## 🎙️ 4. Transcrição de Áudio – `POST /transcricao`

**Envia áudio, recebe texto (ou erro)**

```kotlin
val audioFile = File("audio.wav")  // ou .mp3, .m4a, etc.
val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
    .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/wav".toMediaType()))
    .build()

val request = Request.Builder()
    .url("http://<seu_host>:5000/transcricao")
    .addHeader("Authorization", "Bearer $token")
    .post(requestBody)
    .build()

val response = client.newCall(request).execute()
val transcricao = gson.fromJson(response.body?.string(), Map::class.java)["text"] as String
```

---

## ✅ 5. Rota Inicial – `GET /`

```kotlin
val request = Request.Builder()
    .url("http://<seu_host>:5000/")
    .get()
    .build()

val response = client.newCall(request).execute()
val mensagem = gson.fromJson(response.body?.string(), Map::class.java)["message"]
```

---

Se quiser, posso organizar isso em um arquivo Kotlin completo, criar uma classe `ApiClient`, adicionar tratamento de erros HTTP, autenticação automática com refresh de token e mais. Quer que a gente leve esse código para o próximo nível? 😄🚀📲
