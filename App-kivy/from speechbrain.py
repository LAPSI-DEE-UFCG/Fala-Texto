from speechbrain.inference.speaker import SpeakerRecognition
import torch

verification = SpeakerRecognition.from_hparams(
    source="speechbrain/spkrec-ecapa-voxceleb",
    savedir="pretrained_models/spkrec-ecapa-voxceleb",
    run_opts={"device": "cuda"}  # se quiser usar GPU
)

score, prediction = verification.verify_files("/home/fala-texto/Downloads/cadastro.wav", '/home/fala-texto/Downloads/teste4.wav')
print(prediction)
float_value = score.item()
print(float_value)
if prediction and float_value > 0.6:
    print("Acesso autorizado!")
else:
    print("Acesso negado.")


