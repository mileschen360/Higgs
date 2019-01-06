"""plot results
"""
from . import labeled
from . import dbinfo
from . import cpnpa
import psycopg2
import matplotlib.pyplot as plt
from matplotlib.ticker import MultipleLocator
import matplotlib.dates as mdates
import matplotlib.colors as colors
import numpy as np
import pandas as pd
import datetime

clrs = {'Relevant':'C0',
        'both':'C0',
        'Secure': 'forestgreen',
        'Insecure': 'tomato',
        'Mixed': 'orange'}


clrs = {'Relevant':'none',
        'both':'none',
        'Secure': 'lightblue',
        'Insecure': 'whitesmoke',
        'Mixed': 'none'}

clrs = {'Relevant':'none',
        'both':'none',
        'Secure': 'none',
        'Insecure': 'none',
        'Mixed': 'none'}

clrs = {'Relevant':'gray',
        'both':'gray',
        'Secure': 'white',
        'Insecure': 'black',
        'Mixed': 'gray'}

bar_hatches = {'Relevant':'///',
        'both':'///',
        'Secure': '...',
        'Insecure': 'xxx',
        'Mixed': 'ooo'}

bar_hatches = {'Relevant': None,
        'both': None,
        'Secure': None,
        'Insecure': None,
        'Mixed': '////'}


bar_ec = {'Relevant':'C0',
        'both':'C0',
        'Secure': 'forestgreen',
        'Insecure': 'tomato',
        'Mixed': 'orange'}

bar_ec = {'Relevant':'black',
        'both':'black',
        'Secure': 'black',
        'Insecure': 'black',
        'Mixed': 'black'}

def gcfa():
    fig = plt.gcf()
    fig.set_size_inches(6, 4.5)
    return fig, fig.gca()

def finfig(fig, style='paper'):
    if style == 'paper':
        fig.set_size_inches(4.5, 3)
    if style == 'thesis':
        fig.set_size_inches(6, 4.5)
    fig.tight_layout()


def labels_summary(tb_name_suffix=dbinfo.tb_name_suffix, slabels=range(0,8)):
    fig, axes = plt.subplots(nrows=4, ncols=2, figsize=(7,8))
    plot_n_groups_vs_slabel(axes[0][0], slabels)
    print("")
    plot_n_groups_vs_scategory(axes[0][1])
    print("")
    plot_n_elements_vs_slabel(axes[1][0], slabels)
    print("")
    plot_n_elements_vs_scategory(axes[1][1])
    print("")
    plot_n_answers_vs_slabel(axes[2][0], slabels)
    print("")
    plot_n_answers_vs_scategory(axes[2][1])    
    print("")
    plot_n_questions_vs_slabel(axes[3][0], slabels)
    print("")
    plot_n_questions_vs_scategory(axes[3][1])

def n_answers_over_time(ax, **kwds):
    bins = [mdates.date2num(q) for q in pd.date_range(start='2008-01-31', end='2018-1-31', freq='A-JAN').date]
    bins = [mdates.date2num(datetime.datetime(y,1,1)) for y in range(2008, 2019)]
    labels = ['Secure', 'Insecure']
    filter_strs = dict(Secure=labeled.is_secure_answer(),
                        Insecure=labeled.is_insecure_answer())
    # filter_strs = dict(secure=labeled.is_secure_answer()+" AND "+labeled.is_answer_cat(1),
    #                     insecure=labeled.is_insecure_answer()+" AND "+labeled.is_answer_cat(1))

    creationdates = [labeled.attrs_postonly('answers.creationdate', filter_strs[label])['answers.creationdate']
                     for label in labels]
    for i in range(len(labels)):
        assert(len(creationdates[i]) == labeled.n_answer_posts(filter_strs[labels[i]]))
    density = False if 'density' not in kwds else kwds['density']
    color = [clrs[label] for label in labels]
    nhist, _, patches = ax.hist(creationdates, bins=bins, color=color, rwidth=0.9, label=labels, **kwds)
    ax.xaxis.set_ticks(bins)
    ax.xaxis.set_major_formatter(mdates.DateFormatter("'%y"))
    if density:
        ax.set_ylabel('density distribution of answers')
    else:
        ax.set_ylabel('number of answers')

    for i in range(len(patches)):
        j = 0
        for patch in patches[i]:
            patch.set_hatch(bar_hatches[labels[i]])
            patch.set_ec(bar_ec[labels[i]])
            ax.text(bins[j]+100+180*i, nhist[i][j], int(nhist[i][j]),
                    size='x-small', ha='center')
            j += 1


    ax.legend()


