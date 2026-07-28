# RunAnalyticsSystem
A data-driven framework for monitoring and predicting performance in 
recreational running communities using wearable technology data

Running Analytics System is a web-based platform developed to help 
recreational runners monitor their training activities using data 
collected from the Strava API. The system securely synchronises 
running activities, stores them in a database and provides meaningful 
insights into running performance for recreational running clubs.

The application analyse historical running data and generate predictions 
related to performance. The objective is to 
support runners and coaches in making informed, data-driven training 
decisions while reducing the risk of overtraining and injury.

Authorization URL : [link](https://www.strava.com/oauth/authorize?client_id=218954&response_type=code&redirect_uri=https://runninganalyticsoauth.onrender.com/runanalytics-oauth/api/authenticate&approval_prompt=force&scope=read,activity:read_all)

GitHub repository for the main application: [https://github.com/cjpannila/RunningAnalyticsSystem](https://github.com/cjpannila/RunningAnalyticsSystem)

GitHub repository for the authorization module: [https://github.com/cjpannila/RunningAnalyticsOAuth](https://github.com/cjpannila/RunningAnalyticsOAuth)

Maven 3.x should be installed in the server to build the code
Maven version
```bash
mvn -v
Apache Maven 3.9.5
```
Java 21 should be installed in the server to run the application
Java version
```bash
java -version
java 21.0.11
```

DB setup
PostgreSQL should be installed in the server to run the application
```bash
cd <RunningAnalyticsSystem_home>
postgres --version
postgres (PostgreSQL) 16.1
# 1. Create the database in PostgreSQL
createdb -U postgres RunAnalytics
# 2. Execute the dbscript located at `dbscritps/dbscript.sql` into the DB
psql -U postgres -d RunAnalytics -f dbscripts/dbscript.sql
# 3. If the database user password is not set to 'root', update the `application.properties` file with the correct password
spring.datasource.password=root
# 4. Import the data from the `dbscripts/RunAnalytics_backup.sql` file into the database
psql -U postgres -d RunAnalytics -f dbscripts/RunAnalytics_backup.sql
```

Build with maven from the project root directory
```bash
mvn clean install
```

How to package from the project root directory
```bash
mvn clean package
```
run the application with the following command:
```bash
java -jar target/runanalytics-0.0.1-SNAPSHOT.jar
```

Run the application from the project root directory
```bash
mvn spring-boot:run
```

Main page
http://localhost:8080/runanalytics/

Main features
- Securely synchronise running activities from Strava API
- Store running activities in a database
- Analyse historical running data
- Generate predictions related to performance, training load and recovery
- Provide meaningful insights into running performance for recreational running clubs

ML training
- Reads `Downloads/training_dataset.csv` and trains a Random Forest regressor / Linear Regression / Gradient Boosting model
- Run python ML/ml_train.py to train via api call `POST http://127.0.0.1:8001/train?target={target}&model_type={model_type}`
- The trained model is saved as `ML/models/{model_type}_{target}.pkl` (eg. `ML/models/random_forest_target_next_week_km.pkl`)
- Evaluation metrics are printed to the console after training
- Predict using api call `http://127.0.0.1:8001/predict?target={target}&model_type={model_type}`
  with input features saved in file `Downloads/prediction_dataset.csv`
- Evaluation results can be obtained via api call `http://127.0.0.1:8001/evaluate?target={target}&model_type={model_type}`

For above three ML api calls
The `{target}` parameter can be one of the following:
- `target_next_week_km`
- `target_next_week_pace`

The `{model_type}` parameter can be one of the following:
- `random_forest`
- `linear_regression`
- `gradient_boosting`

Run ML prediction module
Start Python FastAPI server with the following command:
```bash
cd <RunningAnalyticsSystem_home>\ML\
uvicorn ml_server:app --host 127.0.0.1 --port 8001
```

It has below endpoints:
- GET /health: Returns a simple health check response to verify that the server is running.
- POST /train: Accepts a JSON payload with training data and retrains the model, returning the updated evaluation metrics.
  - parameters target, model_type
- POST /evaluate: Accepts a JSON payload with evaluation data and returns the evaluation metrics for the current model.
  - parameters target, model_type
- POST /predict: Accepts a JSON payload with input features and returns the predicted performance metrics.
  - parameters target, model_type

How to evaluate the previously trained model
- Copy the trained model file from `ML/models_bakup/{model_type}_{target}.pkl` to `ML/models/{model_type}_{target}.pkl`
- Call the evaluate api endpoint `POST http://127.0.0.1:8001/evaluate?target={target}&model_type={model_type}`
- Or can directly use thee `Evaluate Model` button in the web application in the `Performance Predictions` page.

How to predict using the previously trained model
- Copy the trained model file from `ML/models_bakup/{model_type}_{target}.pkl` to `ML/models/{model_type}_{target}.pkl`
- Copy the prediction dataset from `ML/dataset_backup/prediction_dataset.csv` to `Downloads` directory.
- Call the predict api endpoint `POST http://127.0.0.1:8001/predict?target={target}&model_type={model_type}`
- Or can directly use thee `Predict` button in the web application in the `Performance Predictions` page.

How to train a new model
- Copy the training dataset from `ML/dataset_backup/training_dataset.csv` to `Downloads` directory.
- Call the train api endpoint `POST http://127.0.0.1:8001/train?target={target}&model_type={model_type}`
- Or can directly use thee `Train Model` button in the web application in the `Performance Predictions` page.
