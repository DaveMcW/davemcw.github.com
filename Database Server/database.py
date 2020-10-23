#!/usr/bin/python
"""
CS 340 Final Project - RESTful API
Author: David McWilliams
Date: June 22, 2020
"""

import hashlib
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


def password_hash(password):
    """ Hash the password """
    # TODO: Use a salt and a stronger hash
    return hashlib.sha256(password.encode('utf-8')).hexdigest()


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
    try:
        # Validate fields
        assert isinstance(request.json['username'], str)
        assert isinstance(request.json['password'], str)

        # Hash password
        hash = password_hash(request.json['password'])

        # Read from database
        filters = {'name': request.json['username'], 'password': hash}
        projection = {'_id': 1}
        result = db.user.find_one(filters, projection)

        if result is None:
            abort(401, "Invalid username or password.")

        # Create access token
        user_id = result['_id']
        token = secrets.token_hex()
        db.user.update_one({'_id': user_id}, {"$set": {'token': token}})

        # Return access token
        output = {'token': f"{str(user_id)}-{token}"}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/user-exists/<username>', method='GET')
def user_exits(username):
    try:
        # Validate fields
        assert isinstance(username, str)

        # Return result
        output = {'result': user_exists(username)}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/create-user', method='POST')
def create_user():
    """ Create a new user """
    try:
        # Validate fields
        assert isinstance(request.json['username'], str)
        assert not user_exists(request.json['username'])
        assert isinstance(request.json['password'], str)

        # Hash password
        hash = password_hash(request.json['password'])

        # Insert into database
        new_user = {
            'name': request.json['username'],
            'password': hash,
            'app_rating': -1,
        }
        result = db.user.insert_one(new_user)

        # Return user id
        output = {'id': str(result.inserted_id)}
        return json.dumps(output)

    except Exception as e:
        abort(500, "Error:" + str(e))


@route('/create-campsite', method='POST')
def create_campsite():
    """ Create a new campsite """
    check_login()

    try:
        # Validate fields
        assert isinstance(request.json.name, str)
        assert isinstance(request.json.location, str)
        assert isinstance(request.json.lat, str)
        assert isinstance(request.json.long, str)
        assert isinstance(request.json.features, str)
        assert isinstance(request.json.twitter, str)

        # Insert into database
        new_site = {
            'name': request.json.name,
            'location': request.json.location,
            'geo': {'type': "Point", 'coordinates': [request.json.lat, request.json.long]},
            'features': request.json.features,
            'twitter': request.json.twitter,
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

        # Check if it is a favorite
        filters = {'user_id': user_id, 'campsite_id': campsite_id, 'favorite': 1}
        projection = {'_id': 1}
        favorite = db.user_campsite.find_one(filters, projection) is not None

        # Return campsite details
        output = {
            'id': str(campsite_id),
            'name': result.name,
            'location': result.location,
            'lat': result.geo.coordinates[0],
            'long': result.geo.coordinates[1],
            'features': request.json.features,
            'twitter': request.json.twitter,
            'favorite': favorite,
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
        if request.json.name:
            assert isinstance(request.json.name, str)
            changes['name'] = request.json.name
        if request.json.location:
            assert isinstance(request.json.location, str)
            changes['location'] = request.json.location
        if request.json.lat or request.json.long:
            assert isinstance(request.json.lat, str)
            assert isinstance(request.json.long, str)
            changes['geo'] = {'type': "Point", 'coordinates': [request.json.lat, request.json.long]},
        if request.json.features:
            assert isinstance(request.json.features, str)
            changes['features'] = request.json.features
        if request.json.twitter:
            assert isinstance(request.json.twitter, str)
            changes['twitter'] = request.json.twitter

        # Update database
        db.campsite.update_one({'_id': objectid.ObjectId(campsite_id)}, {"$set": changes})

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


@route('/set-favorite/<campsite_id>/<favorite>', method='PUT')
def set_favorite(campsite_id, favorite):
    """ Set a campsite as a favorite """
    user_id = check_login()

    try:
        # Validate fields
        assert isinstance(favorite, str)

        # Set or unset favorite
        if int(favorite) == 0:
            changes = {'$unset': {'favorite': 0}}
        else:
            changes = {'$set': {'favorite': 1}}

        # Update database
        filters = {'user_id': user_id, 'campsite_id': objectid.ObjectId(campsite_id)}
        db.user_campsite.update_one(filters, changes, upsert=True)

    except Exception as e:
        abort(500, "Error:" + str(e))


if __name__ == '__main__':
    run(host='localhost', port=8080)