def n_answers_vs_viewcount(ax, **kwds):
    basex = 2
    bins = [basex**i for i in range(23)]
    labels = ['Secure', 'Insecure']
    filter_strs = dict(Secure=labeled.is_secure_answer(),
                        Insecure=labeled.is_insecure_answer())
    # filter_strs = dict(secure=labeled.is_secure_answer()+" AND "+labeled.is_answer_cat(1),
    #                     insecure=labeled.is_insecure_answer()+" AND "+labeled.is_answer_cat(1))

    # viewcounts = [labeled.attrs_postonly('questions.viewcount', filter_strs[label])['questions.viewcount']
    #                  for label in labels]

    viewcounts = [labeled.attrs_postonly('questions.viewcount', 'TRUE')['questions.viewcount']]
    viewcounts.append(labeled.jbaker_attrs('questions.viewcount', 
                            '(answers.id NOT IN (SELECT postid FROM labels WHERE postid=answers.id))'))
    viewcounts.append(labeled.jbaker_attrs('questions.viewcount', 'TRUE'))

    print("clone detected answers max viewcount =", max(viewcounts[0]))
    print("unselected answers max view=", max(viewcounts[1]))
    print("javabaker extracted answers max view =", max(viewcounts[2]))

    print("Selected answers count =", len(viewcounts[0]))
    print("Unselected answers count =", len(viewcounts[1]))
    print("All answers count =", len(viewcounts[2]))


    # for i in range(len(labels)):
    #     assert(len(viewcounts[i]) == labeled.n_answer_posts(filter_strs[labels[i]]))
    density = False if 'density' not in kwds else kwds['density']
    color = [clrs[label] for label in labels]
    color = ['blue', 'red', 'black']
    legend_labels = ['Selected Answers', 'Unselected Answers', 'All']
    nhist, _, patches = ax.hist(viewcounts, bins=bins, color=color, rwidth=0.9, label=legend_labels, 
                                cumulative=True, histtype='step', **kwds)

    ax.set_xscale('log', basex=basex)

    # ax.xaxis.set_ticks(bins)
    # ax.xaxis.set_major_formatter(mdates.DateFormatter("'%y"))
    if density:
        ax.set_ylabel('Cumulative Percent')
        ax.yaxis.set_ticks([0., 0.2, 0.4, 0.6, 0.8, 1.0])
        ax.yaxis.set_ticklabels(['0', '20', '40', '60', '80', '100'])
    else:
        ax.set_ylabel('number of answers')
    ax.set_xlabel('View Count')
    ax.set_xlim(xmax=2**22)

    linst = ['-', ':', '--']
    for i in range(len(patches)):
        j = 0
        for patch in patches[i]:
            patch.set_linestyle(linst[i])
            #patch.set_hatch(bar_hatches[labels[i]])
            #patch.set_ec(bar_ec[labels[i]])
            #ax.text(bins[j]+100+180*i, nhist[i][j], int(nhist[i][j]),
            #        size='x-small', ha='center')
            j += 1
    ax.legend(loc='upper left')


