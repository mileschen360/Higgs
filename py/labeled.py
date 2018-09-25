"""Get attributes of labeled clone instances
"""

import pandas as pd

from . import dbinfo
import psycopg2
import importlib

importlib.reload(dbinfo)


cat_names = ['SSL', 'Symmetric', "Asymmetric", "Hash", "Random"]

cat_names_1b = ['DUMMY', 'SSL', 'Symmetric', "Asymmetric", "Hash", "Random"]


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

def attrs(attr, filter_str, dist='answers.id', inc_dist=False, print_sql=False):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT %s, %s
                FROM labels
                INNER JOIN posts answers
                ON labels.postid = answers.id
                INNER JOIN posts questions
                ON answers.parentid = questions.id
                INNER JOIN users
                ON answers.owneruserid = users.id
                WHERE %s
                ;""" % (attr, dist, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        header = [name.strip() for name in attr.split(",")]
        if inc_dist:
            header.append(dist)
            data = rows
        else:
            data = [row[:-1] for row in rows]
        return pd.DataFrame(data, columns=header)

def attrs_postonly(attr, filter_str, inc_aid=False, print_sql=False):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT %s, answers.id
                FROM labels
                INNER JOIN posts AS answers
                ON labels.postid = answers.id
                INNER JOIN posts AS questions
                ON answers.parentid = questions.id
                WHERE %s
                ;""" % (attr, filter_str)
        if print_sql:
            print(sql)
        cursor.execute(sql)
        rows = cursor.fetchall()
        header = [name.strip() for name in attr.split(",")]
        if inc_aid:
            header.append('answers.id')
            data = rows
        else:
            data = [row[:-1] for row in rows]
        return pd.DataFrame(data, columns=header)

def jbaker_attrs(attr, filter_str, inc_aid=False, print_sql=False):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT %s, answers.id
                 FROM source_files_%s jbk
                 INNER JOIN posts answers
                   ON jbk.postid = answers.id
                 INNER JOIN posts questions
                   ON answers.parentid = questions.id
                WHERE %s
                ;
                """ % (attr, dbinfo.tb_name_suffix, filter_str)
        if print_sql:
            print(sql)
        cursor.execute(sql)
        rows = cursor.fetchall()
        header = [name.strip() for name in attr.split(",")]
        if inc_aid:
            header.append('answers.id')
            data = rows
        else:
            if "," not in attr:
                data = [row[0] for row in rows]
            else:
                data = [row[:-1] for row in rows]
        return data

"""
SELECT DISTINCT questions.viewcount, answers.id
                 FROM source_files_all jbk
                 INNER JOIN posts answers
                   ON jbk.postid = answers.id
                 INNER JOIN posts questions
                   ON answers.parentid = questions.id
                ;
                """


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

def is_clninst_cat(i_cat):
    return """ (labels.scategory %% 10 = %d
            OR labels.scategory / 10 = %d
            OR labels.scategory / 100 = %d
            OR labels.scategory / 1000 = %d
            )""" % (i_cat, i_cat, i_cat, i_cat)

def is_answer_cat(i_cat):
    return """ (labels.postid IN (
                    SELECT postid FROM labels
                    WHERE labels.scategory %% 10 = %d
                        OR labels.scategory / 10 = %d
                        OR labels.scategory / 100 = %d
                        OR labels.scategory / 1000 = %d)
            )""" % (i_cat, i_cat, i_cat, i_cat)


def is_secure_answer(inc_slabels=[2,3,6,7], exc_slabels=[4], postid_name='labels.postid'):

    inc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in inc_slabels])
    exc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in exc_slabels]) if exc_slabels else 'FALSE'

    return """ ((%s) AND %s NOT IN (
                    SELECT postid FROM labels WHERE %s)) """ % (inc_filter, postid_name, exc_filter)

def is_insecure_answer():
    return ' (labels.slabel=4) '

def is_accepted_answer():
    return ' (EXISTS (SELECT * FROM posts WHERE acceptedanswerid=labels.postid)) '


def is_irrelevant_answer(inc_slabels=[0], exc_slabels=range(1,8), postid_name='labels.postid'):
    inc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in inc_slabels])
    exc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in exc_slabels]) if exc_slabels else 'FALSE'

    return """ ((%s) AND %s NOT IN (
                    SELECT postid FROM labels WHERE %s)) """ % (inc_filter, postid_name, exc_filter)

def is_relevant_answer():
    return " ( NOT (%s)) " % is_irrelevant_answer()

def is_relevant_clninst():
    return " ( NOT (%s)) " % is_irrelevant_clninst()


def is_mixed_answer(inc_slabels=[5], exc_slabels=[1,2,3,4,6,7], postid_name='labels.postid'):
    inc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in inc_slabels])
    exc_filter = " OR ".join(["labels.slabel=%d"%slabel for slabel in exc_slabels]) if exc_slabels else 'FALSE'

    return """ ((%s) AND %s NOT IN (
                    SELECT postid FROM labels WHERE %s)) """ % (inc_filter, postid_name, exc_filter)



def is_irrelevant_clnclss():
    return ' (labels.gslabel=0)'

def is_relevant_clnclss():
    return ' (labels.gslabel>0)'


def is_insecure_clnclss():
    return ' (labels.gslabel=4) '

def is_secure_clnclss():
    return ' (labels.gslabel=2 OR labels.gslabel=3 OR labels.gslabel=6 OR labels.gslabel=7) '

def is_mixed_clnclss():
    return ' (labels.gslabel=5) '


def is_irrelevant_clninst():
    return ' (labels.slabel=0)'

def is_relevant_clninst():
    return ' (labels.slabel>0)'

def is_insecure_clninst():
    return ' (labels.slabel=4) '

def is_secure_clninst():
    return ' (labels.slabel=2 OR labels.slabel=3 OR labels.slabel=6 OR labels.slabel=7) '

def is_mixed_clninst():
    return ' (labels.slabel=5) '


def n_answer_posts(filter_str, print_sql=False):
    ''' eg.
        filter_str = 'labels.slabel=2 AND postid NOT IN (SELECT postid FROM labels WHERE slabel=4)'
    '''
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT postid) FROM labels
WHERE %s
;
""" % (filter_str)
        if print_sql:
            print(sql)
        cursor.execute(sql)
        rows = cursor.fetchall()
        assert(len(rows) == 1)
        return rows[0][0]

