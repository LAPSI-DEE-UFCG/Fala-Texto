# SPDX-License-Identifier: Apache-2.0
# Copyright 2025 Lapsi/Fala-texto


# manipulação de PDFs, processamento de áudio e outras utilidades.

# Importações necessárias para criar a API, gerenciar autenticação, limites de acesso,
from flask import Flask, request, jsonify, send_file, render_template
from flask_jwt_extended import JWTManager, create_access_token, jwt_required
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
import fitz  # PyMuPDF: biblioteca para manipulação de arquivos PDF
import os
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
from datetime import timedelta
import whisper  # Biblioteca para transcrição de áudio
import uuid  # Geração de IDs únicos para nomear arquivos
import torch  # Biblioteca para computação com GPU/CPU
import librosa  # Biblioteca para processamento de áudio
import numpy as np  # Biblioteca para operações numéricas
from deepface import DeepFace
import glob
import threading


# Gera uma chave secreta para a API (usada para JWT e outras operações de segurança)
secret_key = os.urandom(32)
# Gera os hashes das senhas para autenticar usuários (as senhas originais estão em texto claro)
hash_senha = generate_password_hash("Transcrição_de_fala_em_texto_api")
hash_senha2 = generate_password_hash("Transcrição_de_fala_api")

# Inicializa a aplicação Flask e configura parâmetros essenciais
app = Flask(__name__)
app.config['JWT_SECRET_KEY'] = secret_key                                     # Chave secreta para JWT
app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(hours=6)                   # Token expira em 6 horas
app.config['UPLOAD_FOLDER'] = 'uploads'
app.config['IMAGE_FOLDER'] = 'imagens'                                # Diretório para armazenar uploads
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)                       # Cria o diretório, se não existir
os.makedirs(app.config['IMAGE_FOLDER'], exist_ok=True)

# Configuração do JWTManager e do Limiter para controlar a taxa de requisições
jwt = JWTManager(app)
limiter = Limiter(app=app, key_func=get_remote_address)

# Lista de usuários com senhas (armazenadas como hashes) para autenticação
usuarios = {
    'Fala-texto': hash_senha,
    'whisperadm': hash_senha2
}

# Verifica se uma GPU (CUDA) está disponível para operações com Torch,
# se não, usa a CPU.
device = "cuda" if torch.cuda.is_available() else "cpu"

# Carrega o modelo Whisper (modelo "medium") para a transcrição do áudio, alocando-o no dispositivo adequado.
modelo = whisper.load_model("turbo", device=device)

# ------------------------------------------------------------------------------
# Função: transcricao_pdf
# Realiza a transcrição de um arquivo de áudio usando o modelo Whisper.
# ------------------------------------------------------------------------------
def transcricao_pdf(audio):
    try:
        # Transcreve o áudio com o parâmetro fp16 e define o idioma para Português ("pt")
        result = modelo.transcribe(audio, fp16=True, language="pt")
        return result
    except Exception as e:
        # Em caso de erro, retorna um dicionário com a mensagem de erro
        return {"error": str(e)}

# ------------------------------------------------------------------------------
# Função: listar_campos_pdf
# Extrai os campos de formulário (widgets) de um PDF utilizando a biblioteca PyMuPDF.
# ------------------------------------------------------------------------------
def listar_campos_pdf(pdf_path):
    try:
        pdf_document = fitz.open(pdf_path)  # Abre o documento PDF
        dados = {}  # Inicializa um dicionário para armazenar os campos
        # Itera por todas as páginas do PDF
        for page_num in range(pdf_document.page_count):
            page = pdf_document.load_page(page_num)
            annotations = page.widgets()  # Recupera os widgets/formulários da página
            if annotations:
                for annot in annotations:
                    # Junta o nome do campo e o tipo em uma única string chave
                    chave = f"{annot.field_name}|{annot.field_type}"
                    dados[chave] = None  # Inicialmente, o valor do campo é None
        return dados
    except Exception as e:
        return {"error": str(e)}

