from scipy import stats
import pandas as pd
from IPython.display import display, Markdown

import statistics
from collections import OrderedDict

from . import labeled


def mw(a, b, alternative, **kwds):
    '''peform the Mann-Whitney U test'''

    if alternative is None:
        raise Exception("alternative=None is not alowed")

    m = len(a)
    n = len(b)
    if min(m,n) < 20:
        #pass
        print("%% WARNING: scipy Mann-Whitney U test work only for sample size > 20")
    U_b, p = stats.mannwhitneyu(a, b, alternative=alternative, **kwds)
    U_a = m*n - U_b
    a_lt_b =  U_a > U_b
    U = min(U_a, U_b)
    d = 1 - 2.0*U/(m*n)
    return [p, d, a_lt_b]

def group_size_mw(**kwds):
    filter_str_secure = labeled.is_secure_clnclss()
    filter_str_insecure = labeled.is_insecure_clnclss()
    secure = labeled.cln_clss_sizes(filter_str_secure)
    insecure = labeled.cln_clss_sizes(filter_str_insecure)
    secure_mean = statistics.mean(secure)
    insecure_mean = statistics.mean(insecure)
    df = pd.DataFrame(index=['secure mean', 'insecure mean', 'p-value', "Cliff's d", 'result', ])
    pds = mw(secure, insecure, **kwds)
    if pds[-1]:
        pds[-1] = 'secure < insecure'
    df['group size'] = [secure_mean, insecure_mean] + pds
    res = df.transpose()
    return res    

def mw_table(filter_str='TRUE', **kwds):
    ''''''
    attr_name = OrderedDict([('answers.score', 'score'),
                 ('answers.commentcount', 'commentcount'),
                 ('users.reputation', 'reputation'),
                 ('questions.favoritecount', 'favoritecount'),                 
                 ('questions.viewcount', 'viewcount'),
                 #('users.views', 'profileviews'),
                 #('questions.answercount', 'answercount'),
                 #('users.age', 'age'),
                  ('answers.id', 'aid'),
                  #('answers.creationdate', 'time')
                 ])
    secure = "(%s) AND (%s) " % (filter_str, labeled.is_secure_answer())
    insecure = "(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer())

    df = pd.DataFrame(index=['p', 'd', 'secure < insecure', 'secure mean', 'insecure mean'])
    for attr in attr_name:
        secure_data = labeled.attrs(attr, secure).squeeze()
        insecure_data = labeled.attrs(attr, insecure).squeeze()
        if attr == 'questions.favoritecount':
            secure_data = secure_data.fillna(0)
            insecure_data = insecure_data.fillna(0)
        pds = mw(secure_data, insecure_data, **kwds)
        secure_mean = statistics.mean(secure_data)
        insecure_mean = statistics.mean(insecure_data)
        df[attr_name[attr]] = pds + [secure_mean, insecure_mean]
    return df.transpose()




def interp_d(d):
    if abs(d) < 0.147:
        effect = "negligible"
    elif abs(d) < 0.33:
        effect = "small"
    elif abs(d) < 0.474:
        effect = "medium"
    else:
        effect = "large"
    return effect

def pretty_d(d):
    effect = interp_d(d)
    return "%.2f (%s)" % (d, effect)

def pretty_p(p):
    if p < 0.01:
        return p
    else:
        return round(p,2)

def pretty_mean(m):
    if m > 1000:
        ndp = 0
    else:
        ndp = 1
    return round(m, ndp)

def friendly_mw_table(mw_tbl):
    df = pd.DataFrame()
    df['secure mean'] = mw_tbl['secure mean'].apply(pretty_mean)
    df['insecure mean'] = mw_tbl['insecure mean'].apply(pretty_mean)
    df["p-value"] = mw_tbl['p'].apply(pretty_p)
    df["Cliff's d"] = mw_tbl['d'].apply(pretty_d)
    sgnfc_larger = []
    alpha = 0.05
    for p,lt in mw_tbl[['p', 'secure < insecure']].values:
        if p > 0.05:
            sgnfc_larger.append('None')
        elif lt:
            sgnfc_larger.append('insecure')
        else:
            sgnfc_larger.append('secure')
            
    df["larger (alpha=0.05)"] = sgnfc_larger
    return df.transpose()
    

def mwutest(alternative, latex=False):
    cat_name = [('TRUE', 'All categories'),
    ('labels.scategory=1', 'category SSL/TLS'),
    ('labels.scategory=2', 'category Symmetric Cryptography'),
    ('labels.scategory=3', 'category Asymmetric Cryptography'),
    ('labels.scategory=4', 'category One Way Hash Function'),
    ('labels.scategory=5', 'category Random Number Generation')
    ]

    latex_tb_head_0 = r"""
%Table
\begin{table*}[h]%%%%
\caption{"""
    latex_tb_head_1 =r"""}\label{tab:one}
\begin{minipage}{\columnwidth}
\begin{center}
"""
    latex_tb_tail = r"""
\end{center}
\bigskip\centering
\footnotesize\emph{Source:} This is a table
 sourcenote. This is a table sourcenote. This is a table
 sourcenote.
 \emph{Note:} This is a table footnote.
\end{minipage}
\end{table*}%%%%
"""

    for cat, name in cat_name:
        if latex:
            latex_tb_head = latex_tb_head_0+name+latex_tb_head_1
            print(latex_tb_head+
                 friendly_mw_table(mw_table(cat, alternative=alternative)).to_latex() +
                 latex_tb_tail)
        else:
            display(Markdown("## %s:" % name))
            display(friendly_mw_table(mw_table(cat, alternative=alternative)))
        print("")    
