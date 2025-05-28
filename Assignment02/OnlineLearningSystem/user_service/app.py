from flask import Flask, request, jsonify
from pymongo import MongoClient
import jwt
import datetime
from functools import wraps
from flasgger import Swagger, swag_from

app = Flask(__name__)
app.config['SECRET_KEY'] = 'my_secret_key'  # Change this to a secure value
# http://localhost:5001/apidocs/#/

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
users_collection = db.users

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('Authorization')
        if token and token.startswith("Bearer "):
            token = token.split(" ")[1]
        else:
            return jsonify({'message': 'Token is missing!'}), 401

        try:
            data = jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
            current_user = users_collection.find_one({"username": data['username']})
            if not current_user:
                raise Exception('User not found')
        except Exception as e:
            return jsonify({'message': f'Token is invalid: {str(e)}'}), 401

        return f(*args, **kwargs)
    return decorated

@app.route('/register', methods=['POST'])
@swag_from({
    'tags': ['Auth'],
    'parameters': [{
        'name': 'body',
        'in': 'body',
        'required': True,
        'schema': {
            'type': 'object',
            'properties': {
                'username': {'type': 'string'},
                'password': {'type': 'string'}
            },
            'required': ['username', 'password']
        }
    }],
    'responses': {
        200: {
            'description': 'User registered successfully',
            'schema': {
                'type': 'object',
                'properties': {
                    'message': {'type': 'string'}
                }
            }
        },
        400: {
            'description': 'User already exists',
            'schema': {
                'type': 'object',
                'properties': {
                    'message': {'type': 'string'}
                }
            }
        },
        500: {
            'description': 'Internal server error',
            'schema': {
                'type': 'object',
                'properties': {
                    'message': {'type': 'string'}
                }
            }
        }
    }
})
def register():
    data = request.json
    if users_collection.find_one({"username": data['username']}):
        return jsonify({"message": "User already exists"}), 400
    users_collection.insert_one(data)
    return jsonify({"message": "User registered successfully"})

@app.route('/login', methods=['POST'])
@swag_from({
    'tags': ['Auth'],
    'parameters': [{
        'name': 'body',
        'in': 'body',
        'required': True,
        'schema': {
            'type': 'object',
            'properties': {
                'username': {'type': 'string'},
                'password': {'type': 'string'}
            },
            'required': ['username', 'password']
        }
    }],
    'responses': {
        200: {'description': 'Token returned'},
        401: {'description': 'Invalid credentials'}
    }
})
def login():
    data = request.json
    user = users_collection.find_one({"username": data['username']})
    if not user or user.get('password') != data['password']:
        return jsonify({'message': 'Invalid credentials'}), 401

    token = jwt.encode({
        'username': user['username'],
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=1)
    }, app.config['SECRET_KEY'], algorithm="HS256")

    return jsonify({'token': token})

@app.route('/get_user/<username>', methods=['GET'])
@token_required
@swag_from({
    'tags': ['Users'],
    'parameters': [{
        'name': 'Authorization',
        'in': 'header',
        'type': 'string',
        'required': True
    }],
    'responses': {
        200: {'description': 'User found'},
        404: {'description': 'User not found'}
    }
})
def get_user(username):
    user = users_collection.find_one({"username": username}, {"_id": 0})
    if user:
        return jsonify(user)
    return jsonify({"message": "User not found"}), 404

@app.route('/users', methods=['GET'])
@token_required
@swag_from({
    'tags': ['Users'],
    'parameters': [{
        'name': 'Authorization',
        'in': 'header',
        'type': 'string',
        'required': True
    }],
    'responses': {
        200: {'description': 'All users returned'}
    }
})
def get_all_users():
    users = users_collection.find({}, {"_id": 0})
    return jsonify(list(users))



@app.route('/testusers', methods=['GET'])
@swag_from({
    'tags': ['Users'],
    'parameters': [{
        'name': 'Authorization',
        'in': 'header',
        'type': 'string',
        'required': True
    }],
    'responses': {
        200: {'description': 'All users returned'}
    }
})
def get_all_users_test():
    users = users_collection.find({}, {"_id": 0})
    return jsonify(list(users))

@app.route('/user/<string:username>', methods=['PUT'])
@token_required
@swag_from({
    'tags': ['Users'],
    'parameters': [
        {
            'name': 'Authorization',
            'in': 'header',
            'type': 'string',
            'required': True
        },
        {
            'name': 'username',
            'in': 'path',
            'type': 'string',
            'required': True
        },
        {
            'name': 'body',
            'in': 'body',
            'required': True,
            'schema': {
                'type': 'object',
                'properties': {
                    'email': {'type': 'string'},
                    'name': {'type': 'string'}
                }
            }
        }
    ],
    'responses': {
        200: {'description': 'User updated'},
        404: {'description': 'User not found'}
    }
})
def update_user(username):
    data = request.json
    result = users_collection.update_one({"username": username}, {"$set": data})
    if result.matched_count > 0:
        return jsonify({"message": "User updated"})
    else:
        return jsonify({"error": "User not found"}), 404

@app.route('/user/<string:username>', methods=['DELETE'])
@token_required
@swag_from({
    'tags': ['Users'],
    'parameters': [
        {
            'name': 'Authorization',
            'in': 'header',
            'type': 'string',
            'required': True
        },
        {
            'name': 'username',
            'in': 'path',
            'type': 'string',
            'required': True
        }
    ],
    'responses': {
        200: {'description': 'User deleted'},
        404: {'description': 'User not found'}
    }
})
def delete_user(username):
    result = users_collection.delete_one({"username": username})
    if result.deleted_count > 0:
        return jsonify({"message": "User deleted"})
    else:
        return jsonify({"error": "User not found"}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)
