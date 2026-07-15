# RunAnalyticsSystem
A Data-Driven Framework for Monitoring and Predicting Performance in 
Recreational Running Communities Using Wearable Technology Data

Running Analytics System is a web-based platform developed to help 
recreational runners monitor their training activities using data 
collected from the Strava API. The system securely synchronises 
running activities, stores them in a database and provides meaningful 
insights into running performance for recreational running clubs.

The application analyse historical running data and generate predictions 
related to performance, training load and recovery. The objective is to 
support runners and coaches in making informed, data-driven training 
decisions while reducing the risk of overtraining and injury.

Authorization URL
https://www.strava.com/oauth/authorize?client_id=218954&response_type=code&redirect_uri=http://localhost/exchange_token&approval_prompt=force&scope=read,activity:read_all

Build with maven
mvn clean install

Run the application
mvn spring-boot:run

DB setup
dbscritps/dbsetup.sql

Main page
http://localhost:8080/runanalytics/

Main features
- Securely synchronise running activities from Strava API
- Store running activities in a database
- Analyse historical running data
- Generate predictions related to performance, training load and recovery
- Provide meaningful insights into running performance for recreational running clubs

ML training
- Reads training_dataset.csv custom path with `--input`
- Run python ML/train_model.py to train a Random Forest regressor
- The trained model is saved as `ML/trained_model.pkl`
- Evaluation metrics are printed to the console after training

