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
import warnings
warnings.filterwarnings('ignore')

# Get the Flask service path
flask_service_path = Path(__file__).parent
print(f"Flask service path: {flask_service_path}")

# Find static folder
static_path = flask_service_path / "static"
if not static_path.exists():
    print(f"📁 Creating static folder at: {static_path}")
    static_path.mkdir(parents=True, exist_ok=True)

# Check for HTML file
html_path = static_path / "credit_card_prediction.html"
if not html_path.exists():
    print(f"⚠ HTML file not found, will create it at runtime")

# Create models directory
models_path = flask_service_path / "models"
if not models_path.exists():
    print(f"📁 Creating models folder at: {models_path}")
    models_path.mkdir(parents=True, exist_ok=True)

app = Flask(__name__, 
            static_folder=str(static_path),
            static_url_path='')
CORS(app, resources={r"/*": {"origins": "*"}})

# Global variables for models
model = None
scaler = None
feature_columns = None
model_accuracy = 0.80  # Default 80% accuracy based on your information

def load_models():
    """Load the trained ML models and scaler"""
    global model, scaler, feature_columns
    
    try:
        print("\n" + "="*50)
        print("🔍 Looking for trained ML models...")
        
        # Look for model file
        model_files = [
            models_path / "credit_card_model.pkl",
            models_path / "credit_card_approval.pkl",
            models_path / "model.pkl",
            flask_service_path / "credit_card_model.pkl",
            flask_service_path / "credit_card_approval.pkl",
            Path.cwd() / "credit_card_model.pkl",
        ]
        
        model_path = None
        for path in model_files:
            if path.exists():
                model_path = path
                print(f"✅ Found model at: {path}")
                model = joblib.load(str(path))
                break
        
        # Look for scaler file
        scaler_files = [
            models_path / "credit_card_scaler.pkl",
            models_path / "scaler.pkl",
            flask_service_path / "credit_card_scaler.pkl",
            flask_service_path / "scaler.pkl",
            Path.cwd() / "credit_card_scaler.pkl",
        ]
        
        scaler_path = None
        for path in scaler_files:
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
                print(f"📊 Model expects {len(feature_columns)} features")
                for i, feat in enumerate(feature_columns[:10]):
                    print(f"  {i+1:2d}. {feat}")
                if len(feature_columns) > 10:
                    print(f"  ... and {len(feature_columns) - 10} more")
            elif hasattr(model, 'n_features_in_'):
                print(f"📊 Model expects {model.n_features_in_} features")
                feature_columns = [f"feature_{i}" for i in range(model.n_features_in_)]
            else:
                print("⚠ Model doesn't have feature information")
                feature_columns = None
            
            if scaler_path:
                print(f"✅ Scaler loaded successfully!")
                
                # Check scaler features
                if hasattr(scaler, 'feature_names_in_'):
                    scaler_features = list(scaler.feature_names_in_)
                    print(f"📊 Scaler expects {len(scaler_features)} features")
                elif hasattr(scaler, 'n_features_in_'):
                    print(f"📊 Scaler expects {scaler.n_features_in_} features")
                
                # Verify model and scaler compatibility
                if hasattr(model, 'n_features_in_') and hasattr(scaler, 'n_features_in_'):
                    if model.n_features_in_ == scaler.n_features_in_:
                        print("✅ Model and scaler are compatible!")
                    else:
                        print(f"⚠ Warning: Model expects {model.n_features_in_} features, but scaler expects {scaler.n_features_in_}")
            else:
                print("❌ Scaler not found! Scaling will be skipped")
            
            return True
        else:
            print("❌ Model not found. Please place your trained model (.pkl file) in:")
            print(f"   📁 {models_path}")
            print("   Common names: credit_card_model.pkl, credit_card_approval.pkl")
            print("\n💡 The app will use rule-based prediction until model is added")
            return False
            
    except Exception as e:
        print(f"❌ Error loading models: {str(e)}")
        traceback.print_exc()
        return False

# Load models on startup
print("\n" + "="*60)
print("🚀 Starting Credit Card Approval Flask Service")
print("="*60)

models_loaded = load_models()
if models_loaded:
    print("✅ Using trained ML models for predictions (80% accuracy)")
else:
    print("⚠ No trained models found, using rule-based prediction")
    print("💡 To use ML prediction, place your trained models in the models folder")

