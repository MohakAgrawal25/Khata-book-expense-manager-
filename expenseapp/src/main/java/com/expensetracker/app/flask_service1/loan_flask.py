import os
import sys
import json
import pandas as pd
import numpy as np
import joblib
from pathlib import Path
from flask import Flask, request, jsonify, send_from_directory, render_template_string
from flask_cors import CORS
import traceback
from datetime import datetime

# Get the Flask service path
flask_service_path = Path(__file__).parent
print(f"Flask service path: {flask_service_path}")

# Find static folder
static_path = flask_service_path / "static"
if not static_path.exists():
    print(f"📁 Creating static folder at: {static_path}")
    static_path.mkdir(parents=True, exist_ok=True)

# Check for loan_prediction.html
html_path = static_path / "loan_prediction.html"
if html_path.exists():
    print(f"✅ HTML file found: {html_path}")
else:
    print(f"❌ loan_prediction.html not found in {static_path}!")
    # Create a simple HTML file if it doesn't exist
    create_simple_html(html_path)

# Create models directory
models_path = flask_service_path / "models"
if not models_path.exists():
    print(f"📁 Creating models folder at: {models_path}")
    models_path.mkdir(parents=True, exist_ok=True)

app = Flask(__name__, 
            static_folder=str(static_path),
            static_url_path='')
# Enable CORS for all domains (for development)
CORS(app, resources={r"/*": {"origins": "*"}})

# Global variables for models
model = None
scaler = None
feature_columns = None
scaler_columns = None

def create_simple_html(file_path):
    """Create a simple HTML interface if the file doesn't exist"""
    html_content = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Loan Approval Prediction</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f8f9fa; padding: 20px; }
        .card { max-width: 800px; margin: 0 auto; }
    </style>
</head>
<body>
    <div class="card">
        <div class="card-header bg-primary text-white">
            <h3>Loan Approval Prediction</h3>
        </div>
        <div class="card-body">
            <form id="predictionForm">
                <div class="row">
                    <div class="col-md-6">
                        <div class="mb-3">
                            <label>Age</label>
                            <input type="number" class="form-control" name="person_age" value="35" required>
                        </div>
                        <div class="mb-3">
                            <label>Income ($)</label>
                            <input type="number" class="form-control" name="person_income" value="75000" required>
                        </div>
                        <div class="mb-3">
                            <label>Employment Experience (years)</label>
                            <input type="number" step="0.1" class="form-control" name="person_emp_exp" value="5" required>
                        </div>
                        <div class="mb-3">
                            <label>Loan Amount ($)</label>
                            <input type="number" class="form-control" name="loan_amnt" value="20000" required>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="mb-3">
                            <label>Interest Rate (%)</label>
                            <input type="number" step="0.1" class="form-control" name="loan_int_rate" value="7.5" required>
                        </div>
                        <div class="mb-3">
                            <label>Credit Score</label>
                            <input type="number" class="form-control" name="credit_score" value="720" required>
                        </div>
                        <div class="mb-3">
                            <label>Loan Grade</label>
                            <select class="form-control" name="loan_grade" required>
                                <option value="A">A</option>
                                <option value="B" selected>B</option>
                                <option value="C">C</option>
                                <option value="D">D</option>
                                <option value="E">E</option>
                                <option value="F">F</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label>Home Ownership</label>
                            <select class="form-control" name="person_home_ownership" required>
                                <option value="RENT" selected>Rent</option>
                                <option value="MORTGAGE">Mortgage</option>
                                <option value="OWN">Own</option>
                            </select>
                        </div>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary w-100">Get Prediction</button>
            </form>
            <div id="result" class="mt-3"></div>
        </div>
    </div>
    <script>
        document.getElementById('predictionForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(e.target);
            const data = Object.fromEntries(formData.entries());
            
            const response = await fetch('/api/predict', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data)
            });
            const result = await response.json();
            document.getElementById('result').innerHTML = `
                <div class="alert ${result.prediction === 'approved' ? 'alert-success' : 'alert-danger'}">
                    <h4>${result.prediction === 'approved' ? '✅ Approved' : '❌ Rejected'}</h4>
                    <p>Probability: ${(result.probability * 100).toFixed(1)}%</p>
                </div>
            `;
        });
    </script>
