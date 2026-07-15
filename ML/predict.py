from pathlib import Path

import joblib
import pandas as pd

print("Generating Predictions...")
# Load the trained model
model = joblib.load("trained_model.pkl")

# Load new weekly data
DATASET = Path.home() / "Downloads" / "next_week_features.csv"
df = pd.read_csv(DATASET)

# Convert date and extract time features
df["week_start"] = pd.to_datetime(df["week_start"])

df["month"] = df["week_start"].dt.month
df["week_number"] = df["week_start"].dt.isocalendar().week

# Drop columns that were not used during training
df = df.drop(columns=["user_id", "week_start"])

# Make predictions
prediction = model.predict(df)

print("Predictions for the next week:")
print(prediction)