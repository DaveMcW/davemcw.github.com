#!/usr/bin/python
"""
CS 499 Final Project - RESTful API
Author: David McWilliams
Date: October, 2020
"""

import bcrypt
import json
import re
import secrets
from bottle import route, run, request, abort
from bson import objectid
from pymongo import MongoClient

# Connect to database
db_host = "localhost"
db_name = "cs499"
db_user = "cs499"
db_password = "cs499"
db_connection = MongoClient(f"mongodb://{db_user}:{db_password}@{db_host}/{db_name}")
db = db_connection[db_name]


def check_login():
    """ Check user's login credentials """

    # Validate Authorization header
    authorization = request.headers.get("Authorization", None)
    if not isinstance(authorization, str):
        abort(401, "Please log in again.")

    result = None
    try:
        # Read user_id and token from Authorization header
        user_id, token = authorization.split('-', 1)

        # Compare with database
        filters = {'_id': objectid.ObjectId(user_id), 'token': token}
        projection = {'_id': 1}
        result = db.user.find_one(filters, projection)

    except Exception as e:
        abort(500, "Error:" + str(e))

    if result is None:
        abort(401, "Please log in again.")

    # Return user id
    return result['_id']


def user_exists(username):
    """ User name lookup helper function """
    filters = {'name': username}
    projection = {'_id': 1}
    result = db.user.find_one(filters, projection)
    return result is not None


def campsite_search_result(pipeline):
    """ Search helper function """

    # Sort results
    pipeline.append({"$sort": {'name': 1}})

    # Only return id and name
    pipeline.append({"$project": {"_id": 1, "name": 1}})

    output = []
    for item in db.campsite.aggregate(pipeline):
        # Return id and name for each campsite found
        output.append({'id': str(item['_id']), 'name': item['name']})
    return json.dumps(output)


@route('/login', method='POST')
def login():
    """ Authenticate a user and grant an access token """
    user_id = ""
    password = ""
    hashed = ""
    try:
        # Validate fields
        username = request.json.get('username')
        assert isinstance(username, str)
        password = request.json.get('password')
        assert isinstance(password, str)

        password = password.encode('utf-8')

        # Read from database
        filters = {'name': username}
        projection = {'password': 1}
        result = db.user.find_one(filters, projection)
        hashed = result['password']
        user_id = result['_id']

    except Exception as e:
        abort(500, "Error:" + str(e))

    # Check password
    if not bcrypt.checkpw(password, hashed):
        abort(401, "Invalid username or password.")

    try:
        # Create access token
        token = secrets.token_hex()
        db.user.update_one({'_id': user_id}, {"$set": {'token': token}})

        # Return id and access token
        output = {'id': str(user_id), 'token': f"{str(user_id)}-{token}"}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/create-user', method='POST')
def create_user():
    """ Create a new user """
    try:
        # Validate fields
        if not request.json.get('username'):
            output = {'error': "Username is required."}
            return json.dumps(output)
        if user_exists(request.json.get('username')):
            output = {'error': "An account with that username already exists."}
            return json.dumps(output)
        if not request.json.get('password'):
            output = {'error': "Password is required."}
            return json.dumps(output)

        # Hash the password using a salt
        password = request.json.get('password').encode('utf-8')
        salt = bcrypt.gensalt()
        hashed = bcrypt.hashpw(password, salt)

        # Insert into database
        new_user = {
            'name': request.json.get('username'),
            'password': hashed,
        }
        result = db.user.insert_one(new_user)
        user_id = result.inserted_id

        # Add a sample favorite campsite
        changes = {'$set': {'favorite': 1}}
        filters = {'user_id': user_id, 'campsite_id': objectid.ObjectId("5f7147f0b52c31ce37f0e687")}
        db.user_campsite.update_one(filters, changes, upsert=True)

        # Return user id
        output = {'id': str(user_id)}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/create-campsite', method='POST')