def n_hash_answers_over_time(ax, **kwds):
    bins = [mdates.date2num(q) for q in pd.date_range(start='2008-01-31', end='2018-1-31', freq='A-JAN').date]
    bins = [mdates.date2num(datetime.datetime(y,1,1)) for y in range(2008, 2019)]
    labels = ['Secure', 'Insecure']
    legend_labels = ['SHA256', 'MD5']
    filter_strs = dict(Secure=labeled.is_secure_answer()+' AND '+
                        labeled.is_answer_cat(4)
                        #+" AND (labels.reason LIKE '%SHA%' OR labels.reason LIKE '%sha%' OR labels.reason IS NULL)"
                        ,
                        Insecure=labeled.is_insecure_answer()+' AND '+
                        labeled.is_answer_cat(4)
                        #+" AND (labels.reason LIKE '%MD%' OR labels.reason LIKE '%md%')" 
                        )

    filter_strs = dict(Secure=labeled.is_relevant_answer()+" AND (answers.body LIKE '%SHA256%' OR answers.body LIKE '%sha256%')"
                        ,
                        Insecure=labeled.is_relevant_answer()+" AND (answers.body LIKE '%MD5%' OR answers.body LIKE '%md5%')"
                        )



    # filter_strs = dict(secure=labeled.is_secure_answer()+" AND "+labeled.is_answer_cat(1),
    #                     insecure=labeled.is_insecure_answer()+" AND "+labeled.is_answer_cat(1))

    creationdates = [labeled.attrs_postonly('answers.creationdate', filter_strs[label], print_sql=True)['answers.creationdate']
                     for label in labels]
    #for i in range(len(labels)):
    #    assert(len(creationdates[i]) == labeled.n_answer_posts(filter_strs[labels[i]]))
    density = False if 'density' not in kwds else kwds['density']
    color = [clrs[label] for label in labels]
    nhist, _, patches = ax.hist(creationdates, bins=bins, color=color, rwidth=0.9, label=legend_labels, **kwds)
    ax.xaxis.set_ticks(bins)
    ax.xaxis.set_major_formatter(mdates.DateFormatter("'%y"))
    if density:
        ax.set_ylabel('density distribution of answers')
    else:
        ax.set_ylabel('number of answers')

    for i in range(len(patches)):
        j = 0
        for patch in patches[i]:
            patch.set_hatch(bar_hatches[labels[i]])
            patch.set_ec(bar_ec[labels[i]])
            ax.text(bins[j]+100+180*i, nhist[i][j], int(nhist[i][j]),
                    size='x-small', ha='center')
            j += 1

    ax.legend()



