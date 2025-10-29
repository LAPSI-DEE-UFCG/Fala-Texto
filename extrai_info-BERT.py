import whisper
model = whisper.load_model("small")

import warnings
warnings.filterwarnings("ignore")

import transformers
from transformers import pipeline



    
def transcricao(arquivo):
	result = model.transcribe(arquivo, language = 'Portuguese')
	return result['text']

#from unidecode import unidecode
import os
directory = os.getcwd()
wav_files = []

for file_path in os.listdir(directory):
    # check if current file_path is a file
    if file_path.endswith('.wav'):
    #if os.path.isfile(os.path.join(files, file_path)):
        # add filename to list
        wav_files.append(file_path)
wav_files = sorted(wav_files, key=lambda t: -os.stat(t).st_mtime)
#print(wav_files)
#indexed_list = [f'{index}: {value}' for index, value in enumerate(wav_files)]
for index, value in enumerate(wav_files):
    print(f'{index}:\t{value}')
#print(indexed_list)
i = input('Escolha o indice do arquivo de áudio que você deseja: ')

text5 = transcricao(wav_files[int(i)])
#
text5 = text5.upper()
text5 = text5.replace(".", "")
text5 = text5.replace(",", "")
#
print(text5)

#text5 = text5.split()
#print(text5)

#---------------------------------------------------------------------------
'''
model_name = 'pierreguillou/bert-base-cased-squad-v1.1-portuguese'
nlp = pipeline("question-answering", model=model_name)
'''
nlp = pipeline("ner", model="Davlan/bert-base-multilingual-cased-ner-hrl")


question1 = "Nome?"
question2 = "Telefone?"
#question3 = "Endereço ou onde mora?"
question3 = "Qual é o local de residência, incluindo rua, número e bairro?"
question4 = "Data?"
question5 = "Cor favorita?"

result1 = nlp(question=question1, context=text5)
result2 = nlp(question=question2, context=text5)
result3 = nlp(question=question3, context=text5)
result4 = nlp(question=question4, context=text5)
result5 = nlp(question=question5, context=text5)

print(f"Answer: {result1['answer']}")
print(f"Answer: {result2['answer']}")
print(f"Answer: {result3['answer']}")
print(f"Answer: {result4['answer']}")
print(f"Answer: {result5['answer']}")
