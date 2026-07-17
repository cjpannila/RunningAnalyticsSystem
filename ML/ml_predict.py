from pathlib import Path

import joblib
import pandas as pd

from config import PREDICTION_DATASET, MODELS

def generate_predictions(target):
    print("Generating Predictions...")
    # Load the trained model
    saved = joblib.load(Path(MODELS) / f"{target}.pkl")
    model = saved["model"]
    features = saved["features"]

    df = load_dataset()

    # Keep identifying columns
    result = df[["user_id", "week_start"]].copy()

    X = prepare_features(df, features)

    # Predict and assign to result
    result[f"{target}_prediction"] = model.predict(X)

    print("Predictions for the next week:")
    print(result)
    return result.to_dict(orient="records")

def load_dataset():
    # Load new weekly data
    df = pd.read_csv(PREDICTION_DATASET)
    print(f"Dataset loaded: {df.shape}")
    return df

def prepare_features(df, features):
    # Convert date and extract time features
    df["week_start"] = pd.to_datetime(df["week_start"])
    df["month"] = df["week_start"].dt.month
    df["week_number"] = df["week_start"].dt.isocalendar().week

    # Drop columns that were not used during training
    X = df.drop(columns=["user_id", "week_start"])

    # Convert categorical variables to numeric features
    X = pd.get_dummies(X)
    X = X.reindex(columns=features, fill_value=0)
    return X