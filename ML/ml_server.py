from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from ml_service import train_model
from config import TARGET

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
def train_model_endpoint(target: str = TARGET):
    print("/train api called")
    return train_model(target)
