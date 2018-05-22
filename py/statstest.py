from scipy import stats
from . import labeled

def mw(pattr, a, b, **kwds):
    '''peform the Mann-Whitney U test'''

    pattr_a = labeled.pattrs(pattr, a)
    pattr_b = labeled.pattrs(pattr, b)
    u, p = stats.mannwhitneyu(pattr_a, pattr_b)
    print("U=", u)
    print("p=", p)