def security_summary_barh(ax):
    '''horizontal bar'''
    def normalized(lst):
        return [100*float(x)/sum(lst) for x in lst]
    def add(a, b):
        return [x+y for x,y in zip(a,b)]
    
    ns_cln_clsss = [
        labeled.n_cln_clsss(labeled.is_secure_clnclss()),
        labeled.n_cln_clsss(labeled.is_insecure_clnclss()),
        labeled.n_cln_clsss(labeled.is_mixed_clnclss()),
        labeled.n_cln_clsss(labeled.is_irrelevant_clnclss())
    ]
    if sum(ns_cln_clsss) != labeled.n_cln_clsss('TRUE'):
        print("sum(ns_cln_clsss):", sum(ns_cln_clsss))
        print("labeled.n_cln_clsss('TRUE'):", labeled.n_cln_clsss('TRUE'))
        assert(sum(ns_cln_clsss) == labeled.n_cln_clsss('TRUE'))
    
    ns_cln_insts = [
        labeled.n_cln_insts(labeled.is_secure_clninst()),
        labeled.n_cln_insts(labeled.is_insecure_clninst()),
        labeled.n_cln_insts(labeled.is_mixed_clninst()),
        labeled.n_cln_insts(labeled.is_irrelevant_clninst())
    ]
    assert(sum(ns_cln_insts) == labeled.n_cln_insts('TRUE'))
    
    ns_answers = [labeled.n_answer_posts(labeled.is_secure_answer()),
                  labeled.n_answer_posts(labeled.is_insecure_answer()),
                  labeled.n_answer_posts(labeled.is_mixed_answer()),
                  labeled.n_answer_posts(labeled.is_irrelevant_answer())]
    assert(sum(ns_answers) == labeled.n_answer_posts('TRUE'))
        
    print("                 SE,  IN,  MI,  IR")
    print("clone classes:  ", ns_cln_clsss)
    clnclss = normalized(ns_cln_clsss)

    print("clone instances:", ns_cln_insts)
    clninst = normalized(ns_cln_insts)

    print("answers:        ", ns_answers)
    answers = normalized(ns_answers)
    #questions = normalized([680, 500, 30, 700])
    
    secure, insecure, mixing, irrelevant = zip(clnclss, clninst, answers)
    
    ind = range(len(secure))
    ax.barh(ind, secure, label='Secure', color='green')
    left = secure
    ax.barh(ind, insecure, left=left, label='Insecure', color='red')
    left = add(left,insecure)
    ax.text(left[2], 2, ns_answers[2])
    ax.barh(ind, mixing, left=left, label='mixing', color='orange')
    left = add(left, mixing)
    ax.barh(ind, irrelevant, left=left, label='irrelevant', color='grey')
    ax.set_yticks(ind)
    ax.set_yticklabels(['Group', 'Instance', 'Answers'])
    ax.set_xlabel('percentage [%]')
    poss = [8, 22, 34, 60]
    for i in range(len(poss)):
        ax.text(poss[i], 0, ns_cln_clsss[i])
        ax.text(poss[i], 1, ns_cln_insts[i])
        if i != 2:
            ax.text(poss[i], 2, ns_answers[i])
    ax.legend()


def n_snippets_vs_category(ax):
    n_snippets = [labeled.n_snippets(labeled.is_answer_cat(i)) for i in range(1,6)]
    ax.bar(range(1,6), n_snippets)


def n_answers_vs_category(ax):
    categories = np.arange(1,6)
    width = 0.2
    gap = 0.000
    bartext_fontsize = 'x-small'
    n_answer_posts = [labeled.n_answer_posts(labeled.is_answer_cat(i)) for i in categories]
    ax.bar(categories, n_answer_posts, width, 
          color=clrs['both'], label='Secure + Insecure', hatch=bar_hatches['both'], ec=bar_ec['both'])
    for i in range(len(categories)):
        ax.text(categories[i], n_answer_posts[i], n_answer_posts[i], ha='center', size=bartext_fontsize)

    n_answer_posts_secure = [labeled.n_answer_posts(labeled.is_answer_cat(i)+" AND "+
                        labeled.is_secure_answer()) for i in categories]
    ax.bar(categories+width+gap, n_answer_posts_secure, width,
           color=clrs['Secure'], label="Secure", hatch=bar_hatches['Secure'], ec=bar_ec['Secure'])
    for i in range(len(categories)):
        ax.text(categories[i]+width+gap, n_answer_posts_secure[i], n_answer_posts_secure[i], ha='center',
                size=bartext_fontsize)

    n_answer_posts_insecure = [labeled.n_answer_posts(labeled.is_answer_cat(i)+" AND "+
                        labeled.is_insecure_answer()) for i in categories]
    ax.bar(categories+2*(width+gap), n_answer_posts_insecure, width, 
           color=clrs['Insecure'], label='Insecure', hatch=bar_hatches['Insecure'], ec=bar_ec['Insecure'])
    for i in range(len(categories)):
        ax.text(categories[i]+2*(width+gap), n_answer_posts_insecure[i], n_answer_posts_insecure[i],
                ha='center', size=bartext_fontsize, va='bottom')

    ax.set_xticks(categories+width)
    ax.set_xticklabels(labeled.cat_names)
    ax.set_ylabel("# of answers")
    ax.legend()


