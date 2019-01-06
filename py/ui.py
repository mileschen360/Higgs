def print_clninst(tb_name_suffix, postid, indx, tbegin, tend, show_label=False):
    '''print clone instance'''
    with psycopg2.connect(connect_str) as conn:
        cursor = conn.cursor()
        sql = """SELECT code0
FROM clone_pairs_%s
WHERE postid0=%d
  AND indx0=%d
  AND tbegin0=%d
  AND tend0=%d
;
""" % (tb_name_suffix, postid, indx, tbegin, tend)
        cursor.execute(sql)
        rows = cursor.fetchall()
        codeprint(rows[0][0])
        if show_label:
            sql = """SELECT slabel, scategory, reason, comment
FROM labels
WHERE postid=%d
  AND indx=%d
  AND tbegin=%d
  AND tend=%d
;
""" % (postid, indx, tbegin, tend)
            cursor.execute(sql)
            rows = cursor.fetchall()
            print(rows[0])