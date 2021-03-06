#!/usr/bin/python
__doc__ = """Quick Cache.

This module stores responses in a hashmap so that the same URL
is never retrieved more than once.

TODO Should implement a real HTTP 1.1 proxy and cache all headers.

Authors:
Baard H. Rehn Johansen <baard@rehn.no>
Jonathan Share <jonathan@sharebear.com>
"""

from optparse import OptionParser
import BaseHTTPServer
import hashlib
import os
import urllib2
from urllib2 import HTTPError

class CacheHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_GET(self):
        options = self.server.proxy_options
        verbose = options.verbose
        path = trim_path(self.path)
        if verbose: print ("Path: %s was trimmed to: %s" % (self.path, path))
        m = hashlib.md5()
        m.update(path)
        digest = m.hexdigest()
        cache_filename = os.path.join(options.cache_dir, digest)
        if verbose: print "Request for: %s, digest: %s" % (path, digest)
        if os.path.exists(cache_filename):
            if verbose: print "Cache hit: %s" % cache_filename
            data = open(cache_filename).readlines()
        else:
            request_path = options.url + path
            if verbose: print "Cache miss, fetching %s" % request_path
            try:            
                data = urllib2.urlopen(request_path).readlines()
                file = open(cache_filename, 'wb')
                file.writelines(data)
                file.close()
                log = open(options.log_file, "a")
                log.write("%s: %s\n" % (digest, path))
                log.close()
            except HTTPError, e:
                print "fetch failed: %s" % e 
                self.send_response(e.code)
                return
        self.send_response(200)
        self.end_headers()
        self.wfile.writelines(data)

def trim_path(path):
    # use regexp instead
    path = path.replace('http://', '')
    return path[path.index('/'):]

def make_cache_dir(path):
    if os.path.isdir(path):
        pass
    elif os.path.isfile(path):
        raise OSError("%s is a file!" % path)
    else:
        os.mkdir(path)

def run(options):
    make_cache_dir(options.cache_dir)
    server_address = (options.bind, options.port)
    httpd = BaseHTTPServer.HTTPServer(server_address, CacheHandler)
    httpd.proxy_options = options
    print "Started proxy for %s, redirect clients to http://%s:%d/" \
        % ( options.url, httpd.server_name, httpd.server_port)
    httpd.serve_forever()

def main():
    usage = "usage: %prog [options] base-url"
    parser = OptionParser(usage)
    parser.add_option("-v", "--verbose", dest="verbose", action="store_true", 
        default=False, help="be verbose")
    parser.add_option("-b", "--bind", dest="bind", default="localhost",
        help="bind to other address than %default")
    parser.add_option("-p", "--port", dest="port", default=8080,
        help="listens on other port than %default")
    parser.add_option("-l", "--log", dest="log_file", default="quickcache.log",
        help="log to other file than %default")
    parser.add_option("-d", "--dir", dest="cache_dir", 
        default="%s/.quickcache" % os.getenv("HOME"), metavar="DIR",
        help="specify cache directory (default ~/.quickcache)")

    (options, args) = parser.parse_args()
    if len(args) != 1:
        parser.error("incorrect number of arguments")
    options.url = args[0]
    run(options)

if __name__ == '__main__':
    main()

