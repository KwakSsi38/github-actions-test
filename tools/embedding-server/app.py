from typing import List
from fastapi import FastAPI
from pydantic import BaseModel
from FlagEmbedding import BGEM3FlagModel

app = FastAPI(title="query-embedding-server")

# CPU 개발용이면 use_fp16=False
model = BGEM3FlagModel(
    "BAAI/bge-m3",
    use_fp16=False
)

class EmbedRequest(BaseModel):
    texts: List[str]
    batch_size: int = 16
    max_length: int = 1200

class EmbedResponse(BaseModel):
    embeddings: List[List[float]]
    model: str
    dimension: int

@app.get("/health")
def health():
    return {
        "status": "ok",
        "model": "BAAI/bge-m3"
    }

@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    result = model.encode(
        req.texts,
        batch_size=req.batch_size,
        max_length=req.max_length,
        return_dense=True,
        return_sparse=False,
        return_colbert_vecs=False
    )

    dense_vecs = result["dense_vecs"].tolist()

    return {
        "embeddings": dense_vecs,
        "model": "BAAI/bge-m3",
        "dimension": len(dense_vecs[0]) if dense_vecs else 0
    }