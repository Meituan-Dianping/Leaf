#!/usr/bin/env bash
if [ $# != 1 ]; then
	echo "usage: $0 [commit id]"
	exit 1
fi

mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$1
exit 0
