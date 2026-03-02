from fastapi import FastAPI, HTTPException
import httpx
#import feedparser

app = FastAPI()

JAVA_RSS_URL = "http://localhost:9090/news"

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