def n_cln_insts_vs_category(ax):
    categories = np.arange(1,6)
    width = 0.2
    gap = 0.00 #0.04
    bartext_fontsize = 'x-small'
    n_clninsts = [labeled.n_cln_insts(labeled.is_clninst_cat(i)) for i in categories]
    ax.bar(categories, n_clninsts, width, 
          color=clrs['both'], label='Secure + Insecure', hatch=bar_hatches['both'], ec=bar_ec['both'])
    for i in range(len(categories)):
        ax.text(categories[i], n_clninsts[i], n_clninsts[i], ha='center', size=bartext_fontsize)

    n_clninst_secure = [labeled.n_cln_insts(labeled.is_clninst_cat(i)+" AND "+
                        labeled.is_secure_clninst()) for i in categories]
    ax.bar(categories+width+gap, n_clninst_secure, width,
           color=clrs['Secure'], label="Secure", hatch=bar_hatches['Secure'], ec=bar_ec['Secure'])
    for i in range(len(categories)):
        ax.text(categories[i]+width+gap, n_clninst_secure[i], n_clninst_secure[i], ha='center',
                size=bartext_fontsize)

    n_clninst_insecure = [labeled.n_cln_insts(labeled.is_clninst_cat(i)+" AND "+
                        labeled.is_insecure_clninst()) for i in categories]
    ax.bar(categories+2*(width+gap), n_clninst_insecure, width, 
           color=clrs['Insecure'], label='Insecure', hatch=bar_hatches['Insecure'], ec=bar_ec['Insecure'])
    for i in range(len(categories)):
        ax.text(categories[i]+2*(width+gap), n_clninst_insecure[i], n_clninst_insecure[i],
                ha='center', size=bartext_fontsize, va='bottom')

    #ax.set_xlabel("category")
    ax.set_xticks(categories+width)
    ax.set_xticklabels(labeled.cat_names)
    ax.set_ylabel("# of clone instances")
    ax.legend()


def n_auids_vs_category(ax):
    categories = np.arange(1,6)
    width = 0.2
    n_auids = [labeled.n_auids(labeled.is_answer_cat(i)) for i in categories]
    ax.bar(categories, n_auids, width, color=clrs['both'],
            label='secure + insecure', hatch='///')

    n_auids_secure = [labeled.n_auids(labeled.is_answer_cat(i)+" AND "+
                        labeled.is_secure_answer()) for i in categories]
    ax.bar(categories+width, n_auids_secure, width,
           color=clrs['Secure'], label="secure", hatch='...')

    n_auids_insecure = [labeled.n_auids(labeled.is_answer_cat(i)+" AND "+
                        labeled.is_insecure_answer()) for i in categories]
    ax.bar(categories+2*width, n_auids_insecure, width, color=clrs['Insecure'], label='Insecure', hatch='xxx')

    #ax.set_xlabel("category")
    ax.set_xticks(categories+width)
    ax.set_xticklabels(labeled.cat_names)
    ax.set_ylabel("# of auids")
    ax.legend()


