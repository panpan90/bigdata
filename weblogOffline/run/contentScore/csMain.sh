#!/bin/bash


baseDir=$(cd "$(dirname "$0")"; pwd)
source $baseDir/../weblogHeader.sh


beforeRunMRMoudle csMain


jiguangDataDest=/home/weblog/webAnalysis/data/toJiGuang/$yesterday
if [ ! -d $jiguangDataDest ];then
    mkdir -p $jiguangDataDest
fi


sh $baseDir/csYcInfo.sh $yesterday $model
csYcInfoStatus=$?
echo csYcInfoStatus=$csYcInfoStatus

sh $baseDir/csGenTie.sh $yesterday $model
csGenTieStatus=$?
echo csGenTieStatus=$csGenTieStatus

sh $baseDir/csZy.sh $yesterday
csZyStatus=$?
echo csZyStatus=$csZyStatus

sh $baseDir/csWeblog.sh $yesterday
csWeblogStatus=$?
echo csWeblogStatus=$csWeblogStatus

if [ $csYcInfoStatus -ne 0 -o $csGenTieStatus -ne 0 -o $csZyStatus -ne 0 -o $csWeblogStatus -ne 0 ];then
	errorAlarm csMain:csYcInfoStatus=${csYcInfoStatus},csGenTieStatus=${csGenTieStatus},csZyStatus=${csZyStatus},csWeblogStatus=$csWeblogStatus
	exit 1
fi

sh $baseDir/csFinal.sh $yesterday
csFinalStatus=$?
echo csFinalStatus=$csFinalStatus

afterRunMRMoudle csMain 3 600 
if [ "$errorList" != "" ];then
	errorAlarm csMain:$errorList
fi


 








