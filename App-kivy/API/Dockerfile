# Use uma imagem base do Python
#FROM python:3.10
FROM nvidia/cuda:12.4.0-base-ubuntu20.04

RUN apt-get update && apt-get install -y ffmpeg

RUN apt-get install -y python3 python3-pip python3-dev && rm -rf /var/lib/apt/lists/*

# Defina o diretório de trabalho no contêiner
WORKDIR /app

# Copie os arquivos de requisitos
COPY requirements.txt requirements.txt

# Instale as dependências
RUN pip install --no-cache-dir -r requirements.txt

# Instale o Gunicorn
RUN pip install --no-cache-dir gunicorn

# Copie o conteúdo do diretório local para o diretório de trabalho do contêiner
COPY . .

# Exponha a porta do serviço
EXPOSE 5000

# Comando para rodar a aplicação usando o Gunicorn
CMD ["gunicorn","-b", "0.0.0.0:5000", "wsgi:app"]


