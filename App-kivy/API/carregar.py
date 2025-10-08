import whisper
import torch

def get_model():
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(device)
    model = whisper.load_model("turbo",device=device)
    return model
