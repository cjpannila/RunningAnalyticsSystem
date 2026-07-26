from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from ml_train import train_model
from ml_predict import generate_predictions, load_evaluation_results
from config import TARGET_DEFAULT, MODEL_TYPE_RANDOM_FOREST

import time #for tracking api execution time

app = FastAPI()

# Enable CORS in FastAPI
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/train")
def train_model_endpoint(target: str = TARGET_DEFAULT, model_type: str = MODEL_TYPE_RANDOM_FOREST):
    start = time.perf_counter()
    print("/train api called")
    result = train_model(target, model_type)
    end = time.perf_counter()
    print(f"/ML train api execution time: {end - start:.4f} seconds")
    return result

@app.post("/predict")
def predict_model_endpoint(target: str = TARGET_DEFAULT, model_type: str = MODEL_TYPE_RANDOM_FOREST):
    start = time.perf_counter()
    print("/predict api called")
    result = generate_predictions(target, model_type)
    end = time.perf_counter()
    print(f"/ML predict api execution time: {end - start:.4f} seconds")
    return result

@app.post("/evaluate")
def evaluate_model_endpoint(target: str = TARGET_DEFAULT, model_type: str = MODEL_TYPE_RANDOM_FOREST):
    start = time.perf_counter()
    print("/evaluate api called")
    result = load_evaluation_results(target, model_type)
    end = time.perf_counter()
    print(f"/ML evaluate api execution time: {end - start:.4f} seconds")
    return result
