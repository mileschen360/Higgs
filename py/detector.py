'''use this module to run CCFinder and dump result to SQL database
'''

connect_str = "dbname='stackoverflow' user='extractor' host='localhost' " + \
              "password='extractor'"


def codeprint(code):
    display(Markdown("""
```java
%s
```
""" % (code)))
    
def export_as_java(start_id, end_id, snippet_dpath):
    '''export from database to java files for id within range [start_id, end_id)'''
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        cursor.execute("""SELECT postid, indx, code FROM snippets WHERE id >= %d AND id < %d;""" % (start_id, end_id))
        rows = cursor.fetchall()
        for row in rows:
            fname = '%d_%d.java' % (row[0], row[1])
            if not os.path.isdir(snippet_dpath):
                os.makedirs(snippet_dpath)
            fpath = os.path.join(snippet_dpath,fname)
            with open(fpath, 'w') as f:
                f.write(row[-1])
        print(len(rows), "snippets exported to", snippet_dpath)




########### process .ccfxd file
def get_prepro_suffix(ccfxd_txt_fpath):
    with open(ccfxd_txt_fpath) as f:
        for line in f:
            if "option: -preprocessed_file_postfix" in line:
                return line.split()[-1]

def sql_4ccfxd_file_block(line, tb_name, fid_postid):
    '''prepare SQL for ccfxd file block'''
    file_info = line.split()
    fid = file_info[0]
    fname = "".join(file_info[1].split('/')[-1].split('.')[:-1])
    postid, indx = fname.split('_')
    fid_postid[fid] = (postid, indx) 
    length = file_info[2]
    sql = """INSERT INTO %s(fid, postid, indx, length)
VALUES(%s, %s, %s, %s) ON CONFLICT DO NOTHING;""" % (tb_name,
    fid, postid, indx, length)
    return sql

def sql_4ccfxd_pair_block(line, tb_name, fid_postid):
    '''prepare SQL for ccfxd clone pair block'''
    clone_pair = line.split()
    pid = ":".join(clone_pair[1:])  # unique pair id
    cid = clone_pair[0]
    clone0 = clone_pair[1].split(".")
    fid0 = clone0[0]
    tmp = clone0[1].split("-")
    tbegin0 = tmp[0]
    tend0 = tmp[1]
    clone1 = clone_pair[2].split(".")
    fid1 = clone1[0]
    tmp = clone1[1].split("-")
    tbegin1 = tmp[0]
    tend1 = tmp[1]
    sql = """INSERT INTO %s(pid, cid,
postid0, indx0, tbegin0, tend0,
postid1, indx1, tbegin1, tend1
) VALUES ($aesc6$%s$aesc6$, %s,
%s, %s, %s, %s,
%s, %s, %s, %s) ON CONFLICT DO NOTHING;""" % (tb_name, pid, cid,
    fid_postid[fid0][0], fid_postid[fid0][1], tbegin0, tend0,
    fid_postid[fid1][0], fid_postid[fid1][1], tbegin1, tend1)
    return sql

def ccfxd2db(ccfxd_txt_fpath, connect_str):
    '''dump .ccfxd file to postgresql database'''
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        tb_name_suffix = "".join(os.path.basename(ccfxd_txt_fpath).split(".")[:-1])
        file_tb_name = "source_files_%s" % (tb_name_suffix)
        pair_tb_name = "clone_pairs_%s" % (tb_name_suffix)
        sql_create = """CREATE TABLE IF NOT EXISTS %s(
fid INT,
postid INT,
indx INT,
length INT,
PRIMARY KEY (fid),
FOREIGN KEY (postid, indx) REFERENCES snippets(postid, indx)
);
""" % (file_tb_name)
        cursor.execute(sql_create)
        
        sql_create = """CREATE TABLE IF NOT EXISTS %s(
pid VARCHAR(64), 
cid INT,
postid0 INT,
indx0 INT,
tbegin0 INT,
tend0 INT,
lbegin0 INT,
lend0 INT,
code0 VARCHAR(24060),
postid1 INT,
indx1 INT,
tbegin1 INT,
tend1 INT,
lbegin1 INT,
lend1 INT,
code1 VARCHAR(24060),
PRIMARY KEY (pid)
); 
""" % (pair_tb_name)
        cursor.execute(sql_create)
                
        fid_postid = {}
        with open(ccfxd_txt_fpath) as f:
            parsing_fid = False
            parsing_cid = False
            for line in f:
                if line == 'source_files {\n':
                    parsing_fid = True
                    continue
                if parsing_fid:
                    if line == '}\n':
                        parsing_fid = False
                        continue
                    sql = sql_4ccfxd_file_block(line, file_tb_name, fid_postid)
                    cursor.execute(sql)

                if line == 'clone_pairs {\n':
                    parsing_cid = True
                    continue
                if parsing_cid:
                    if line == '}\n':
                        parsing_cid = False
                        continue
                    sql = sql_4ccfxd_pair_block(line, pair_tb_name, fid_postid)
                    cursor.execute(sql)
    print("Done:", ccfxd_txt_fpath,"written to", file_tb_name, "and", pair_tb_name)


