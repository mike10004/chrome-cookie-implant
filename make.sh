#!/bin/bash

CHROME=$(which google-chrome)

fail() {
    echo $1 >&2
    exit $2 
}

KEY_OPT=""
rm -rf src.crx || fail "failed to remove existing .crx" $?

KEY_FILE="${PWD}/src.pem"

if [ -f "${KEY_FILE}" ] ; then
    echo "using existing key ${KEY_FILE}"
    KEY_OPT="--pack-extension-key=${KEY_FILE}"
fi

$CHROME --pack-extension=${PWD}/src "${KEY_OPT}" || fail "failed to pack" $?

ls src.*

OUTFILE="${PWD}/src.crx"
EXT_ID=$(python crxmetadata.py --extension-id "$OUTFILE")
STATUS=$?
if [ $STATUS -ne 0 ]; then
  fail "crx metadata parse failure" $?
fi
echo "extension id: $EXT_ID"
