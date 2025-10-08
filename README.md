[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

# Fala-Texto

<div align="center">
  <img width="350" height="2000" alt="Image" src="https://github.com/user-attachments/assets/1d1e4d24-db10-4e1a-8598-04288cc1a91a" />
</div>
O projeto Conversão Fala-Texto apresenta uma tecnologia promissora para o reconhecimento de voz, oferecendo uma solução eficaz para a documentação clínica. Esta tecnologia permite que a validação dos dados possa ocorrer simultaneamente ao preenchimento das informações, permitindo que os profissionais de saúde registrem informações de forma mais rápida e precisa. Essa protótipo visa melhorar a captura de dados médicos, reduzir a carga de trabalho dos profissionais de saúde e aprimorar o  a precisão das informações coletadas nos procedimentos de saúde. 

## Desenvolvimento do app android baseado no Kivy

O desenvolvimento de aplicativos Android no contexto da saúde apresenta uma oportunidade valiosa para transformar a maneira como serviços médicos e informações são acessados e gerenciados. Pensando nisso, o app traz para o cotidiano do ambiente hospitalar acessibilidade, inovação tecnológica e eficiência no preenchimento de documentos.

### Pré-requisitos:
- Ambiente linux para o desenvolvimento do apk
- App baseado no Kivy

#### Ambiente de desenvolvimento

O ambiente de desenvolvimento escolhido é o Ubuntu 20.04, uma das distribuições Linux mais utilizadas devido à sua estabilidade e vasto suporte da comunidade. Neste ambiente é necessário instalar dependências para o funcionamento do Buildozer, ferramenta amplamente usada para compilar aplicativos Python em pacotes Android (APK). Essas dependências garantem que o ambiente esteja configurado corretamente para o funcionamento do Buildozer e para lidar com os processos de compilação de maneira eficiente. 

Segue abaixo a lista de comandos para a instalação das dependências requeridas.

Ferramentas de dev:
```bash
sudo apt-get install -y \
python3-pip \
build-essential \
git \
python3 \
python3-dev \
ffmpeg \
libsdl2-dev \
libsdl2-image-dev \
libsdl2-mixer-dev \
libsdl2-ttf-dev \
libportmidi-dev \
libswscale-dev \
libavformat-dev \
libavcodec-dev \
zlib1g-dev
```
```bash
sudo pip3 install cython
sudo pip3 install kivy
sudo apt-get install libltdl-dev libffi-dev libssl-dev autoconf autotools-dev
```

Instalação do Buildozer:
```bash
pip3 install --user --upgrade buildozer
```
Instalação do Java versão 17
```bash
sudo apt update
sudo apt install -y git zip unzip openjdk-17-jdk python3-pip autoconf libtool pkg-config zlib1g-dev libncurses5-dev libncursesw5-dev libtinfo5 cmake libffi-dev libssl-dev
pip3 install --user --upgrade Cython==0.29.33 virtualenv  # the --user should be removed if you do this in a venv
```

Adicionando o caminho para a execução do java e do buildozer
```bash
export PATH=$PATH:~/.local/bin/
```

#### Construção do App

No diretório onde esta todos os arquivos do app, realize os seguintes comandos:

```bash
buildozer init
```
O comando anterior cria um arquivo de especificações para o buildozer chamado 'buildozer.spec', Este arquivo de configuração define todas as informações necessárias para construir o pacote do aplicativo, incluindo detalhes sobre dependências, permissões, plataforma-alvo, versão, orientações de compilação e outros parâmetros importantes. 

Para gerar uma versão de debug do aplicativo, utilize o comando abaixo, que será responsável por criar um APK adequado para testes.
```bash
buildozer -v android debug
```

No caso de implementar uma versão de lançamento (release) do aplicativo, o comando buildozer release gera um arquivo .APK ou .AAB otimizado, pronto para distribuição em plataformas como a Play Store ou para uso final pelos usuários. Antes de executar o comando, é importante garantir que todas as dependências estejam corretamente configuradas e que as configurações no arquivo buildozer.spec estejam alinhadas com os requisitos do aplicativo.
```bash
buildozer -v android release
```

No entanto, o arquivo gerado não conta com uma assinatura válida para a Play Store. Por isso, é necessário assiná-lo. Os comandos abaixo realizam esse processo para arquivos no formato .APK ou .AAB.

- Para realizar a assinatura de um APK, utilize os comandos a seguir. Antes disso, é necessário instalar as ferramentas apksigner e zipalign.
```bash
sudo apt install -y apksigner zipalign
```

Comando para otimizar um arquivo .APK (Opcional)
```bash
zipalign -v 4 meu-app.apk meu-app-alinhado.apk
```

E por fim realize a assinatura, no comando keytool será requirido a criação de uma senha para a sua keystore, que vai assinar o aplicativo, e outras informações como o nome da sua organização.
```bash
keytool -genkey -v -keystore meu-keystore.jks -alias meu-alias -keyalg RSA -keysize 2048 -validity 20000
apksigner sign --ks meu-keystore.jks --ks-key-alias meu-alias meu-app.apk
apksigner verify meu-app.apk
```

- Para assinar um arquivo .aab, use os seguintes comandos:
```bash
keytool -genkey -v -keystore meu-keystore.jks -alias meu-alias -keyalg RSA -keysize 2048 -validity 20000
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore meu-keystore.jks meu-app.aab meu-alias
jarsigner -verify meu-app.aab
```
