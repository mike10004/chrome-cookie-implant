#!/usr/bin/env python

#
# crxmetadata.py
#
# Parse Google Chrome extension ID from CRX file.
#
# https://stackoverflow.com/questions/16993486/how-to-programmatically-calculate-chrome-extension-id 
#

import sys
import struct
import hashlib
import string
import binascii
from argparse import ArgumentParser

def get_pub_key_from_crx(crx_file):
    with open(crx_file, 'rb') as f:
        data = f.read()
    header = struct.unpack('<4sIII', data[:16])
    pubkey = struct.unpack('<%ds' % header[2], data[16:16+header[2]])[0]
    return pubkey

def get_extension_id_from_pub_key(pubkey):
    digest = hashlib.sha256(pubkey).hexdigest()
    trans = string.maketrans('0123456789abcdef', string.ascii_lowercase[:16])
    return string.translate(digest[:32], trans)

def main(args):
    no_specifications = not args.public_key and not args.extension_id
    pubkey = get_pub_key_from_crx(args.crx_file)
    extension_id = get_extension_id_from_pub_key(pubkey)
    pubkey = binascii.b2a_base64(pubkey)
    if no_specifications:
        props = {
            'Public-Key': pubkey,
            'Extension-Id': extension_id,
        }
        for k in props:
            print k + ':', props[k]
    else:
        if args.public_key:
            print pubkey
        if args.extension_id:
            print extension_id
    return 0

if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("crx_file")
    parser.add_argument("--public-key", action="store_true", help="print public key by itself on a line", default=False)
    parser.add_argument("--extension-id", action="store_true", help="print extension id by itself on a line", default=False)
    args = parser.parse_args()
    sys.exit(main(args))
