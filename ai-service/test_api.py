"""
Minimal FastAPI test for Planning Service
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime

app = FastAPI(
    title="Planning Service Test",
    description="Test API for planning features",
    version="1.0.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    return {
        "message": "Planning Service Test API",
        "status": "running",
        "timestamp": datetime.now().isoformat()
    }

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "planning-test",
        "timestamp": datetime.now().isoformat()
    }

@app.get("/test")
async def test_endpoint():
    return {
        "message": "Test endpoint working",
        "features": ["Health Check", "Basic API"],
        "timestamp": datetime.now().isoformat()
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("test_api:app", host="0.0.0.0", port=8001, reload=True)