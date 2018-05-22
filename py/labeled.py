from . import dbinfo
import psycopg2
import importlib

importlib.reload(dbinfo)

def pattrs(attr, filter_str):
    '''get posts attributes
       attr is the column of posts, eg DISTINCT posts.score, posts.id'''
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT %s
FROM labels
INNER JOIN posts 
  ON labels.postid=posts.id
WHERE %s
;
""" % (attr, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        return [row[0] for row in rows]
