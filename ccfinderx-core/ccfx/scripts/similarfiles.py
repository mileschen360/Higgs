#!/usr/bin/python
# -*- coding: utf-8 -*-
# Peter Senna Tschudin <peter.senna@gmail.com>
"""Given the xml output of post-prettyprint.pl, produces a text file
containing pair of source files with the number of probable cloned
lines. The goal is to have a list of files that are probably very
similar"""

import xml.etree.ElementTree as ET
import itertools
import sys

class FastIdentifiers(object):
    """The goal is to have a fast way to store identifiers and reference
    them by unique numbers"""

    def __init__(self):
        self.str_num = {}
        self.num_str = {}
        self.max_num = 0

    def add(self, identifier):
        """Adds a string to the _db_ and return its number. If it
        already exists, just return it's number"""

        if not identifier:
            return None

        id_num = self.get_num(identifier)
        if id_num:
            return id_num

        self.max_num += 1
        self.num_str[self.max_num] = identifier
        self.str_num[identifier] = self.max_num

        return self.str_num[identifier]

    def get_num(self, identifier):
        """Return the number for a string or None if there is
        no number on the _db_"""

        return self.str_num.get(identifier, None)

    def get_str(self, id_num):
        """Return the string for a number or None if there is
        no number on the _db_"""

        return self.num_str.get(id_num, None)

    def is_defined(self, identifier):
        """Is this identifier already known?"""
        if identifier in self.str_num:
            return True

        return False

def main():
    """The good old main!"""

    try:
        print "Using: " + sys.argv[1]
    except IndexError:
        print "I need a XML file as first argument..."
        exit(-1)

    tree = ET.parse(sys.argv[1])
    root = tree.getroot()

    clone_pairs = {}
    # Using ints instead of strs for indices/keys is more time and memory
    # efficient
    ids = FastIdentifiers()

    for clone in root:
        path_dict = {}
        for cloned_file in clone:
            # the [28:] removes /home/peter/devel/git/linux from the path
            # short_path = cloned_file[0].text[28:]
            short_path = cloned_file[0].text
            short_path_num = ids.add(short_path)
            line_count = int(cloned_file[2].text) - int(cloned_file[1].text)
            path_dict[short_path_num] = line_count

        if len(path_dict) > 1:
            for combination in itertools.combinations(path_dict.keys(), 2):
                # Always use (min, max) for avoindig duplicates like (a, b)
                # and (b, a)
                min_max_combination = (min(combination), max(combination))
                if min_max_combination in clone_pairs:
                    clone_pairs[min_max_combination] +=\
                                                path_dict[min(combination)]
                else:
                    clone_pairs[min_max_combination] =\
                                                path_dict[min(combination)]

    for clone in clone_pairs.keys():
        print ",".join([str(clone_pairs[clone]), ids.get_str(clone[0]),\
                       ids.get_str(clone[1])])

if __name__ == "__main__":
    main()
