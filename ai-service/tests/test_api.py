"""
Test FastAPI AI service endpoints
"""

import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_health_check():
    """Test health endpoint"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"

def test_classify_transaction():
    """Test transaction classification endpoint"""
    test_data = {
        "description": "Mua cà phê Highland 45000",
        "amount": 45000,
        "user_id": 1
    }
    response = client.post("/classify", json=test_data)
    assert response.status_code == 200
    
    result = response.json()
    assert "category" in result
    assert "confidence" in result
    assert isinstance(result["confidence"], float)

def test_financial_advice():
    """Test financial advice endpoint"""
    test_data = {
        "query": "Tôi muốn tiết kiệm 10 triệu trong 6 tháng",
        "user_id": 1,
        "financial_context": {"monthly_income": 20000000}
    }
    response = client.post("/advice", json=test_data)
    assert response.status_code == 200
    
    result = response.json()
    assert "advice" in result
    assert "suggestions" in result
    assert isinstance(result["suggestions"], list)

def test_chat():
    """Test chat endpoint"""
    test_data = {
        "query": "Chi tiêu tháng này như thế nào?",
        "user_id": 1
    }
    response = client.post("/chat", json=test_data)
    assert response.status_code == 200
    
    result = response.json()
    assert "response" in result