</body>
</html>
    """
    with open(file_path, 'w') as f:
        f.write(html_content)
    print(f"✅ Created simple HTML interface at: {file_path}")

def load_models():
    """Load the trained ML models"""
    global model, scaler, feature_columns, scaler_columns
    
    try:
        print("\n" + "="*50)
        print("🔍 Looking for trained ML models...")
        
        # Try multiple possible locations for models
        possible_model_locations = [
            models_path / "loan_approval.pkl",
            models_path / "loan_model.pkl",
            models_path / "model.pkl",
            flask_service_path / "loan_approval.pkl",
            flask_service_path / "loan_model.pkl",
            Path.cwd() / "loan_approval.pkl",
            Path.cwd() / "loan_model.pkl",
        ]
        
        possible_scaler_locations = [
            models_path / "loan_scaler.pkl",
            models_path / "scaler.pkl",
            flask_service_path / "loan_scaler.pkl",
            flask_service_path / "scaler.pkl",
            Path.cwd() / "loan_scaler.pkl",
            Path.cwd() / "scaler.pkl",
        ]
        
        # Find and load model
        model_path = None
        for path in possible_model_locations:
            if path.exists():
                model_path = path
                print(f"✅ Found model at: {path}")
                model = joblib.load(str(path))
                break
        
        # Find and load scaler
        scaler_path = None
        for path in possible_scaler_locations:
            if path.exists():
                scaler_path = path
                print(f"✅ Found scaler at: {path}")
                scaler = joblib.load(str(path))
                break
        
        if model_path:
            print(f"✅ Model loaded successfully!")
            
            # Get feature information from model
            if hasattr(model, 'feature_names_in_'):
                feature_columns = list(model.feature_names_in_)
                print(f"📊 Model expects {len(feature_columns)} features:")
                for i, feat in enumerate(feature_columns):
                    print(f"  {i+1:2d}. {feat}")
            elif hasattr(model, 'n_features_in_'):
                print(f"📊 Model expects {model.n_features_in_} features")
                # Create default feature names
                feature_columns = [f"feature_{i}" for i in range(model.n_features_in_)]
            else:
                print("⚠ Model doesn't have feature information")
                feature_columns = None
            
            if scaler_path:
                print(f"✅ Scaler loaded successfully!")
                # Get scaler information
                if hasattr(scaler, 'feature_names_in_'):
                    scaler_columns = list(scaler.feature_names_in_)
                    print(f"📊 Scaler expects {len(scaler_columns)} features:")
                    for i, feat in enumerate(scaler_columns):
                        print(f"  {i+1:2d}. {feat}")
                elif hasattr(scaler, 'n_features_in_'):
                    print(f"📊 Scaler expects {scaler.n_features_in_} features")
                    scaler_columns = [f"feature_{i}" for i in range(scaler.n_features_in_)]
                else:
                    print("⚠ Scaler doesn't have feature information")
                    scaler_columns = None
            else:
                print("⚠ No scaler found, using fallback prediction without scaling")
            
            return True
        else:
            print("❌ Model not found. Please place your model .pkl file in:")
            print(f"   📁 {models_path}")
            print("   Common names: loan_approval.pkl, loan_model.pkl")
            return False
            
    except Exception as e:
        print(f"❌ Error loading models: {str(e)}")
        traceback.print_exc()
        return False

# Load models on startup
print("\n" + "="*60)
print("🚀 Starting Loan Approval Flask Service")
print("="*60)

if load_models():
    print("✅ Using trained ML models for predictions")
else:
    print("❌ ERROR: No trained models found!")
    print("💡 Please place your trained model (.pkl file) in the models folder")
    print("💡 The application cannot run without trained model")
    print("="*60)
    sys.exit(1)

# HTML template for fallback
HTML_FALLBACK = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Loan Approval Prediction API</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 2rem;
        }
        .card {
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            border: none;
        }
        .prediction-result {
            padding: 20px;
            border-radius: 10px;
            margin: 20px 0;
        }
        .approved {
            background-color: #d4edda;
            border: 1px solid #c3e6cb;
        }
        .rejected {
            background-color: #f8d7da;
            border: 1px solid #f5c6cb;
        }
        .test-form {
            background-color: #f8f9fa;
            padding: 20px;
            border-radius: 10px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h3 class="mb-0"><i class="fas fa-robot"></i> Loan Approval Prediction API</h3>
                    </div>
                    <div class="card-body">
                        <div class="alert alert-success">
                            <h6><i class="fas fa-check-circle"></i> Model Status</h6>
                            <p>✅ Trained model loaded successfully</p>
                            <p>📊 Model expects: {{ model_features }} features</p>
                            <p>📊 Scaler expects: {{ scaler_features }} features</p>
                        </div>
                        
                        <div class="test-form">
                            <h5><i class="fas fa-vial"></i> Test Prediction</h5>
                            <form id="testPredictionForm">
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">Age</label>
                                            <input type="number" class="form-control" name="person_age" value="35" required>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Income ($)</label>
                                            <input type="number" class="form-control" name="person_income" value="75000" required>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Employment Experience (years)</label>
                                            <input type="number" step="0.1" class="form-control" name="person_emp_exp" value="5" required>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Loan Amount ($)</label>
                                            <input type="number" class="form-control" name="loan_amnt" value="20000" required>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">Interest Rate (%)</label>
                                            <input type="number" step="0.1" class="form-control" name="loan_int_rate" value="7.5" required>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Credit Score</label>
                                            <input type="number" class="form-control" name="credit_score" value="720" required>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Loan Grade</label>
                                            <select class="form-control" name="loan_grade" required>
                                                <option value="A">A</option>
                                                <option value="B" selected>B</option>
                                                <option value="C">C</option>
                                                <option value="D">D</option>
                                                <option value="E">E</option>
                                                <option value="F">F</option>
                                            </select>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Home Ownership</label>
                                            <select class="form-control" name="person_home_ownership" required>
                                                <option value="RENT" selected>Rent</option>
                                                <option value="MORTGAGE">Mortgage</option>
                                                <option value="OWN">Own</option>
                                            </select>
                                        </div>
                                    </div>
                                </div>
                                <button type="submit" class="btn btn-primary">
                                    <i class="fas fa-bolt"></i> Test Prediction
                                </button>
                            </form>
                            
                            <div id="predictionResult" class="prediction-result" style="display: none;">
                                <h5>Prediction Result:</h5>
                                <div id="resultContent"></div>
                            </div>
                        </div>
                        
                        <div class="text-center mt-4">
                            <div class="alert alert-warning">
                                <i class="fas fa-exclamation-triangle"></i>
                                The HTML interface (loan_prediction.html) was not found at the expected location.
                                Using this fallback interface instead.
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script>
        document.getElementById('testPredictionForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const formData = new FormData(this);
            const data = {
                person_age: parseFloat(formData.get('person_age')),
                person_income: parseFloat(formData.get('person_income')),
                person_emp_exp: parseFloat(formData.get('person_emp_exp')),
                loan_amnt: parseFloat(formData.get('loan_amnt')),
                loan_int_rate: parseFloat(formData.get('loan_int_rate')),
                credit_score: parseFloat(formData.get('credit_score')),
                loan_grade: formData.get('loan_grade'),
                person_gender: 'male',
                person_home_ownership: formData.get('person_home_ownership'),
                loan_intent: 'PERSONAL',
                previous_loan_defaults_on_file: 'No',
                debt_to_income_ratio: 0.3,
                cb_person_cred_hist_length: 5,
                loan_percent_income: parseFloat(formData.get('loan_amnt')) / parseFloat(formData.get('person_income'))
            };
            
            const button = this.querySelector('button');
            const originalText = button.innerHTML;
            button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Predicting...';
            button.disabled = true;
            
            try {
                const response = await fetch('/api/predict', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(data)
                });
                
                const result = await response.json();
                const resultDiv = document.getElementById('resultContent');
                const resultContainer = document.getElementById('predictionResult');
                
                if (result.status === 'success') {
                    const isApproved = result.prediction === 'approved';
                    const probPercent = (result.probability * 100).toFixed(1);
                    
                    resultDiv.innerHTML = `
                        <div class="${isApproved ? 'approved' : 'rejected'} p-3 rounded">
                            <h4 class="${isApproved ? 'text-success' : 'text-danger'}">
                                <i class="fas fa-${isApproved ? 'check-circle' : 'times-circle'}"></i>
                                Loan ${result.prediction.toUpperCase()}
                            </h4>
                            <p><strong>Probability:</strong> ${probPercent}%</p>
                            <p><strong>Confidence:</strong> ${(result.confidence * 100).toFixed(1)}%</p>
                            <p><strong>Method:</strong> ${result.model_used}</p>
                            
                            ${result.risk_factors.length > 0 ? `
                            <div class="mt-3">
                                <h6><i class="fas fa-exclamation-triangle"></i> Risk Factors:</h6>
                                <ul>
                                    ${result.risk_factors.map(factor => `<li>${factor}</li>`).join('')}
                                </ul>
                            </div>
                            ` : ''}
                            
                            ${result.recommendations.length > 0 ? `
                            <div class="mt-3">
                                <h6><i class="fas fa-lightbulb"></i> Recommendations:</h6>
                                <ul>
                                    ${result.recommendations.map(rec => `<li>${rec}</li>`).join('')}
                                </ul>
                            </div>
                            ` : ''}
                        </div>
                    `;
                } else {
                    resultDiv.innerHTML = `
                        <div class="alert alert-danger">
                            <h5>Error</h5>
                            <p>${result.message}</p>
                        </div>
                    `;
                }
                
                resultContainer.style.display = 'block';
                
            } catch (error) {
                document.getElementById('resultContent').innerHTML = `
                    <div class="alert alert-danger">
                        <h5>Connection Error</h5>
                        <p>${error.message}</p>
                    </div>
                `;
                document.getElementById('predictionResult').style.display = 'block';
            } finally {
                button.innerHTML = originalText;
                button.disabled = false;
            }
        });
    </script>
</body>
</html>
"""

