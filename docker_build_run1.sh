#!/bin/sh

docker build -t kongo1 . && docker run --rm -v `pwd`:/kongo1 -ti kongo1
