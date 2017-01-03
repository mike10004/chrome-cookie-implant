#!/bin/bash

if [ -n "$1" ] ; then
  FINAL_NAME="$1"
fi

CHROME=$(which google-chrome)
if [ -z "$CHROME" ] ; then
  echo "make.sh: google-chrome is not installed" >&2
  exit 1
fi

fail() {
    echo $1 >&2
    exit $2 
}

INPUT="src"
OUTPUT="chrome-cookie-implant"

KEY_OPT=""
rm -rf "./${INPUT}.crx" || fail "failed to remove existing .crx" $?

KEY_FILE="${PWD}/${INPUT}.pem"

if [ -f "${KEY_FILE}" ] ; then
    KEY_OPT="--pack-extension-key=${KEY_FILE}"
else
    echo "make.sh: no existing key; expected at ${KEY_FILE}" >&2
fi

$CHROME --pack-extension=${PWD}/${INPUT} "${KEY_OPT}" 2>/dev/null || fail "failed to pack" $?

TEMPFILE="${PWD}/${INPUT}.crx"
EXT_ID=$(python crxmetadata.py --extension-id "$TEMPFILE")
if [ -z "$EXT_ID" ] ; then
  fail "could not parse extension id from file $TEMPFILE"
fi

if [ -z "$FINAL_NAME" ] ; then
  OUTNAME="${EXT_ID}"
  VERSION=$(python -c 'import sys, json; print json.load(sys.stdin)["version"]' < "${INPUT}/manifest.json")
  if [ -n "$VERSION" ] ; then
    OUTNAME="${OUTNAME}-${VERSION}"
  fi
  OUTNAME="${OUTNAME}.crx"
else
  OUTNAME="${FINAL_NAME}"
fi

mv "$TEMPFILE" "$OUTNAME"

STATUS=$?
if [ $STATUS -ne 0 ]; then
  fail "crx metadata parse failure" $STATUS
fi
echo "${OUTNAME}"
if [ ! -f "${OUTNAME}" ] ; then
  fail "not created: ${OUTNAME}" 2
fi
