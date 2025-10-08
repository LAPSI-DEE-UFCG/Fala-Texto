# SPDX-License-Identifier: Apache-2.0
# Copyright 2025 Lapsi/Fala-texto

# imports 
import os
import uuid
import glob
from typing import Dict, Any, Optional, List
import pandas as pd 
import torch
import whisper
import fitz       # PyMuPDF
import librosa
import soundfile as sf
import noisereduce as nr
from pydub import AudioSegment
import numpy as np
from deepface import DeepFace
import secrets

from fastapi import (
    FastAPI, HTTPException, Request,
    UploadFile, File, Depends, BackgroundTasks, Request
)
from fastapi.responses import JSONResponse, FileResponse, HTMLResponse
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from fastapi_jwt_auth import AuthJWT
from fastapi_jwt_auth.exceptions import AuthJWTException
from pydantic import BaseModel

from slowapi import Limiter
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware

from passlib.context import CryptContext
from werkzeug.utils import secure_filename
from fastapi.middleware.cors import CORSMiddleware

origins = [
    "https://processarcadastro.cyberedu.com.br",  # substitua pelo domínio real
]

# 1) CONFIGURAÇÕES DE JWT
class JWTSettings(BaseModel):
    authjwt_secret_key: str = secrets.token_hex(32)
    authjwt_access_token_expires: int = 6 * 60 * 60  # 6 horas

@AuthJWT.load_config
def get_jwt_settings() -> JWTSettings:
    return JWTSettings()


# 2) INICIALIZANDO O APP & RATE-LIMITER
app = FastAPI(title="API FastAPI: PDF, Áudio e Face",docs_url=None, redoc_url=None, openapi_url=None)
app.mount("/static", StaticFiles(directory="static"), name="static")
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
templates = Jinja2Templates(directory="templates")
limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_middleware(SlowAPIMiddleware)

@app.exception_handler(RateLimitExceeded)
def rate_limit_handler(request, exc):
    return JSONResponse({"detail": "Rate limit exceeded"}, status_code=429)

@app.exception_handler(AuthJWTException)
def authjwt_exception_handler(request, exc):
    return JSONResponse({"detail": exc.message}, status_code=exc.status_code)


# 3) USUARIOS DO "DB" & PASSWORD HASHING
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
usuarios = {
    "Fala-texto": pwd_context.hash("Transcrição_de_fala_em_texto_api"),
    "whisperadm":  pwd_context.hash("Transcrição_de_fala_api"),
}


# 4) DIRETORIOS DE UPLOAD
UPLOAD_FOLDER = "uploads"
IMAGE_FOLDER  = "imagens"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(IMAGE_FOLDER,  exist_ok=True)


# 5) DEFININDO O DISPOSITIVO & MODELO WHISPER 
device = "cuda" if torch.cuda.is_available() else "cpu"
modelo = whisper.load_model("turbo", device=device)


# 6) FUNÇÕES DE PROCESSAMENTO
def transcricao_pdf(audio_path: str) -> Dict[str, Any]:
    try:
        use_fp16 = True if device == "cuda" else False
        return modelo.transcribe(audio_path, fp16=use_fp16, language="pt")
    except Exception as e:
        return {"error": str(e)}

def listar_campos_pdf(pdf_path: str) -> Dict[str, Optional[str]]:
    try:
        doc = fitz.open(pdf_path)
        campos: Dict[str, Optional[str]] = {}
        for i in range(doc.page_count):
            for w in doc.load_page(i).widgets() or []:
                key = f"{w.field_name}|{w.field_type}"
                campos[key] = None
        return campos
    except Exception as e:
        return {"error": str(e)}

def preencher_campos_pdf(pdf_path: str, output_path: str, data: Dict[str, Any]) -> Any:
    try:
        doc = fitz.open(pdf_path)
        for i in range(doc.page_count):
            for w in doc.load_page(i).widgets() or []:
                if w.field_name in data:
                    w.field_value = data[w.field_name]
                    w.update()
        doc.save(output_path)
        return output_path
    except Exception as e:
        return {"error": str(e)}