@app.route('/')
def serve_index():
    """Serve the main HTML page or fallback"""
    try:
        # Check if HTML file exists
        html_file = Path(app.static_folder) / "loan_prediction.html"
        if html_file.exists():
            print(f"✅ Serving HTML from: {html_file}")
            return send_from_directory(app.static_folder, 'loan_prediction.html')
        else:
            print(f"⚠ HTML file not found: {html_file}")
            # Show fallback with test form
            model_features = len(feature_columns) if feature_columns else 'Unknown'
            scaler_features = len(scaler_columns) if scaler_columns else 'Unknown'
            return render_template_string(HTML_FALLBACK, 
                                         model_features=model_features,
                                         scaler_features=scaler_features)
    except Exception as e:
        # Show model status in fallback
        model_features = len(feature_columns) if feature_columns else 'Unknown'
        scaler_features = len(scaler_columns) if scaler_columns else 'Unknown'
        print(f"⚠ Error serving HTML: {e}")
        return render_template_string(HTML_FALLBACK, 
                                     model_features=model_features,
                                     scaler_features=scaler_features)

@app.route('/health')
def health_check():
    """Health check endpoint"""
    try:
        html_exists = (Path(app.static_folder) / "loan_prediction.html").exists()
        status = {
            'status': 'healthy',
            'model_loaded': model is not None,
            'scaler_loaded': scaler is not None,
            'html_file_exists': html_exists,
            'static_folder': app.static_folder,
            'model_features': len(feature_columns) if feature_columns else 'Unknown',
            'scaler_features': len(scaler_columns) if scaler_columns else 'Unknown',
            'timestamp': datetime.now().isoformat()
        }
        return jsonify(status)
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