def n_snippets(filter_str):
    ''' eg.
        filter_str = 'labels.slabel=2 AND postid NOT IN (SELECT postid FROM labels WHERE slabel=4)'
    '''
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(*) 
                 FROM (SELECT DISTINCT postid, indx FROM labels
                 WHERE %s) AS tmp
                ;""" % (filter_str)
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
WHERE (%s);
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


def n_cln_clsss_on_users(filter_str):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT p.cid)
FROM labels 
INNER JOIN clone_pairs_all p
ON labels.postid  = p.postid0
  AND labels.indx = p.indx0
  AND labels.tbegin = p.tbegin0
  AND labels.tend = p.tend0
INNER JOIN posts
ON posts.id = labels.postid
INNER JOIN users
ON posts.owneruserid = users.id
WHERE %s
;
""" % (filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        assert(len(rows) == 1)
        return rows[0][0]


def cln_clss_attrs(attrs, filter_str, include_cid=False, print_sql=False):
    """return group attributes,, average viewcount, group size (i.e. the number of instance in it)"""
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT cid, %s FROM
                (SELECT DISTINCT p.cid, p.postid0, p.indx0, p.tbegin0, p.tend0, 
                                 questions.viewcount, answers.score, answers.commentcount
                FROM labels 
                INNER JOIN clone_pairs_all p
                  ON labels.postid  = p.postid0
                    AND labels.indx = p.indx0
                    AND labels.tbegin = p.tbegin0
                    AND labels.tend = p.tend0
                INNER JOIN posts answers
                  ON answers.id = labels.postid
                INNER JOIN posts questions
                  ON questions.id = answers.parentid
                WHERE %s) AS tmp
                GROUP BY cid
                ;
                """ % (attrs, filter_str)
        if print_sql:
            print(sql)
        cursor.execute(sql)
        rows = cursor.fetchall()
        if include_cid:
            return [(row[1:], row[0]) for row in rows]
        else:
            return [row[1:] for row in rows]
    
"""

SELECT DISTINCT p.cid, p.postid0, p.indx0, p.tbegin0, p.tend0, answers.viewcount
                FROM labels 
                INNER JOIN clone_pairs_all p
                  ON labels.postid  = p.postid0
                    AND labels.indx = p.indx0
                    AND labels.tbegin = p.tbegin0
                    AND labels.tend = p.tend0
                INNER JOIN posts answers
                  ON answers.id = labels.postid
                WHERE  (labels.gslabel=4) 



"""



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




def cln_clss_sizes(filter_str, include_cid=False, print_sql=False):
    """return size of each group, i.e. the number of instance in it"""
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT cid, COUNT(*) FROM
                (SELECT DISTINCT p.cid, p.postid0, p.indx0, p.tbegin0, p.tend0
                FROM labels 
                INNER JOIN clone_pairs_all p
                ON labels.postid  = p.postid0
                AND labels.indx = p.indx0
                AND labels.tbegin = p.tbegin0
                AND labels.tend = p.tend0
                WHERE %s) AS tmp
                GROUP BY cid
                ;
                """ % (filter_str)
        if print_sql:
            print(sql)
        cursor.execute(sql)
        rows = cursor.fetchall()
        if include_cid:
            return [(row[1], row[0]) for row in rows]
        else:
            return [row[1] for row in rows]
    
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


def n_answerers(filter_str):
    '''get all labeled answers'''
    print("WARNING: n_answerers is deprecated, use n_auids instead")
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT posts.owneruserid)
                 FROM posts
                 INNER JOIN        
                (SELECT DISTINCT p.postid0 AS postid
                FROM labels
                INNER JOIN clone_pairs_%s p
                ON labels.postid  = p.postid0
                AND labels.indx = p.indx0
                AND labels.tbegin = p.tbegin0
                AND labels.tend = p.tend0
                WHERE %s) answers
                ON posts.id = answers.postid
                ;
                """ % (dbinfo.tb_name_suffix, filter_str)
        cursor.execute(sql)        
        rows = cursor.fetchall()
        return rows[0][0]

def n_auids(filter_str):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT posts.owneruserid)
                FROM posts
                INNER JOIN labels
                ON labels.postid = posts.id
                INNER JOIN users
                ON users.id = posts.owneruserid
                WHERE (%s);
                """ % (filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        assert(len(rows) == 1)
        return rows[0][0]


def n_askers(filter_str):
    '''get all labeled answers'''
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT COUNT(DISTINCT posts.owneruserid)
                FROM posts
                INNER JOIN
                    (SELECT DISTINCT posts.parentid as postid
                    FROM posts
                    INNER JOIN        
                        (SELECT DISTINCT p.postid0 AS postid
                        FROM labels
                        INNER JOIN clone_pairs_%s p
                        ON labels.postid  = p.postid0
                        AND labels.indx = p.indx0
                        AND labels.tbegin = p.tbegin0
                        AND labels.tend = p.tend0
                        WHERE %s) AS answers
                    ON posts.id = answers.postid) AS questions
                ON posts.id = questions.postid
                ;
                """ % (dbinfo.tb_name_suffix, filter_str)
        cursor.execute(sql)        
        rows = cursor.fetchall()
        return rows[0][0]





def print_summary():
    with psycopg2.connect(dbinfo.connect_str) as conn:
        filter_str = "TRUE"
        n_labeled_clone_groups = n_cln_clsss(filter_str)         
        n_labeled_clone_elements = n_cln_insts(filter_str)        
        n_labeled_answers = n_answer_posts(filter_str)
        n_labeled_questions = n_question_posts(filter_str)
        
        print("We labeled:")
        print("%d clone groups" % n_labeled_clone_groups)
        print("%d clone elements" % n_labeled_clone_elements)
        print("%d answer posts" % n_labeled_answers)
        print("%d question posts" % n_labeled_questions)
        # print("security: 0=irrelavant, 1=I don't know, 2=secure, 3=somewhat secure, 4=insecure, ,5=mixing,  6=context, 7=no impact")
        # print("category: 1=SSL, 2=symmetric, 3=asymmetric, 4=hash, 5=random")


def print_accepted_rate():
    n_labeled_answers_accepted = n_answer_posts(is_accepted_answer())
    n_secure_answers = n_answer_posts(is_secure_answer())
    n_secure_answers_accepted = n_answer_posts(is_secure_answer()+ ' AND ' + is_accepted_answer())
    n_insecure_answers = n_answer_posts(is_insecure_answer())
    n_insecure_answers_accepted = n_answer_posts(is_insecure_answer()+ ' AND '+ is_accepted_answer())
    n_labeled_answers = n_secure_answers + n_insecure_answers
    p_secure = round(n_secure_answers*100./n_labeled_answers)
    p_insecure = 100 - p_secure
    p_secure_accepted = round(n_secure_answers_accepted*100./n_secure_answers)
    p_insecure_accepted = round(n_insecure_answers_accepted*100./n_insecure_answers)
    print("total =  secure  + insecure")
    print(" %d  = %d (%d%%) + %d (%d%%)"
         % (n_labeled_answers, n_secure_answers, p_secure,
                            n_insecure_answers, p_insecure))
    print("secure =  accepted  + not accepted")
    print(" %d  = %d (%d%%) + %d (%d%%)"
         % (n_secure_answers, n_secure_answers_accepted, p_secure_accepted,
            n_secure_answers-n_secure_answers_accepted, 100-p_secure_accepted))
    print("insecure =  accepted  + not accepted")
    print(" %d  = %d (%d%%) + %d (%d%%)"
         % (n_insecure_answers, n_insecure_answers_accepted, p_insecure_accepted,
            n_insecure_answers-n_insecure_answers_accepted, 100-p_insecure_accepted))


def test():
    n_secure_clnclss = len(cln_clss_sizes(is_secure_clnclss()))
    n_insecure_clnclss = len(cln_clss_sizes(is_insecure_clnclss()))
    n_mixed_clnclss = len(cln_clss_sizes(is_mixed_clnclss()))
    n_relevant_clnclss = len(cln_clss_sizes("NOT "+is_irrelevant_clnclss()))
    assert(n_secure_clnclss+n_insecure_clnclss+n_mixed_clnclss == n_relevant_clnclss)
    assert(n_secure_clnclss == n_cln_clsss(is_secure_clnclss()))
    assert(n_insecure_clnclss == n_cln_clsss(is_insecure_clnclss()))
    assert(n_mixed_clnclss == n_cln_clsss(is_mixed_clnclss()))
    assert(n_relevant_clnclss == n_cln_clsss("NOT "+is_irrelevant_clnclss()))


    assert(n_answer_posts(is_relevant_answer()) == 
            n_answer_posts(is_secure_answer()) + 
            n_answer_posts(is_insecure_answer()))

    print("All tests passed!")

def write_rpd(rpd):


    rpd['CCFinderGroups'] = n_cln_clsss('TRUE')
    rpd['RelevantGroups'] = n_cln_clsss(is_relevant_clnclss())
    rpd['IrrelevantGroups'] = n_cln_clsss(is_irrelevant_clnclss())
    assert(rpd['CCFinderGroups'] == rpd['RelevantGroups'] + rpd['IrrelevantGroups'])
    rpd['SecureGroups'] = n_cln_clsss(is_secure_clnclss())
    rpd['InsecureGroups'] = n_cln_clsss(is_insecure_clnclss())
    rpd['MixedGroups'] = n_cln_clsss(is_mixed_clnclss())
    assert(rpd['SecureGroups']+rpd['InsecureGroups']+rpd['MixedGroups']
        == rpd['RelevantGroups'])


    # instances 
    rpd['RelevantInstances'] = n_cln_insts(is_relevant_clninst())
    rpd['SecureInstances'] = n_cln_insts(is_secure_clninst())
    rpd['InsecureInstances'] = n_cln_insts(is_insecure_clninst())
    rpd['IrrelevantInstances'] = n_cln_insts(is_irrelevant_clninst())
    assert(rpd['RelevantInstances'] == rpd['SecureInstances'] + rpd['InsecureInstances'])
    rpd['AllInstances'] = n_cln_insts('TRUE')
    assert(rpd['AllInstances'] == rpd['RelevantInstances']+rpd['IrrelevantInstances'])

    # answers
    rpd['SecureAnswers'] = n_answer_posts(is_secure_answer())
    rpd['InsecureAnswers'] = n_answer_posts(is_insecure_answer())
    rpd['RelevantAnswers'] = n_answer_posts(is_relevant_answer())
    assert(rpd['SecureAnswers']+rpd['InsecureAnswers'] == rpd['RelevantAnswers'])
    rpd['IrrelevantAnswers'] = n_answer_posts(is_irrelevant_answer())
    rpd['RelevantAnswersSSL'] = n_answer_posts(is_relevant_answer()+ ' AND '
                                                    +is_answer_cat(1))
    rpd['SecureAnswersSSL'] = n_answer_posts(is_secure_answer()+' AND '
                                                            +is_answer_cat(1))
    rpd['InsecureAnswersSSL'] = n_answer_posts(is_insecure_answer()+' AND '
                                                                +is_answer_cat(1))
    rpd['SecureAnswersRandom'] = n_answer_posts(is_secure_answer()+' AND '
                                                            +is_answer_cat(5))
    rpd['InsecureAnswersRandom'] = n_answer_posts(is_insecure_answer()+' AND '
                                                                +is_answer_cat(5))

    rpd['SumCategoryAnswers'] = sum([n_answer_posts(is_answer_cat(i)) for i in range(1,6)])



    rpd['AverViewsSecureAnswers'] = attrs('questions.viewcount', 
                                                            is_secure_answer()
                                                        )['questions.viewcount'].mean().round()
    rpd['AverViewsInsecureAnswers'] = attrs('questions.viewcount', 
                                                            is_insecure_answer()
                                                        )['questions.viewcount'].mean().round()

    rpd['AverScoreSecureAnswers'] = attrs('answers.score',
                                                            is_secure_answer()
                                                        )['answers.score'].mean().round()
    rpd['AverScoreInsecureAnswers'] = attrs('answers.score',
                                                            is_insecure_answer()
                                                        )['answers.score'].mean().round()

    rpd['AverCommentsSecureAnswers'] = attrs('answers.commentcount', 
                                                            is_secure_answer()
                                                        )['answers.commentcount'].mean().round()
    rpd['AverCommentsInsecureAnswers'] = attrs('answers.commentcount', 
                                                            is_insecure_answer()
                                                        )['answers.commentcount'].mean().round()

    rpd['AverReputationSecureAnswers'] = attrs('users.reputation', 
                                                            is_secure_answer()
                                                        )['users.reputation'].mean().round()
    rpd['AverReputationInsecureAnswers'] = attrs('users.reputation', 
                                                            is_insecure_answer()
                                                        )['users.reputation'].mean().round()


    rpd['AverFavoritesSecureAnswers'] = attrs('questions.favoritecount', 
                                                            is_secure_answer()
                                                        )['questions.favoritecount'].fillna(0).mean().round()
    rpd['AverFavoritesInsecureAnswers'] = attrs('questions.favoritecount', 
                                                            is_insecure_answer()
                                                        )['questions.favoritecount'].fillna(0).mean().round()


    # users
    rpd['CoveredUsers'] = n_auids(is_relevant_answer())
    rpd['SecureUsers'] = n_auids(is_secure_answer())
    rpd['InsecureUsers'] = n_auids(is_insecure_answer())
    reputations = attrs('users.reputation', 
            is_relevant_answer(), dist='users.id')['users.reputation']
    rpd['HighestReputation'] = reputations.max()
    rpd['LowestReputation'] = reputations.min()

    rpd['RelevantAnswersByTrustedUsers'] = len(attrs('answers.id', is_relevant_answer()+
              " AND users.reputation>=20000"))
    rpd['SecureAnswersByTrustedUsers'] = len(attrs('answers.id', is_secure_answer()+
              " AND users.reputation>=20000"))
    rpd['InsecureAnswersByTrustedUsers'] = len(attrs('answers.id', is_insecure_answer()+
              " AND users.reputation>=20000"))
    assert(rpd['RelevantAnswersByTrustedUsers'] ==
            rpd['SecureAnswersByTrustedUsers'] + rpd['InsecureAnswersByTrustedUsers'])


    rpd['AcceptedSecureAnswers'] = n_answer_posts(is_secure_answer()+" AND "
                                                            +is_accepted_answer())

    rpd['AcceptedInsecureAnswers'] = n_answer_posts(is_insecure_answer()+" AND "
                                                            +is_accepted_answer())

    rpd['AcceptedRelevantAnswers'] = n_answer_posts(is_relevant_answer()+" AND "
                                                            +is_accepted_answer())

    rpd['HighReputationInsecureAnswers'] = len(attrs('users.reputation', is_insecure_answer()
                        +' AND users.reputation >=20000 AND users.reputation < 40000')['users.reputation'])
    rpd['HighReputationRelevantAnswers'] = len(attrs('users.reputation', is_relevant_answer()
                        +' AND users.reputation >=20000 AND users.reputation < 40000')['users.reputation'])

    rpd['UsersFirstFiveK'] = len(attrs('users.reputation', is_relevant_answer()
                        +' AND users.reputation >=20000 AND users.reputation < 40000')['users.reputation'])



    return rpd

