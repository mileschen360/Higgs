'''use this module to view and label clone instances and clone groups'''
import psycopg2
import IPython.display

from . import dbinfo
from . import labeled

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

def print_clninsts(filter_str):
    with psycopg2.connect(dbinfo.connect_str) as conn:
        cursor = conn.cursor()
        sql = """
        SELECT postid, indx, tbegin, tend, slabel, scategory, reason, comment, aslabel
        FROM labels
        WHERE %s
        """ % (filter_str)
        cursor.execute(sql)
        rows = cursor.fetchall()
        for row in rows:
            print("<<<<<<\n\n>>>>>>>>>>>>>")
            sql = """SELECT code0
                    FROM clone_pairs_%s
                    WHERE postid0=%d
                    AND indx0=%d
                    AND tbegin0=%d
                    AND tend0=%d
                    ;
            """ % (dbinfo.tb_name_suffix,
                    row[0], row[1],
                    row[2], row[3])
            cursor.execute(sql)
            single_rows = cursor.fetchall()
            codeprint(single_rows[0][0])
            print(row[4:])



class ClnClss:
    '''Clone class'''
    def __init__(self, cid, tb_name_suffix='all', url_only=False):
        self.cid = cid
        self.tb_name_suffix = tb_name_suffix
        self.url_only = url_only

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
                print(code0_url, "   %d: %d-%d" % (row[2], row[3], row[4]))
                print("")
                if not self.url_only:
                    codeprint(row[0])

                sql = """SELECT slabel, scategory, reason, comment, gslabel
                        FROM labels
                        WHERE postid=%d
                        AND indx=%d
                        AND tbegin=%d
                        AND tend=%d
                        ;
                        """ % (row[1], row[2], row[3], row[4])
                cursor.execute(sql)
                tmp_single_rows = cursor.fetchall()
                print(pretty(tmp_single_rows[0]))

        return "" 

def pretty(label_tuple):
    if len(label_tuple) <= 5:
        if label_tuple[1]>=0 and label_tuple[1]<=5:
            cat_name = labeled.cat_names_1b[label_tuple[1]]
        else:
            cat_name = str(label_tuple[1])
        repr_str = "%d, %s, %s, %s" % (
            label_tuple[0], cat_name, label_tuple[2], label_tuple[3])
    if len(label_tuple) == 5:
        repr_str += ", %d" % (label_tuple[4])
    return "("+repr_str+")"
            
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



def label_ui_for_mixed(cids, view_only=False, i_grp_range=None):
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
            n_rows_labeled = 0
            while n_rows_labeled < n_rows:
                in_strs = input("instances to be labeled:").split(',')
                rows_1based = [int(row.strip()) for row in in_strs]
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
                for r_1based in rows_1based:
                    row = rows[r_1based-1]
                    label_sql = """INSERT INTO labels(postid, indx, tbegin, tend, slabel, scategory, reason, comment)
    VALUES(%d, %d, %d, %d, %d, %d, '%s', '%s') ON CONFLICT(postid, indx, tbegin, tend) DO UPDATE
    SET (postid, indx, tbegin, tend, slabel_c, scategory, reason, comment)
    =   (%d, %d, %d, %d, %d, 10*labels.scategory+%d, '%s', '%s');""" % (row[1], row[2], row[3], row[4], slabel, scategory, reason, comment,
                                                row[1], row[2], row[3], row[4], slabel, scategory, reason, comment)
                    cursor.execute(label_sql)
                n_rows_labeled += len(rows_1based)



def import_from_combined_view(tb_name_suffix, connect_str):
    slabel_postid = np.loadtxt("combined_view.csv", delimiter=',', skiprows=1)
    cids = set()
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        for i in range(len(slabel_postid)):
            slabel = slabel_postid[i][0]
            postid = slabel_postid[i][1]   
            sql = """SELECT DISTINCT cid FROM clone_pairs_%s WHERE postid0=%d""" % (tb_name_suffix, postid)
            print(sql)
            cursor.execute(sql)
            rows = cursor.fetchall()
            if len(rows) == 1:
                cid = rows[0][0]
                cids.add(cid)
                #label_sql = """INSERT INTO labels_%s(cid, slabel) VALUES(%d, %d) ON CONFLICT(cid) DO UPDATE
#SET (cid, slabel) = (%d, %d);""" % (tb_name_suffix, cid, slabel, cid, slabel)
                #cursor.execute(label_sql)
            elif len(rows) >1:
                print("\n\n\n**************************")
                print("postid %d has %d clone groups" % (postid, len(rows)))
                print("please help to label the following:")
                cids = [row[0] for row in rows]
                label_ui(tb_name_suffix, cids, connect_str)
    print("%d snippets in combined view, they belong to %d clones groups in clone_pairs" 
           % (len(slabel_postid), len(cids)))