@app.route('/api/predict', methods=['POST'])
def predict():
    """Make loan approval prediction using real user data"""
    try:
        data = request.json
        print(f"📥 Received prediction request")
        
        # Validate required fields
        required_fields = [
            'person_age', 'person_income', 'loan_amnt', 
            'loan_int_rate', 'credit_score', 'loan_grade'
        ]
        
        missing_fields = [f for f in required_fields if f not in data]
        if missing_fields:
            return jsonify({
                'status': 'error',
                'message': f'Missing required fields: {", ".join(missing_fields)}'
            }), 400
        
        # Extract and validate data
        user_data = {
            'person_age': float(data.get('person_age', 0)),
            'person_income': float(data.get('person_income', 0)),
            'person_emp_exp': float(data.get('person_emp_exp', 0)),
            'loan_amnt': float(data.get('loan_amnt', 0)),
            'loan_int_rate': float(data.get('loan_int_rate', 0)),
            'loan_percent_income': float(data.get('loan_percent_income', 
                float(data.get('loan_amnt', 0)) / max(1, float(data.get('person_income', 1))))),
            'cb_person_cred_hist_length': float(data.get('cb_person_cred_hist_length', 0)),
            'credit_score': float(data.get('credit_score', 0)),
            'debt_to_income_ratio': float(data.get('debt_to_income_ratio', 0.3)),
            'person_gender': str(data.get('person_gender', 'male')).lower(),
            'person_home_ownership': str(data.get('person_home_ownership', 'RENT')).upper(),
            'loan_intent': str(data.get('loan_intent', 'PERSONAL')).upper(),
            'previous_loan_defaults_on_file': str(data.get('previous_loan_defaults_on_file', 'No')),
            'loan_grade': str(data.get('loan_grade', 'B')).upper()
        }
        
        print(f"📊 User data received: {user_data}")
        
        # Try with scaled features first (if scaler exists and matches model)
        if scaler is not None and scaler_columns is not None:
            print("🔧 Attempting prediction with scaled features...")
            result = predict_with_scaler(user_data)
            if result is not None:
                return jsonify(result)
        
        # If scaler fails or doesn't exist, try without scaling
        print("🔧 Attempting prediction without scaling...")
        result = predict_without_scaler(user_data)
        if result is not None:
            return jsonify(result)
        
        # If both methods fail, use fallback prediction
        print("🔧 Using fallback prediction...")
        result = fallback_prediction(user_data)
        return jsonify(result)
        
    except Exception as e:
        print(f"❌ Prediction error: {str(e)}")
        traceback.print_exc()
        return jsonify({
            'status': 'error',
            'message': f'Prediction failed: {str(e)}',
            'timestamp': datetime.now().isoformat()
        }), 500

