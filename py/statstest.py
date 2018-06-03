from scipy import stats
import pandas as pd

from . import labeled


def mw(attr_get, attr, a, b, **kwds):
    '''peform the Mann-Whitney U test'''

    attr_a = [attrs[0] if attrs[0] is not None else 0 for attrs in attr_get(attr, a)] # TODO: why favorite has None
    attr_b = [attrs[0] if attrs[0] is not None else 0 for attrs in attr_get(attr, b)]

    m = len(attr_a)
    n = len(attr_b)
    U_b, p = stats.mannwhitneyu(attr_a, attr_b, **kwds)
    if 'alternative' in kwds:
        if kwds['alternative'] == 'greater':
            kwds['alternative'] = 'less'
        if kwds['alternative'] == 'less':
            kwds['alternative'] = 'greater'
    U_a, _ = stats.mannwhitneyu(attr_b, attr_a, **kwds)
    U = min(U_a, U_b)
    #print("%40s" % attr, ":   n_a=", m, "  n_b=", n)
    d = 1 - 2.0*U/(m*n)
    return [U, p, d]


def mw_table(filter_str='TRUE', **kwds):
    ''''''
    cat_name = {'TRUE': 'All categories:',
        'labels.scategory=1': 'category SSL/TLS:',
        'labels.scategory=2': 'category Symmetric Cryptography:',
        'labels.scategory=3': 'category Asymmetric Cryptography:',
        'labels.scategory=4': 'category One Way Hash Function:',
        'labels.scategory=5': 'category Random Number Generation:',
        }
    print("\n", cat_name[filter_str])
    upd = pd.DataFrame(index=['U', 'p', 'd'])
    upd['score'] = mw(labeled.pattrs, 'DISTINCT posts.score, posts.id',
                a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
                b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
                **kwds)
    upd['reputation'] = mw(labeled.uattrs, 'users.reputation, users.id',
                a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
                b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
                **kwds)
    upd['profileviews'] = mw(labeled.uattrs, 'users.views, users.id',
                a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
                b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
                **kwds)
    # upd['userupvotes'] = mw(labeled.uattrs, 'users.upvotes, users.id',
    #             a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
    #             b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
    #             **kwds)
    # upd['userdownvotes'] = mw(labeled.uattrs, 'users.downvotes, users.id',
    #             a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
    #             b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
    #             **kwds)
    upd['commentcount'] = mw(labeled.pattrs, 'DISTINCT posts.commentcount, posts.id',
                a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
                b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
                **kwds)
    upd['viewcount'] = mw(labeled.qattrs, 'posts.viewcount, posts.id',
                a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
                b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
                **kwds)
    upd['favoritecount'] = mw(labeled.qattrs, 'posts.favoritecount, posts.id',
                a="(%s) AND (%s) " % (filter_str, labeled.is_secure_answer()),
                b="(%s) AND (%s) " % (filter_str, labeled.is_insecure_answer()),
                **kwds)
    return upd