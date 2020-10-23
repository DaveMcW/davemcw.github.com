db.getCollection("campsite").createIndex({"name":"text"},{unique:false},{background:1})
db.getCollection("campsite").createIndex({"geo":"2dsphere"},{unique:false},{background:1})
db.getCollection("user").createIndex({"name":1},{unique:true},{background:1})
db.getCollection("user_campsite").createIndex({"user_id":1},{unique:false},{background:1})
db.getCollection("user_campsite").createIndex({"campsite_id":1},{unique:false},{background:1})