########## process intermediate file

def itoken2iline(itokens, prepro_fpath):
    '''convert token index to line index, both are 0 based'''
    ilines = []
    with open(prepro_fpath) as f:
        for i, line in enumerate(f):
            if i in itokens:
                iline = int(line.split('.')[0],16)-1
                ilines.append(iline)
    return itokens.__class__(ilines)



def update_clone_pair_db(snippet_dpath, connect_str,
                         prepro_suffix='.java.2_0_0_0.default.ccfxprep',
                         prepro_dname = '.ccfxprepdir/'):
    with psycopg2.connect(connect_str) as conn:
        tb_name_suffix = os.path.basename(snippet_dpath)
        file_tb_name = "source_files_%s" % (tb_name_suffix)
        pair_tb_name = "clone_pairs_%s" % (tb_name_suffix)
        cursor = conn.cursor()
    #   1       2        3         4           5
        sql = """SELECT p.pid,
p.postid0, p.indx0, s0.code, p.tbegin0, p.tend0,
p.postid1, p.indx1, s1.code, p.tbegin1, p.tend1
FROM %s p
INNER JOIN snippets s0
  ON p.postid0 = s0.postid AND p.indx0 = s0.indx
INNER JOIN snippets s1
  ON p.postid1 = s1.postid AND p.indx1 = s1.indx
;""" % (pair_tb_name)
        cursor.execute(sql)
        rows = cursor.fetchall()
        for row in rows:
            snippet0_fname = "%d_%d.java" % (row[1], row[2])
            snippet1_fname = "%d_%d.java" % (row[6], row[7])
            snippet0_url = "http://stackoverflow.com/questions/%d" % (row[1])
            snippet1_url = "http://stackoverflow.com/questions/%d" % (row[6])
            snippet0_fpath = os.path.join(snippet_dpath, snippet0_fname)
            snippet1_fpath = os.path.join(snippet_dpath, snippet1_fname)
            prepro0_fpath = os.path.join(snippet_dpath, prepro_dname, snippet0_fname+prepro_suffix)
            prepro1_fpath = os.path.join(snippet_dpath, prepro_dname, snippet1_fname+prepro_suffix)
            snippet0_trange = row[4:6]
            snippet1_trange = row[9:11]
            if not (os.path.exists(prepro0_fpath) and os.path.exists(prepro1_fpath)):
                continue
            snippet0_lrange = itoken2iline(snippet0_trange, prepro0_fpath)
            snippet1_lrange = itoken2iline(snippet1_trange, prepro1_fpath)
            code0 = row[3].split('\n')
            code1 = row[8].split('\n')
            update_sql = """INSERT INTO %s
(pid, lbegin0, lend0, code0, lbegin1, lend1, code1)
VALUES
($aesc6$%s$aesc6$, %d, %d, $aesc6$%s$aesc6$, %d, %d, $aesc6$%s$aesc6$) ON CONFLICT(pid) DO UPDATE
SET
(lbegin0, lend0, code0, lbegin1, lend1, code1) = 
(%d, %d, $aesc6$%s$aesc6$, %d, %d, $aesc6$%s$aesc6$);
""" % (pair_tb_name, row[0],
       snippet0_lrange[0], snippet0_lrange[1], "\n".join(code0[snippet0_lrange[0]:snippet0_lrange[1]]),
       snippet1_lrange[0], snippet1_lrange[1], "\n".join(code1[snippet1_lrange[0]:snippet1_lrange[1]]),
       snippet0_lrange[0], snippet0_lrange[1], "\n".join(code0[snippet0_lrange[0]:snippet0_lrange[1]]),
       snippet1_lrange[0], snippet1_lrange[1], "\n".join(code1[snippet1_lrange[0]:snippet1_lrange[1]]))
            #print(update_sql)
            cursor.execute(update_sql)
            #print("========================")
            #print(snippet0_url)
            #codeprint("\n".join(code0[snippet0_lrange[0]:snippet0_lrange[1]]))
            #print("-----------")
            #print(snippet1_url)
            #codeprint("\n".join(code0[snippet1_lrange[0]:snippet1_lrange[1]]))


def call_ccfinder(opt, snippet_dpath, ccfxd_fpath, ccfxd_txt_fpath):
    cmds = [
        "cd ~/code/ccfinderx-core/ccfx",
        "ccfx d java %s -dn %s -o %s" % (opt, snippet_dpath, ccfxd_fpath),
        "ccfx p %s > %s" % (ccfxd_fpath, ccfxd_txt_fpath)
    ] 
    cmd = " && ".join(cmds)
    print(cmd)
    print("...", end='')
    os.system(cmd)
    print("Done")

            