def calculate_snr_speech(audio_path: str) -> float:
    y, sr = librosa.load(audio_path, sr=None)
    intervals = librosa.effects.split(y, top_db=20)
    signal_p = np.mean([np.mean(y[s:e]**2) for s, e in intervals])
    if len(intervals) > 1:
        noise = np.concatenate([y[j1:i2] for (_, j1), (i2, _) in zip(intervals, intervals[1:])])
        noise_p = np.mean(noise**2)
    else:
        noise_p = np.mean(y[-int(sr*0.1):]**2)
    return 10 * np.log10(signal_p / noise_p)

def analyze_audio(audio_path: str):
    y, sr = librosa.load(audio_path, sr=None)
    rms = float(np.mean(librosa.feature.rms(y=y)))
    pitches, _ = librosa.core.piptrack(y=y, sr=sr)
    pitch = float(np.mean(pitches[pitches > 0]))
    centroid = float(np.mean(librosa.feature.spectral_centroid(y=y, sr=sr)))
    return rms, pitch, centroid

def authenticate_face_multi(captura_path: str) -> bool:
    pattern = os.path.join(IMAGE_FOLDER, "registered_*.jpg")
    refs = glob.glob(pattern)
    if not refs:
        return False
    for ref in refs:
        try:
            res = DeepFace.verify(
                ref, captura_path,
                enforce_detection=True,
                model_name="ArcFace",
                detector_backend="mtcnn"
            )
            if res.get("verified"):
                return True
        except:
            continue
    return False

def extract_pdf_image(pdf_path: str, image_path: str):
    doc = fitz.open(pdf_path)
    page = doc[0]
    pix = page.get_pixmap()
    pix.save(image_path)

def preprocess_audio(input_path, output_path='processed_audio.wav', target_sr=16000):
    # 1. Carrega o áudio (em qualquer formato)
    if '.wav' in input_path: 
        return input_path
    
    audio = AudioSegment.from_file(input_path)
    
    # 2. Converte para mono e 16kHz
    audio = audio.set_channels(1)
    audio = audio.set_frame_rate(target_sr)
    
    # 3. Exporta temporariamente para WAV para usar com librosa
    
    temp_path = os.path.join(UPLOAD_FOLDER, "temp.wav")
    audio.export(temp_path, format="wav")

    # 4. Usa librosa para análise e redução de ruído
    y, sr = librosa.load(temp_path, sr=target_sr)
    
    # 5. Redução de ruído
    reduced_noise = nr.reduce_noise(y=y, sr=sr)
    
    # 6. Normalização (ajuste de volume para amplitude máxima sem clipar)
    peak = max(abs(reduced_noise))
    if peak > 0:
        reduced_noise = reduced_noise / peak

    # 7. Salva em formato WAV 16-bit PCM
    final = os.path.join(UPLOAD_FOLDER, f"{uuid.uuid4()}_{output_path}")
    sf.write(final, reduced_noise, sr, subtype='PCM_16')

    os.remove(temp_path)
    return final

# 7) ESQUEMA LOGIN
class LoginModel(BaseModel):
    username: str
    password: str


# 8) ROUTES
@app.get("/", response_class=HTMLResponse)
def home(request: Request):
    # Se tiver index.html no diretório templates, configure Jinja2Templates
    return templates.TemplateResponse("index.html", {"request": request})

@app.post("/login")
@limiter.limit("15/minute")
def login(request: Request,data: LoginModel, Authorize: AuthJWT = Depends()):
    if data.username in usuarios and pwd_context.verify(data.password, usuarios[data.username]):
        token = Authorize.create_access_token(subject=data.username)
        return {"access_token": token}
    raise HTTPException(status_code=401, detail="Usuário ou senha incorretos")

