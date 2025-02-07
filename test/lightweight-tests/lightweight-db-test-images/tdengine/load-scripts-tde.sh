#!/bin/sh
# /tini -s -- /usr/bin/entrypoint.sh
# start the tdengine server
taosadapter &
taosd &
sleep 5

# create the database
taos -f /var/custom/db.sql

pkill taosd
sleep 5

taosd