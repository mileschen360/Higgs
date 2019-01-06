#!/bin/bash
libtoolize
aclocal -I m4 --install
autoconf
automake --foreign --add-missing
