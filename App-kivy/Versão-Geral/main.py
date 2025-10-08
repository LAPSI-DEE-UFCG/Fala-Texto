# SPDX-License-Identifier: Apache-2.0
# Copyright 2025 Lapsi/Fala-texto


from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.image import Image
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
from kivy.core.window import Window
from kivy.properties import StringProperty
import requests
import threading
import time
from android.permissions import request_permissions, Permission
import numpy as np
from jnius import autoclass
import re
import os
import platform
from kivy.uix.popup import Popup
from kivy.clock import Clock
from android import activity


# variaveis globais

limiar = 0.01
Dados_atualizados = {}

MediaRecorder = autoclass('android.media.MediaRecorder')
AudioSource = autoclass('android.media.MediaRecorder$AudioSource')
OutputFormat = autoclass('android.media.MediaRecorder$OutputFormat')
AudioEncoder = autoclass('android.media.MediaRecorder$AudioEncoder')
Environment = autoclass('android.os.Environment')

# whisper
API_URL = "https://api-inference.huggingface.co/models/openai/whisper-medium"

# pdf
PDF_URL = "https://processarpdffalatex.zapto.org"


class Preencher(App):
    label = StringProperty(" ")
    label2 = StringProperty(" ")

    def inferencia_modelo(self, filename):
        with open(filename, "rb") as f:
            data = f.read()
        headers = {'Authorization': f'Bearer {self.access_token}'}
        url = f"{PDF_URL}/transcricao"
        files = {'file': data}
        response = requests.post(
            url,
            files=files,
            headers=headers,
            verify=self.certificado)
        return response.json()

    def login(self):
        url = f"{PDF_URL}/login"
        payload = {'username': 'Fala-texto',
                   'password': 'Transcrição_de_fala_em_texto_api'}
        self.certificado = self.arquivos_caminho('certificado', 'ca_cert.pem')
        #self.documento = self.arquivos_caminho('arquivo', 'teste.pdf')
        response = requests.post(url, json=payload, verify=self.certificado)
        if response.status_code == 200:
            self.access_token = response.json().get('access_token')
        else:
            self.label = 'Falha no login'

    def salvar_arquivos(self, diretorio, arquivo):

        # Caminho do armazenamento externo
        diretorio_externo = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
        caminho_destino = os.path.join(diretorio_externo, diretorio)

        # Verificar se o diretório existe; se não, criá-lo
        if not os.path.exists(caminho_destino):
            os.makedirs(caminho_destino)

        caminho_arquivo = os.path.join(caminho_destino, arquivo)
        return caminho_arquivo

    def arquivos_caminho(self, diretorio, arquivo):
        diretorio_atual = os.path.abspath(os.path.dirname(__file__))
        caminho_destino = os.path.join(diretorio_atual, diretorio)
        # Verificar se o diretório existe; se não, criá-lo
        if not os.path.exists(caminho_destino):
            os.makedirs(caminho_destino)
        caminho_arquivo = os.path.join(caminho_destino, arquivo)
        return caminho_arquivo

    def build(self):
        Window.clearcolor = (0.031, 0.247, 0.463, 1)  # Cor hexadecimal #083f76

        layout = BoxLayout(orientation='vertical', padding=10)

        # Adicionar a imagem
        cami_imagem = self.arquivos_caminho('imagem', 'Projeto (4).png')
        img = Image(
            source=cami_imagem, allow_stretch=True, size=(
                250, 250), size_hint=(
                None, None), pos_hint={
                'center_x': 0.5, 'center_y': 0.5})
        layout.add_widget(img)

        # Label do nome dos campos
        texto_campos = Label(
            text='Nome dos campos', font_size="18sp", color=(
                1, 1, 1, 1), size_hint=(
                1, 0.1))
        layout.add_widget(texto_campos)

        # Listbox usando ScrollView e GridLayout
        scrollview = ScrollView(size_hint=(1, 0.2))
        self.grid = GridLayout(cols=1, size_hint_y=None)
        self.grid.bind(minimum_height=self.grid.setter('height'))
        scrollview.add_widget(self.grid)
        layout.add_widget(scrollview)

        # Labels com texto dinâmico
        label_var2 = Label(
            text=self.label2, font_size="16sp", color=(
                1, 1, 1, 1), size_hint=(
                1, 0.1))
        self.bind(label2=label_var2.setter('text'))
        layout.add_widget(label_var2)

        label_var = Label(
            text=self.label, font_size="16sp", color=(
                1, 1, 1, 1), size_hint=(
                1, 0.1))
        self.bind(label=label_var.setter('text'))
        layout.add_widget(label_var)

        # Botões
        self.importar = Button(
            text="Selecionar PDF", size_hint=(
                None, 0.05), background_color=(
                0, 1, 1, 1), size=(
                340, 80), pos_hint={
                    'center_x': 0.5, 'center_y': 0.5}, on_press=self.abrir_pdf)
        layout.add_widget(self.importar)
        
        self.botao1 = Button(
            text="Gravar audio", size_hint=(
                None, 0.05), disabled=True,background_color=(
                0, 1, 1, 1), size=(
                340, 80), pos_hint={
                    'center_x': 0.5, 'center_y': 0.5}, on_press=self.thread_calibra)
        layout.add_widget(self.botao1)

        self.bt_abrir = Button(
            text="Preencher PDF", size_hint=(
                None, 0.05), disabled=True, background_color=(
                0, 1, 1, 1), size=(
                340, 80), pos_hint={
                    'center_x': 0.5, 'center_y': 0.5}, on_press=self.thread_preencher)
        layout.add_widget(self.bt_abrir)

        self.bt_sair = Button(
            text="Encerrar Aplicação", size_hint=(
                None, 0.05), background_color=(
                0, 1, 1, 1), size=(
                340, 80), pos_hint={
                    'center_x': 0.5, 'center_y': 0.5}, on_press=self.stop_app)
        layout.add_widget(self.bt_sair)
        request_permissions([Permission.WRITE_EXTERNAL_STORAGE,
                             Permission.READ_EXTERNAL_STORAGE,
                             Permission.RECORD_AUDIO])

        return layout

    def on_start(self):
        self.title = "Preenchimento de PDF's"
        self.login()

    def show_warning(self, frase):
        content = BoxLayout(orientation='vertical', padding=10, spacing=10)
        label = Label(text=frase)
        button = Button(text='Ok', size_hint=(None, None), size=(80, 60))

        popup = Popup(title='Aviso',
                      content=content,
                      size_hint=(None, None), size=(650, 260),
                      auto_dismiss=False)

        content.add_widget(label)
        content.add_widget(button)
        button.bind(on_release=popup.dismiss)
        popup.open()

    def thread_calibra(self, instance):
        # Função de calibração do microfone
        #dados = self.listar_campos_pdf(self.documento)
        #Clock.schedule_once(lambda dt: self.exibir_dicionario(dados), 0.05)
        threading.Thread(target=self.habilitar_calibra, args=(
            self.dados,), daemon=True).start()

    def habilitar_calibra(self, dados):
        global limiar
        global Dados_atualizados

        try:
            self.label2 = 'Fale algo:'
            self.botao1.disabled = True
            self.recorder = MediaRecorder()
            self.recorder.setAudioSource(AudioSource.MIC)
            self.recorder.setOutputFormat(OutputFormat.MPEG_4)
            self.recorder.setAudioEncoder(AudioEncoder.AAC)
            self.recorder.setAudioSamplingRate(44100)
            audio2 = self.salvar_arquivos('audios', "comando2.mp4")
            self.recorder.setOutputFile(audio2)
            self.recorder.prepare()
            self.recorder.start()

            threading.Event().wait(7)  # Duração de gravação em segundos
            if self.recorder:
                self.recorder.stop()
                self.recorder.release()
                self.recorder = None

            Clock.schedule_once(
                lambda dt: self.show_warning("Audio gravado \ncom sucesso !!!"),
                0)  # Agendar a função para a thread principal
            voz = self.inferencia_modelo(audio2)
            if 'text' in voz:
                comando = voz['text']
                self.label = comando
                texto = self.salvar_arquivos(
                    'transcrições', "registro-fala.txt")
                with open(texto, 'a', encoding='utf-8') as arquivo:
                    arquivo.write(comando + '\n')
                for campo in dados.keys():
                    if campo[0].lower() in comando.lower():
                        if campo[1] == 7:  # Tipo texto
                            valor = comando.lower().split(campo[0])[-1].strip()
                            if len(valor) != 0:
                                if bool(
                                    re.search(
                                        r'[^\w\s]',
                                        valor[0])):  # verifica se tem pontuação no inicio da string
                                    valor = valor[1:].strip()
                            Dados_atualizados[campo] = valor

                        elif campo[1] == 5:
                            Dados_atualizados[campo] = 1

                        elif campo[1] == 2:
                            Dados_atualizados[campo] = True

            self.bt_abrir.disabled = False
            self.label2 = ' '
            self.botao1.disabled = False
            

        except Exception as e:
            self.label = f"Erro ao gravar áudio: {e}"


    def abrir_pdf(self, instance):
        Intent = autoclass('android.content.Intent')
        PythonActivity = autoclass('org.kivy.android.PythonActivity').mActivity
        intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType('application/pdf')  # Selecionar apenas arquivos PDF
        result_code = 1
        PythonActivity.startActivityForResult(intent, result_code)
        activity.bind(on_activity_result=self.on_activity_result)

    def on_activity_result(self, request_code, result_code, intent):

        if request_code == 1 and result_code == autoclass(
                'android.app.Activity').RESULT_OK:
            uri = intent.getData()
            ContentResolver = autoclass('android.content.ContentResolver')
            PythonActivity2 = autoclass(
                'org.kivy.android.PythonActivity').mActivity
            content_resolver = PythonActivity2.getContentResolver()
            # Abra um InputStream para ler o conteúdo do arquivo
            input_stream = content_resolver.openInputStream(uri)
            # Leia o conteúdo do arquivo
            content = []
            buffer = bytearray(1024)  # Tamanho do buffer
            bytes_read = input_stream.read(buffer)
            while bytes_read != -1:
                content.append(buffer[:bytes_read])
                bytes_read = input_stream.read(buffer)
            input_stream.close()  # Feche o fluxo de entrada
            # Combine o conteúdo em um único arquivo binário ou texto
            self.full_content = b''.join(content)  # Para binários como PDFs

            self.dados = self.listar_campos_pdf(self.full_content)
            Clock.schedule_once(lambda dt: self.exibir_dicionario(self.dados), 0.05)
            self.importar.disabled = True
            self.botao1.disabled = False
            

    def thread_preencher(self, instance):
        global Dados_atualizados
        #self.botao1.disabled = True
        download_path = self.salvar_arquivos("Formularios", 'Preenchido.pdf')
        threading.Thread(
            target=self.preencher_campos_pdf,
            args=(
                self.full_content,
                download_path,
                Dados_atualizados,
            ),
            daemon=True).start()
        Clock.schedule_once(
            lambda dt: self.show_warning("Documento preenchido !!"), 0)
        self.label = ' '
        #self.botao1.disabled = False
        self.importar.disabled = False
        Clock.schedule_once(lambda dt: self.clear_text(), 0.05)

    def exibir_dicionario(self, dados):
        # Adicione os itens do dicionário na Listbox
        for chave, valor in dados.items():
            lbl = Label(text=f"{chave[0]}", size_hint_y=None, font_size="18sp",height=55)
            self.grid.add_widget(lbl)

    def clear_text(self):
        self.grid.clear_widgets()

    def preencher_campos_pdf(self, pdf_path, output_path, data):
        dados = {}
        url = f"{PDF_URL}/preencher-campos"
        headers = {'Authorization': f'Bearer {self.access_token}'}
        #with open(pdf_path, "rb") as fe:
            #conteudo = fe.read()
        conteudo = pdf_path
        files = {'file': conteudo}
        for c, v in data.items():
            dados[f"{c[0]}|{str(c[1])}"] = v
        response = requests.post(
            url,
            files=files,
            data=dados,
            headers=headers,
            verify=self.certificado)
        if response.status_code == 200:
            # Salva o arquivo recebido
            with open(output_path, 'wb') as f:
                f.write(response.content)
        else:
            self.label = f'Erro: {response.status_code} - {response.json()}'

    def listar_campos_pdf(self, pdf_path):
        saida = {}
        headers = {'Authorization': f'Bearer {self.access_token}'}
        url = f"{PDF_URL}/listar-campos"
        # with open(pdf_path, "rb") as f:
        # data = f.read()
        data = pdf_path
        files = {'file': data}
        response = requests.post(
            url,
            files=files,
            headers=headers,
            verify=self.certificado)
        resposta = response.json()
        for c, v in resposta.items():
            # dados[(annot.field_name, annot.field_type)] = None
            chaves = c.split('|')
            saida[(chaves[0], int(chaves[1]))] = v
        return saida

    def stop_app(self, instance):
        # Método que encerra a aplicação
        self.stop()


relatorio = Preencher()
relatorio.run()

