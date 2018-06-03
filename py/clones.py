'''use this module to view and label clone instances and clone groups'''
import psycopg2
import IPython.display

from . import dbinfo

import time
import importlib
importlib.reload(dbinfo)

def codeprint(code):
    IPython.display.display(IPython.display.Markdown("""
```java
%s
```
""" % (code)))

class ClnInst:
    '''Clone Instance'''
    def __init__(self, postid, indx, tbegin, tend, tb_name_suffix='all'):
        self.postid = postid
        self.indx = indx
        self.tbegin = tbegin
        self.tend = tend
        self.tb_name_suffix = tb_name_suffix

    def __repr__(self):
        with psycopg2.connect(dbinfo.connect_str) as conn:
            cursor = conn.cursor()
            sql = """SELECT code0
                    FROM clone_pairs_%s
                    WHERE postid0=%d
                    AND indx0=%d
                    AND tbegin0=%d
                    AND tend0=%d
                    ;
            """ % (self.tb_name_suffix, 
                    self.postid, self.indx,
                    self.tbegin, self.tend)
            cursor.execute(sql)
            rows = cursor.fetchall()
            code = rows[0][0]
            codeprint(code)
            sql = """SELECT slabel, scategory, reason, comment
                    FROM labels
                    WHERE postid=%d
                    AND indx=%d
                    AND tbegin=%d
                    AND tend=%d
                    ;
                    """ % (self.postid, self.indx, self.tbegin, self.tend)
            cursor.execute(sql)
            rows = cursor.fetchall()
            print(rows[0])
            return ""

class ClnClss:
    '''Clone class'''
    def __init__(self, cid, tb_name_suffix='all'):
        self.cid = cid
        self.tb_name_suffix = tb_name_suffix

    def __repr__(self):
        url_prefix = "http://stackoverflow.com/questions/"
        with psycopg2.connect(dbinfo.connect_str) as conn:
            cursor = conn.cursor()
            sql = """SELECT DISTINCT code0,postid0,indx0,tbegin0,tend0
                     FROM clone_pairs_%s WHERE cid=%d""" % (
                   self.tb_name_suffix, self.cid)
            cursor.execute(sql)
            rows = cursor.fetchall()
            n_rows = len(rows)
            print("Clone group cid: %d, containing %d clone elements=========" % (
                       self.cid, n_rows))
            for i in range(n_rows):
                print("----%d/%d----" % (i+1, n_rows))
                row = rows[i]
                code0_url = url_prefix+str(row[1])
                print(code0_url)
                print("")
                codeprint(row[0])

            sql = """SELECT slabel, scategory, reason, comment
                    FROM labels
                    WHERE postid=%d
                    AND indx=%d
                    AND tbegin=%d
                    AND tend=%d
                    ;
                    """ % (rows[-1][1], rows[-1][2], rows[-1][3], rows[-1][4])
            cursor.execute(sql)
            rows = cursor.fetchall()
            print(rows[0])

        return "" 
            
def get_cids(filterstr):
    """use labels.postid IS NULL to filter out already labeled snippets,
    Example filterstr: p.code0 LIKE '%.setSeed%'"""
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        #sql = """SELECT DISTINCT cid FROM clone_pairs_%s %s ORDER BY cid""" % (tb_name_suffix, filterstr)
        #filterstr = 'AND '+filterstr if filterstr != '' else ''
        sql = """SELECT DISTINCT p.cid
FROM clone_pairs_%s p
LEFT JOIN labels
ON labels.postid  = p.postid0
  AND labels.indx = p.indx0
  AND labels.tbegin = p.tbegin0
  AND labels.tend = p.tend0
WHERE 
  %s
ORDER BY p.cid;""" % (dbinfo.tb_name_suffix, filterstr)
        cursor.execute(sql)
        rows = cursor.fetchall()
        cids = [row[0] for row in rows]
        return cids


def label_ui(cids, view_only=False, i_grp_range=None):
    '''use this function to manually label '''
    print("start labeling %d clone groups" % (len(cids)))
    url_prefix = "http://stackoverflow.com/questions/"
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        if i_grp_range is None or view_only == False:
            i_grp_range = range(len(cids))
        for i_grp in i_grp_range:
            cid = cids[i_grp]
#             check_sql = """select exists(select 1 from labels_%s where cid=%d);""" % (tb_name_suffix, cid)
#             cursor.execute(check_sql)
#             rows = cursor.fetchall()
#             already_labelled = rows[0][0]
#             if already_labelled:
#                 retrieve_sql = """select slabel from labels_%s where cid=%d;""" % (tb_name_suffix, cid)
#                 cursor.execute(retrieve_sql)
#                 rows = cursor.fetchall()
#                 existing_label = rows[0][0]

            sql = """SELECT DISTINCT code0,postid0,indx0,tbegin0,tend0 FROM clone_pairs_%s WHERE cid=%d""" % (
                   dbinfo.tb_name_suffix, cid)
            cursor.execute(sql)
            rows = cursor.fetchall()
            n_rows = len(rows)
            print("\n====%d/%d====Clone group cid: %d, containing %d clone elements=========" % (
                       i_grp, len(cids), cid, n_rows))
            for i in range(n_rows):
                print("----%d/%d----" % (i+1, n_rows))
                row = rows[i]
                code0_url = url_prefix+str(row[1])
                print(code0_url)
                print("")
                codeprint(row[0])
            if view_only:
                continue
            print("vvvvvvvvvvvvv--INPUT--vvvvvvvvvvvvvv")
            time.sleep(1)
#             if already_labelled:
#                 slabel = int(input("existing label (%d)" % existing_label))
#             else:
            in_strs = input("slabel; scategory; reason; comment:").split(';')
            if in_strs[0] == 'stop':
                return
            if in_strs[0] == 'commit':
                conn.commit()
                in_strs = input("slabel; scategory; reason; comment:").split(';')
            slabel = int(in_strs[0])
            if len(in_strs) >= 2 and len(in_strs[1].strip()) > 0:
                scategory = int(in_strs[1].strip())
            else:
                scategory = -1;
            if len(in_strs) >= 3:
                reason = in_strs[2].strip()
            else:
                reason = ''
            if len(in_strs) >= 4:
                comment = in_strs[3].strip()
            else:
                comment = ''
            print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^")
            for row in rows:
                label_sql = """INSERT INTO labels(postid, indx, tbegin, tend, slabel, scategory, reason, comment)
VALUES(%d, %d, %d, %d, %d, %d, '%s', '%s') ON CONFLICT(postid, indx, tbegin, tend) DO UPDATE
SET (postid, indx, tbegin, tend, slabel_c, scategory, reason, comment)
=   (%d, %d, %d, %d, %d, 10*labels.scategory+%d, '%s', '%s');""" % (row[1], row[2], row[3], row[4], slabel, scategory, reason, comment,
                                            row[1], row[2], row[3], row[4], slabel, scategory, reason, comment)
                cursor.execute(label_sql)