def create_campsite():
    """ Create a new campsite """
    check_login()

    try:
        # Validate fields
        assert isinstance(request.json['name'], str)
        assert isinstance(request.json['location'], str)
        assert isinstance(request.json['features'], str)
        assert isinstance(request.json['twitter'], str)

        # Insert into database
        new_site = {
            'name': request.json['name'],
            'location': request.json['location'],
            'features': request.json['features'],
            'twitter': request.json['twitter'],
        }
        result = db.campsite.insert_one(new_site)

        # Return campsite id
        output = {'id': str(result.inserted_id)}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/get-campsite/<campsite_id>', method='GET')
def get_campsite(campsite_id):
    """ Get campsite details """
    user_id = check_login()

    try:
        # Read from database
        campsite_id = objectid.ObjectId(campsite_id)
        result = db.campsite.find_one({'_id': campsite_id})

        # Check user's rating and favorite
        rating = -1
        favorite = 0
        filters = {'user_id': user_id, 'campsite_id': campsite_id}
        projection = {'_id': 0, 'rating': 1, 'favorite': 1}
        user_result = db.user_campsite.find_one(filters, projection)
        if user_result is not None:
            rating = user_result.get("rating")
            if not rating:
                rating = -1
            favorite = int(bool(user_result.get("favorite")))

        # Return campsite details
        output = {
            'id': str(result['_id']),
            'name': result['name'],
            'location': result['location'],
            'lat': result['geo']['coordinates'][1],
            'long': result['geo']['coordinates'][0],
            'features': result['features'],
            'twitter': result['twitter'],
            'favorite': favorite,
            'rating': rating,
            'average_rating': result['average_rating'],
        }
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error: " + str(e))


@route('/update-campsite/<campsite_id>', method='PUT')
def update_campsite(campsite_id):
    """ Update campsite details """
    check_login()

    try:
        # Validate fields
        changes = {}
        if request.json.get('name'):
            assert isinstance(request.json['name'], str)
            changes['name'] = request.json['name']
        if request.json.get('location'):
            assert isinstance(request.json['location'], str)
            changes['location'] = request.json['location']
        if request.json.get('lat') or request.json.get('long'):
            assert isinstance(request.json['lat'], (int, float))
            assert isinstance(request.json['long'], (int, float))
            changes['geo'] = {'type': "Point", 'coordinates': [request.json['long'], request.json['lat']]},
        if request.json.get('features'):
            assert isinstance(request.json['features'], str)
            changes['features'] = request.json['features']
        if request.json.get('twitter'):
            assert isinstance(request.json['twitter'], str)
            changes['twitter'] = request.json['twitter']

        # Update database
        filters = {'_id': objectid.ObjectId(campsite_id)}
        db.campsite.update_one(filters, {"$set": changes})

        # Return campsite id
        output = {'id': campsite_id}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/delete-campsite/<campsite_id>', method='DELETE')
def delete_campsite(campsite_id):
    """ Delete campsite """
    check_login()

    try:
        # Delete from database
        db.campsite.delete_one({'_id': objectid.ObjectId(campsite_id)})

        # Return nothing
        output = {}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/get-all-campsites', method='GET')
def get_all_campsites():
    """ Return all campsites """
    check_login()

    try:
        # Do search
        return campsite_search_result([])

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/search-campsite-name/<name>', method='GET')
def search_campsite_name(name):
    """ Search for campsites by name """
    check_login()

    try:
        # Validate name
        assert isinstance(name, str)

        # Escape double quotes
        name = name.replace('"', '\\"')

        # Do search
        pipeline = [
            {'$match': {'$text': {'$search': '"' + name + '"'}}},
        ]
        return campsite_search_result(pipeline)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/search-campsite-location/<location>', method='GET')
def search_campsite_location(location):
    """ Search for campsites by location """
    check_login()

    try:
        # Validate location
        assert isinstance(location, str)

        # Build regular expression
        regex = re.compile(re.escape(location), re.IGNORECASE)

        # Do search
        pipeline = [
            {'$match': {'location': regex}},
        ]
        return campsite_search_result(pipeline)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/search-campsite-feature/<feature>', method='GET')
