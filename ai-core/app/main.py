from fastapi import FastAPI, HTTPException, Body
import httpx


app = FastAPI()

JAVA_RSS_URL = "http://127.0.0.1:9090/news"
JAVA_SYSTEM_URL = "http://127.0.0.1:9090/system/execute"

@app.get('/')
async def root():
    return {"Message": "Hello"}

@app.get("/archnews")
async def archnews():
    async with httpx.AsyncClient(timeout=10) as client:
        try:
            r = await client.get(JAVA_RSS_URL)
            r.raise_for_status()
        except httpx.HTTPError as e:
            raise HTTPException(status_code=502, detail=str(e))

    return r.json()

@app.post("/systeminfo")
async def systeminfo(command: str = Body(..., media_type="text/plain")):
    async with httpx.AsyncClient(timeout=10) as client:
        try:
            r = await client.post(
                JAVA_SYSTEM_URL,
                content=command,
                headers={"Content-Type": "text/plain"},
            )
            r.raise_for_status()
        except httpx.HTTPError as e:
            raise HTTPException(status_code=502, detail=str(e))

    return r.json()