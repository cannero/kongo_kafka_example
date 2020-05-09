#!/bin/sh
echoerr() { echo "$@" 1>&2; }

export GRADLE_USER_HOME=/var/gradle
if [ ! -d "$GRADLE_USER_HOME" ]; then
    echoerr "gradle home $GRADLE_USER_HOME not a directory"
fi

export PATH=$PATH:/opt/gradle/bin
