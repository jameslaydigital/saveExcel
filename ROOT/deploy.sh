#!/bin/bash

service tomcat7 stop
rm -rfv /var/lib/tomcat7/webapps/ROOT*  #remove outdated version
cp -v deploy/ROOT.war /var/lib/tomcat7/webapps/ #add new version
service tomcat7 start