def category_summary_barh(ax):
    def normalized(lst):
        return [100*float(x)/sum(lst) for x in lst]
    def add(a, b):
        return [x+y for x,y in zip(a,b)]
    
    ns_cln_clsss = [
        labeled.n_cln_clsss(labeled.is_secure_clnclss()),
        labeled.n_cln_clsss(labeled.is_insecure_clnclss()),
        labeled.n_cln_clsss(labeled.is_mixed_clnclss()),
        labeled.n_cln_clsss(labeled.is_irrelevant_clnclss())
    ]
    assert(sum(ns_cln_clsss) == labeled.n_cln_clsss('TRUE'))
    
    ns_cln_insts = [
        labeled.n_cln_insts(labeled.is_secure_clninst()),
        labeled.n_cln_insts(labeled.is_insecure_clninst()),
        labeled.n_cln_insts(labeled.is_mixed_clninst()),
        labeled.n_cln_insts(labeled.is_irrelevant_clninst())
    ]
    assert(sum(ns_cln_insts) == labeled.n_cln_insts('TRUE'))
    
    ns_answers = [labeled.n_answer_posts(labeled.is_secure_answer()),
                  labeled.n_answer_posts(labeled.is_insecure_answer()),
                  labeled.n_answer_posts(labeled.is_mixed_answer()),
                  labeled.n_answer_posts(labeled.is_irrelevant_answer())]
    assert(sum(ns_answers) == labeled.n_answer_posts('TRUE'))
    
    print("                SE,  IN,  MI,  IR")
    print("clone classes:", ns_cln_clsss)
    clnclss = normalized(ns_cln_clsss)

    print("clone instances:", ns_cln_insts)
    clninst = normalized(ns_cln_insts)

    print("answers:", ns_answers)
    answers = normalized(ns_answers)
    #questions = normalized([680, 500, 30, 700])
    
    secure, insecure, mixing, irrelevant = zip(clnclss, clninst, answers)
    
    ind = range(len(secure))
    ax.barh(ind, secure, label='Secure', color='green')
    left = secure
    ax.barh(ind, insecure, left=left, label='Insecure', color='red')
    left = add(left,insecure)
    ax.barh(ind, mixing, left=left, label='Mixed', color='orange')
    left = add(left, mixing)
    ax.barh(ind, irrelevant, left=left, label='Irrelevant', color='grey')
    ax.set_yticks(ind)
    ax.set_yticklabels(['Class', 'Instance', 'Answers'])
    ax.set_xlabel('percentage')
    ax.legend()


def hist(ax, x, **kwds):
    hatches = kwds.pop('hatches', None)
    nhist, bins, patches = ax.hist(x, **kwds)
    if hatches: 
        for i in range(len(patches)):
            for patch in patches[i]:
                patch.set_hatch(hatches[i])

def n_users_vs_n_repeated_answers(ax):
    labels = ['both', 'Secure', 'Insecure']
    filter_strs = {'both': "secure+insecure",
                'Secure': labeled.is_secure_answer(),
                'Insecure': labeled.is_insecure_answer()}
    cpcount_uid_lst = [cpnpa.ns_answers_user_copied(filter_strs[label], include_uid=False) for label in labels]
    color = [clrs[label] for label in labels]
    nhist, bins, patches = ax.hist(cpcount_uid_lst, bins=range(2, 20), label=labels, color=color)
    for i in range(len(patches)):
        j = 0
        for patch in patches[i]:
            patch.set_hatch(bar_hatches[labels[i]])
            patch.set_ec(bar_ec[labels[i]])
            ax.text(bins[j]+0.2+0.3*i, nhist[i][j], int(nhist[i][j]),
                    size='x-small', ha='center')
            j += 1
    ax.xaxis.set_major_locator(MultipleLocator(1))
    ax.set_xlabel('# of repeated answers')
    ax.set_ylabel('# of users')
    ax.legend()

def group_size_hist(ax, **kwds):
    '''histogram of group size'''
    basex = 2
    max_power = 8
    bins = [basex**i for i in range(1,max_power)]

    filter_strs = dict(Relevant='NOT '+labeled.is_irrelevant_clnclss(),
                   Secure=  labeled.is_secure_clnclss(),
                   Insecure=labeled.is_insecure_clnclss(),
                   Mixed=   labeled.is_mixed_clnclss())

    labels = ['Relevant', 'Secure', 'Insecure', 'Mixed']
    legend_label = ['Secure\n+Insecure\n+Mixed', 'Secure', 'Insecure', 'Mixed']

    grp_sizes = [labeled.cln_clss_sizes(filter_strs[label]) for label in labels]

        # print(label, max(grp_sizes))
        # assert(basex**max_power >= max(grp_sizes))

    color = [clrs[label] for label in labels]

    nhist, bins, patches = ax.hist(grp_sizes, bins=bins, rwidth=0.9, 
                color=color, label=legend_label)
    for i in range(len(patches)):
        j = 0
        for patch in patches[i]:
            patch.set_hatch(bar_hatches[labels[i]])
            patch.set_ec(bar_ec[labels[i]])
            patch._hatch_color = colors.to_rgba('white', 1.)
            ax.text(bins[j]*basex**(0.2+0.24*i), nhist[i][j], int(nhist[i][j]),
                    size='x-small', ha='center', va='bottom')
            j += 1


    ax.set_xlabel("Group size")
    ax.set_ylabel("# of groups")
    ax.set_xscale('log', basex=basex)
    ax.set_yscale('log')
    ax.legend()
    ax.minorticks_off()
    
