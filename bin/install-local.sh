#!/bin/bash

projects[i++]="io.github.athingx.athing.thing.nls:thing-nls"
projects[i++]="io.github.athingx.athing.thing.nls:thing-nls-aliyun"

mvn clean install \
  -f ../pom.xml \
  -pl "$(printf "%s," "${projects[@]}")" -am \
  '-Dmaven.test.skip=true'
