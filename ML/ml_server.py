from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from ml_service import train_model
from ml_predict import generate_predictions
from config import TARGET_DEFAULT, MODEL_TYPE_RANDOM_FOREST

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
    print("/train api called")
    return train_model(target, model_type)

@app.post("/predict")
def predict_model_endpoint(target: str = TARGET_DEFAULT, model_type: str = MODEL_TYPE_RANDOM_FOREST):
    print("/predict api called")
    return generate_predictions(target, model_type)