# ------------------------------------------------------------------------------
# Função: preencher_campos_pdf
# Preenche campos de formulário de um PDF com os dados fornecidos e salva o novo PDF.
# ------------------------------------------------------------------------------
def preencher_campos_pdf(pdf_path, output_path, data):
    try:
        pdf_document = fitz.open(pdf_path)
        # Itera por cada página do PDF
        for page_num in range(pdf_document.page_count):
            page = pdf_document.load_page(page_num)
            annotations = page.widgets()  # Recupera os widgets/formulários
            if annotations:
                for annot in annotations:
                    field_name = annot.field_name  # Nome do campo no PDF
                    # Verifica nos dados enviados se existe informação para preencher esse campo
                    for campo in data.keys():
                        if field_name == campo[0]:
                            annot.field_value = data[campo]  # Preenche o campo com o valor adequado
                            annot.update()  # Atualiza o widget com o novo valor
        pdf_document.save(output_path)  # Salva o PDF preenchido no caminho especificado
        return output_path
    except Exception as e:
        return {"error": str(e)}

# ------------------------------------------------------------------------------
# Função: calculate_snr_speech
# Calcula a relação sinal-ruído (SNR) de um arquivo de áudio para avaliar sua qualidade.
# ------------------------------------------------------------------------------
def calculate_snr_speech(audio_path):
    # Carrega o áudio usando o Librosa sem alterar a taxa de amostragem (sr=None)
    y, sr = librosa.load(audio_path, sr=None)
    # Detecta intervalos não silenciosos para identificar as partes com fala
    intervals = librosa.effects.split(y, top_db=20)
    # Calcula a potência do sinal para os intervalos identificados (partes com fala)
    signal_power = np.mean([np.mean(np.square(y[start:end])) for start, end in intervals])
    # Se existir mais de um intervalo, calcula a potência do ruído com base nas partes intermediárias entre falas
    if len(intervals) > 1:
        noise_intervals = np.concatenate([y[i:j] for i, j in zip(intervals[:-1, 1], intervals[1:, 0])])
        noise_power = np.mean(np.square(noise_intervals))
    else:
        # Se não houver intervalos suficientes, usa os últimos 10% do áudio como estimativa de ruído
        noise_power = np.mean(np.square(y[-int(sr * 0.1):]))
    # Calcula o SNR (em decibéis) utilizando a razão entre potência do sinal e do ruído
    snr = 10 * np.log10(signal_power / noise_power)
    return snr

# ------------------------------------------------------------------------------
# Função: analyze_audio
# Analisa diversas características do áudio, como amplitude RMS, frequência fundamental e
# espectro (centroide espectral) para auxiliar na avaliação da qualidade do áudio.
# ------------------------------------------------------------------------------
def analyze_audio(audio_path):
    y, sr = librosa.load(audio_path, sr=None)
    # Calcula a amplitude RMS (Root Mean Square) do áudio
    rms = np.mean(librosa.feature.rms(y=y))
    # Calcula a frequência fundamental utilizando a função piptrack; retira frequências nulas
    pitches, magnitudes = librosa.core.piptrack(y=y, sr=sr)
    pitch = np.mean(pitches[pitches > 0])  # Médias das frequências detectadas
    # Calcula o centroide espectral, que indica a "brilho" do som
    spectral_centroid = np.mean(librosa.feature.spectral_centroid(y=y, sr=sr))
    return rms, pitch, spectral_centroid

###################################################
def authenticate_face_multi(captura):
        """
        Realiza a autenticação comparando a face capturada com as imagens cadastradas
        (em diferentes ângulos) para o usuário. Se qualquer verificação for bem-sucedida,
        a autenticação é considerada positiva.
        """
        # Carrega todas as imagens registradas para o usuário
        diretorio_atual = os.path.abspath(os.path.dirname(__file__))
        caminho_destino = os.path.join(diretorio_atual,"registered_faces")
        #pattern = os.path.join(caminho_destino, f"registered_*.jpg")
        pattern = os.path.join(app.config['IMAGE_FOLDER'], f"registered_*.jpg")
        reference_images = glob.glob(pattern)

        if not reference_images:
            print("Nenhuma imagem cadastrada encontrada para o usuário. Realize o cadastro primeiro.")
            return False

        auth_filename = captura
        # Compara a imagem de autenticação com cada uma das imagens registradas
        for ref_image in reference_images:
            try:
                result = DeepFace.verify(ref_image, auth_filename, enforce_detection=True,model_name='ArcFace', detector_backend="mtcnn")
                
                if result.get("verified"):
                    print(f"Autenticação bem-sucedida !!!")
                    return True
            except Exception as e:
                print(f"Erro ao verificar com a imagem {ref_image}: {e}")

        print("Falha na autenticação! A face não corresponde a nenhuma das imagens registradas.")
        return False

