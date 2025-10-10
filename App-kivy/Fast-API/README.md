[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

# Fala-Texto

<div align="center">
  <img src="https://github.com/user-attachments/assets/af5cbef0-822c-44da-8ed0-d8f151b00301" alt="Imagem1">
</div>
Este documento instrui como implementar o serviço de preenchimento de formulários em um servidor Ubuntu v20.04+ com Gpu NVIDIA e seus drivers. 


## Pré-requisitos:
- Ambiente linux: Ubuntu
- Docker 
- Nginx
- NVIDIA Container Toolkit

### Instalação de dependencias 

Para começar, instale o Docker e o Nginx executando os comandos abaixo.

Docker:
```bash
sudo apt update
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io
```
Verifique se a instalação foi bem-sucedida:
```bash
docker --version
```

Nginx:
```bash
sudo apt update  
sudo apt install -y nginx  
```

NVIDIA Container Toolkit:
```bash
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg \
  && curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | \
    sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | \
    sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list
sudo apt-get update
export NVIDIA_CONTAINER_TOOLKIT_VERSION=1.17.8-1
  sudo apt-get install -y \
      nvidia-container-toolkit=${NVIDIA_CONTAINER_TOOLKIT_VERSION} \
      nvidia-container-toolkit-base=${NVIDIA_CONTAINER_TOOLKIT_VERSION} \
      libnvidia-container-tools=${NVIDIA_CONTAINER_TOOLKIT_VERSION} \
      libnvidia-container1=${NVIDIA_CONTAINER_TOOLKIT_VERSION}
```

configurando o container runtime:
```bash
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker
```

#### Implementando o container Docker

Agora é hora de executar o container que irá disponibilizar o serviço de preenchimento. Antes disso, baixe os arquivos da pasta API-fastapi desse repositório e certifique-se de que o diretório do projeto esteja com a seguinte estrutura:

/meu_projeto

<div align="center">
  <img width="586" height="261" alt="Captura de tela 2025-10-09 105818" src="https://github.com/user-attachments/assets/d445cb10-5b8d-4184-9239-08067d91b0e3" />
</div>


No diretório do projeto execute o comando abaixo para contruir a imagem Docker:
```bash
sudo docker build -t nome-da-imagem .
sudo docker images
```

Depois de confirmar que as portas especificadas no Dockerfile e em servico.py estão iguais, execute o comando abaixo para subir o container.
```bash
sudo docker run -d -p 3050:3050 --restart always -m 6g --gpus all -v endereço-da-pasta-do-volume:/app/imagens nome-da-imagem
sudo docker ps -a
```

#### Configurando o Nginx

Você deve permitir conexões http/https do Nginx no seu servidor. 
```bash
sudo ufw allow 'Nginx Full'
sudo ufw reload
sudo ufw status 
```
Agora, mova o arquivo 'Processo', que contém toda a configuração necessária para o Nginx expor o serviço para o domínio público, para esse diretório: 
```bash
sudo mv endereço-de-origem/Processo /etc/nginx/sites-available/Processo
```

Consolidando essas configurações:
```bash
sudo nginx -t
sudo systemctl restart nginx
```

Assim, o serviço esta funcionando normalmente ao final desses passos e o App cliente já pode estabelecer requisições.
