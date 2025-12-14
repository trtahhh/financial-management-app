"""
Planning API - Test Version
Only for planning features
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import uvicorn
from datetime import datetime

app = FastAPI(title="Planning API", version="1.0.0")

class PlanningRequest(BaseModel):
    transactions: List[Dict[str, Any]]
    monthly_income: float
    goals: Optional[List[Dict[str, Any]]] = []

@app.get("/health")
async def health():
    return {"status": "ok", "service": "planning-api"}

@app.post("/planning/analyze")  
async def analyze_plan(request: PlanningRequest):
    # Mock response for now
    return {
        "monthly_income": request.monthly_income,
        "total_spending": sum(t.get('amount', 0) for t in request.transactions),
        "message": "Planning analysis working!",
        "timestamp": datetime.now().isoformat()
    }

if __name__ == "__main__":
    print("Starting Planning API on port 8002...")
    uvicorn.run(app, host="127.0.0.1", port=8002, reload=False)