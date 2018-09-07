"""clone graph stored to neo4j"""
from . import dbinfo
from . import labeled

import importlib
import psycopg2

importlib.reload(dbinfo)
importlib.reload(labeled)

def clear_all():
    with dbinfo.driver.session() as session:
        session.run("MATCH ()-[r]->() DELETE r;")
        session.run("Match (n) DELETE n;")

def create_cid_nodes(filter_str="TRUE"):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        rows = get_unique_cids(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            tx.run("CREATE (:ClnClss {cid:%d})" % row[0])
        tx.commit()

def create_answer_nodes(filter_str="TRUE"):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        rows = get_unique_aids(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            cypher = """MATCH (q:Question {id:%d})
                        CREATE (q)-[:Has]->(:Answer {id:%d})
                    """ % (row[0], row[1])
            tx.run(cypher)
        tx.commit()


def create_question_nodes(filter_str="TRUE"):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        rows = get_unique_qids(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            tx.run("CREATE (:Question {id:%d})" % row[0])
        tx.commit()


def create_user_nodes(filter_str="TRUE"):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        rows = get_ans_usr(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            cypher = """MATCH (ans:Answer {id: %d})
                        MERGE (usr:User {id: %d})
                        ON CREATE SET usr.id = %d
                        CREATE UNIQUE (usr)-[:Offer]->(ans)
                     """ % (row[0], row[1], row[1])
            tx.run(cypher)

        rows = get_qst_usr(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            cypher = """MATCH (qst:Question {id: %d})
                        MERGE (usr:User {id: %d})
                        ON CREATE SET usr.id = %d
                        CREATE UNIQUE (usr)-[:Ask]->(qst)
                     """ % (row[0], row[1], row[1])
            tx.run(cypher)
        tx.commit()

def create_snippet_nodes(filter_str="TRUE"):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        rows = get_unique_snippets(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            cypher = """MATCH (ans:Answer {id:%d}) 
                        CREATE (ans)-[:Contains]->(:Snippet {indx:%d})
                     """ % (row[0], row[1])
            tx.run(cypher)
        tx.commit()        

def create_instance_nodes(filter_str="TRUE"):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        rows = get_unique_clone_instances(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            cypher = """MATCH (:Answer {id:%d})-[:Contains]->(snippet:Snippet {indx:%d}), (clss:ClnClss {cid:%d})
                        CREATE (snippet)-[:Provide]->(:ClnInst {tbegin:%d, tend:%d})-[:Form]->(clss)
                     """ % (row[1], row[2], row[0], row[3], row[4])
            tx.run(cypher)
        tx.commit()        

def label_instance_nodes(filter_str="TRUE"):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        rows = get_inst_labels()
        for row in rows:
            cypher = """MATCH (:Answer {id:%d})-[:Contains]->(snippet:Snippet {indx:%d})-->(inst:ClnInst {tbegin:%d, tend:%d})
                        SET inst.slabel = %d, inst.scategory = %d
                    """ % (row[0], row[1], row[2], row[3], row[4], row[5])
            tx.run(cypher)
        tx.commit()

def get_unique_cids(tb_name_suffix, connect_str, filter_str="TRUE"):
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT p.cid
                FROM labels
                INNER JOIN clone_pairs_%s p
                ON labels.postid  = p.postid0
                AND labels.indx = p.indx0
                AND labels.tbegin = p.tbegin0
                AND labels.tend = p.tend0
                WHERE %s
                ;
                """ % (tb_name_suffix, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        return rows
    
def get_unique_aids(tb_name_suffix=dbinfo.tb_name_suffix, connect_str=dbinfo.connect_str, filter_str="TRUE"):
    '''get all labeled answers'''
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT posts.parentid, posts.id
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
                """ % (tb_name_suffix, filter_str)
        cursor.execute(sql)        
        rows = cursor.fetchall()
        return rows

def get_qst_usr(tb_name_suffix=dbinfo.tb_name_suffix, connect_str=dbinfo.connect_str, filter_str="TRUE"):
    '''get all labeled answers'''
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT posts.id, posts.owneruserid
                FROM posts
                INNER JOIN
                (SELECT DISTINCT posts.parentid AS postid
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
                """ % (tb_name_suffix, filter_str)
        cursor.execute(sql)        
        rows = cursor.fetchall()
        replaced_rows = []
        n_none_uid = 0
        for aid, uid in rows:
            if uid is None:
                n_none_uid += 1
                #print(aid, "doesn't have owner")
            else:
                replaced_rows.append((aid, uid))
        print("there are", n_none_uid, "questions that don't have owner")
        return replaced_rows



def get_ans_usr(tb_name_suffix=dbinfo.tb_name_suffix, connect_str=dbinfo.connect_str, filter_str="TRUE"):
    '''get all labeled answers'''
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT posts.id, posts.owneruserid
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
                """ % (tb_name_suffix, filter_str)
        cursor.execute(sql)        
        rows = cursor.fetchall()
        replaced_rows = []
        n_none_uid = 0
        for aid, uid in rows:
            if uid is None:
                n_none_uid += 1
            else:
                replaced_rows.append((aid, uid))
        print("there are", n_none_uid, "answers that don't have owner")
        return replaced_rows

def get_inst_labels():
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT postid, indx, tbegin, tend, slabel, scategory
                FROM labels;"""
        cursor.execute(sql)
        rows = cursor.fetchall()
        return rows


def get_unique_qids(tb_name_suffix, connect_str, filter_str="TRUE"):
    '''get all labeled answers'''
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT posts.parentid
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
                """ % (tb_name_suffix, filter_str)
        cursor.execute(sql)        
        rows = cursor.fetchall()
        return rows


def get_unique_snippets(tb_name_suffix, connect_str, filter_str="TRUE"):
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT p.postid0, p.indx0
                FROM labels
                INNER JOIN clone_pairs_%s p
                ON labels.postid  = p.postid0
                AND labels.indx = p.indx0
                AND labels.tbegin = p.tbegin0
                AND labels.tend = p.tend0
                WHERE %s
                ;
                """ % (tb_name_suffix, filter_str)
        cursor.execute(sql)        
        rows = cursor.fetchall()
        return rows
    
       
def get_unique_clone_instances(tb_name_suffix, connect_str, filter_str="TRUE"):
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT p.cid, p.postid0, p.indx0, p.tbegin0, p.tend0
                FROM labels
                INNER JOIN clone_pairs_%s p
                ON labels.postid  = p.postid0
                AND labels.indx = p.indx0
                AND labels.tbegin = p.tbegin0
                AND labels.tend = p.tend0
                WHERE %s
                ;
                """ % (tb_name_suffix, filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        return rows


def n_cln_clsss(filter_str):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        n = tx.run("MATCH (:ClnClss) return count(*)").single().value()
        tx.commit()
    return n

def n_cln_insts(filter_str):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        n = tx.run("MATCH (:ClnInst) return count(*)").single().value()
        tx.commit()
    return n

def n_question_posts(filter_str):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        n = tx.run("MATCH (:Question) return count(*)").single().value()
        tx.commit()
    return n

def n_answer_posts(filter_str):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        n = tx.run("MATCH (:Answer) return count(*)").single().value()
        tx.commit()
    return n

def n_answerers(filter_str):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        n = tx.run("MATCH (usr:User)-[:Offer]->(:Answer) return COUNT(DISTINCT usr)").single().value()
        tx.commit()
    return n

def n_askers(filter_str):
    with dbinfo.driver.session() as session:
        tx = session.begin_transaction()
        n = tx.run("MATCH (usr:User)-[:Ask]->(:Question) return COUNT(DISTINCT usr)").single().value()
        tx.commit()
    return n


def test_n_summary():
    filter_str = "TRUE"
    assert(n_cln_clsss(filter_str) == labeled.n_cln_clsss(filter_str))
    assert(n_cln_insts(filter_str) == labeled.n_cln_insts(filter_str))
    assert(n_question_posts(filter_str) == labeled.n_question_posts(filter_str))
    assert(n_answer_posts(filter_str) == labeled.n_answer_posts(filter_str))
    assert(n_answerers(filter_str) == labeled.n_answerers(filter_str))
    assert(n_askers(filter_str) == labeled.n_askers(filter_str))
    labeled.print_summary()