from flask import Flask, jsonify
from pymongo import MongoClient
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

# Connect to MongoDB
client = MongoClient("mongodb://mongo:27017/")
db = client.online_learning

enrollments_collection = db.enrollments
courses_collection = db.courses

@app.route('/stats/enrollments', methods=['GET'])
@swag_from({
    'tags': ['Statistics'],
    'summary': 'Get enrollment count per course',
    'description': 'Returns a list of course IDs with corresponding enrollment counts sorted in descending order.',
    'responses': {
        200: {
            'description': 'List of enrollment stats',
            'content': {
                'application/json': {
                    'example': [
                        {"course_id": "course123", "count": 45},
                        {"course_id": "course456", "count": 30}
                    ]
                }
            }
        }
    }
})
def enrollment_stats():
    pipeline = [
        {"$group": {"_id": "$course_id", "count": {"$sum": 1}}},
        {"$sort": {"count": -1}}
    ]
    stats = list(enrollments_collection.aggregate(pipeline))
    for stat in stats:
        stat["course_id"] = stat.pop("_id")
    return jsonify(stats)

@app.route('/stats/popular_courses', methods=['GET'])
@swag_from({
    'tags': ['Statistics'],
    'summary': 'Get top 5 most popular courses',
    'description': 'Returns a list of the top 5 courses with the highest number of enrollments, including course titles.',
    'responses': {
        200: {
            'description': 'List of popular courses with titles and enrollment counts',
            'content': {
                'application/json': {
                    'example': [
                        {
                            "course_id": "course123",
                            "course_title": "Intro to Python",
                            "enrollments": 100
                        },
                        {
                            "course_id": "course456",
                            "course_title": "Advanced AI",
                            "enrollments": 80
                        }
                    ]
                }
            }
        }
    }
})
def popular_courses():
    pipeline = [
        {"$group": {"_id": "$course_id", "count": {"$sum": 1}}},
        {"$sort": {"count": -1}},
        {"$limit": 5}
    ]
    popular = list(enrollments_collection.aggregate(pipeline))
    course_titles = {}
    
    for item in popular:
        course_id = item['_id']
        course = courses_collection.find_one({"course_id": course_id})
        course_titles[course_id] = course['title'] if course else "Unknown"

    result = [
        {
            "course_id": item['_id'],
            "course_title": course_titles[item['_id']],
            "enrollments": item['count']
        }
        for item in popular
    ]
    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5005)
