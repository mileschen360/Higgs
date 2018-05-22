from . import dbinfo
import psycopg2
import IPython.display

reload(dbinfo)

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
            