"""!!! input your database username and password here, 
then change this file name to dbinfo.py"""

import psycopg2
from neo4j.v1 import GraphDatabase

tb_name_suffix = 'all'

connect_str = "dbname='stackoverflow' user='yourdatabaseusername' host='yourdatabasehostname' " + \
             "password='yourdatabasepassword'"

neo4j_uri="bolt://youneo4jhostname:7687"
neo4j_auth=('neo4j', 'yourneo4jpassword')

driver = GraphDatabase.driver(neo4j_uri, auth=neo4j_auth)


