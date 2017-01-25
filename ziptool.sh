#!/usr/bin/env bash

TMP_DIR="/tmp"
ZIP_DIR=$(mktemp -d)
ZIP_FILE="$TMP_DIR/kiekerlogs.zip"
COUNT="$1"

if [ -f $ZIP_FILE ]; then
  rm $ZIP_FILE
fi

LATEST_LOGS=$(ls -d $TMP_DIR/kieker-* | tail -n 1)

cp $LATEST_LOGS/kieker.map $ZIP_DIR/.

TCOUNT=$(($COUNT+1))

for datfile in $(ls $LATEST_LOGS/*.dat | tail -n $TCOUNT | head -n $COUNT);
do
  cp $datfile $ZIP_DIR/.
done

zip -qj $ZIP_FILE $ZIP_DIR/*

rm -r $ZIP_DIR

echo $ZIP_FILE
