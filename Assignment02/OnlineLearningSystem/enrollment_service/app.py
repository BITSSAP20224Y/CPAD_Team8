from flask import Flask, request, jsonify
from pymongo import MongoClient
import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry
from flasgger import Swagger, swag_from

app = Flask(__name__)

# Swagger configuration
swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": 'apispec_1',
            "route": '/apispec_1.json',
            "rule_filter": lambda rule: True,
            "model_filter": lambda tag: True,
        }
    ],
    "static_url_path": "/flasgger_static",
    "swagger_ui": True,
    "specs_route": "/apidocs/",
    "openapi": "3.0.2"
}

swagger = Swagger(app, config=swagger_config)

# Retry session setup
def get_retry_session():
    session = requests.Session()
    retry = Retry(
        total=3,
        backoff_factor=1,
        status_forcelist=[500, 502, 503, 504]
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount('http://', adapter)
    return session

session = get_retry_session()

# MongoDB setup
client = MongoClient("mongodb://mongo:27017/")
db = client.online_learning
enrollments_collection = db.enrollments

@app.route('/enroll', methods=['POST'])
@swag_from({
    'tags': ['Enrollment'],
    'summary': 'Enroll a user in a course',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'example': {
                    'username': 'john_doe',
                    'course_id': 'course123'
                }
            }
        }
    },
    'responses': {
        200: {
            'description': 'User enrolled successfully',
            'content': {
                'application/json': {
                    'example': {'message': 'Enrolled successfully'}
                }
            }
        },
        404: {
            'description': 'User not found'
        }
    }
})
def enroll():
    data = request.json
    username = data.get('username')
    course_id = data.get('course_id')

    user_response = session.get(f'http://user-service:5001/get_user/{username}')

    if user_response.status_code != 200:
        return jsonify({"message": "User does not exist"}), 404

    enrollment = {"username": username, "course_id": course_id}
    enrollments_collection.insert_one(enrollment)
    return jsonify({"message": "Enrolled successfully"})

@app.route('/my-courses/<username>', methods=['GET'])
@swag_from({
    'tags': ['Enrollment'],
    'summary': 'Get all courses a user is enrolled in',
    'parameters': [
        {
            'name': 'username',
            'in': 'path',
            'required': True,
            'schema': {'type': 'string'}
        }
    ],
    'responses': {
        200: {
            'description': 'List of enrolled courses with course titles',
            'content': {
                'application/json': {
                    'example': [
                        {
                            'username': 'john_doe',
                            'course_id': 'course123',
                            'course_title': 'Intro to Python'
                        }
                    ]
                }
            }
        }
    }
})
def get_user_courses(username):
    enrolled = list(enrollments_collection.find({"username": username}, {"_id": 0}))
    enriched_courses = []

    for record in enrolled:
        course_id = record['course_id']
        try:
            response = session.get(f'http://course-service:5002/course/{course_id}')
            if response.status_code == 200:
                course_info = response.json()
                enriched_courses.append({
                    "username": username,
                    "course_id": course_id,
                    "course_title": course_info.get("title", "Unknown")
                })
            else:
                enriched_courses.append({
                    "username": username,
                    "course_id": course_id,
                    "course_title": "Not Found"
                })
        except requests.exceptions.RequestException:
            enriched_courses.append({
                "username": username,
                "course_id": course_id,
                "course_title": "Error Fetching Title"
            })

    return jsonify(enriched_courses)

@app.route('/enroll', methods=['PUT'])
@swag_from({
    'tags': ['Enrollment'],
    'summary': 'Update a user enrollment from one course to another',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'example': {
                    'username': 'john_doe',
                    'old_course_id': 'course123',
                    'new_course_id': 'course456'
                }
            }
        }
    },
    'responses': {
        200: {
            'description': 'Enrollment updated',
            'content': {
                'application/json': {
                    'example': {'message': 'Enrollment updated'}
                }
            }
        },
        404: {
            'description': 'Enrollment not found'
        }
    }
})
def update_enrollment():
    data = request.json
    username = data.get('username')
    old_course_id = data.get('old_course_id')
    new_course_id = data.get('new_course_id')

    result = enrollments_collection.update_one(
        {"username": username, "course_id": old_course_id},
        {"$set": {"course_id": new_course_id}}
    )

    if result.matched_count == 0:
        return jsonify({"error": "Enrollment not found"}), 404

    return jsonify({"message": "Enrollment updated"})

@app.route('/enroll', methods=['DELETE'])
@swag_from({
    'tags': ['Enrollment'],
    'summary': 'Delete a user enrollment from a course',
    'requestBody': {
        'required': True,
        'content': {
            'application/json': {
                'example': {
                    'username': 'john_doe',
                    'course_id': 'course123'
                }
            }
        }
    },
    'responses': {
        200: {
            'description': 'Enrollment deleted',
            'content': {
                'application/json': {
                    'example': {'message': 'Enrollment deleted'}
                }
            }
        },
        404: {
            'description': 'Enrollment not found'
        }
    }
})
def delete_enrollment():
    data = request.json
    username = data.get('username')
    course_id = data.get('course_id')

    result = enrollments_collection.delete_one({"username": username, "course_id": course_id})

    if result.deleted_count == 0:
        return jsonify({"error": "Enrollment not found"}), 404

    return jsonify({"message": "Enrollment deleted"})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5003)
