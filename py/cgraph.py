"""clone graph stored to neo4j"""
from . import dbinfo
import importlib
import psycopg2

importlib.reload(dbinfo)

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
        rows = get_unique_postids(dbinfo.tb_name_suffix, dbinfo.connect_str, filter_str)
        for row in rows:
            tx.run("CREATE (:Answer {id:%d})" % row[0])
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
    
def get_unique_postids(tb_name_suffix, connect_str, filter_str="TRUE"):
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT DISTINCT p.postid0
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