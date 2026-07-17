from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.linear_model import LinearRegression
from sklearn.impute import SimpleImputer
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

from config import DATASET, MODELS, MODEL_TYPE_LINEAR_REGRESSION, MODEL_TYPE_GRADIENT_BOOSTING, validate_model_type

def train_model(target, model_type):
    model_type = validate_model_type(model_type)

    # Complete training pipeline
    df = load_dataset()

    X, y = prepare_features(df, target)

    X_train, X_test, y_train, y_test = split_dataset(X, y)

    model = build_model(model_type)

    model = perform_model_training(model, X_train, y_train)

    metrics = evaluate_model(model, X_test, y_test, target, model_type)

    save_model(model, X, target, model_type)

    return metrics

def load_dataset():
    # Load data
    df = pd.read_csv(DATASET)
    print(f"Dataset loaded: {df.shape}")
    return df

def prepare_features(df, target):
    # Convert date and extract time features
    df["week_start"] = pd.to_datetime(df["week_start"])

    df["month"] = df["week_start"].dt.month
    df["week_number"] = df["week_start"].dt.isocalendar().week

    # Drop non feature columns
    X = df.drop(
        columns=[
            target,
            "user_id",
            "week_start"
        ]
    )
    #Set Target values to y
    y = df[target]

    # Convert categorical variables to numeric features
    X = pd.get_dummies(X)

    print("\nFeature columns:")
    print(X.columns.tolist())
    print("\nTarget column:")
    print(y.name)

    return X, y

def split_dataset(X, y):
    # train_test_split - Divide data for learning and evaluation
    # training 80% | testing 20%
    # random_state 42 makes the split repeatable to reproduce the results
    return train_test_split(
        X,
        y,
        test_size=0.2,
        random_state=42
    )

def build_model(model_type):
    # Model pipeline configuration
    # SimpleImputer - Set missing values with median
    if model_type == MODEL_TYPE_LINEAR_REGRESSION:
        model = LinearRegression()

    elif model_type == MODEL_TYPE_GRADIENT_BOOSTING:
        model = GradientBoostingRegressor(random_state=42)

    else:
        # Random Forest - builds many decision trees (300)
        # random_state 42 makes the pipeline reproducible
        model = RandomForestRegressor(
            n_estimators=300,
            random_state=42
        )

    return Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("model", model)
    ])

def perform_model_training(model, X_train, y_train):
    # Perform - Fill missing values, Train Random Forest
    model.fit(X_train, y_train)
    return model

def evaluate_model(model, X_test, y_test, target, model_type):
    # Evaluate - Fill missing values, Predict the pace and store in prediction
    prediction = model.predict(X_test)

    # Evaluate - Calculate evaluation metrics comparing prediction with y_test
    # MAE (Mean Absolute Error) | RMSE (Root Mean Squared Error) | R^2 Score
    mae = mean_absolute_error(y_test, prediction)
    rmse = mean_squared_error(y_test, prediction) ** 0.5
    r2 = r2_score(y_test, prediction)

    return {
        "message": "Model trained successfully",
        "modelPath": f"{MODELS}/{model_type}_{target}.pkl",
        "modelType": f"{model_type}",
        "mae": round(mae, 4),
        "rmse": round(rmse, 4),
        "r2": round(r2, 4)
    }

def save_model(model, X, target, model_type):
    # Save model to file
    MODEL_DIR = Path(MODELS)
    MODEL_DIR.mkdir(exist_ok=True)
    model_path = MODEL_DIR / f"{model_type}_{target}.pkl"
    feature_names = X.columns.tolist()
    joblib.dump(
        {
            "model": model,
            "features": feature_names
        },
        model_path
    )
