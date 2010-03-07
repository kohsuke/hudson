#!/bin/bash

java -jar cli/target/cli-*-jar-with-dependencies.jar -s http://localhost:8080/ $*
