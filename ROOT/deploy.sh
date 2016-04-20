#!/bin/bash

trap "echo ERROR && exit 1" ERR

echo "DEPLOYMENT BEGIN"
echo "stopping tomcat service."
service tomcat7 stop

maindir=`pwd -P`
hostdir='/var/lib/tomcat7'

if [ -d /var/lib/tomcat7/webapps/ROOT ]; then

    #variable setup:
    echo "CREATING BACKUP OF CURRENT DEPLOYMENT."
    dstnt_digest="$(cd $hostdir/webapps/ROOT &>/dev/null && tar -cf - ./*|openssl md5|cut -d' ' -f2- && cd $maindir &>/dev/null)"
    backup_name="ROOT-$dstnt_digest.tgz"

    #make backup dir:
    if [ ! -d $hostdir/backups ]; then
        mkdir -p $hostdir/backups
    fi

    #backup to a tarball:
    cd $hostdir/webapps
    tar -czf $hostdir/backups/$backup_name ROOT/
    cd $maindir

    #remove old install
    echo "removing old installation."
    rm -rf $hostdir/webapps/ROOT*  #remove outdated version

else
    echo "no current deployment found. Continuing."
fi

echo "installing new application."

#if there's already a ROOT.war, the above operation failed.
if [ -f $hostdir/webapps/ROOT.war ]; then
    echo "ROOT.war already exists. Exiting."
    exit 1
fi

if [ -f deploy/ROOT.war ]; then
    cp -v deploy/ROOT.war /var/lib/tomcat7/webapps/ #add new version
else
    echo "No warfile found in local dir. Have you compiled?"
    echo "exiting"
    exit 1
fi

echo "restarting tomcat."
service tomcat7 start

echo "DEPLOYMENT DONE"
