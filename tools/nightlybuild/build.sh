#!/bin/sh

/bin/date

cd `dirname $0`

# Remove old build result and update to the current level.
gmake update
# Start build (separate command to ensure that everything can be updated).
gmake all
