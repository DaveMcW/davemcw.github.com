{% include navigation.html %}
## Algorithms and Data Structures Enhancement ##

My original artifact used the SHA-256 hash algorithm to encrypt user passwords. There are two problems with this. The first problem is SHA-256 is so fast, that it is vulnerable to brute force attacks calculating millions of passwords per second. After researching secure algorithms, I decided to use bcrypt, which is based on the Blowfish cipher. Blowfish and bcrypt allow the number of rounds in the hash to be adjusted, so that calculating one hash takes a moderate amount of time on a typical computer. Brute force attacks take much longer. (Schneier, 1994).

The second problem with SHA-256 is that it always produces the same output. This means an attacker can pre-compute common hashes in a "rainbow table", and instantly crack them. It also allows an attacker to identify users with the same password. The solution is to add a random value, called a salt, to make each user's password unique (Naiakshina, 2017). I used the python implementation of bcrypt, which includes support for a salt (Bodnar, 2020). You can view my [secure password code here](https://github.com/DaveMcW/davemcw.github.io/blob/master/Database%20Server/database.py#L134). This demonstrates my ability to anticipate adversarial exploits, mitigate design flaws, and ensure privacy and enhanced security of data and resources.

My original artifact searched for nearby campsites by reading the entire database and calculating the distance to each site. This had complexity O(N). I was able to improve this to O(log N) by using a B-tree data structure. I built the B-tree by adding a MongoDB "2dsphere" index (MongoDB, 2013). You can view the [index creation code](https://github.com/DaveMcW/davemcw.github.io/blob/master/Database%20Server/create_database.js#L2) and the [2dsphere search code](https://github.com/DaveMcW/davemcw.github.io/blob/master/Database%20Server/database.py#L391). This demonstrates my ability to solve a given problem using algorithmic principles and computer science practices and standards appropriate to its solution.

#### Reflection ####

Using the most efficent algorithms and data structures available is very important for large-scale projects. There is no discernible difference between O(n) and O(log n) search performance in my database of 20 campsites. But when the application has thousands of users and thousands of campsites, inefficient algorithms cost valuable server resource and can even make it grind to a halt.

Hackers are constantly looking for ways to break into applications, so it is important to implement industry-standard practices. Secure password hashing can mitigate exposure to data loss and lawsuits, even if a database is compromised by hackers.

#### References ####
Bodnar, J. (2020). Python bcrypt tutorial. Retrieved from http://zetcode.com/python/bcrypt/
MongoDB. (2013). New Geo Features in MongoDB 2.4. Retrieved from https://www.mongodb.com/blog/post/new-geo-features-in-mongodb-24
Naiakshina, A., Danilova, A., Tiefenau, C., Herzog, M., Dechand, S., & Smith, M. (2017). Why Do Developers Get Password Storage Wrong? A Qualitative Usability Study. https://doi-org.ezproxy.snhu.edu/10.1145/3133956.3134082
Schneier B. (1994). Fast Software Encryption, Cambridge Security Workshop Proceedings (December 1993), Springer-Verlag, 1994, pp. 191-204. Retrieved from https://www.schneier.com/academic/archives/1994/09/description_of_a_new.html

Continue to [Software Design and Engineering Enhancement](/design_and_engineering.html).