def detect_clones(snippet_dname, ccf_opt, snippet_id_range, connect_str):
    '''snippet_dname must be unique'''
    script_workdir = os.getcwd()
    snippet_dpath = os.path.join(script_workdir, snippet_dname)
    ccfxd_fpath = os.path.join(script_workdir, os.path.basename(snippet_dpath) + ".ccfxd")
    ccfxd_txt_fpath = os.path.join(script_workdir, os.path.basename(snippet_dpath) + ".txt")
    export_as_java(snippet_id_range[0], snippet_id_range[1], snippet_dpath=snippet_dpath)
    call_ccfinder(opt=ccf_opt, snippet_dpath=snippet_dpath, ccfxd_fpath=ccfxd_fpath, 
                  ccfxd_txt_fpath=ccfxd_txt_fpath)
    ccfxd2db(ccfxd_txt_fpath, connect_str=connect_str)
    update_clone_pair_db(snippet_dpath=snippet_dpath, connect_str=connect_str)


def rand_pair_check(n_rows, tb_name_suffix, connect_str, export=False):
    with psycopg2.connect(connect_str) as conn:
        file_tb_name = "source_files_%s" % (tb_name_suffix)
        pair_tb_name = "clone_pairs_%s" % (tb_name_suffix)
        cursor = conn.cursor()
        sql = """SELECT f0.postid, p.code0, f1.postid, p.code1
FROM %s p
INNER JOIN %s f0
  ON p.fid0 = f0.fid
INNER JOIN snippets s0
  ON f0.postid = s0.postid
INNER JOIN snippets ss0
  ON f0.indx = ss0.indx
INNER JOIN %s f1
  ON p.fid1 = f1.fid
INNER JOIN snippets s1
  ON f1.postid = s1.postid
INNER JOIN snippets ss1
  ON f1.indx = ss1.indx
WHERE p.fid0 = f0.fid
  AND p.fid1 = f1.fid
  AND f0.postid = s0.postid
  AND f0.indx = ss0.indx
  AND s0.postid = ss0.postid
  AND s0.indx = ss0.indx
  AND f1.postid = s1.postid
  AND f1.indx = ss1.indx
  AND s1.postid = ss1.postid
  AND s1.indx = ss1.indx
  AND (p.fid0 < p.fid1
       OR (p.fid0 = p.fid1 AND p.tbegin0 < p.tbegin1))
ORDER BY random()
LIMIT %d
;""" % (pair_tb_name, file_tb_name, file_tb_name, n_rows)
        cursor.execute(sql)
        rows = cursor.fetchall()
        url_prefix = "http://stackoverflow.com/questions/"
        for row in rows:
            print("=================")
            code0_url = url_prefix+str(row[0])
            print(code0_url)
            codeprint(row[1])
            print("-------")
            code1_url = url_prefix+str(row[2])
            print(code1_url)
            codeprint(row[3])



def specific_group_check(cid, tb_name_suffix, connect_str, dry=False):
    with psycopg2.connect(connect_str) as conn:
        file_tb_name = "source_files_%s" % (tb_name_suffix)
        pair_tb_name = "clone_pairs_%s" % (tb_name_suffix)
        cursor = conn.cursor()
        sql = """SELECT DISTINCT s.postid, p.code0, p.lbegin0, p.lend0, ppo.title, ppo.tags
FROM %s p
INNER JOIN %s f
  ON p.fid0 = f.fid
INNER JOIN snippets s
  ON f.postid = s.postid
INNER JOIN snippets ss
  ON f.indx = ss.indx
INNER JOIN posts po
  ON f.postid = po.id
INNER JOIN posts ppo
  ON po.parentid = ppo.id
WHERE p.fid0 = f.fid
  AND f.postid = s.postid
  AND f.indx = ss.indx
  AND s.postid = ss.postid
  AND s.indx = ss.indx
  AND f.postid = po.id
  AND cid=%d
;""" % (pair_tb_name, file_tb_name, cid)
        if dry:
            print(sql)
            return
        cursor.execute(sql)
        rows = cursor.fetchall()
        url_prefix = "http://stackoverflow.com/questions/"
        n_rows = len(rows)
        print("========Clone group cid: %d, containing %d clone elements=========" % (cid, n_rows))
        for i in range(n_rows):
            print("----%d/%d----" % (i, n_rows))
            row = rows[i]
            print(row[4])
            print(row[5])
            code0_url = url_prefix+str(row[0])+"    Ln %d-%d" % (row[2], row[3])
            print(code0_url)
            codeprint(row[1])
            
def drop_tables(tb_name_suffix, connect_str):
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """DROP TABLE IF EXISTS clone_pairs_%s, source_files_%s;""" % (tb_name_suffix, tb_name_suffix)
        cursor.execute(sql)

def create_labels_table(connect_str):
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()    
        sql_create = """CREATE TABLE labels(
postid INT, indx INT, tbegin INT, tend INT, slabel INT, scategory INT, reason VARCHAR(128), comment VARCHAR(128),
PRIMARY KEY (postid, indx, tbegin, tend));        
"""
        cursor.execute(sql_create)