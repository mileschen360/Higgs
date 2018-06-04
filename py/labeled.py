"""Get attributes of labeled clone instances
"""

import pandas as pd

from . import dbinfo
import psycopg2
import importlib

importlib.reload(dbinfo)

def pattrs(attr, filter_str):
    '''get posts attributes
       attr is the column of posts, 
       eg. 
        DISTINCT posts.score, posts.id'''
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
        return rows


def qattrs(attr, filter_str):
    '''get question post attributes, such as view count
    attr: eg DISTINCT posts.score, posts.id
    '''
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT %s
                FROM posts
                INNER JOIN
                (SELECT DISTINCT posts.parentid AS prtid, posts.id
                FROM posts
                INNER JOIN labels
                    ON posts.id = labels.postid
                WHERE %s) AS labeled_q
                ON posts.id = labeled_q.prtid
                ;""" % (attr, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        return rows


def uattrs(attr, filter_str, col=None):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT %s
                FROM users
                INNER JOIN 
                    (SELECT DISTINCT posts.id, posts.owneruserid
                    FROM labels
                    INNER JOIN posts
                    ON labels.postid=posts.id
                WHERE %s) AS ans_users
                ON users.id=ans_users.owneruserid
                ;""" % (attr, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        if col is not None:
            return [row[col] for row in rows]
        else:
            return rows

def attrs_old(attr, filter_str, cols=None):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT %s
                FROM labels
                INNER JOIN posts answers
                ON labels.postid = answers.id
                INNER JOIN posts questions
                ON answers.parentid = questions.id
                INNER JOIN users
                ON answers.owneruserid = users.id
                WHERE %s
                ;""" % (attr, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        if cols is None:
            return np.array(rows)
        if isinstance(cols, int):
            return np.array([row[cols] for row in rows])
        else:
            return np.array([[row[col] for col in cols] for row in rows])

def attrs(attr, filter_str, inc_aid=False):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT %s, answers.id
                FROM labels
                INNER JOIN posts answers
                ON labels.postid = answers.id
                INNER JOIN posts questions
                ON answers.parentid = questions.id
                INNER JOIN users
                ON answers.owneruserid = users.id
                WHERE %s
                ;""" % (attr, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        header = [name.strip() for name in attr.split(",")]
        if inc_aid:
            header.append('answers.id')
            data = rows
        else:
            data = [row[:-1] for row in rows]
        return pd.DataFrame(data, columns=header)


def ask_learn_answer():
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT p.cid,
                FROM posts q
                INNER JOIN 
                INNER JOIN labels
                ON labels.postid = a.id
                INNER JOIN clone_pairs_all p
                ON labels.postid = p.postid0
                    AND labels.indx = p.indx0
                    AND labels.tbegin = p.tbegin0
                    AND labels.tend = p.tend0
                ;
        

        
        
        """

    
def no_owner_answers(attr='DISTINCT posts.id, posts.owneruserid', filter_str='TRUE'):
    """return answers that have NULL owneruserid"""
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT %s
                    FROM labels
                    INNER JOIN posts
                    ON labels.postid=posts.id
                WHERE %s AND posts.owneruserid is NULL
                ;""" % (attr, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        return rows

def is_secure_answer(inc_slabels=[1,2,3,6,7], exc_slabels=[4], postid_name='labels.postid'):

    inc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in inc_slabels])
    exc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in exc_slabels]) if exc_slabels else 'FALSE'

    return """ ((%s) AND %s NOT IN (
                    SELECT postid FROM labels WHERE %s)) """ % (inc_filter, postid_name, exc_filter)

def is_insecure_answer():
    return ' (labels.slabel=4) '


def is_irrelevant_answer(inc_slabels=[0], exc_slabels=range(1,8), postid_name='labels.postid'):
    inc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in inc_slabels])
    exc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in exc_slabels]) if exc_slabels else 'FALSE'

    return """ ((%s) AND %s NOT IN (
                    SELECT postid FROM labels WHERE %s)) """ % (inc_filter, postid_name, exc_filter)


def is_mixed_answer(inc_slabels=[5], exc_slabels=[1,2,3,4,6,7], postid_name='labels.postid'):
    inc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in inc_slabels])
    exc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in exc_slabels]) if exc_slabels else 'FALSE'

    return """ ((%s) AND %s NOT IN (
                    SELECT postid FROM labels WHERE %s)) """ % (inc_filter, postid_name, exc_filter)



def is_irrelevant_clnclss():
    return ' (labels.slabel=0)'

def is_insecure_clnclss():
    return ' (labels.slabel=4) '

def is_secure_clnclss():
    return ' (labels.slabel=2 OR labels.slabel=3 OR labels.slabel=6 OR labels.slabel=7) '

def is_mixed_clnclss():
    return ' (labels.slabel=5) '


def is_irrelevant_clninst():
    return ' (labels.slabel=0)'

def is_insecure_clninst():
    return ' (labels.slabel=4) '

def is_secure_clninst():
    return ' (labels.slabel=2 OR labels.slabel=3 OR labels.slabel=6 OR labels.slabel=7) '

def is_mixed_clninst():
    return ' (labels.slabel=5) '



def n_answer_posts(filter_str):
    ''' eg.
        filter_str = 'labels.slabel=2 AND postid NOT IN (SELECT postid FROM labels WHERE slabel=4)'
    '''
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT postid) FROM labels
WHERE %s
;
""" % (filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        assert(len(rows) == 1)
        return rows[0][0]


def n_question_posts(filter_str):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT posts.parentid)
FROM posts
INNER JOIN labels
  ON labels.postid = posts.id
WHERE %s;
""" % (filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        assert(len(rows) == 1)
        return rows[0][0]

def n_cln_clsss(filter_str):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT p.cid)
FROM labels 
INNER JOIN clone_pairs_all p
ON labels.postid  = p.postid0
  AND labels.indx = p.indx0
  AND labels.tbegin = p.tbegin0
  AND labels.tend = p.tend0
WHERE %s
;
""" % (filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        assert(len(rows) == 1)
        return rows[0][0]
    
def n_cln_insts(filter_str):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(*)
FROM labels 
WHERE %s
;
""" % (filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        assert(len(rows) == 1)
        return rows[0][0]

