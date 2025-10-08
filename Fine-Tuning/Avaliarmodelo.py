import torch
import librosa
from transformers import WhisperProcessor, WhisperForConditionalGeneration

# Configurações
MODEL_PATH = "./whisper-small-finetuned"  
AUDIO_FILE_PATH = "/home/fala-texto/Documents/Fine-Tuning/O abatacepte é usado para tratar artrite reumatoide, ajudando a controlar a inflamação nas articulações.m4a" 

#'O abatacepte é usado para tratar artrite reumatoide, ajudando a controlar a inflamação nas articulações.m4a'
#'O abciximabe é um medicamento utilizado para evitar a formação de coágulos durante procedimentos cardíacos.m4a'
#'O ácido nicotínico (niacina) é uma vitamina B3 que ajuda a controlar os níveis de colesterol e triglicerídeos.m4a'
#'O acompanhamento psicopedagógico ajuda crianças com dificuldades de aprendizagem a desenvolver novas habilidades.m4a'
#'O adalimumabe é um medicamento usado para tratar doenças autoimunes, como artrite reumatoide e psoríase.m4a'

device = "cuda" if torch.cuda.is_available() else "cpu"

# Carregar modelo e processor
processor = WhisperProcessor.from_pretrained(MODEL_PATH)
model = WhisperForConditionalGeneration.from_pretrained(MODEL_PATH).to(device)

# Função de transcrição
def transcribe(audio_path):
    """
    Carrega um arquivo de áudio, reamostra, e retorna a transcrição.
    """
    # Carrega o áudio com librosa
    speech_array, sampling_rate = librosa.load(audio_path, sr=16000)
    
    # Processa o áudio
    input_features = processor(speech_array, sampling_rate=16000, return_tensors="pt").input_features
    
    # Move para o dispositivo correto
    input_features = input_features.to(device)
    
    # Gera a predição
    predicted_ids = model.generate(input_features)
    
    # Decodifica e retorna a transcrição
    transcription = processor.batch_decode(predicted_ids, skip_special_tokens=True)[0]
    return transcription

# Executa a transcrição e imprime ela
print(f"Transcrevendo o arquivo: {AUDIO_FILE_PATH}")
texto_transcrito = transcribe(AUDIO_FILE_PATH)
print("-" * 30)
print("Texto Transcrito:")
print(texto_transcrito)
print("-" * 30)