def predict_with_scaler(user_data):
    """Try to predict with scaled features"""
    try:
        # Prepare features for scaler (8 features)
        scaled_features = prepare_scaled_features(user_data)
        if scaled_features is None:
            return None
        
        # Create DataFrame with correct feature order for scaler
        if scaler_columns:
            df_data = {}
            for col in scaler_columns:
                df_data[col] = scaled_features.get(col, 0)
            features_df = pd.DataFrame([df_data])[scaler_columns]
        else:
            features_df = pd.DataFrame([scaled_features])
        
        print(f"📊 Features for scaler: {len(features_df.columns)} columns")
        
        # Scale features
        try:
            features_scaled = scaler.transform(features_df)
            print(f"✅ Features scaled successfully")
        except Exception as e:
            print(f"⚠ Scaling error: {e}")
            return None
        
        # Prepare features for model (27 features)
        model_features = prepare_model_features(user_data)
        if model_features is None:
            return None
        
        # Create DataFrame for model
        if feature_columns:
            model_df_data = {}
            for col in feature_columns:
                model_df_data[col] = model_features.get(col, 0)
            model_features_df = pd.DataFrame([model_df_data])[feature_columns]
        else:
            model_features_df = pd.DataFrame([model_features])
        
        print(f"📊 Features for model: {len(model_features_df.columns)} columns")
        
        # Make prediction
        try:
            prediction = model.predict(model_features_df)[0]
            prediction_proba = model.predict_proba(model_features_df)[0]
            print(f"✅ Scaled prediction made: {prediction}, probabilities: {prediction_proba}")
        except Exception as e:
            print(f"❌ Scaled prediction error: {e}")
            return None
        
        # Prepare result
        return prepare_result(user_data, prediction, prediction_proba, "trained_scaled")
        
    except Exception as e:
        print(f"❌ Scaled prediction failed: {e}")
        return None

def predict_without_scaler(user_data):
    """Try to predict without scaling"""
    try:
        # Prepare features for model
        features = prepare_model_features(user_data)
        if features is None:
            return None
        
        # Create DataFrame for model
        if feature_columns:
            df_data = {}
            for col in feature_columns:
                df_data[col] = features.get(col, 0)
            features_df = pd.DataFrame([df_data])[feature_columns]
        else:
            features_df = pd.DataFrame([features])
        
        print(f"📊 Features for model (no scaling): {len(features_df.columns)} columns")
        
        # Make prediction without scaling
        try:
            prediction = model.predict(features_df)[0]
            prediction_proba = model.predict_proba(features_df)[0]
            print(f"✅ Unscaled prediction made: {prediction}, probabilities: {prediction_proba}")
        except Exception as e:
            print(f"❌ Unscaled prediction error: {e}")
            return None
        
        # Prepare result
        return prepare_result(user_data, prediction, prediction_proba, "trained_unscaled")
        
    except Exception as e:
        print(f"❌ Unscaled prediction failed: {e}")
        return None

