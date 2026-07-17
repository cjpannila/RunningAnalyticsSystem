from pathlib import Path

DATASET = Path.home() / "Downloads" / "training_dataset.csv"
PREDICTION_DATASET = Path.home() / "Downloads" / "prediction_dataset.csv"
MODELS = "models"
TARGET_DEFAULT = "target_next_week_km"
MODEL_TYPE_LINEAR_REGRESSION = "linear_regression"
MODEL_TYPE_RANDOM_FOREST = "random_forest"
MODEL_TYPE_GRADIENT_BOOSTING = "gradient_boosting"

def validate_model_type(model_type):
    # Validate model type
    if model_type not in [
        MODEL_TYPE_LINEAR_REGRESSION,
        MODEL_TYPE_GRADIENT_BOOSTING,
        MODEL_TYPE_RANDOM_FOREST
    ]:
        model_type = MODEL_TYPE_RANDOM_FOREST
    return model_type