#############################################################
def extract_pdf_image(pdf_path, image_path):
        """Extrai a primeira página do PDF e salva como imagem"""
        doc = fitz.open(pdf_path)
        page = doc[0]
        pix = page.get_pixmap()
        pix.save(image_path)

# ------------------------------------------------------------------------------
# Rota: /
# Rota principal que retorna uma mensagem de boas-vindas à API.
# ------------------------------------------------------------------------------
@app.route('/')
def home():
    #return jsonify({"message": "Bem-vindo à API!"})
    return render_template('index.html')

# ------------------------------------------------------------------------------
# Rota: /login
# Validação de credenciais do usuário e criação de um token JWT para autenticação.
# ------------------------------------------------------------------------------
@app.route('/login', methods=['POST'])
def login():
    username = request.json.get('username')
    password = request.json.get('password')
    # Verifica se o usuário existe e se a senha fornecida corresponde ao hash armazenado
    if username in usuarios and check_password_hash(usuarios[username], password):
        # Cria um token JWT de acesso com a identidade do usuário
        access_token = create_access_token(identity=str(username))
        return jsonify(access_token=access_token.decode("utf-8"))
    else:
        return jsonify({"msg": "Nome de usuário ou senha incorretos"}), 401

# ------------------------------------------------------------------------------
# Rota: /listar-campos
# Extrai e retorna os campos de formulário de um PDF enviado via requisição.
# Limite de 15 requisições por minuto e acesso protegido por JWT.
# ------------------------------------------------------------------------------
@app.route('/listar-campos', methods=['POST'])
@jwt_required()
@limiter.limit("15 per minute")
def listar_campos():
    if 'file' not in request.files:
        return jsonify({"error": "Nenhum arquivo encontrado"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "Nenhum arquivo selecionado"}), 400
    # Gera um nome único para o arquivo enviado para evitar conflitos
    unique_filename = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
    file.save(file_path)  # Salva o arquivo no diretório de uploads
    campos = listar_campos_pdf(file_path)  # Extrai os campos do PDF
    os.remove(file_path)  # Remove o arquivo temporário após o processamento
    return jsonify(campos)

# ------------------------------------------------------------------------------
# Rota: /preencher-campos
# Preenche os campos de um PDF com os dados fornecidos e retorna o PDF preenchido.
# Utiliza proteção JWT e limite de 15 requisições por minuto.
# ------------------------------------------------------------------------------
@app.route('/preencher-campos', methods=['POST'])
@jwt_required()
@limiter.limit("15 per minute")
def preencher_campos():
    dados = {}
    if 'file' not in request.files:
        return jsonify({"error": "Nenhum arquivo encontrado"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "Nenhum arquivo selecionado"}), 400
    unique_filename = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
    file.save(file_path)

    # Define um nome único para o arquivo PDF preenchido
    output_filename = f"{uuid.uuid4()}_preenchido_teste.pdf"
    output_path = os.path.join(app.config['UPLOAD_FOLDER'], output_filename)
    # Converte os valores do formulário para um dicionário
    data = request.form.to_dict()
    # Processa cada par chave-valor para definir os tipos corretos com base em informações do campo
    for c, v in data.items():
        chaves = c.split('|')
        if int(chaves[1]) == 7:
            dados[(chaves[0], int(chaves[1]))] = v
        elif int(chaves[1]) == 5:
            dados[(chaves[0], int(chaves[1]))] = int(v)
        elif int(chaves[1]) == 2:
            dados[(chaves[0], int(chaves[1]))] = bool(v)
    # Preenche o PDF com os dados e obtém o caminho para o PDF resultante
    resultado = preencher_campos_pdf(file_path, output_path, dados)
    os.remove(file_path)  # Remove o arquivo original
    resposta = send_file(resultado, as_attachment=True)
   
    # no linux
    #@resposta.call_on_close
    #def remover_arquivo():
        #try:
        #    os.remove(resultado)
        #    print(f'Arquivo {resultado} deletado com sucesso.')
        #except Exception as e:
        #    print(f'Erro ao deletar o arquivo: {e}')
    
    # no windows
    def remover_depois(path, delay=0.5):
        def remover():
            try:
                os.remove(path)
                print(f'Arquivo {path} deletado com sucesso.')
            except Exception as e:
                print(f'Erro ao deletar o arquivo: {e}')
        threading.Timer(delay, remover).start()
    remover_depois(resultado)

    return resposta