def n_users_vs_reputation(ax):
    basex = 2
    bins = [basex**i for i in np.linspace(0,20,20+1)]

    labels = ['Relevant']#, 'Secure', 'Insecure']
    filter_strs = dict(Relevant=labeled.is_relevant_answer(),
                   Secure=  labeled.is_secure_answer(),
                   Insecure=labeled.is_insecure_answer())

    legend_label = ['Secure\n+Insecure', 'Secure', 'Insecure']

    reputations = [labeled.attrs('users.reputation', filter_strs[label],
                   dist='users.id')['users.reputation'] for label in labels]
    nhist, bins, patches = ax.hist(reputations,
                           bins=bins, rwidth=0.9)
    ax.set_xscale('log', basex=basex)
    ax.set_xlabel('reputation')
    ax.set_ylabel('# of users')

    # for i in range(len(patches)):
    j = 0
    for patch in patches:
        #patch.set_hatch(bar_hatches[labels[i]])
        #patch.set_ec(bar_ec[labels[i]])
        #patch._hatch_color = colors.to_rgba('white', 1.)
        ax.text(bins[j]*basex**(0.2), nhist[j], int(nhist[j]),
                size='x-small', ha='center', va='bottom')
        j += 1

def n_aids_vs_reputation(ax):
    basex = 2
    bins = [basex**i for i in np.linspace(0,20,20+1)]

    basex = 10
    power_range = np.linspace(0,6,6+1)
    bins = [basex**i for i in power_range]


    reputations_secure_ans = labeled.attrs('users.reputation', labeled.is_secure_answer())['users.reputation']
    reputations_insecure_ans = labeled.attrs('users.reputation', labeled.is_insecure_answer())['users.reputation']

    assert(len(reputations_secure_ans)+len(reputations_insecure_ans) == 
           labeled.n_answer_posts(labeled.is_relevant_answer()) -
           len(labeled.no_owner_answers(filter_str=labeled.is_relevant_answer())))

    rwidth=0.8
    barwidth = rwidth/2.
    margin = (1-rwidth)/2.
    n_secure_ans, _ = np.histogram(reputations_secure_ans, bins)
    ax.bar(power_range[0:-1]+margin, n_secure_ans, 
            align='edge', width=barwidth, 
            color=clrs['Secure'], ec=bar_ec['Secure'], label='Secure')
    for i in range(len(n_secure_ans)):
        ax.text(power_range[i]+margin+0.5*barwidth, n_secure_ans[i]+5,
                n_secure_ans[i], size='x-small', ha='center')

    n_insecure_ans, _ = np.histogram(reputations_insecure_ans, bins)
    ax.bar(power_range[0:-1]+margin+barwidth, n_insecure_ans, 
            align='edge', width=barwidth,
            color=clrs['Insecure'], ec=bar_ec['Insecure'],  label='Insecure')
    for i in range(len(n_insecure_ans)):
        ax.text(power_range[i]+margin+1.5*barwidth, n_insecure_ans[i]+5,
                n_insecure_ans[i], size='x-small', ha='center')

    ax.xaxis.set_ticks(power_range)
    ax.xaxis.set_ticklabels([r"$%d^{%d}$" % (basex, power) for power in power_range])


    # reputations = [reputations_secure_ans, reputations_insecure_ans]
    

    # labels = ['Secure', 'Insecure']
    # colors = [clrs[label] for label in labels]
    # nhist, bins, patches = ax.hist(reputations, bins=bins, label=labels, density=False, color=colors)
    # for i in range(len(patches)):
    #     j = 0
    #     for patch in patches[i]:
    #         patch.set_hatch(bar_hatches[labels[i]])
    #         patch.set_ec(bar_ec[labels[i]])
    #         ax.text(bins[j]*basex**(0.2+i*0.2), nhist[i][j], int(nhist[i][j]),
    #                 size='x-small', ha='center')
    #         j += 1
    
    # ax.set_xscale('log', basex=basex)

    ax.legend()
    #ax.set_xlabel(r"$\log_{10}$(reputation of answer owner)")
    ax.set_xlabel("Reputation of answer owner")
    ax.set_ylabel("# of answers")



