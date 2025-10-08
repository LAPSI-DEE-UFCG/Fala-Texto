# SPDX-License-Identifier: Apache-2.0
# Copyright 2025 Lapsi/Fala-texto

# Importações das ferramentas
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.textinput import TextInput
from kivy.uix.image import Image
from kivy.uix.modalview import ModalView
from kivy.uix.camera import Camera
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.scatter import Scatter
from kivy.uix.gridlayout import GridLayout
from kivy.core.window import Window
from kivy.properties import StringProperty
from kivy.graphics import PushMatrix, PopMatrix, Rotate
import requests
import threading
import time
from android.permissions import request_permissions, Permission
import numpy as np
from jnius import autoclass
import re
import os
from kivy.uix.popup import Popup
from kivy.uix.widget import Widget
from kivy.clock import Clock
from android import activity
from functools import partial
from datetime import datetime
import uuid


# variaveis globais
marcadores = [
    "nome do paciente",
    "data de nascimento",
    "prontuário",
    "sala",
    "identidade",
    "sítio cirúrgico",
    "marcar procedimento",
    "consentimento",
    "sítio demarcado",
    "montagem da so de acordo com o procedimento",
    "material anestésico disponível",
    "outro",
    "via aérea difícil",
    "risco de grande perda sanguínea",
    "reserva de sangue disponível",
    "acesso venoso adequado",
    "histórico de reação alérgica",
    "qual",
    "você deseja continuar para a proxima etapa ?",
    "apresentação oral",
    "confirmam verbalmente",
    "antibiótico profilático",
    "revisão do cirurgião",
    "revisão do anestesista",
    "correta esterilização",
    "placa de eletrocautério",
    "equipamentos disponíveis",
    "insumos e instrumentais",
    "você deseja continuar para a proxima etapa ?",
    "confirmação do procedimento",
    "contagem de compressas",
    "contagem de compressas entregues",
    "contagem de compressas conferidas",
    "contagem de instrumentos",
    "contagem de instrumentos entregues",
    "contagem de instrumentos conferidos",
    "contagem de agulhas",
    "contagem de agulhas entregues",
    "contagem de agulhas conferidas",
    "amostra cirúrgica identificada",
    "requisição completa",
    "problema com equipamentos",
    "comunicado à enfermeira",
    "comentário do cirurgião",
    "comentário da anestesista",
    "comentário da enfermagem",
    "responsável",
    "data",
]

limiar = 0.01
Dados_atualizados = {}

MediaRecorder = autoclass("android.media.MediaRecorder")
AudioSource = autoclass("android.media.MediaRecorder$AudioSource")
OutputFormat = autoclass("android.media.MediaRecorder$OutputFormat")
AudioEncoder = autoclass("android.media.MediaRecorder$AudioEncoder")
Environment = autoclass("android.os.Environment")

# whisper
API_URL = "https://api-inference.huggingface.co/models/openai/whisper-medium"


# pdf
PDF_URL = "https://192.168.1.5:5000"
session = requests.Session()


class PDFModalView(ModalView):
    def __init__(self, pdf_path="", **kwargs):
        super().__init__(**kwargs)
        self.size_hint = (0.7, 0.7)
        self.auto_dismiss = False  # Evita fechar o modal clicando fora

        # Carregar imagem do PDF
        # refe = self.arquivos_caminho('imagem','pagina.png')
        self.refe = pdf_path

        # Layout principal
        layout = FloatLayout(size=(1200, 1200))

        scatter = Scatter(
            do_scale=True,
            do_rotation=False,
            do_translation=True,
            size_hint=(None, None),
            size=(800, 1000),
            pos_hint={"center_x": 0.5, "center_y": 0.5},
        )

        # Criar imagem centralizada
        self.img = Image(source=self.refe, allow_stretch=True, size_hint=(None, None))
        self.img.size = (800, 1000)  # Define tamanho inicial
        self.img.pos_hint = {"center_x": 0.5, "center_y": 0.5}  # Mantém centralizada

        scatter.add_widget(self.img)
        layout.add_widget(scatter)

        # Criar botões de controle
        close_button = Button(
            text="Fechar",
            size_hint=(None, None),
            size=(130, 60),
            pos_hint={"center_x": 0.5, "center_y": 0.05},
        )
        close_button.bind(on_press=self.fechar)

        layout.add_widget(close_button)
        self.add_widget(layout)

    def arquivos_caminho(self, diretorio, arquivo):
        diretorio_atual = os.path.abspath(os.path.dirname(__file__))
        caminho_destino = os.path.join(diretorio_atual, diretorio)
        # Verificar se o diretório existe; se não, criá-lo
        if not os.path.exists(caminho_destino):
            os.makedirs(caminho_destino)
        caminho_arquivo = os.path.join(caminho_destino, arquivo)
        return caminho_arquivo

    def fechar(self, *args):
        if os.path.exists(self.refe):
            os.remove(self.refe)
        self.dismiss()


