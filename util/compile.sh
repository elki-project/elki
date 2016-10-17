#!/bin/sh
# Default profiles for compling and packaging ELKI:
exec mvn -P svg,svm,uncertain,tutorial,bundle package