def fallback_prediction(user_data):
    """Fallback prediction using rule-based logic"""
    try:
        # Simple rule-based prediction
        credit_score = user_data['credit_score']
        loan_to_income = user_data['loan_amnt'] / max(1, user_data['person_income'])
        debt_to_income = user_data.get('debt_to_income_ratio', 0.3)
        
        # Calculate probability based on rules
        probability = 0.5
        
        # Positive factors
        if credit_score >= 750:
            probability += 0.3
        elif credit_score >= 650:
            probability += 0.15
        elif credit_score >= 550:
            probability += 0.05
        
        # Negative factors
        if loan_to_income > 0.5:
            probability -= 0.3
        elif loan_to_income > 0.4:
            probability -= 0.15
        
        if debt_to_income > 0.5:
            probability -= 0.2
        elif debt_to_income > 0.4:
            probability -= 0.1
        
        if user_data['previous_loan_defaults_on_file'].upper() == 'YES':
            probability -= 0.25
        
        # Ensure probability is between 0 and 1
        probability = max(0.1, min(0.9, probability))
        
        # Make binary prediction
        prediction = 1 if probability > 0.5 else 0
        
        # Generate insights
        risk_factors = analyze_risk_factors(user_data, probability)
        recommendations = generate_recommendations(user_data, probability)
        
        result = {
            'status': 'success',
            'prediction': 'approved' if prediction == 1 else 'rejected',
            'probability': round(probability, 4),
            'confidence': round(min(max(abs(probability - 0.5) * 2, 0), 1), 4),
            'probabilities': {
                'approved': round(probability, 4),
                'rejected': round(1 - probability, 4)
            },
            'risk_factors': risk_factors,
            'recommendations': recommendations,
            'model_used': 'fallback_rules',
            'timestamp': datetime.now().isoformat(),
            'input_summary': {
                'credit_score': user_data['credit_score'],
                'income': user_data['person_income'],
                'loan_amount': user_data['loan_amnt'],
                'loan_to_income_ratio': round(loan_to_income, 3)
            },
            'note': 'Using fallback rule-based prediction due to model/scaler mismatch'
        }
        
        print(f"✅ Fallback prediction: {result['prediction']} ({probability:.2%})")
        return result
        
    except Exception as e:
        print(f"❌ Fallback prediction failed: {str(e)}")
        raise

def prepare_scaled_features(user_data):
    """Prepare 8 features for scaler"""
    try:
        features = {}
        
        # Based on typical scaler features (8 features)
        # These are the most important numerical features
        features['person_age'] = user_data['person_age']
        features['person_income'] = user_data['person_income']
        features['person_emp_exp'] = user_data['person_emp_exp']
        features['loan_amnt'] = user_data['loan_amnt']
        features['loan_int_rate'] = user_data['loan_int_rate']
        features['credit_score'] = user_data['credit_score']
        features['loan_percent_income'] = user_data['loan_percent_income']
        features['cb_person_cred_hist_length'] = user_data['cb_person_cred_hist_length']
        
        print(f"📊 Prepared {len(features)} features for scaler")
        return features
        
    except Exception as e:
        print(f"❌ Scaler feature preparation error: {str(e)}")
        return None