############################################################
@app.route('/autenticacao', methods=['POST'])
@jwt_required()
@limiter.limit("15 per minute")
def autenticacao():
    if 'file' not in request.files:
        return jsonify({"error": "Nenhum arquivo encontrado"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "Nenhum arquivo selecionado"}), 400
    unique_filename = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    file_path = os.path.join(app.config['IMAGE_FOLDER'], unique_filename)
    file.save(file_path)
    
    verifica = authenticate_face_multi(file_path)
    autentica = {'analise': verifica}
    os.remove(file_path)  # Remove o arquivo de imagem após o processamento
    return jsonify(autentica)


############################################################
@app.route('/imagem', methods=['POST'])
@jwt_required()
@limiter.limit("15 per minute")
def imagem():
    if 'file' not in request.files:
        return jsonify({"error": "Nenhum arquivo encontrado"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "Nenhum arquivo selecionado"}), 400
    unique_filename = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
    file.save(file_path)
    
    caminho_arquivo = os.path.join(app.config['IMAGE_FOLDER'],f"{uuid.uuid4()}_pagina.png")
    extract_pdf_image(file_path, caminho_arquivo)
    os.remove(file_path)  # Remove o arquivo de imagem após o processamento
    resposta = send_file(caminho_arquivo, as_attachment=True)
    
    # no linux
    #@resposta.call_on_close
    #def remover_arquivo():
        #try:
        #    os.remove(caminho_arquivo)
        #    print(f'Arquivo {caminho_arquivo} deletado com sucesso.')
        #except Exception as e:
        #    print(f'Erro ao deletar o arquivo: {e}')
    
    # no windows
    def remover_depois(path, delay=0.5):
        def remover():
            try:
                os.remove(path)
                print(f'Arquivo {path} deletado com sucesso.')
            except Exception as e:
                print(f'Erro ao deletar o arquivo: {e}')
        threading.Timer(delay, remover).start()
    remover_depois(caminho_arquivo)

    return resposta


# ------------------------------------------------------------------------------
# Rota: /transcricao
# Recebe um arquivo de áudio, verifica sua qualidade (SNR, RMS, pitch, spectral centroid)
# e, se aceitável, realiza a transcrição usando o modelo Whisper.
# Limite de 15 requisições por minuto, acesso protegido por JWT.
# ------------------------------------------------------------------------------
@app.route('/transcricao', methods=['POST'])
@jwt_required()
@limiter.limit("15 per minute")
def transcricao():
    if 'file' not in request.files:
        return jsonify({"error": "Nenhum arquivo encontrado"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "Nenhum arquivo selecionado"}), 400
    unique_filename = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
    file.save(file_path)
    # Calcula a relação sinal-ruído e outras métricas acústicas do áudio
    snr_value = calculate_snr_speech(file_path)
    rms, pitch, spectral_centroid = analyze_audio(file_path)
    # Se o áudio apresentar boa qualidade, realiza a transcrição; caso contrário, retorna erro.
    if snr_value > 12 and pitch > 100 and rms >= 0.009 and spectral_centroid > 700:
        texto = transcricao_pdf(file_path)
    else:
        texto = {'erro': 'audio ruim'}
    os.remove(file_path)  # Remove o arquivo de áudio após o processamento
    return jsonify(texto)

# -----------------------------------------------------------------------------------------
@app.route('/upload-imagem', methods=['POST'])
@jwt_required()
@limiter.limit("10 per minute")
def upload_imagem():
    if 'file' not in request.files:
        return jsonify({"error": "Nenhum arquivo enviado"}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "Nome do arquivo vazio"}), 400

    # Salvar com nome único
    filename = f"registered_{uuid.uuid4()}.jpg"
    file_path = os.path.join(app.config['IMAGE_FOLDER'], filename)
    file.save(file_path)
    try:
        result = DeepFace.extract_faces(img_path=file_path,detector_backend="mtcnn")
        if len(result) == 1:
            print("Rosto detectado!")
            return jsonify({"mensagem": "Imagem recebida com sucesso"}), 200
        else:
            os.remove(file_path)
            return jsonify({"mensagem": "Imagem com muitos rostos, arquivo não foi salvo."}), 400
    except:
        print("Nenhum rosto encontrado.")
        os.remove(file_path)
        return jsonify({"mensagem": "Imagem sem face, arquivo não foi salvo."}), 400


# ------------------------------------------------------------------------------
# Inicializa a aplicação Flask configurada para HTTPS, escutando em todas as interfaces na porta 5000.
# ------------------------------------------------------------------------------
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)


