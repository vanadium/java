SHELL := /bin/bash -euo pipefail

VDLPATH ?= $(shell pwd)/src
export VDLPATH

VANADIUM_CORE_REPO ?= $(shell pwd)
export VANADIUM_CORE_REPO

.PHONY: java
java:
	@echo "VANADIUM_CORE_REPO" ${VANADIUM_CORE_REPO}
	cd lib && ../gradlew -i publishToMavenLocal

.PHONY: test-java
test-java: java
	cd lib && ../gradlew -i test