def prepare_model_features(user_data):
    """Prepare all 27 features for model"""
    try:
        features = {}
        
        # Add numerical features
        numerical_features = {
            'person_age': user_data['person_age'],
            'person_income': user_data['person_income'],
            'person_emp_exp': user_data['person_emp_exp'],
            'loan_amnt': user_data['loan_amnt'],
            'loan_int_rate': user_data['loan_int_rate'],
            'loan_percent_income': user_data['loan_percent_income'],
            'cb_person_cred_hist_length': user_data['cb_person_cred_hist_length'],
            'credit_score': user_data['credit_score'],
            'debt_to_income_ratio': user_data.get('debt_to_income_ratio', 0.3)
        }
        
        features.update(numerical_features)
        
        # One-hot encode categorical features
        # Gender
        gender = user_data['person_gender']
        features['person_gender_female'] = 1 if gender == 'female' else 0
        features['person_gender_male'] = 1 if gender == 'male' else 0
        
        # Home ownership
        home_ownership = user_data['person_home_ownership']
        features['person_home_ownership_MORTGAGE'] = 1 if home_ownership == 'MORTGAGE' else 0
        features['person_home_ownership_OWN'] = 1 if home_ownership == 'OWN' else 0
        features['person_home_ownership_RENT'] = 1 if home_ownership == 'RENT' else 0
        features['person_home_ownership_OTHER'] = 1 if home_ownership == 'OTHER' else 0
        
        # Loan intent
        loan_intent = user_data['loan_intent']
        features['loan_intent_DEBTCONSOLIDATION'] = 1 if loan_intent == 'DEBTCONSOLIDATION' else 0
        features['loan_intent_EDUCATION'] = 1 if loan_intent == 'EDUCATION' else 0
        features['loan_intent_HOMEIMPROVEMENT'] = 1 if loan_intent == 'HOMEIMPROVEMENT' else 0
        features['loan_intent_MEDICAL'] = 1 if loan_intent == 'MEDICAL' else 0
        features['loan_intent_PERSONAL'] = 1 if loan_intent == 'PERSONAL' else 0
        features['loan_intent_VENTURE'] = 1 if loan_intent == 'VENTURE' else 0
        
        # Previous defaults
        defaults = user_data['previous_loan_defaults_on_file']
        features['previous_loan_defaults_on_file_No'] = 1 if defaults.upper() == 'NO' else 0
        features['previous_loan_defaults_on_file_Yes'] = 1 if defaults.upper() == 'YES' else 0
        
        # Loan grade
        loan_grade = user_data['loan_grade']
        features['loan_grade_A'] = 1 if loan_grade == 'A' else 0
        features['loan_grade_B'] = 1 if loan_grade == 'B' else 0
        features['loan_grade_C'] = 1 if loan_grade == 'C' else 0
        features['loan_grade_D'] = 1 if loan_grade == 'D' else 0
        features['loan_grade_E'] = 1 if loan_grade == 'E' else 0
        features['loan_grade_F'] = 1 if loan_grade == 'F' else 0
        features['loan_grade_G'] = 1 if loan_grade == 'G' else 0
        
        # Add any missing features that the model expects
        if feature_columns:
            for col in feature_columns:
                if col not in features:
                    features[col] = 0
        
        print(f"📊 Prepared {len(features)} features for model")
        return features
        
    except Exception as e:
        print(f"❌ Model feature preparation error: {str(e)}")
        return None

def prepare_result(user_data, prediction, prediction_proba, model_type):
    """Prepare standardized result"""
    is_approved = bool(prediction == 1)
    approval_probability = float(prediction_proba[1])
    
    # Generate insights
    risk_factors = analyze_risk_factors(user_data, approval_probability)
    recommendations = generate_recommendations(user_data, approval_probability)
    
    result = {
        'status': 'success',
        'prediction': 'approved' if is_approved else 'rejected',
        'probability': round(approval_probability, 4),
        'confidence': round(min(max(abs(approval_probability - 0.5) * 2, 0), 1), 4),
        'probabilities': {
            'approved': round(approval_probability, 4),
            'rejected': round(float(prediction_proba[0]), 4)
        },
        'risk_factors': risk_factors,
        'recommendations': recommendations,
        'model_used': model_type,
        'timestamp': datetime.now().isoformat(),
        'input_summary': {
            'credit_score': user_data['credit_score'],
            'income': user_data['person_income'],
            'loan_amount': user_data['loan_amnt'],
            'loan_to_income_ratio': round(user_data['loan_amnt'] / max(1, user_data['person_income']), 3)
        }
    }
    
    print(f"✅ {model_type} prediction: {result['prediction']} ({approval_probability:.2%})")
    return result

def analyze_risk_factors(user_data, probability):
    """Analyze risk factors based on user data"""
    risk_factors = []
    
    try:
        credit_score = user_data['credit_score']
        if credit_score < 580:
            risk_factors.append(f"Very low credit score ({credit_score}) - high risk")
        elif credit_score < 650:
            risk_factors.append(f"Low credit score ({credit_score}) - moderate risk")
        
        loan_to_income = user_data['loan_amnt'] / max(1, user_data['person_income'])
        if loan_to_income > 0.5:
            risk_factors.append(f"Very high loan-to-income ratio ({loan_to_income:.2%})")
        elif loan_to_income > 0.4:
            risk_factors.append(f"High loan-to-income ratio ({loan_to_income:.2%})")
        
        if user_data['previous_loan_defaults_on_file'].upper() == 'YES':
            risk_factors.append("Previous loan defaults on record")
        
        interest_rate = user_data['loan_int_rate']
        if interest_rate > 20:
            risk_factors.append(f"Very high interest rate ({interest_rate}%)")
        elif interest_rate > 15:
            risk_factors.append(f"High interest rate ({interest_rate}%)")
        
        debt_to_income = user_data.get('debt_to_income_ratio', 0.3)
        if debt_to_income > 0.5:
            risk_factors.append(f"High debt-to-income ratio ({debt_to_income:.2%})")
        
        if probability < 0.3:
            risk_factors.append("Low approval probability")
        
    except Exception as e:
        risk_factors.append("Unable to analyze all risk factors")
    
    return risk_factors if risk_factors else ["No significant risk factors identified"]

