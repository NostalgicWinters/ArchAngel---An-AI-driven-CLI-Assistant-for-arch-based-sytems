from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import requests
import os

app = FastAPI()

# ---------------- CONFIG ----------------
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
GROQ_MODEL = "gpt-oss-20b"   # ✅ FREE + FAST ENOUGH

HF_API_KEY = os.getenv("API_KEY")  # fixed env var name
HF_MODEL = "mistralai/Mistral-7B-Instruct-v0.2"

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")


# ---------------- MODELS ----------------
class LogRequest(BaseModel):
    logs: str


class GeneratedResponse(BaseModel):
    severity: str
    summary: str
    recommendedAction: str
    analysisValid: bool


# ---------------- PARSER ----------------
def parse_ai_response(text: str):
    try:
        severity = "UNKNOWN"
        summary = ""
        action = ""

        for line in text.split("\n"):
            l = line.lower()

            if "severity" in l:
                severity = line.split(":")[-1].strip().upper()
            elif "summary" in l:
                summary = line.split(":", 1)[-1].strip()
            elif "action" in l:
                action = line.split(":", 1)[-1].strip()

        return GeneratedResponse(
            severity=severity or "UNKNOWN",
            summary=summary or "Could not parse summary",
            recommendedAction=action or "No action parsed",
            analysisValid=True
        )

    except Exception:
        return None


# ---------------- GROQ (PRIMARY FAST AI) ----------------
def analyze_with_groq(logs: str):
    if not GROQ_API_KEY:
        return None

    try:
        res = requests.post(
            GROQ_URL,
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json"
            },
            json={
                "model": GROQ_MODEL,
                "messages": [{
                    "role": "user",
                    "content": f"""
Analyze these Linux logs.

Return:
Severity: <LOW/MEDIUM/HIGH/CRITICAL>
Summary: <short explanation>
Action: <what to do>

Logs:
{logs}
"""
                }],
                "temperature": 0.3,
                "max_tokens": 200
            },
            timeout=10
        )

        res.raise_for_status()
        content = res.json()["choices"][0]["message"]["content"]

        if content:
            return parse_ai_response(content)

    except Exception:
        pass

    return None


# ---------------- HUGGINGFACE (FREE FALLBACK) ----------------
def analyze_with_huggingface(logs: str):
    if not HF_API_KEY:
        return None

    try:
        res = requests.post(
            f"https://api-inference.huggingface.co/models/{HF_MODEL}",
            headers={
                "Authorization": f"Bearer {HF_API_KEY}"
            },
            json={
                "inputs": f"""
Analyze these Linux logs.

Return:
Severity: <LOW/MEDIUM/HIGH/CRITICAL>
Summary: <short explanation>
Action: <what to do>

Logs:
{logs}
"""
            },
            timeout=20
        )

        res.raise_for_status()
        data = res.json()

        if isinstance(data, list):
            content = data[0].get("generated_text", "")
        else:
            content = data.get("generated_text", "")

        if content:
            return parse_ai_response(content)

    except Exception:
        pass

    return None


# ---------------- OPENAI (OPTIONAL) ----------------
def analyze_with_openai(logs: str):
    if not OPENAI_API_KEY:
        return None

    try:
        res = requests.post(
            "https://api.openai.com/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {OPENAI_API_KEY}"
            },
            json={
                "model": "gpt-4o-mini",
                "messages": [{
                    "role": "user",
                    "content": f"""
Analyze these Linux logs.

Return:
Severity: <LOW/MEDIUM/HIGH/CRITICAL>
Summary: <short explanation>
Action: <what to do>

Logs:
{logs}
"""
                }]
            },
            timeout=15
        )

        res.raise_for_status()
        content = res.json()["choices"][0]["message"]["content"]

        if content:
            return parse_ai_response(content)

    except Exception:
        pass

    return None


# ---------------- MAIN API ----------------
@app.post("/analyze", response_model=GeneratedResponse)
def analyze(req: LogRequest):
    logs = req.logs.strip()

    if not logs:
        raise HTTPException(status_code=400, detail="Logs cannot be empty")

    # 1. Groq (PRIMARY)
    result = analyze_with_groq(logs)
    if result:
        return result

    # 2. HuggingFace
    result = analyze_with_huggingface(logs)
    if result:
        return result

    # 3. OpenAI
    result = analyze_with_openai(logs)
    if result:
        return result

    # 4. Total failure
    return GeneratedResponse(
        severity="UNKNOWN",
        summary="All AI providers failed (Groq + HF + OpenAI)",
        recommendedAction="Check GROQ_API_KEY / HF_API_KEY / OPENAI_API_KEY",
        analysisValid=False
    )


# ---------------- COMPAT ROUTE (DO NOT TOUCH JAVA) ----------------
@app.post("/analysis/logs", response_model=GeneratedResponse)
def analyze_logs_compat(req: LogRequest):
    return analyze(req)


# ---------------- HEALTH ----------------
@app.get("/health")
def health():
    return {"status": "brain alive"}