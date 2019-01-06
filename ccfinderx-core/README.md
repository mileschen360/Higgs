CCFinderX core
==============

This is a clone of [CCFinderX][1] that has the settings to build with autoconf
on a Linux machine. Actually this is a clone of a clone from [gpoo/ccfinderx][2].

I've split [gpoo/ccfinderx][2] into two separate projects. This is the core and
do not need java, and there is the [ccfinderx-gui][3]. The gui is not working
yet, but you can compile ccfinderx-core without OpenJDK dependencies.

**Known build dependencies(Debian/Ubuntu):**

    python-dev (2.x)
    libboost-dev
    libboost-thread-dev
    libboost-system-dev
    libicu-dev

You could face problems if trying to compile using Python 3.x or newer. Please
use Python 2.x.

The autoconf setting is not finished (it does not pass `make distcheck`), but it
is something to start with. **The process to build `ccfinderx` is:**

    $ ./autoconf_init.sh
    $ ./configure
    $ make

If you want to install CCFinderX in your system, it may be a good idea to tell
Python about easytorq.so. This is not needed for running it from the build
directory. For Fedora19:

    $ sudo cp ./torq/pyeasytorq/.libs/easytorq.so /usr/lib64/python2.7/site-packages/

**For testing:**

    $ ./ccfx/ccfx d cpp -d </root/dir/of/the/project/> # Detect clones recursively on the directory
    $ ./ccfx/ccfx d cpp </dir/with/c/files/*.c> # Detect clones only on that level. Using only * is not a good idea.

    $ ./ccfx/ccfx p a.ccfxd > /tmp/ccfinderx.out # Pretty? print the results
    $ ./ccfx/scripts/post-prettyprint.pl /tmp/ccfinderx.out /tmp/ccfinderx.xml # Convert line numbers, save to XML
    $ ./ccfx/scripts/similarfiles.py /tmp/ccfinderx.xml |sort -n -r > /tmp/dups # Make a list of files that are likely to be very similar

    $ ./ccfx/ccfx m a.ccfxd -c # Calculates clone metrics
    $ ./ccfx/ccfx m a.ccfxd -f # Calculates file metrics

**post-prettyprint.pl:**

This script reads CCFinderX pretty print format, convert line numbers from
intermediate CCFinderX token files to source code line numbers and saves the
result in XML format.

**For help:**

    $ ./ccfx/ccfx -h
    $ ./ccfx/ccfx d -h
    $ ./ccfx/ccfx p -h
    $ ./ccfx/ccfx m -h


  [1]: http://www.ccfinder.net/ccfinderxos.html
  [2]: https://github.com/gpoo/ccfinderx
  [3]: https://github.com/petersenna/ccfinderx-gui
