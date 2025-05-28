FROM python:3.10
WORKDIR /app
COPY app.py .
RUN pip install flask pymongo flasgger PyJWT flask-restx python-dotenv Werkzeug flask_limiter
CMD ["python", "app.py"]