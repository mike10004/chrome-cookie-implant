#!/bin/bash

CHROME=$(which google-chrome)

fail() {
    echo $1 >&2
    exit $2 
}

rm -rf src.crx src.pem || fail "failed to remove existing .crx and .pem" $?

$CHROME --pack-extension=${PWD}/src || fail "failed to pack" $?

ls src.*