@app.post("/listar-campos")
@limiter.limit("15/minute")
async def listar_campos(request: Request,
    file: UploadFile = File(...),
    Authorize: AuthJWT = Depends(),
):
    Authorize.jwt_required()
    if not file.filename:
        raise HTTPException(400, "Nenhum arquivo selecionado")
    fn = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    fp = os.path.join(UPLOAD_FOLDER, fn)
    with open(fp, "wb") as buf:
        buf.write(await file.read())
    campos = listar_campos_pdf(fp)
    os.remove(fp)
    return campos

@app.post("/preencher-campos")
@limiter.limit("15/minute")
async def preencher_campos(
    request: Request,background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    Authorize: AuthJWT = Depends(),
    
):
    Authorize.jwt_required()
    if not file.filename:
        raise HTTPException(400, "Nenhum arquivo selecionado")
    in_fn = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    in_fp = os.path.join(UPLOAD_FOLDER, in_fn)
    with open(in_fp, "wb") as buf:
        buf.write(await file.read())
    out_fn = f"{uuid.uuid4()}_preenchido.pdf"
    out_fp = os.path.join(UPLOAD_FOLDER, out_fn)
    form = await request.form()
    data: Dict[str, Any] = {}
    for key, val in form.items():
        if key != "file":
            name, t = key.split("|")
            t = int(t)
            if t == 7:
                data[name] = val
            elif t == 5:
                data[name] = int(val)
            elif t == 2:
                data[name] = bool(val)
    preencher_campos_pdf(in_fp, out_fp, data)
    os.remove(in_fp)
    background_tasks.add_task(os.remove, out_fp)
    return FileResponse(path=out_fp, filename=os.path.basename(out_fp),
                        media_type="application/pdf",
                        background=background_tasks)

@app.post("/autenticacao")
@limiter.limit("15/minute")
async def autenticacao(request: Request,
    file: UploadFile = File(...),
    Authorize: AuthJWT = Depends(),
):
    Authorize.jwt_required()
    if not file.filename:
        raise HTTPException(400, "Nenhum arquivo selecionado")
    fn = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    fp = os.path.join(IMAGE_FOLDER, fn)
    with open(fp, "wb") as buf:
        buf.write(await file.read())
    ok = authenticate_face_multi(fp)
    os.remove(fp)
    return {"analise": ok}

@app.post("/imagem")
@limiter.limit("15/minute")
async def imagem(request: Request,background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    Authorize: AuthJWT = Depends(),
    
):
    Authorize.jwt_required()
    if not file.filename:
        raise HTTPException(400, "Nenhum arquivo selecionado")
    in_fn = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    in_fp = os.path.join(UPLOAD_FOLDER, in_fn)
    with open(in_fp, "wb") as buf:
        buf.write(await file.read())
    out_fn = f"{uuid.uuid4()}_pagina.png"
    out_fp = os.path.join(IMAGE_FOLDER, out_fn)
    extract_pdf_image(in_fp, out_fp)
    os.remove(in_fp)
    background_tasks.add_task(os.remove, out_fp)
    return FileResponse(path=out_fp, filename=out_fn,
                        media_type="image/png",
                        background=background_tasks)

@app.post("/transcricao")
@limiter.limit("15/minute")
async def transcricao(request: Request,
    file: UploadFile = File(...),
    Authorize: AuthJWT = Depends(),
):
    Authorize.jwt_required()
    if not file.filename:
        raise HTTPException(400, "Nenhum arquivo selecionado")
    fn = f"{uuid.uuid4()}_{secure_filename(file.filename)}"
    fp = os.path.join(UPLOAD_FOLDER, fn)
    with open(fp, "wb") as buf:
        buf.write(await file.read())
    audio = preprocess_audio(fp)
    snr_value = calculate_snr_speech(audio)
    rms, pitch, spectral_centroid = analyze_audio(audio)

    if snr_value > 12 and pitch > 100 and rms >= 0.012:
        texto = transcricao_pdf(audio)
    else:
        texto = {"text": "audio ruim"}
    os.remove(fp)
    if os.path.exists(audio):
        os.remove(audio)
    return texto

