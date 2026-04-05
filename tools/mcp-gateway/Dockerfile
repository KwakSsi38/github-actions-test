FROM python:3.11-slim

WORKDIR /app

ENV PIP_NO_CACHE_DIR=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY requirements.txt .
RUN pip install --upgrade pip setuptools wheel && \
    pip install -r requirements.txt

COPY server.py .
COPY gateway ./gateway

CMD ["python", "server.py"]