# HTML template for fallback
HTML_FALLBACK = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Credit Card Approval Prediction</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        body { background: #f8f9fa; padding: 20px; }
        .card { max-width: 1200px; margin: 0 auto; box-shadow: 0 0 20px rgba(0,0,0,0.1); }
        .form-section { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; border-left: 4px solid #ffc107; }
        .loading-spinner {
            border: 4px solid rgba(255, 193, 7, 0.2);
            border-top: 4px solid #ffc107;
            border-radius: 50%;
            width: 3rem;
            height: 3rem;
            animation: spin 1s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .result-approved { background: #d4edda; border: 1px solid #c3e6cb; }
        .result-rejected { background: #f8d7da; border: 1px solid #f5c6cb; }
    </style>
</head>
<body>
    <div class="card">
        <div class="card-header bg-warning text-white">
            <h3 class="mb-0"><i class="fas fa-credit-card"></i> Credit Card Approval Prediction</h3>
        </div>
        <div class="card-body">
            <div class="alert alert-info">
                <h6><i class="fas fa-info-circle"></i> Model Status</h6>
                <p>{{ model_status }}</p>
                <p>Model Accuracy: {{ model_accuracy }}%</p>
            </div>
            
            <form id="predictionForm">
                <div class="row">
                    <!-- Personal Info -->
                    <div class="col-md-4">
                        <div class="form-section">
                            <h5><i class="fas fa-user"></i> Personal Information</h5>
                            <div class="mb-3">
                                <label class="form-label">Gender</label>
                                <select class="form-control" name="Applicant_Gender" required>
                                    <option value="M">Male</option>
                                    <option value="F">Female</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Age</label>
                                <input type="number" class="form-control" name="Applicant_Age" value="35" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Total Children</label>
                                <input type="number" class="form-control" name="Total_Children" value="0" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Total Family Members</label>
                                <input type="number" class="form-control" name="Total_Family_Members" value="3" required>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Financial Info -->
                    <div class="col-md-4">
                        <div class="form-section">
                            <h5><i class="fas fa-money-bill-wave"></i> Financial Information</h5>
                            <div class="mb-3">
                                <label class="form-label">Total Income ($)</label>
                                <input type="number" class="form-control" name="Total_Income" value="50000" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Years of Working</label>
                                <input type="number" class="form-control" name="Years_of_Working" value="5" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Total Bad Debt ($)</label>
                                <input type="number" class="form-control" name="Total_Bad_Debt" value="0" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Total Good Debt ($)</label>
                                <input type="number" class="form-control" name="Total_Good_Debt" value="10000" required>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Asset & Profile Info -->
                    <div class="col-md-4">
                        <div class="form-section">
                            <h5><i class="fas fa-home"></i> Asset & Profile</h5>
                            <div class="mb-3">
                                <label class="form-label">Own Car?</label>
                                <select class="form-control" name="Owned_Car" required>
                                    <option value="Y">Yes</option>
                                    <option value="N">No</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Own Realty?</label>
                                <select class="form-control" name="Owned_Realty" required>
                                    <option value="Y">Yes</option>
                                    <option value="N">No</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Income Type</label>
                                <select class="form-control" name="Income_Type" required>
                                    <option value="Working">Working</option>
                                    <option value="Commercial associate">Commercial associate</option>
                                    <option value="State servant">State servant</option>
                                    <option value="Pensioner">Pensioner</option>
                                    <option value="Student">Student</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Education Type</label>
                                <select class="form-control" name="Education_Type" required>
                                    <option value="Higher education">Higher education</option>
                                    <option value="Academic degree">Academic degree</option>
                                    <option value="Secondary / secondary special">Secondary / secondary special</option>
                                    <option value="Incomplete higher">Incomplete higher</option>
                                    <option value="Lower secondary">Lower secondary</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="row">
                    <!-- Additional Info -->
                    <div class="col-md-6">
                        <div class="form-section">
                            <h5><i class="fas fa-users"></i> Family & Housing</h5>
                            <div class="mb-3">
                                <label class="form-label">Family Status</label>
                                <select class="form-control" name="Family_Status" required>
                                    <option value="Married">Married</option>
                                    <option value="Single / not married">Single / not married</option>
                                    <option value="Civil marriage">Civil marriage</option>
                                    <option value="Separated">Separated</option>
                                    <option value="Widow">Widow</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Housing Type</label>
                                <select class="form-control" name="Housing_Type" required>
                                    <option value="House / apartment">House / apartment</option>
                                    <option value="Rented apartment">Rented apartment</option>
                                    <option value="With parents">With parents</option>
                                    <option value="Municipal apartment">Municipal apartment</option>
                                    <option value="Office apartment">Office apartment</option>
                                    <option value="Co-op apartment">Co-op apartment</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Job Title</label>
                                <select class="form-control" name="Job_Title" required>
                                    <option value="Managers">Managers</option>
                                    <option value="Core staff">Core staff</option>
                                    <option value="Sales staff">Sales staff</option>
                                    <option value="IT staff">IT staff</option>
                                    <option value="Accountants">Accountants</option>
                                </select>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Contact Info -->
                    <div class="col-md-6">
                        <div class="form-section">
                            <h5><i class="fas fa-phone"></i> Contact Information</h5>
                            <div class="row">
                                <div class="col-6 mb-3">
                                    <label class="form-label">Own Mobile Phone?</label>
                                    <select class="form-control" name="Owned_Mobile_Phone" required>
                                        <option value="1">Yes</option>
                                        <option value="0">No</option>
                                    </select>
                                </div>
                                <div class="col-6 mb-3">
                                    <label class="form-label">Own Work Phone?</label>
                                    <select class="form-control" name="Owned_Work_Phone" required>
                                        <option value="1">Yes</option>
                                        <option value="0">No</option>
                                    </select>
                                </div>
                            </div>
                            <div class="row">
                                <div class="col-6 mb-3">
                                    <label class="form-label">Own Phone?</label>
                                    <select class="form-control" name="Owned_Phone" required>
                                        <option value="1">Yes</option>
                                        <option value="0">No</option>
                                    </select>
                                </div>
                                <div class="col-6 mb-3">
                                    <label class="form-label">Own Email?</label>
                                    <select class="form-control" name="Owned_Email" required>
                                        <option value="1">Yes</option>
                                        <option value="0">No</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="text-center mt-4">
                    <button type="submit" class="btn btn-warning btn-lg">
                        <i class="fas fa-bolt"></i> Predict Approval
                    </button>
                </div>
            </form>
            
            <div id="loading" class="text-center" style="display: none;">
                <div class="loading-spinner"></div>
                <p>Processing your data with ML model...</p>
            </div>
            
            <div id="result" class="mt-4"></div>
        </div>
    </div>
    
    <script>
        document.getElementById('predictionForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const form = e.target;
            const button = form.querySelector('button');
            const originalText = button.innerHTML;
            const loadingDiv = document.getElementById('loading');
            const resultDiv = document.getElementById('result');
            
            // Show loading
            button.disabled = true;
            button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
            loadingDiv.style.display = 'block';
            resultDiv.innerHTML = '';
            
            const formData = new FormData(form);
            const data = Object.fromEntries(formData.entries());
            
            // Convert numeric fields
            data.Applicant_Age = parseInt(data.Applicant_Age);
            data.Total_Children = parseInt(data.Total_Children);
            data.Total_Family_Members = parseInt(data.Total_Family_Members);
            data.Total_Income = parseFloat(data.Total_Income);
            data.Years_of_Working = parseInt(data.Years_of_Working);
            data.Total_Bad_Debt = parseFloat(data.Total_Bad_Debt);
            data.Total_Good_Debt = parseFloat(data.Total_Good_Debt);
            data.Owned_Mobile_Phone = parseInt(data.Owned_Mobile_Phone);
            data.Owned_Work_Phone = parseInt(data.Owned_Work_Phone);
            data.Owned_Phone = parseInt(data.Owned_Phone);
            data.Owned_Email = parseInt(data.Owned_Email);
            
            try {
                const response = await fetch('/api/predict', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(data)
                });
                
                const result = await response.json();
                
                // Hide loading
                loadingDiv.style.display = 'none';
                button.disabled = false;
                button.innerHTML = originalText;
                
                if (result.status === 'success') {
                    const isApproved = result.prediction === 'approved';
                    const probPercent = (result.probability * 100).toFixed(1);
                    const confidencePercent = (result.confidence * 100).toFixed(1);
                    
                    let resultHtml = `
                        <div class="${isApproved ? 'result-approved' : 'result-rejected'} p-4 rounded">
                            <div class="row align-items-center">
                                <div class="col-md-8">
                                    <h4 class="${isApproved ? 'text-success' : 'text-danger'}">
                                        <i class="fas fa-${isApproved ? 'check-circle' : 'times-circle'}"></i>
                                        Credit Card ${result.prediction.toUpperCase()}
                                    </h4>
                                    <p class="mb-1"><strong>Probability:</strong> ${probPercent}%</p>
                                    <p class="mb-1"><strong>Confidence:</strong> ${confidencePercent}%</p>
                                    <p class="mb-1"><strong>Method:</strong> ${result.model_used}</p>
                                    <p class="mb-1"><strong>Estimated Limit:</strong> $${result.estimated_limit.toLocaleString()}</p>
                                </div>
                                <div class="col-md-4 text-end">
                                    <div class="display-4 ${isApproved ? 'text-success' : 'text-danger'}">
                                        ${probPercent}%
                                    </div>
                                    <small>Approval Chance</small>
                                </div>
                            </div>
                            
                            ${result.risk_factors.length > 0 ? `
                            <div class="mt-3">
                                <h6><i class="fas fa-exclamation-triangle"></i> Risk Factors</h6>
                                <ul class="mb-0">
                                    ${result.risk_factors.map(factor => `<li>${factor}</li>`).join('')}
                                </ul>
                            </div>
                            ` : ''}
                            
                            ${result.recommendations.length > 0 ? `
                            <div class="mt-3">
                                <h6><i class="fas fa-lightbulb"></i> Recommendations</h6>
                                <ul class="mb-0">
                                    ${result.recommendations.map(rec => `<li>${rec}</li>`).join('')}
                                </ul>
                            </div>
                            ` : ''}
                            
                            <div class="mt-3 pt-3 border-top">
                                <small class="text-muted">
                                    <i class="fas fa-info-circle"></i> 
                                    Model Accuracy: ${(result.model_accuracy * 100).toFixed(1)}% | 
                                    Processed at: ${new Date(result.timestamp).toLocaleTimeString()}
                                </small>
                            </div>
                        </div>
                    `;
                    
                    resultDiv.innerHTML = resultHtml;
                } else {
                    resultDiv.innerHTML = `
                        <div class="alert alert-danger">
                            <h5><i class="fas fa-exclamation-circle"></i> Error</h5>
                            <p>${result.message}</p>
                        </div>
                    `;
                }
                
            } catch (error) {
                // Hide loading
                loadingDiv.style.display = 'none';
                button.disabled = false;
                button.innerHTML = originalText;
                
                resultDiv.innerHTML = `
                    <div class="alert alert-danger">
                        <h5><i class="fas fa-exclamation-circle"></i> Connection Error</h5>
                        <p>${error.message}</p>
                    </div>
                `;
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
        html_file = Path(app.static_folder) / "credit_card_prediction.html"
        if html_file.exists():
            print(f"✅ Serving HTML from: {html_file}")
            return send_from_directory(app.static_folder, 'credit_card_prediction.html')
        else:
            print(f"⚠ HTML file not found, using fallback")
            # Show fallback
            model_status = "✅ Using trained ML model with scaler" if model and scaler else "⚠ Using rule-based prediction"
            return render_template_string(HTML_FALLBACK, 
                                         model_status=model_status,
                                         model_accuracy=model_accuracy*100)
    except Exception as e:
        print(f"⚠ Error serving HTML: {e}")
        model_status = "✅ Using trained ML model with scaler" if model and scaler else "⚠ Using rule-based prediction"
        return render_template_string(HTML_FALLBACK, 
                                     model_status=model_status,
                                     model_accuracy=model_accuracy*100)

@app.route('/health')
def health_check():
    """Health check endpoint"""
    try:
        html_exists = (Path(app.static_folder) / "credit_card_prediction.html").exists()
        status = {
            'status': 'healthy',
            'model_loaded': model is not None,
            'scaler_loaded': scaler is not None,
            'model_accuracy': model_accuracy,
            'html_file_exists': html_exists,
            'features_expected': len(feature_columns) if feature_columns else 'Unknown',
            'timestamp': datetime.now().isoformat()
        }
        return jsonify(status)
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

@app.route('/api/predict', methods=['POST'])
def predict():
    """Make credit card approval prediction using ML model with scaler"""
    try:
        data = request.json
        print(f"📥 Received prediction request")
        
        # Validate required fields
        required_fields = [
            'Applicant_Gender', 'Owned_Car', 'Owned_Realty',
            'Total_Children', 'Total_Income', 'Income_Type',
            'Education_Type', 'Family_Status', 'Housing_Type',
            'Owned_Mobile_Phone', 'Owned_Work_Phone', 'Owned_Phone',
            'Owned_Email', 'Job_Title', 'Total_Family_Members',
            'Applicant_Age', 'Years_of_Working', 'Total_Bad_Debt',
            'Total_Good_Debt'
        ]
        
        missing_fields = [f for f in required_fields if f not in data]
        if missing_fields:
            return jsonify({
                'status': 'error',
                'message': f'Missing required fields: {", ".join(missing_fields)}'
            }), 400
        
        # Extract and validate data
        user_data = {
            'Applicant_Gender': str(data.get('Applicant_Gender', 'M')).upper(),
            'Owned_Car': str(data.get('Owned_Car', 'N')).upper(),
            'Owned_Realty': str(data.get('Owned_Realty', 'N')).upper(),
            'Total_Children': int(data.get('Total_Children', 0)),
            'Total_Income': float(data.get('Total_Income', 0)),
            'Income_Type': str(data.get('Income_Type', 'Working')),
            'Education_Type': str(data.get('Education_Type', 'Higher education')),
            'Family_Status': str(data.get('Family_Status', 'Married')),
            'Housing_Type': str(data.get('Housing_Type', 'House / apartment')),
            'Owned_Mobile_Phone': int(data.get('Owned_Mobile_Phone', 1)),
            'Owned_Work_Phone': int(data.get('Owned_Work_Phone', 0)),
            'Owned_Phone': int(data.get('Owned_Phone', 1)),
            'Owned_Email': int(data.get('Owned_Email', 1)),
            'Job_Title': str(data.get('Job_Title', 'Laborers')),
            'Total_Family_Members': int(data.get('Total_Family_Members', 1)),
            'Applicant_Age': int(data.get('Applicant_Age', 30)),
            'Years_of_Working': int(data.get('Years_of_Working', 5)),
            'Total_Bad_Debt': float(data.get('Total_Bad_Debt', 0)),
            'Total_Good_Debt': float(data.get('Total_Good_Debt', 0))
        }
        
        print(f"📊 User data received")
        
        # Try with ML model if available
        if model is not None:
            print("🔧 Attempting prediction with ML model and scaler...")
            result = predict_with_ml_model(user_data)
            if result is not None:
                return jsonify(result)
        
        # Fallback to rule-based prediction
        print("🔧 Using rule-based prediction as fallback...")
        result = rule_based_prediction(user_data)
        return jsonify(result)
        
    except Exception as e:
        print(f"❌ Prediction error: {str(e)}")
        traceback.print_exc()
        return jsonify({
            'status': 'error',
            'message': f'Prediction failed: {str(e)}',
            'timestamp': datetime.now().isoformat()
        }), 500

def predict_with_ml_model(user_data):
    """Predict using ML model with scaler preprocessing"""
    try:
        # Prepare features for the model
        features = prepare_features_for_model(user_data)
        if features is None:
            return None
        
        print(f"📊 Features prepared: {len(features)} features")
        
        # Create DataFrame with correct feature order
        features_df = pd.DataFrame([features])
        
        # Ensure we have all expected features
        if feature_columns:
            # Add missing columns with default values
            for col in feature_columns:
                if col not in features_df.columns:
                    features_df[col] = 0
            
            # Reorder columns to match model expectation
            features_df = features_df[feature_columns]
        
        print(f"📊 DataFrame shape: {features_df.shape}")
        
        # Scale features if scaler exists
        if scaler is not None:
            try:
                print("🔧 Scaling features...")
                features_scaled = scaler.transform(features_df)
                features_df = pd.DataFrame(features_scaled, columns=features_df.columns)
                print("✅ Features scaled successfully")
            except Exception as e:
                print(f"⚠ Scaling error: {e}")
                print("⚠ Using unscaled features for prediction")
        
        # Make prediction
        try:
            print("🔧 Making ML prediction...")
            
            # Check if model has predict_proba method
            if hasattr(model, 'predict_proba'):
                prediction = model.predict(features_df)[0]
                prediction_proba = model.predict_proba(features_df)[0]
                print(f"✅ Prediction: {prediction}, Probabilities: {prediction_proba}")
            else:
                # For models without probability estimates
                prediction = model.predict(features_df)[0]
                # Create artificial probabilities
                if prediction == 1:
                    prediction_proba = [0.2, 0.8]  # 80% confidence for approval
                else:
                    prediction_proba = [0.8, 0.2]  # 80% confidence for rejection
                print(f"✅ Prediction: {prediction} (no probability estimates available)")
            
        except Exception as e:
            print(f"❌ ML prediction error: {e}")
            traceback.print_exc()
            return None
        
        # Prepare result
        return prepare_result(user_data, prediction, prediction_proba, "ml_model_with_scaler")
        
    except Exception as e:
        print(f"❌ ML prediction failed: {e}")
        traceback.print_exc()
        return None

def prepare_features_for_model(user_data):
    """Prepare features for ML model (one-hot encoding for categorical variables)"""
    try:
        features = {}
        
        # 1. Binary features (already numeric)
        features['Applicant_Gender'] = 1 if user_data['Applicant_Gender'] == 'M' else 0
        features['Owned_Car'] = 1 if user_data['Owned_Car'] == 'Y' else 0
        features['Owned_Realty'] = 1 if user_data['Owned_Realty'] == 'Y' else 0
        features['Owned_Mobile_Phone'] = user_data['Owned_Mobile_Phone']
        features['Owned_Work_Phone'] = user_data['Owned_Work_Phone']
        features['Owned_Phone'] = user_data['Owned_Phone']
        features['Owned_Email'] = user_data['Owned_Email']
        
        # 2. Numerical features
        features['Total_Children'] = user_data['Total_Children']
        features['Total_Income'] = user_data['Total_Income']
        features['Total_Family_Members'] = user_data['Total_Family_Members']
        features['Applicant_Age'] = user_data['Applicant_Age']
        features['Years_of_Working'] = user_data['Years_of_Working']
        features['Total_Bad_Debt'] = user_data['Total_Bad_Debt']
        features['Total_Good_Debt'] = user_data['Total_Good_Debt']
        
        # 3. One-hot encode categorical features
        # Income Type
        income_types = ['Working', 'Commercial associate', 'Pensioner', 'State servant', 'Student']
        for it in income_types:
            features[f'Income_Type_{it}'] = 1 if user_data['Income_Type'] == it else 0
        
        # Education Type
        education_types = ['Higher education', 'Secondary / secondary special', 
                          'Incomplete higher', 'Lower secondary', 'Academic degree']
        for et in education_types:
            features[f'Education_Type_{et}'] = 1 if user_data['Education_Type'] == et else 0
        
        # Family Status
        family_statuses = ['Married', 'Single / not married', 'Civil marriage', 'Separated', 'Widow']
        for fs in family_statuses:
            features[f'Family_Status_{fs}'] = 1 if user_data['Family_Status'] == fs else 0
        
        # Housing Type
        housing_types = ['House / apartment', 'With parents', 'Municipal apartment',
                        'Rented apartment', 'Office apartment', 'Co-op apartment']
        for ht in housing_types:
            features[f'Housing_Type_{ht}'] = 1 if user_data['Housing_Type'] == ht else 0
        
        # Job Title (simplified to common categories)
        job_categories = {
            'Managers': ['Managers'],
            'Professional': ['Core staff', 'IT staff', 'Accountants', 'Medicine staff', 
                           'High skill tech staff', 'HR staff', 'Realty agents'],
            'Service': ['Sales staff', 'Drivers', 'Cooking staff', 'Security staff',
                       'Cleaning staff', 'Private service staff', 'Waiters/barmen staff'],
            'Labor': ['Laborers', 'Low-skill Laborers'],
            'Support': ['Secretaries']
        }
        
        # One-hot encode job categories
        for category, jobs in job_categories.items():
            features[f'Job_Category_{category}'] = 1 if user_data['Job_Title'] in jobs else 0
        
        # Also keep original job title as a feature (encoded as integer)
        job_title_mapping = {
            'Laborers': 0, 'Core staff': 1, 'Sales staff': 2, 'Managers': 3, 'Drivers': 4,
            'High skill tech staff': 5, 'Accountants': 6, 'Medicine staff': 7, 'Cooking staff': 8,
            'Security staff': 9, 'Cleaning staff': 10, 'Private service staff': 11,
            'Low-skill Laborers': 12, 'Waiters/barmen staff': 13, 'Secretaries': 14,
            'HR staff': 15, 'Realty agents': 16, 'IT staff': 17
        }
        features['Job_Title_Encoded'] = job_title_mapping.get(user_data['Job_Title'], 0)
        
        print(f"✅ Prepared {len(features)} features for ML model")
        return features
        
    except Exception as e:
        print(f"❌ Feature preparation error: {str(e)}")
        traceback.print_exc()
        return None

def rule_based_prediction(user_data):
    """Rule-based prediction as fallback"""
    try:
        # Calculate financial metrics
        total_debt = user_data['Total_Bad_Debt'] + user_data['Total_Good_Debt']
        debt_to_income = total_debt / max(1, user_data['Total_Income'])
        
        # Calculate probability based on rules
        probability = 0.5
        
        # Positive factors
        if user_data['Total_Income'] >= 50000:
            probability += 0.2
        elif user_data['Total_Income'] >= 30000:
            probability += 0.1
        
        if user_data['Years_of_Working'] >= 5:
            probability += 0.15
        elif user_data['Years_of_Working'] >= 2:
            probability += 0.05
        
        if user_data['Applicant_Age'] >= 25 and user_data['Applicant_Age'] <= 55:
            probability += 0.1
        
        if user_data['Owned_Realty'] == 'Y':
            probability += 0.1
        
        if user_data['Education_Type'] in ['Higher education', 'Academic degree']:
            probability += 0.05
        
        # Negative factors
        if debt_to_income > 0.5:
            probability -= 0.3
        elif debt_to_income > 0.3:
            probability -= 0.15
        
        if user_data['Total_Bad_Debt'] > 0:
            probability -= 0.2
        
        if user_data['Total_Children'] > 2:
            probability -= 0.1
        
        if user_data['Job_Title'] in ['Laborers', 'Low-skill Laborers', 'Cleaning staff']:
            probability -= 0.05
        
        # Ensure probability is between 0 and 1
        probability = max(0.1, min(0.9, probability))
        
        # Make binary prediction
        prediction = 1 if probability > 0.5 else 0
        
        # Generate insights
        risk_factors = analyze_risk_factors(user_data, probability)
        recommendations = generate_recommendations(user_data, probability)
        
        # Calculate estimated credit limit
        estimated_limit = calculate_credit_limit(user_data, probability)
        
        result = {
            'status': 'success',
            'prediction': 'approved' if prediction == 1 else 'rejected',
            'probability': round(probability, 4),
            'confidence': round(min(max(abs(probability - 0.5) * 2, 0), 1), 4),
            'probabilities': {
                'approved': round(probability, 4),
                'rejected': round(1 - probability, 4)
            },
            'estimated_limit': estimated_limit,
            'credit_score_impact': 'Low' if probability > 0.7 else 'Medium' if probability > 0.5 else 'High',
            'risk_factors': risk_factors,
            'recommendations': recommendations,
            'model_used': 'rule_based_fallback',
            'model_accuracy': 0.65,  # Rule-based accuracy estimate
            'timestamp': datetime.now().isoformat(),
            'input_summary': {
                'income': user_data['Total_Income'],
                'total_debt': total_debt,
                'debt_to_income_ratio': round(debt_to_income, 3),
                'years_working': user_data['Years_of_Working'],
                'age': user_data['Applicant_Age'],
                'family_size': user_data['Total_Family_Members']
            }
        }
        
        print(f"✅ Rule-based prediction: {result['prediction']} ({probability:.2%})")
        return result
        
    except Exception as e:
        print(f"❌ Rule-based prediction failed: {str(e)}")
        raise

def prepare_result(user_data, prediction, prediction_proba, model_type):
    """Prepare standardized result"""
    is_approved = bool(prediction == 1)
    approval_probability = float(prediction_proba[1])
    
    # Calculate financial metrics
    total_debt = user_data['Total_Bad_Debt'] + user_data['Total_Good_Debt']
    debt_to_income = total_debt / max(1, user_data['Total_Income'])
    
    # Generate insights
    risk_factors = analyze_risk_factors(user_data, approval_probability)
    recommendations = generate_recommendations(user_data, approval_probability)
    
    # Calculate estimated credit limit
    estimated_limit = calculate_credit_limit(user_data, approval_probability)
    
    result = {
        'status': 'success',
        'prediction': 'approved' if is_approved else 'rejected',
        'probability': round(approval_probability, 4),
        'confidence': round(min(max(abs(approval_probability - 0.5) * 2, 0), 1), 4),
        'probabilities': {
            'approved': round(approval_probability, 4),
            'rejected': round(float(prediction_proba[0]), 4)
        },
        'estimated_limit': estimated_limit,
        'credit_score_impact': 'Low' if approval_probability > 0.7 else 'Medium' if approval_probability > 0.5 else 'High',
        'risk_factors': risk_factors,
        'recommendations': recommendations,
        'model_used': model_type,
        'model_accuracy': model_accuracy,
        'timestamp': datetime.now().isoformat(),
        'input_summary': {
            'income': user_data['Total_Income'],
            'total_debt': total_debt,
            'debt_to_income_ratio': round(debt_to_income, 3),
            'years_working': user_data['Years_of_Working'],
            'age': user_data['Applicant_Age'],
            'family_size': user_data['Total_Family_Members']
        }
    }
    
    print(f"✅ {model_type} prediction: {result['prediction']} ({approval_probability:.2%})")
    return result

def calculate_credit_limit(user_data, probability):
    """Calculate estimated credit limit"""
    try:
        # Base limit as percentage of income
        base_limit = user_data['Total_Income'] * 0.3
        
        # Adjust based on probability
        if probability > 0.8:
            multiplier = 1.5
        elif probability > 0.6:
            multiplier = 1.2
        elif probability > 0.4:
            multiplier = 1.0
        else:
            multiplier = 0.5
        
        # Adjust based on work experience
        if user_data['Years_of_Working'] > 10:
            multiplier *= 1.2
        elif user_data['Years_of_Working'] > 5:
            multiplier *= 1.1
        
        # Adjust based on assets
        if user_data['Owned_Realty'] == 'Y':
            multiplier *= 1.3
        if user_data['Owned_Car'] == 'Y':
            multiplier *= 1.1
        
        estimated_limit = base_limit * multiplier
        
        # Round to nearest $500 and set bounds
        estimated_limit = round(estimated_limit / 500) * 500
        estimated_limit = max(500, min(estimated_limit, 50000))
        
        return int(estimated_limit)
        
    except Exception as e:
        print(f"⚠ Credit limit calculation error: {e}")
        return 5000

def analyze_risk_factors(user_data, probability):
    """Analyze risk factors"""
    risk_factors = []
    
    try:
        total_debt = user_data['Total_Bad_Debt'] + user_data['Total_Good_Debt']
        debt_to_income = total_debt / max(1, user_data['Total_Income'])
        
        if debt_to_income > 0.5:
            risk_factors.append(f"Very high debt-to-income ratio ({debt_to_income:.1%})")
        elif debt_to_income > 0.3:
            risk_factors.append(f"High debt-to-income ratio ({debt_to_income:.1%})")
        
        if user_data['Total_Bad_Debt'] > 0:
            risk_factors.append("Existing bad debt on record")
        
        if user_data['Total_Income'] < 20000:
            risk_factors.append(f"Low income (${user_data['Total_Income']:,.0f})")
        
        if user_data['Years_of_Working'] < 1:
            risk_factors.append("Limited work experience")
        
        if user_data['Applicant_Age'] < 21:
            risk_factors.append(f"Young age ({user_data['Applicant_Age']} years)")
        
        if probability < 0.3:
            risk_factors.append("Low approval probability")
        
    except Exception as e:
        risk_factors.append("Unable to analyze all risk factors")
    
    return risk_factors if risk_factors else ["No significant risk factors identified"]

def generate_recommendations(user_data, probability):
    """Generate recommendations"""
    recommendations = []
    
    try:
        total_debt = user_data['Total_Bad_Debt'] + user_data['Total_Good_Debt']
        debt_to_income = total_debt / max(1, user_data['Total_Income'])
        
        if debt_to_income > 0.3:
            recommendations.append("Reduce existing debt before applying for new credit")
        
        if user_data['Total_Income'] < 30000:
            recommendations.append("Consider increasing income or applying for secured credit card")
        
        if user_data['Years_of_Working'] < 2:
            recommendations.append("Build longer work history before applying")
        
        if probability < 0.5:
            recommendations.append("Improve financial profile before applying")
        elif probability > 0.8:
            recommendations.append("Strong application - good chances of approval with competitive terms")
        
        recommendations.append("Maintain low credit utilization (<30%) for better scores")
        recommendations.append("Make timely payments to build positive credit history")
        
    except Exception as e:
        recommendations.append("Continue maintaining good financial habits")
    
    return recommendations if recommendations else ["Good financial profile - continue maintaining good financial habits"]

@app.route('/debug/models')
def debug_models():
    """Debug endpoint to check model status"""
    model_info = {
        'model_loaded': model is not None,
        'scaler_loaded': scaler is not None,
        'model_type': str(type(model)) if model else 'None',
        'scaler_type': str(type(scaler)) if scaler else 'None',
        'model_accuracy': model_accuracy,
        'num_features': len(feature_columns) if feature_columns else 'Unknown',
        'feature_columns': feature_columns if feature_columns else [],
        'model_features_in': int(model.n_features_in_) if model and hasattr(model, 'n_features_in_') else 'Unknown',
        'scaler_features_in': int(scaler.n_features_in_) if scaler and hasattr(scaler, 'n_features_in_') else 'Unknown',
        'timestamp': datetime.now().isoformat()
    }
    
    return jsonify(model_info)

@app.errorhandler(404)
def not_found(error):
    return jsonify({
        'status': 'error',
        'message': 'Endpoint not found',
        'available_endpoints': {
            'GET': ['/', '/health', '/debug/models', '/credit_card_prediction.html'],
            'POST': ['/api/predict']
        }
    }), 404

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5002))
    
    print(f"\n{'='*70}")
    print("🚀 Credit Card Approval Flask Service")
    print(f"{'='*70}")
    print(f"📊 Model Information:")
    print(f"   Model loaded: {'✅ Yes' if model else '❌ No'}")
    print(f"   Scaler loaded: {'✅ Yes' if scaler else '❌ No'}")
    print(f"   Model Accuracy: {model_accuracy*100:.1f}%")
    if feature_columns:
        print(f"   Features expected: {len(feature_columns)}")
    print(f"{'='*70}")
    print(f"🌐 Access URLs:")
    print(f"   Web Interface:     http://localhost:{port}/")
    print(f"   Health Check:      http://localhost:{port}/health")
    print(f"   Model Status:      http://localhost:{port}/debug/models")
    print(f"{'='*70}")
    print("📋 Sample API Request:")
    print(f"curl -X POST http://localhost:{port}/api/predict \\")
    print('  -H "Content-Type: application/json" \\')
    print('  -d \'{"Applicant_Gender": "M", "Owned_Car": "Y", "Owned_Realty": "Y",')
    print('       "Total_Children": 0, "Total_Income": 50000, "Income_Type": "Working",')
    print('       "Education_Type": "Higher education", "Family_Status": "Married",')
    print('       "Housing_Type": "House / apartment", "Owned_Mobile_Phone": 1,')
    print('       "Owned_Work_Phone": 0, "Owned_Phone": 1, "Owned_Email": 1,')
    print('       "Job_Title": "Managers", "Total_Family_Members": 3,')
    print('       "Applicant_Age": 35, "Years_of_Working": 10, "Total_Bad_Debt": 0,')
    print('       "Total_Good_Debt": 10000}\'')
    print(f"{'='*70}\n")
    
    app.run(host='0.0.0.0', port=port, debug=True)