@app.post("/upload-imagem")
@limiter.limit("10/minute")
async def upload_imagem(request: Request,
    file: UploadFile = File(...),
    Authorize: AuthJWT = Depends(),
):
    Authorize.jwt_required()
    if not file.filename:
        raise HTTPException(400, "Nenhum arquivo enviado")
    fn = f"registered_{uuid.uuid4()}.jpg"
    fp = os.path.join(IMAGE_FOLDER, fn)
    with open(fp, "wb") as buf:
        buf.write(await file.read())
    try:
        faces = DeepFace.extract_faces(img_path=fp, detector_backend="mtcnn")
        if len(faces) == 1:
            return JSONResponse({"mensagem": "Imagem recebida com sucesso"}, status_code=200)
        else:
            os.remove(fp)
            raise HTTPException(400, "Imagem com muitos rostos, não foi salva.")
    except Exception:
        os.remove(fp)
        raise HTTPException(400, "Imagem sem face, não foi salva.")

@app.post("/preencher-pdf")
@limiter.limit("15/minute")
async def preencher_pdf(
    request: Request,background_tasks: BackgroundTasks,
    files: List[UploadFile] = File(...),
    Authorize: AuthJWT = Depends(),
    
):
    Authorize.jwt_required()
    for file in files:
        if not file.filename:
            raise HTTPException(400, "Nenhum arquivo selecionado")
        nome = file.filename.lower()
        if nome.endswith('.csv'):
            caminho_csv = os.path.join(UPLOAD_FOLDER, f"{uuid.uuid4()}_{secure_filename(nome)}")
            with open(caminho_csv, "wb") as buf:
                buf.write(await file.read())
        elif nome.endswith('.pdf'):
            in_fn = f"{uuid.uuid4()}_{secure_filename(nome)}"
            in_fp = os.path.join(UPLOAD_FOLDER, in_fn)
            with open(in_fp, "wb") as buf:
                buf.write(await file.read())
        else:
            raise HTTPException(400, f"Tipo de arquivo não suportado: {nome}")
  
    out_fn = f"{uuid.uuid4()}_preenchido.pdf"
    out_fp = os.path.join(UPLOAD_FOLDER, out_fn)
    campos = listar_campos_pdf(in_fp)
    if len(campos) <= 1:
        raise HTTPException(400, f"Erro !!!")

    df = pd.read_csv(caminho_csv, header=0, names=['chave', 'valor'])
    df.fillna("", inplace=True)
    # Converte para dicionário
    dicionario = dict(zip(df['chave'], df['valor']))
    os.remove(caminho_csv)

    form = {}
    for c, v in dicionario.items():
        if v == '':
            continue
        for chave in campos.keys():
            n_campo = chave.split("|")
            if c.lower() == 'nome:' and n_campo[0] == 'nome do paciente':
                form[chave] = v
            elif c.lower() == 'paciente confirmou:':
                if n_campo[0] == 'identidade' and 'Identidade' in v:
                    form[chave] = True
                if n_campo[0] == 'sítio cirúrgico' and 'Sítio Cirúrgico' in v:
                    form[chave] = True
                if n_campo[0] == 'marcar procedimento' and 'Procedimento' in v:
                    form[chave] = True
                if n_campo[0] == 'consentimento' and 'Consentimento' in v:
                    form[chave] = True
            elif c.lower() == 'verificação da segurança anestésica:' or c == 'Verificação da segurança anestésica (Outro):':
                if n_campo[0] == 'montagem da so de acordo com o procedimento' and 'Montagem da SO de acordo com o procedimento' in v:
                    form[chave] = True
                if n_campo[0] == 'material anestésico disponível' and 'Material anestésico disponível' in v:
                    form[chave] = True
                if n_campo[0] == 'outro':
                    form[chave] = v
            elif c.lower() == 'data:' and n_campo[0] == 'data':
                form[chave] = v
            elif c == 'Sítio demarcado (lateralidade):':
                if v.lower() == 'sim' and n_campo[0] == 'sítio demarcado sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'sítio demarcado não':
                    form[chave] = True
                elif v.lower() == 'não se aplica' and n_campo[0] == 'não se aplica sítio demarcado':
                    form[chave] = True  
            elif c == 'Via aérea difícil/broncoaspiração:':
                if v.lower() == 'sim' and n_campo[0] == 'via aérea difícil sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'via aérea difícil não':
                    form[chave] = True
            elif c == 'Risco de grande perda sanguínea superior a 500 ml ou mais 7 ml/kg em crianças:':
                if  'sim' in v.lower() and n_campo[0] == 'risco de grande perda sanguínea sim':
                    form[chave] = True
                elif 'não' in v.lower() and n_campo[0] == 'risco de grande perda sanguínea não':
                    form[chave] = True
                if 'reserva de sangue disponível' in v.lower() and n_campo[0] == 'reserva de sangue disponível':
                    form[chave] = True     
            elif c == 'Acesso venoso adequado e pérvio:':
                if  'sim' == v.lower() and n_campo[0] == 'acesso venoso adequado sim':
                    form[chave] = True
                elif 'não' == v.lower() and n_campo[0] == 'acesso venoso adequado não':
                    form[chave] = True
                elif 'providenciado na so' == v.lower() and n_campo[0] == 'acesso venoso adequado providenciado':
                    form[chave] = True    
            elif c == 'Histórico de reação alérgica:':
                if v.lower() == 'sim' and n_campo[0] == 'histórico de reação alérgica sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'histórico de reação alérgica não':
                    form[chave] = True
            elif c == 'Apresentação oral de cada membro da equipe pelo nome e função:':
                if v.lower() == 'sim' and n_campo[0] == 'apresentação oral sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'apresentação oral não':
                    form[chave] = True
            elif c == 'Cirurgião, o anestesista e equipe de enfermagem confirmam verbalmente: Nome do paciente, sítio cirúrgico e procedimento a ser realizado.':
                if v.lower() == 'sim' and n_campo[0] == 'confirmam verbalmente sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'confirmam verbalmente não':
                    form[chave] = True
            elif c == 'Antibiótico profilático:':
                if  'sim' == v.lower() and n_campo[0] == 'antibiótico profilático sim':
                    form[chave] = True
                elif 'não' == v.lower() and n_campo[0] == 'antibiótico profilático não':
                    form[chave] = True
                elif 'não se aplica' == v.lower() and n_campo[0] == 'não se aplica antibiótico profilático':
                    form[chave] = True   
            elif c == 'Revisão do cirurgião. Momentos críticos do procedimento, tempos principais, riscos, perda sanguínea.:':
                if v.lower() == 'sim' and n_campo[0] == 'revisão do cirurgião sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'revisão do cirurgião não':
                    form[chave] = True
            elif c == 'Revisão do anestesista. Há alguma preocupação em relação ao paciente?':
                if v.lower() == 'sim' and n_campo[0] == 'revisão do anestesista sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'revisão do anestesista não':
                    form[chave] = True
            elif c == 'Revisão da enfermagem. Correta esterilização do material cirúrgico com fixação dos integradores ao prontuário.':
                if v.lower() == 'sim' and n_campo[0] == 'correta esterilização sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'correta esterilização não':
                    form[chave] = True
            elif c == 'Revisão da enfermagem. Placa de eletrocautério posicionada:':
                if v.lower() == 'sim' and n_campo[0] == 'placa de eletrocautério sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'placa de eletrocautério não':
                    form[chave] = True
            elif c == 'Revisão da enfermagem. Equipamentos disponíveis e funcionantes:':
                if v.lower() == 'sim' and n_campo[0] == 'equipamentos disponíveis sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'equipamentos disponíveis não':
                    form[chave] = True
            elif c == 'Revisão da enfermagem. Insumos e instrumentais disponíveis:':
                if v.lower() == 'sim' and n_campo[0] == 'insumos e instrumentais sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'insumos e instrumentais não':
                    form[chave] = True
            elif c == 'Confirmação do procedimento realizado.':
                if v.lower() == 'sim' and n_campo[0] == 'confirmação do procedimento sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'confirmação do procedimento não':
                    form[chave] = True
            elif c == 'Contagem de compressas.':
                if v.lower() == 'sim' and n_campo[0] == 'contagem de compressas sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'contagem de compressas não':
                    form[chave] = True
                elif 'não se aplica' == v.lower() and n_campo[0] == 'não se aplica contagem de compressas':
                    form[chave] = True 
            elif c == 'Compressas entregues:' and n_campo[0] == 'contagem de compressas entregues':
                form[chave] = v
            elif c == 'Compressas conferidas:' and n_campo[0] == 'contagem de compressas conferidas':
                form[chave] = v
            elif c == 'Contagem de instrumentos.':
                if v.lower() == 'sim' and n_campo[0] == 'contagem de instrumentos sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'contagem de instrumentos não':
                    form[chave] = True
                elif 'não se aplica' == v.lower() and n_campo[0] == 'não se aplica contagem de instrumentos':
                    form[chave] = True 
            elif c == 'Instrumentos entregues:' and n_campo[0] == 'contagem de instrumentos entregues':
                form[chave] = v
            elif c == 'Instrumentos conferidos:' and n_campo[0] == 'contagem de instrumentos conferidos':
                form[chave] = v
            elif c == 'Contagem de agulhas.':
                if v.lower() == 'sim' and n_campo[0] == 'contagem de agulhas sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'contagem de agulhas não':
                    form[chave] = True
                elif 'não se aplica' == v.lower() and n_campo[0] == 'não se aplica contagem de agulhas':
                    form[chave] = True 
            elif c == 'Agulhas entregues:' and n_campo[0] == 'contagem de agulhas entregues':
                form[chave] = v
            elif c == 'Agulhas conferidas:' and n_campo[0] == 'contagem de agulhas conferidas':
                form[chave] = v
            elif c == 'Amostra cirúrgica identificada adequadamente:':
                if v.lower() == 'sim' and n_campo[0] == 'amostra cirúrgica identificada sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'amostra cirúrgica identificada não':
                    form[chave] = True
                elif 'não se aplica' == v.lower() and n_campo[0] == 'não se aplica amostra cirúrgica identificada':
                    form[chave] = True 
            elif c == 'Problema com equipamentos que deve ser solucionado:':
                if v.lower() == 'sim' and n_campo[0] == 'problema com equipamentos sim':
                    form[chave] = True
                elif v.lower() == 'não' and n_campo[0] == 'problema com equipamentos não':
                    form[chave] = True
                elif 'não se aplica' == v.lower() and n_campo[0] == 'não se aplica problema com equipamentos':
                    form[chave] = True 
            elif c == 'Comunicado a enfermeira para providenciar a solução:' and n_campo[0] == 'comunicado à enfermeira':
                form[chave] = v
            elif c == 'Recomendações Cirurgião:' and n_campo[0] == 'comentário do cirurgião':
                form[chave] = v
            elif c == 'Recomendações Anestesista:' and n_campo[0] == 'comentário da anestesista':
                form[chave] = v
            elif c == 'Recomendações Enfermagem:' and n_campo[0] == 'comentário da enfermagem':
                form[chave] = v
            elif n_campo[0] in c.lower() and n_campo[0] != 'data':
                form[chave] = v

    data: Dict[str, Any] = {}
    for key, val in form.items():
        if key != "file":
            name, t = key.split("|")
            t = int(t)
            if t == 7:
                data[name] = val
            elif t == 5:
                data[name] = int(val)
            elif t == 2:
                data[name] = bool(val)
    preencher_campos_pdf(in_fp, out_fp, data)
    os.remove(in_fp)
    background_tasks.add_task(os.remove, out_fp)
    return FileResponse(path=out_fp, filename=os.path.basename(out_fp),
                        media_type="application/pdf",
                        background=background_tasks) 















