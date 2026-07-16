from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sklearn.ensemble import RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

app = FastAPI()

# Enable CORS in FastAPI
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

DATASET = Path.home() / "Downloads" / "training_dataset.csv"
MODEL_OUTPUT = "trained_model.pkl"
TARGET = "target_next_week_km"

@app.post("/train")
def train_model():
    # Load data
    df = pd.read_csv(DATASET)

    # Convert date and extract time features
    df["week_start"] = pd.to_datetime(df["week_start"])

    df["month"] = df["week_start"].dt.month
    df["week_number"] = df["week_start"].dt.isocalendar().week

    # Drop non feature columns
    X = df.drop(
        columns=[
            TARGET,
            "user_id",
            "week_start"
        ]
    )
    #Set Target values to y
    y = df[TARGET]

    # Convert categorical variables to numeric features
    X = pd.get_dummies(X)

    print("\nFeature columns:")
    print(X.columns.tolist())
    print("\nTarget column:")
    print(y.name)

    # train_test_split - Divide data for learning and evaluation
    # training 80% | testing 20%
    # random_state 42 makes the split repeatable to reproduce the results
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.2,
        random_state=42
    )

    # Model pipeline configuration
    # SimpleImputer - Set missing values with median
    # Random Forest - builds many decision trees (300)
    # random_state 42 makes the pipeline reproducible
    model = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        (
            "model",
            RandomForestRegressor(
                n_estimators=300,
                random_state=42
            )
        )
    ])
    # Perform - Fill missing values, Train Random Forest, Save trained model
    model.fit(X_train, y_train)

    # Evaluate - Fill missing values, Predict the pace and store in prediction
    prediction = model.predict(X_test)

    # Evaluate - Calculate evaluation metrics comparing prediction with y_test
    # MAE (Mean Absolute Error) | RMSE (Root Mean Squared Error) | R^2 Score
    mae = mean_absolute_error(y_test, prediction)
    rmse = mean_squared_error(y_test, prediction) ** 0.5
    r2 = r2_score(y_test, prediction)

    # Save model to file
    joblib.dump(model, MODEL_OUTPUT)

    return {
        "message": "Model trained successfully",
        "modelPath": MODEL_OUTPUT,
        "mae": round(mae, 4),
        "rmse": round(rmse, 4),
        "r2": round(r2, 4)
    }