class RotatedCamera(Camera):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # Força preencher toda a área e permitir esticar:
        self.allow_stretch = False
        self.keep_ratio = False

        # Matriz de rotação
        with self.canvas.before:
            PushMatrix()
            self._rot = Rotate(angle=90, origin=self.center)
        with self.canvas.after:
            PopMatrix()

        # Atualiza o ponto de origem sempre que mudar tamanho/posição
        self.bind(pos=self._update_origin, size=self._update_origin)

    def _update_origin(self, *l):
        self._rot.origin = self.center


class Preencher(App):
    label = StringProperty(" ")
    label2 = StringProperty(" ")
    prossegir = False
    popup_aberto = False
    conta = 0
    fluxo = 0

    def inferencia_modelo(self, filename):
        with open(filename, "rb") as f:
            data = f.read()
        url = f"{PDF_URL}/transcricao"
        files = {"file": data}
        response = session.post(url, files=files)
        return response.json()

    def CameraModal(self):
        self.modal = ModalView(size_hint=(0.6, 0.55), auto_dismiss=False)
        layout = BoxLayout(orientation="vertical", spacing=10, padding=10)
        self.modal_layout = layout
        self.camera = RotatedCamera(
            index=1, play=True, resolution=(640, 480)
        )  # Câmera ativada
        capture_btn = Button(
            text="Capturar Imagem",
            size_hint=(None, None),
            size=(350, 60),
            pos_hint={"center_x": 0.5, "y": 0.02},
        )
        capture_btn.bind(on_press=self.capture)

        layout.add_widget(self.camera)
        layout.add_widget(Widget(size_hint_y=0.1))
        layout.add_widget(capture_btn)
        layout.add_widget(Widget(size_hint_y=0.03))
        self.modal.add_widget(layout)
        self.modal.open()

    def capture(self, instance):
        salvar_re = self.salvar_arquivos("Formularios", "captured.jpg")
        self.camera.export_to_png(salvar_re)
        self.encerrar_camera()
        self.importar.disabled = False
        condicao = self.auten_face(salvar_re)
        # time.sleep(0.3)
        if os.path.exists(salvar_re):
            os.remove(salvar_re)
        self.modal.dismiss(force=True)
        if not condicao:
            self.stop()

    def encerrar_camera(self):
        if self.camera:
            self.camera.play = False  # Para o stream
            if self.camera in self.modal_layout.children:
                self.modal_layout.remove_widget(self.camera)  # Remove da tela
            self.camera.texture = None  # Libera o buffer de vídeo
            self.camera._camera = None  # Libera a instância da câmera nativa
            self.camera = None  # Remove a referência para permitir o garbage collector

    def auten_face(self, arquivo):
        with open(arquivo, "rb") as f:
            data = f.read()
        url = f"{PDF_URL}/autenticacao"
        files = {"file": data}
        response = session.post(url, files=files)
        aux = response.json()
        if "analise" in aux:
            return aux["analise"]

    def login(self):
        url = f"{PDF_URL}/login"
        payload = {
            "username": "Fala-texto",
            "password": "Transcrição_de_fala_em_texto_api",
        }
        self.certificado = self.arquivos_caminho("certificado", "ca_cert.pem")
        session.verify = self.certificado
        # self.documento = self.arquivos_caminho('arquivo', 'teste.pdf')
        response = session.post(url, json=payload)
        if response.status_code == 200:
            access_token = response.json().get("access_token")
            session.headers.update({"Authorization": f"Bearer {access_token}"})
        else:
            Clock.schedule_once(lambda dt: self.imprimir_erro("Falha no login"), 0.05)
            # self.label = 'Falha no login'

    def salvar_arquivos(self, diretorio, arquivo):

        # Caminho do armazenamento externo
        diretorio_externo = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        ).getAbsolutePath()
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
        # (0.031, 0.247, 0.463, 1)
        Window.clearcolor = (0.776, 0.831, 0.882, 1)

        layout = BoxLayout(orientation="vertical", padding=10)

        # Adicionar a imagem
        cami_imagem = self.arquivos_caminho("imagem", "Imagem1.png")
        img = Image(
            source=cami_imagem,
            allow_stretch=True,
            size=(250, 250),
            size_hint=(None, None),
            pos_hint={"center_x": 0.5, "center_y": 0.5},
        )
        layout.add_widget(img)

        # Label do nome dos campos
        texto_campos = Label(
            text="Campos:", font_size="18sp", color=(0, 0, 0, 1), size_hint=(1, 0.08)
        )
        layout.add_widget(texto_campos)

        # Listbox usando ScrollView e GridLayout
        scrollview = ScrollView(size_hint=(1, 0.1))
        self.grid = GridLayout(cols=1, size_hint_y=None)
        self.grid.bind(minimum_height=self.grid.setter("height"))
        self.oracao = ""
        self.text_input3 = TextInput(
            text=self.oracao, readonly=True, font_size="17sp", size_hint=(1, 0.3)
        )
        scrollview.add_widget(self.text_input3)
        # scrollview.add_widget(self.grid)
        layout.add_widget(scrollview)

        # Labels com texto dinâmico
        label_var2 = Label(
            text=self.label2, font_size="17sp", color=(0, 0, 0, 1), size_hint=(1, 0.05)
        )
        self.bind(label2=label_var2.setter("text"))
        layout.add_widget(label_var2)

        # label_var = Label(text=self.label, font_size="17sp", color=(0, 0, 0, 1), size_hint=(1, 0.1))
        # self.bind(label=label_var.setter('text'))

        self.resposta = " "
        self.text_input2 = TextInput(
            text=self.resposta, readonly=False, font_size="17sp", size_hint=(1, 0.07)
        )

        # self.text_input2.bind(text=self.on_text_change)
        layout.add_widget(self.text_input2)

        # Botões
        self.importar = Button(
            text="Selecionar PDF",
            size_hint=(None, 0.05),
            disabled=True,
            background_color=(0, 1, 1, 1),
            size=(340, 80),
            pos_hint={"center_x": 0.5, "center_y": 0.5},
            on_press=self.abrir_pdf,
        )
        layout.add_widget(self.importar)

        self.botao1 = Button(
            text="Gravar audio",
            size_hint=(None, 0.05),
            disabled=True,
            background_color=(0, 1, 1, 1),
            size=(340, 80),
            pos_hint={"center_x": 0.5, "center_y": 0.5},
            on_press=self.thread_calibra,
        )
        layout.add_widget(self.botao1)

        self.bt_abrir = Button(
            text="Preencher PDF",
            size_hint=(None, 0.05),
            disabled=True,
            background_color=(0, 1, 1, 1),
            size=(340, 80),
            pos_hint={"center_x": 0.5, "center_y": 0.5},
            on_press=self.thread_preencher,
        )
        layout.add_widget(self.bt_abrir)

        self.bt_sair = Button(
            text="Encerrar Aplicação",
            size_hint=(None, 0.05),
            background_color=(0, 1, 1, 1),
            size=(340, 80),
            pos_hint={"center_x": 0.5, "center_y": 0.5},
            on_press=self.stop_app,
        )
        layout.add_widget(self.bt_sair)

        return layout

    def on_start(self):
        self.title = "Preenchimento de PDF's"
        self.login()
        request_permissions(
            [
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.READ_EXTERNAL_STORAGE,
                Permission.RECORD_AUDIO,
                Permission.CAMERA,
            ],
            self.permissoes_callback,
        )
      

    def permissoes_callback(self, permissions, grants):
        try:
            # Encontra o índice da permissão da câmera
            idx_camera = permissions.index(Permission.CAMERA)

            # Verifica se a permissão da câmera foi concedida
            if grants[idx_camera]:
                Clock.schedule_once(lambda dt: self.CameraModal(), 1)
            else:
                print("Permissão da câmera foi negada.")
                self.stop()
        except ValueError:
            print("Permissão da câmera não encontrada na lista de permissões.")
            self.stop()


    def Mostrar_pdf(self, pdf_path):
        url = f"{PDF_URL}/imagem"
        conteudo = pdf_path
        files = {"file": conteudo}
        response = session.post(url, files=files)
        if response.status_code == 200:
            # Salva o arquivo recebido
            output_path = self.salvar_arquivos("Formularios", "pagina.png")
            with open(output_path, "wb") as f:
                f.write(response.content)

            x = PDFModalView(pdf_path=output_path)
            x.open()
        else:
            self.label2 = f"Erro: {response.status_code} - {response.json()}"

    def show_warning(self, frase):
        content = BoxLayout(orientation="vertical", padding=10, spacing=10)
        # Criar um contêiner de rolagem para evitar que o texto sobreponha os
        # botões
        scroll_view = ScrollView(size_hint=(1, 0.7), size=(500, 100))

        label = Label(
            text=frase,
            size_hint_y=None,
            height=35,
            text_size=(480, None),
            font_size="16sp",
            halign="center",
            valign="middle",
        )

        scroll_view.add_widget(label)  # Adiciona a Label dentro do ScrollView

        button = Button(text="Ok", size_hint=(None, None), size=(80, 60))

        popup2 = Popup(
            title="Aviso",
            content=content,
            size_hint=(None, None),
            size=(700, 400),
            auto_dismiss=False,
        )

        content.add_widget(scroll_view)
        content.add_widget(button)
        button.bind(on_release=popup2.dismiss)
        popup2.open()

    def show_warning3(self, frase):
        content = BoxLayout(orientation="vertical", padding=10, spacing=10)

        # Criar um contêiner de rolagem para evitar que o texto sobreponha os
        # botões
        scroll_view = ScrollView(size_hint=(1, 0.7), size=(500, 100))

        text_input = TextInput(text=frase, readonly=True)
        scroll_view.add_widget(text_input)

        button = Button(
            text="Sim",
            size_hint=(None, None),
            pos_hint={"left": 1, "y": 0},
            size=(100, 60),
        )
        # button2 = Button(text='Voltar', size_hint=(None, None),pos_hint={'right': 1, 'y': 0}, size=(120, 50))

        self.popup = Popup(
            title="Aviso",
            content=content,
            size_hint=(None, None),
            size=(700, 400),
            auto_dismiss=False,
        )

        # Adiciona ScrollView em vez da Label diretamente
        content.add_widget(scroll_view)
        content.add_widget(button)
        # content.add_widget(button2)
        self.popup_aberto = True
        button.bind(on_release=self.Sim)
        # button2.bind(on_release=partial(self.verifica_nao, p))
        self.conta = self.conta + 1
        self.popup.open()

    def verifica(self, m, *args):
        global Dados_atualizados
        texto_salvo = self.text_input2.text
        if texto_salvo != self.resposta:
            valor = texto_salvo.lower().split(m[0])[-1].strip()
            if len(valor) != 0:
                if bool(
                    re.search(r"[^\w\s]", valor[0])
                ):  # verifica se tem pontuação no inicio da string
                    valor = valor[1:].strip()
            Dados_atualizados[m] = valor
        self.prossegir = True
        self.text_input2.text = ""
        self.popup_aberto = False

    def atualizar_texto(self, dt):
        self.resposta = self.comando
        self.text_input2.text = self.resposta

    def on_text_change(self, instance, value):
        self.x = value.lower()

    def imprimir_erro(self, erro):
        self.text_input2.text = erro

    def Sim(self, *args):
        global Dados_atualizados
        agora = datetime.now()
        tempo = agora.strftime("%H:%M:%S")
        if self.conta == 2:
            Dados_atualizados[("parte 2", 7)] = tempo
        elif self.conta == 3:
            Dados_atualizados[("parte 3", 7)] = tempo

        self.prossegir = True
        self.popup_aberto = False
        self.text_input3.text = ""
        self.popup.dismiss()

    def thread_calibra(self, instance):
        # Função de calibração do microfone
        # dados = self.listar_campos_pdf(self.documento)
        threading.Thread(target=self.processo, args=(self.dados,), daemon=True).start()

    def gravacao(self):
        global limiar

        try:

            self.label2 = "Fale algo:"
            self.recorder = MediaRecorder()
            self.recorder.setAudioSource(AudioSource.MIC)
            self.recorder.setOutputFormat(OutputFormat.MPEG_4)
            self.recorder.setAudioEncoder(AudioEncoder.AAC)
            self.recorder.setAudioSamplingRate(44100)
            audio2 = self.salvar_arquivos("audios", "comando.mp4")
            self.recorder.setOutputFile(audio2)
            self.recorder.prepare()
            self.recorder.start()

            threading.Event().wait(self.duracao)  # Duração de gravação em segundos
            if self.recorder:
                self.recorder.stop()
                self.recorder.release()
                self.recorder = None

            voz = self.inferencia_modelo(audio2)
            if "text" in voz:
                self.comando = voz["text"]
                # self.label = self.comando
                Clock.schedule_once(self.atualizar_texto, 0.1)
                texto = self.salvar_arquivos("transcrições", "registro-fala.txt")
                with open(texto, "a", encoding="utf-8") as arquivo:
                    arquivo.write(self.comando + "\n")

        except Exception as e:
            # self.label = f"Erro ao gravar áudio: {e}"
            Clock.schedule_once(
                lambda dt: self.imprimir_erro(f"Erro ao gravar áudio: {e}"), 0.05
            )

    def processo(self, dados):
        global marcadores
        global Dados_atualizados

        self.botao1.disabled = True
        if self.conta == 0:
            agora = datetime.now()
            tempo = agora.strftime("%H:%M:%S")
            Dados_atualizados[("parte 1", 7)] = tempo
            self.conta = self.conta + 1
        self.filtro = [
            "sítio demarcado",
            "via aérea difícil",
            "risco de grande perda sanguínea",
            "acesso venoso adequado",
            "histórico de reação alérgica",
            "apresentação oral",
            "confirmam verbalmente",
            "antibiótico profilático",
            "revisão do cirurgião",
            "revisão do anestesista",
            "correta esterilização",
            "placa de eletrocautério",
            "equipamentos disponíveis",
            "insumos e instrumentais",
            "confirmação do procedimento",
            "contagem de compressas",
            "contagem de instrumentos",
            "contagem de agulhas",
            "amostra cirúrgica identificada",
            "problema com equipamentos",
            "reserva de sangue disponível",
        ]
        if self.fluxo >= len(marcadores):
            self.fluxo = 0
            self.conta = 0

        i = marcadores[self.fluxo]
        if i == "você deseja continuar para a proxima etapa ?":
            # Clock.schedule_once(lambda dt: self.show_warning3(i), 0.1)
            self.conta = self.conta + 1
            self.prossegir = True
            agora2 = datetime.now()
            tempo2 = agora2.strftime("%H:%M:%S")
            if self.conta == 2:
                Dados_atualizados[("parte 2", 7)] = tempo2
            elif self.conta == 3:
                Dados_atualizados[("parte 3", 7)] = tempo2

        Clock.schedule_once(lambda dt: self.exibir_dicionario(i), 0.1)
        if i in self.filtro or i in [
            "identidade",
            "sítio cirúrgico",
            "marcar procedimento",
            "consentimento",
            "montagem da so de acordo com o procedimento",
            "material anestésico disponível",
        ]:
            self.duracao = 3
        elif i in [
            "requisição completa",
            "comunicado à enfermeira",
            "comentário do cirurgião",
            "comentário da anestesista",
            "comentário da enfermagem",
        ]:
            self.duracao = 15
        else:
            self.duracao = 7
        while True:
            if self.prossegir:
                Clock.schedule_once(lambda dt: self.clear_text(), 0.08)
                self.prossegir = False
                # self.label = ''
                self.fluxo = self.fluxo + 1
                Clock.schedule_once(lambda dt: self.imprimir_erro(""), 0.08)
                break
            if not self.popup_aberto:
                self.gravacao()

                if i in self.filtro:

                    if "não se aplica" in self.comando.lower():
                        self.comando = "não se aplica" + " " + i
                        
                    elif (
                        "sim" in self.comando.lower()
                        or "não" in self.comando.lower()
                        or "providenciado" in self.comando.lower()
                    ):
                        self.comando = i + " " + self.comando.lower().strip()
                   

                if i in [
                    "identidade",
                    "sítio cirúrgico",
                    "marcar procedimento",
                    "consentimento",
                    "montagem da so de acordo com o procedimento",
                    "material anestésico disponível",
                    "reserva de sangue disponível",
                ]:
                    if "sim" in self.comando.lower():
                        self.comando = i
                    elif "não" in self.comando.lower():
                        self.comando = "não"
                        self.prossegir = True
                        time.sleep(1)
                for campo in dados.keys():
                    if campo[0].lower() in self.comando.lower():

                        if campo[1] == 7 and i == campo[0].lower():  # Tipo texto
                            auxiliar = campo
                            valor = self.comando.lower().split(campo[0])[-1].strip()
                            if len(valor) != 0:
                                if bool(
                                    re.search(r"[^\w\s]", valor[0])
                                ):  # verifica se tem pontuação no inicio da string
                                    valor = valor[1:].strip()
                            Dados_atualizados[campo] = valor
                            time.sleep(10)
                            Clock.schedule_once(lambda dt: self.verifica(auxiliar), 0.1)
                            # self.prossegir = True
                            # self.popup_aberto = False

                        elif campo[1] == 5:
                            Dados_atualizados[campo] = 1
                            # self.prossegir = True
                            # self.popup_aberto = False
                            time.sleep(1)
                            Clock.schedule_once(lambda dt: self.verifica(campo), 0.1)

                        elif campo[1] == 2:
                            Dados_atualizados[campo] = True
                            # self.prossegir = True
                            # self.popup_aberto = False
                            time.sleep(1)
                            Clock.schedule_once(lambda dt: self.verifica(campo), 0.1)

                self.label2 = " "
                time.sleep(0.14)
            if "próximo item" in self.comando.lower():
                # time.sleep(1)
                Clock.schedule_once(lambda dt: self.clear_text(), 1)
                # self.label = " "
                Clock.schedule_once(lambda dt: self.imprimir_erro(""), 1)
                self.fluxo = self.fluxo + 1
                break
        self.botao1.disabled = False
        self.bt_sair.disabled = True
        if i == 'data':
            self.bt_abrir.disabled = False
            self.botao1.disabled = True

    def abrir_pdf(self, instance):
        Intent = autoclass("android.content.Intent")
        PythonActivity = autoclass("org.kivy.android.PythonActivity").mActivity
        intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("application/pdf")  # Selecionar apenas arquivos PDF
        result_code = 1
        PythonActivity.startActivityForResult(intent, result_code)
        activity.bind(on_activity_result=self.on_activity_result)

    def on_activity_result(self, request_code, result_code, intent):

        if (
            request_code == 1
            and result_code == autoclass("android.app.Activity").RESULT_OK
        ):
            uri = intent.getData()
            ContentResolver = autoclass("android.content.ContentResolver")
            PythonActivity2 = autoclass("org.kivy.android.PythonActivity").mActivity
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
            self.full_content = b"".join(content)  # Para binários como PDFs

            self.dados = self.listar_campos_pdf(self.full_content)
            Clock.schedule_once(lambda dt: self.Mostrar_pdf(self.full_content), 0.3)
            # Clock.schedule_once(lambda dt: self.exibir_dicionario(self.dados), 0.05)
            self.importar.disabled = True
            self.botao1.disabled = False

    def thread_preencher(self, instance):
        global Dados_atualizados
        novo = dict(Dados_atualizados)
        download_path = self.salvar_arquivos(
            "Formularios", f"{uuid.uuid4()}_Preenchido.pdf"
        )
        threading.Thread(
            target=self.preencher_campos_pdf,
            args=(
                self.full_content,
                download_path,
                novo,
            ),
            daemon=True,
        ).start()
        Clock.schedule_once(
            lambda dt: self.show_warning("Documento preenchido !!"), 0.1
        )

        Clock.schedule_once(lambda dt: self.imprimir_erro(" "), 0.1)
        # self.label = ''
        self.conta = 0
        self.fluxo = 0
        Dados_atualizados.clear()
        self.importar.disabled = False
        self.bt_abrir.disabled = True
        self.botao1.disabled = True
        self.bt_sair.disabled = False

    def exibir_dicionario(self, chave):
        # Adicione os itens do dicionário na Listbox
        # for chave, valor in dados.items():
        if chave in [
            "sítio demarcado",
            "via aérea difícil",
            "risco de grande perda sanguínea",
            "acesso venoso adequado",
            "histórico de reação alérgica",
            "antibiótico profilático",
            "correta esterilização",
            "placa de eletrocautério",
            "equipamentos disponíveis",
            "insumos e instrumentais",
            "confirmação do procedimento",
            "contagem de compressas",
            "contagem de instrumentos",
            "contagem de agulhas",
            "amostra cirúrgica identificada",
            "problema com equipamentos",
            "montagem da so de acordo com o procedimento",
            "material anestésico disponível",
            "reserva de sangue disponível",
        ]:
            texto_novo = f"{chave}  (x)"
            self.oracao = texto_novo

        elif chave == "revisão do cirurgião":
            texto_novo = f"{chave}. Há momentos críticos, tempos, riscos ?  (x)"
            self.oracao = texto_novo

        elif chave == "revisão do anestesista":
            texto_novo = f"{chave}. Há preocupações em relação ao paciente ?  (x)"
            self.oracao = texto_novo

        elif chave == "qual":
            texto_novo = f"Reação alérgica: {chave}  ..."
            self.oracao = texto_novo
        elif chave in [
            "identidade",
            "sítio cirúrgico",
            "marcar procedimento",
            "consentimento",
        ]:
            if chave == "marcar procedimento":
                texto_novo = f"Paciente Confirmou: {chave[7:]}  (x)"
                self.oracao = texto_novo
            else:
                texto_novo = f"Paciente Confirmou: {chave}  (x)"
                self.oracao = texto_novo
        elif chave == "apresentação oral":
            texto_novo = f"{chave} de cada membro da equipe  (x)"
            self.oracao = texto_novo
        elif chave == "confirmam verbalmente":
            texto_novo = f"Cirurgião, o anestesista e equipe de enfermagem {chave}  (x)"
            self.oracao = texto_novo
        elif chave in [
            "comentário do cirurgião",
            "comentário da anestesista",
            "comentário da enfermagem",
        ]:
            texto_novo = f"Recomendações importantes: {chave}  ..."
            self.oracao = texto_novo
        else:
            texto_novo = f"{chave}  ..."
            self.oracao = texto_novo
        # lbl = Label(text=texto_novo, color=(0, 0, 0, 1),size_hint_y=None,font_size="16sp", height=35)
        self.text_input3.text = self.oracao
        # self.grid.add_widget(lbl)

    def clear_text(self):
        # self.grid.clear_widgets()
        self.oracao = ""
        self.text_input3.text = self.oracao

    def preencher_campos_pdf(self, pdf_path, output_path, data):
        dados = {}
        url = f"{PDF_URL}/preencher-campos"
        # with open(pdf_path, "rb") as fe:
        # conteudo = fe.read()
        conteudo = pdf_path
        files = {"file": conteudo}
        for c, v in data.items():
            dados[f"{c[0]}|{str(c[1])}"] = v
        response = session.post(url, files=files, data=dados)
        if response.status_code == 200:
            # Salva o arquivo recebido
            with open(output_path, "wb") as f:
                f.write(response.content)
        else:
            self.label2 = f"Erro: {response.status_code} - {response.json()}"

    def listar_campos_pdf(self, pdf_path):
        saida = {}
        url = f"{PDF_URL}/listar-campos"
        # with open(pdf_path, "rb") as f:
        # data = f.read()
        data = pdf_path
        files = {"file": data}
        response = session.post(url, files=files)
        resposta = response.json()
        for c, v in resposta.items():
            # dados[(annot.field_name, annot.field_type)] = None
            chaves = c.split("|")
            saida[(chaves[0], int(chaves[1]))] = v
        return saida

    def stop_app(self, instance):
        # Método que encerra a aplicação
        self.stop()


relatorio = Preencher()
relatorio.run()