def group_size_boxplot(ax, filter_str, **kwds):
    '''histogram of group size'''
    grp_sizes = labeled.cln_clss_sizes(filter_str)
    ax.boxplot(grp_sizes)
    # ax.set_xlabel("group size")
    # ax.set_title("filter_str")



def plot_n_groups_vs_slabel(ax, slabels=range(8)):
    n_grps = [labeled.n_cln_clsss("labels.slabel=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_grps)
    print("n_slabels:", slabels)
    print("n_grps:", n_grps)
    ax.set_xlabel("security")
    ax.set_ylabel("# of clone groups")

def plot_n_elements_vs_slabel(ax, slabels=range(8)):
    n_grps = [labeled.n_cln_insts("labels.slabel=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_grps)
    print("n_slabels:", slabels)
    print("n_elements:", n_grps)
    ax.set_xlabel("security")
    ax.set_ylabel("# of clone elements")

def plot_n_answers_vs_slabel(ax, slabels=range(8)):
    n_grps = [labeled.n_answer_posts("labels.slabel=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_grps)
    print("n_slabels:", slabels)
    print("n_answers:", n_grps)
    ax.set_xlabel("security")
    ax.set_ylabel("# of answer posts")

def plot_n_questions_vs_slabel(ax, slabels=range(8)):
    n_questions = [labeled.n_question_posts("labels.slabel=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_questions)
    print("n_slabels:", slabels)
    print("n_questions:", n_questions)
    ax.set_xlabel("security")
    ax.set_ylabel("# of question posts")
            
def plot_n_groups_vs_scategory(ax, slabels=range(1,6)):
    n_grps = [labeled.n_cln_clsss("labels.scategory=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_grps)
    print("n_scategorys:", slabels)
    print("n_grps:", n_grps)
    ax.set_xlabel("category")
    ax.set_ylabel("# of clone groups")
    
def plot_n_elements_vs_scategory(ax, slabels=range(1,6)):
    n_grps = [labeled.n_cln_insts("labels.scategory=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_grps)
    print("n_scategorys:", slabels)
    print("n_elements:", n_grps)
    ax.set_xlabel("category")
    ax.set_ylabel("# of clone elements")
    
def plot_n_answers_vs_scategory(ax, slabels=range(1,6)):
    n_grps = [labeled.n_answer_posts("labels.scategory=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_grps)
    print("n_scategorys:", slabels)
    print("n_answers:", n_grps)
    ax.set_xlabel("category")
    ax.set_ylabel("# of answer posts")
    
    
def plot_n_questions_vs_scategory(ax, slabels=range(1,6)):
    n_questions = [labeled.n_question_posts("labels.scategory=%d" % slabel)  for slabel in slabels]
    ax.bar(slabels, n_questions)
    print("n_scategorys:", slabels)
    print("n_questions:", n_questions)
    ax.set_xlabel("category")
    ax.set_ylabel("# of question posts")






def slabel2postfilter(slabel):
    filter_str = 'labels.slabels=%d ' % slabel
    if slabel == 2:
        filter_str += 'AND postid NOT IN (SELECT postid FROM labels WHERE slabel=4)'