import torch
import librosa
from datasets import load_dataset
from transformers import WhisperProcessor, WhisperForConditionalGeneration, Seq2SeqTrainer, Seq2SeqTrainingArguments
from torch.nn.utils.rnn import pad_sequence
from huggingface_hub import login
import evaluate
import time
import os 

NUM_PROC = os.cpu_count()

# Login Hugging Face
login(token="*")

# Carregar dataset
dataset = load_dataset("brunopbb/ufcg-labmet-fala-texto-main-final")

# Separar em conjuntos de treino e teste
train_dataset = dataset["train"]
eval_dataset = dataset["test"]

# Carregar modelo e processor
model = WhisperForConditionalGeneration.from_pretrained("openai/whisper-small")
processor = WhisperProcessor.from_pretrained("openai/whisper-small")

# Configurar o modelo para português
model.generation_config.language = "portuguese"
model.generation_config.task = "transcribe"

# Métrica WER
wer_metric = evaluate.load("wer")

# Pré-processamento: foco na coluna "transcription"
def preprocess(batch):
    audio_array = batch["audio"]["array"]
    sr = batch["audio"]["sampling_rate"]
    if sr != 16000:
        audio_array = librosa.resample(audio_array, orig_sr=sr, target_sr=16000)
    input_features = processor(audio_array, sampling_rate=16000, return_tensors="pt").input_features.squeeze(0)
    labels = processor.tokenizer(batch["transcription"]).input_ids
    batch["input_features"] = input_features
    batch["labels"] = torch.tensor(labels)
    return batch

train_dataset = train_dataset.map(preprocess, remove_columns=train_dataset.column_names, num_proc=NUM_PROC)
eval_dataset = eval_dataset.map(preprocess, remove_columns=eval_dataset.column_names, num_proc=NUM_PROC)

# Funções de collate e métricas
def collate_fn(batch):
    # Converter todos os inputs e labels para Tensor
    inputs = [torch.tensor(item["input_features"]) if not isinstance(item["input_features"], torch.Tensor) else item["input_features"] for item in batch]
    labels = [torch.tensor(item["labels"]) if not isinstance(item["labels"], torch.Tensor) else item["labels"] for item in batch]

    # Padronizar sequências
    inputs_padded = pad_sequence(inputs, batch_first=True, padding_value=0.0)
    labels_padded = pad_sequence(labels, batch_first=True, padding_value=-100)

    return {"input_features": inputs_padded, "labels": labels_padded}

def compute_metrics(eval_pred):
    predictions, labels = eval_pred
    pred_texts = processor.batch_decode(predictions, skip_special_tokens=True)
    # Decodifica as labels de referência
    labels[labels == -100] = processor.tokenizer.pad_token_id
    label_texts = processor.batch_decode(labels, skip_special_tokens=True)
    # Calcula e retorna o WER
    wer_score = wer_metric.compute(predictions=pred_texts, references=label_texts)
    return {"wer": wer_score}

output_dir="./whisper-small-finetuned"

# Hiperparâmetros e argumentos de treino
training_args = Seq2SeqTrainingArguments(
    output_dir=output_dir,
    per_device_train_batch_size=2,
    gradient_accumulation_steps=4,
    learning_rate=1e-5,
    num_train_epochs=3,
    weight_decay=0.01,
    logging_dir="./logs",
    logging_steps=10,
    fp16=False,
    save_total_limit=2,
    save_steps=200,
    predict_with_generate=True,
    dataloader_num_workers=NUM_PROC,
    max_grad_norm=1.0,
)

# Trainer
trainer = Seq2SeqTrainer(
    model=model,
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=eval_dataset,
    processing_class=processor,
    data_collator=collate_fn,
    compute_metrics=compute_metrics
)

# Treinamento com tempo por epoch
start_total = time.time()
trainer.train()
print("Avaliação final do modelo:")
eval_results = trainer.evaluate()
print(eval_results)
end_total = time.time()
print(f"Treinamento concluído em {end_total - start_total:.1f}s")
# Salvar modelo e processor fine-tuned
output_dir="./whisper-small-finetuned"
model.save_pretrained(output_dir)
processor.save_pretrained(output_dir)