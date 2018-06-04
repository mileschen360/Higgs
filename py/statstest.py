from scipy import stats
import pandas as pd
from IPython.display import display, Markdown

import statistics


from . import labeled


def mw(a, b, alternative, **kwds):
    '''peform the Mann-Whitney U test'''

    if alternative is None:
        raise Exception("alternative=None is not alowed")

    m = len(a)
    n = len(b)
    if min(m,n) < 20:
        pass
        #print("WARNING: scipy Mann-Whitney U test work only for sample size > 20")
    U_b, p = stats.mannwhitneyu(a, b, alternative=alternative, **kwds)
    U_a = m*n - U_b
    a_lt_b =  U_a > U_b
    U = min(U_a, U_b)
    d = 1 - 2.0*U/(m*n)
    return [p, d, a_lt_b]


def mw_table(filter_str='TRUE', **kwds):
    ''''''
    attr_name = {'users.reputation': 'reputation',
                 'users.views':'profileviews',
                 'answers.score':'score',
                 'answers.commentcount':'commentcount',
                 'questions.favoritecount':'favoritecount',
                 'questions.viewcount':'viewcount'}
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
    return df
    

def mwutest(alternative):
    cat_name = [('TRUE', 'All categories'),
    ('labels.scategory=1', 'category SSL/TLS'),
    ('labels.scategory=2', 'category Symmetric Cryptography'),
    ('labels.scategory=3', 'category Asymmetric Cryptography'),
    ('labels.scategory=4', 'category One Way Hash Function'),
    ('labels.scategory=5', 'category Random Number Generation')
    ]

    for cat, name in cat_name:
        display(Markdown("## %s:" % name))
        display(friendly_mw_table(mw_table(cat, alternative=alternative)))
        print("")    