def search_campsite_feature(feature):
    """ Search for campsites by feature """
    check_login()

    try:
        # Validate feature
        assert isinstance(feature, str)

        # Build regular expression
        regex = re.compile(re.escape(feature), re.IGNORECASE)

        # Do search
        pipeline = [
            {'$match': {'features': regex}},
        ]
        return campsite_search_result(pipeline)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/search-campsite-favorite', method='GET')
def search_campsite_favorite():
    """ Search for user's favorite campsites """
    user_id = check_login()

    try:
        # Look up favorite campsite IDs
        filters = {'user_id': user_id, 'favorite': 1}
        projection = {'_id': 0, 'campsite_id': 1}
        ids = []
        for item in db.user_campsite.find(filters, projection):
            ids.append(item['campsite_id'])

        # Do search
        pipeline = [
            {'$match': {'_id': {'$in': ids}}},
        ]
        return campsite_search_result(pipeline)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/search-campsite-near/<lat>/<long>/<distance>', method='GET')
def search_campsite_near(lat, long, distance):
    """ Search for campsites near location """
    check_login()

    try:
        # Do search
        point = {'type': "Point", 'coordinates': [float(long), float(lat)]}
        pipeline = [
            {'$geoNear': {
                'key': 'geo',
                'near': point,
                'maxDistance': float(distance),
                'distanceField': 'distance',
                'spherical': True,
            }},
            {'$sort': {'distance': 1}},
            {'$project': {'name': 1, 'distance': 1}},
        ]

        # Return id and name for each campsite found
        output = []
        for item in db.campsite.aggregate(pipeline):
            # Convert from meters to miles
            distance = item['distance'] / 1609.344
            if distance > 9.9:
                distance = f"{distance:.0f}"
            else:
                distance = f"{distance:.1f}"
            name = f"{distance} mi. {item['name']}"
            output.append({'id': str(item['_id']), 'name': name})
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/set-campsite-favorite/<campsite_id>/<favorite>', method='PUT')
def set_favorite(campsite_id, favorite):
    """ Set a campsite as a favorite """
    user_id = check_login()

    try:
        # Set or unset favorite
        if int(favorite) == 0:
            changes = {'$unset': {'favorite': 0}}
        else:
            changes = {'$set': {'favorite': 1}}

        # Update database
        filters = {'user_id': user_id, 'campsite_id': objectid.ObjectId(campsite_id)}
        db.user_campsite.update_one(filters, changes, upsert=True)

        # Return empty object
        return json.dumps({})

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/set-campsite-rating/<campsite_id>/<rating>', method='PUT')
def set_campsite_rating(campsite_id, rating):
    """ Set a campsite as a favorite """
    user_id = check_login()

    try:
        # Validate fields
        rating = int(rating)
        assert rating >= 0
        assert rating <= 5
        campsite_id = objectid.ObjectId(campsite_id)

        # Update database
        changes = {'$set': {'rating': rating}}
        filters = {'user_id': user_id, 'campsite_id': campsite_id}
        db.user_campsite.update_one(filters, changes, upsert=True)

        # Recalculate average for that campsite
        pipeline = [
            {'$match': {'campsite_id': campsite_id, 'rating': {'$exists': 'true'}}},
            {'$group': {'_id': '$campsite_id', 'sum': {'$sum': "$rating"}, 'total': {'$sum': 1}}},
            {'$project': {'sum': 1, 'total': 1}}
        ]
        cursor = db.user_campsite.aggregate(pipeline)
        result = list(cursor)[0]
        if result['total'] == 0:
            average_rating = -1
        else:
            average_rating = result['sum'] / result['total']

        # Update database
        changes = {'$set': {'average_rating': average_rating}}
        filters = {'_id': campsite_id}
        db.campsite.update_one(filters, changes)

        # Return new average rating
        output = {'average_rating': average_rating}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


if __name__ == '__main__':
    run(host='davemcw.com', port=8080, debug=False)
