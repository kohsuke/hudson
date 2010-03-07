#!/bin/bash

mvn clean install eclipse:eclipse -DdownloadSources -DskipTests -Peclipse,debug
