from flask import Flask, request, jsonify
from pymongo import MongoClient
from flasgger import Swagger, swag_from

app = Flask(__name__)

# http://localhost:5002/apidocs/#/

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

client = MongoClient("mongodb://mongo:27017/")
db = client.online_learning
courses_collection = db.courses


@app.route('/')
@swag_from({
    'responses': {
        200: {
            'description': 'Welcome message'
        }
    }
})
def welcome():
    return "Course Service is running"


@app.route('/courses', methods=['POST'])
@swag_from({
    'tags': ['Courses'],
    'parameters': [
        {
            'name': 'body',
            'in': 'body',
            'required': True,
            'schema': {
                'type': 'object',
                'properties': {
                    'course_id': {'type': 'integer'},
                    'title': {'type': 'string'},
                    'description': {'type': 'string'},
                    'instructor': {'type': 'string'}
                },
                'required': ['course_id', 'title']
            }
        }
    ],
    'responses': {
        200: {'description': 'Course added successfully'},
        400: {'description': 'Course with this course_id already exists'}
    }
})
def add_course():
    data = request.json
    existing_course = courses_collection.find_one({"course_id": data.get("course_id")})
    
    if existing_course:
        return jsonify({"error": "Course with this course_id already exists"}), 400

    courses_collection.insert_one(data)
    return jsonify({"message": "Course added"})


@app.route('/courses', methods=['GET'])
@swag_from({
    'tags': ['Courses'],
    'responses': {
        200: {
            'description': 'List of courses',
            'schema': {
                'type': 'array',
                'items': {
                    'type': 'object',
                    'properties': {
                        'course_id': {'type': 'integer'},
                        'title': {'type': 'string'},
                        'description': {'type': 'string'},
                        'instructor': {'type': 'string'}
                    }
                }
            }
        }
    }
})
def list_courses():
    courses = list(courses_collection.find({}, {'_id': 0}))
    return jsonify(courses)

@app.route('/course/<int:course_id>', methods=['GET'])
@swag_from({
    'tags': ['Courses'],
    'parameters': [
        {
            'name': 'course_id',
            'in': 'path',
            'type': 'integer',
            'required': True
        }
    ],
    'responses': {
        200: {'description': 'Course found'},
        404: {'description': 'Course not found'}
    }
})
def get_course(course_id):
    course = courses_collection.find_one({"course_id": course_id}, {"_id": 0})
    if course:
        return jsonify(course)
    else:
        return jsonify({"error": "Course not found"}), 404


@app.route('/course/<int:course_id>', methods=['PUT'])
@swag_from({
    'tags': ['Courses'],
    'parameters': [
        {
            'name': 'course_id',
            'in': 'path',
            'type': 'integer',
            'required': True
        },
        {
            'name': 'body',
            'in': 'body',
            'required': True,
            'schema': {
                'type': 'object',
                'properties': {
                    'title': {'type': 'string'},
                    'description': {'type': 'string'},
                    'instructor': {'type': 'string'}
                }
            }
        }
    ],
    'responses': {
        200: {'description': 'Course updated'},
        404: {'description': 'Course not found'}
    }
})
def update_course(course_id):
    data = request.json
    result = courses_collection.update_one({"course_id": course_id}, {"$set": data})
    if result.matched_count > 0:
        return jsonify({"message": "Course updated"})
    else:
        return jsonify({"error": "Course not found"}), 404

@app.route('/course/<int:course_id>', methods=['DELETE'])
@swag_from({
    'tags': ['Courses'],
    'parameters': [
        {
            'name': 'course_id',
            'in': 'path',
            'type': 'integer',
            'required': True
        }
    ],
    'responses': {
        200: {'description': 'Course deleted'},
        404: {'description': 'Course not found'}
    }
})
def delete_course(course_id):
    result = courses_collection.delete_one({"course_id": course_id})
    if result.deleted_count > 0:
        return jsonify({"message": "Course deleted"})
    else:
        return jsonify({"error": "Course not found"}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5002)