def generate_recommendations(user_data, probability):
    """Generate recommendations based on user data"""
    recommendations = []
    
    try:
        credit_score = user_data['credit_score']
        if credit_score < 650:
            recommendations.append(f"Improve credit score from {credit_score} to at least 650")
        
        loan_to_income = user_data['loan_amnt'] / max(1, user_data['person_income'])
        if loan_to_income > 0.4:
            recommended_amount = 0.4 * user_data['person_income']
            recommendations.append(f"Consider reducing loan amount to ${recommended_amount:,.0f} or less")
        
        if user_data['previous_loan_defaults_on_file'].upper() == 'YES':
            recommendations.append("Avoid new loan applications until improving payment history")
        
        if probability < 0.5:
            recommendations.append("Consider improving financial profile before applying")
        elif probability > 0.8:
            recommendations.append("Strong application - good chances of approval")
        
        # Loan grade specific recommendations
        loan_grade = user_data['loan_grade']
        if loan_grade in ['D', 'E', 'F', 'G']:
            recommendations.append("Work on improving loan grade by reducing existing debt")
        
    except Exception as e:
        recommendations.append("Continue maintaining good financial habits")
    
    return recommendations if recommendations else ["Good financial profile - continue maintaining good financial habits"]

@app.route('/debug/models')
def debug_models():
    """Debug endpoint to check model status"""
    model_info = {
        'model_loaded': model is not None,
        'scaler_loaded': scaler is not None,
        'model_type': str(type(model)),
        'scaler_type': str(type(scaler)),
        'model_features': len(feature_columns) if feature_columns else 'Unknown',
        'scaler_features': len(scaler_columns) if scaler_columns else 'Unknown',
        'model_feature_columns': feature_columns if feature_columns else [],
        'scaler_feature_columns': scaler_columns if scaler_columns else [],
        'static_folder': app.static_folder,
        'html_file_exists': (Path(app.static_folder) / "loan_prediction.html").exists(),
        'timestamp': datetime.now().isoformat()
    }
    
    if hasattr(model, 'n_features_in_'):
        model_info['model_n_features_in'] = int(model.n_features_in_)
    
    if hasattr(scaler, 'n_features_in_'):
        model_info['scaler_n_features_in'] = int(scaler.n_features_in_)
    
    return jsonify(model_info)

@app.errorhandler(404)
def not_found(error):
    return jsonify({
        'status': 'error',
        'message': 'Endpoint not found',
        'available_endpoints': {
            'GET': ['/', '/health', '/debug/models', '/loan_prediction.html'],
            'POST': ['/api/predict']
        }
    }), 404

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5001))
    
    print(f"\n{'='*70}")
    print("🚀 Loan Approval Flask Service")
    print(f"{'='*70}")
    print(f"📊 Model Information:")
    print(f"   Model loaded: ✅ Yes")
    print(f"   Scaler loaded: {'✅ Yes' if scaler else '❌ No'}")
    if feature_columns:
        print(f"   Model expects: {len(feature_columns)} features")
    if scaler_columns:
        print(f"   Scaler expects: {len(scaler_columns)} features")
    print(f"\n📁 File Information:")
    print(f"   Static folder: {app.static_folder}")
    print(f"   HTML exists: {(Path(app.static_folder) / 'loan_prediction.html').exists()}")
    print(f"{'='*70}")
    print(f"🌐 Access URLs:")
    print(f"   Web Interface:     http://localhost:{port}/")
    print(f"   Web Interface:     http://localhost:{port}/loan_prediction.html")
    print(f"   Model Status:      http://localhost:{port}/debug/models")
    print(f"   Health Check:      http://localhost:{port}/health")
    print(f"{'='*70}")
    print("📋 Sample API Request:")
    print(f"curl -X POST http://localhost:{port}/api/predict \\")
    print('  -H "Content-Type: application/json" \\')
    print('  -d \'{"person_age": 35, "person_income": 75000, "person_emp_exp": 5,')
    print('       "loan_amnt": 20000, "loan_int_rate": 7.5, "credit_score": 720,')
    print('       "loan_grade": "B", "person_gender": "male",')
    print('       "person_home_ownership": "RENT", "loan_intent": "PERSONAL"}\'')
    print(f"{'='*70}\n")
    
    app.run(host='0.0.0.0', port=port, debug=True)