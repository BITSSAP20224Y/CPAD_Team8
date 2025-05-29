from flask import Flask, request, jsonify
from pymongo import MongoClient
import datetime
import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry
from flasgger import Swagger, swag_from

app = Flask(__name__)
swagger = Swagger(app)

# Retry-enabled HTTP session (for future use)
def get_retry_session():
    session = requests.Session()
    retry = Retry(
        total=3,
        backoff_factor=1,
        status_forcelist=[500, 502, 503, 504]
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("http://", adapter)
    return session

session = get_retry_session()

# Connect to MongoDB
client = MongoClient("mongodb://mongo:27017/")
db = client.online_learning
feedback_collection = db.feedbacks

@app.route('/feedback', methods=['POST'])
@swag_from({
    'tags': ['Feedback'],
    'summary': 'Submit feedback',
    'description': 'Allows a user to submit feedback for a course.',
    'parameters': [
        {
            'name': 'body',
            'in': 'body',
            'required': True,
            'schema': {
                'type': 'object',
                'properties': {
                    'username': {'type': 'string'},
                    'course_id': {'type': 'string'},
                    'rating': {'type': 'number'},
                    'comment': {'type': 'string'}
                },
                'required': ['username', 'course_id', 'rating', 'comment']
            }
        }
    ],
    'responses': {
        201: {'description': 'Feedback submitted successfully'},
        400: {'description': 'Missing required fields'}
    }
})
def submit_feedback():
    data = request.get_json()
    required_fields = ["username", "course_id", "rating", "comment"]
    if not all(field in data for field in required_fields):
        return jsonify({"message": "Missing required fields"}), 400

    feedback = {
        "username": data["username"],
        "course_id": data["course_id"],
        "rating": data["rating"],
        "comment": data["comment"],
        "submitted_at": str(datetime.datetime.now())
    }

    feedback_collection.insert_one(feedback)
    return jsonify({"message": "Feedback submitted successfully"}), 201

@app.route('/feedback/<course_id>', methods=['GET'])
@swag_from({
    'tags': ['Feedback'],
    'summary': 'Get feedback for a course',
    'description': 'Retrieves all feedback entries for a given course ID.',
    'parameters': [
        {
            'name': 'course_id',
            'in': 'path',
            'type': 'string',
            'required': True,
            'description': 'Course ID to retrieve feedback for'
        }
    ],
    'responses': {
        200: {
            'description': 'List of feedback entries',
            'schema': {
                'type': 'array',
                'items': {
                    'type': 'object',
                    'properties': {
                        'username': {'type': 'string'},
                        'course_id': {'type': 'string'},
                        'rating': {'type': 'number'},
                        'comment': {'type': 'string'},
                        'submitted_at': {'type': 'string'}
                    }
                }
            }
        }
    }
})
def get_feedback(course_id):
    feedbacks = list(feedback_collection.find({"course_id": course_id}, {"_id": 0}))
    return jsonify(feedbacks), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5006)
