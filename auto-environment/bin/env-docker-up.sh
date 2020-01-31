#!/bin/bash
echo "#############################################[Docker build from scratch]##############################################"
# USER_NAME='sudo' PASSWORD='xxxxxxxx' DOCKER_REPO='/Users/GITHUB/CCD-Project/ccd-docker' WEB_REPO='/Users/GITHUB/Codeceptjs-POM'
for ARGUMENT in "$@"
do
    KEY=$(echo $ARGUMENT | cut -f1 -d=)
    VALUE=$(echo $ARGUMENT | cut -f2 -d=)
    case "$KEY" in
            USER_NAME)              USER_NAME=${VALUE} ;;
            PASSWORD)               PASSWORD=${VALUE} ;;
            DOCKER_REPO)            DOCKER_REPO=${VALUE} ;;
            WEB_REPO)               WEB_REPO=${VALUE} ;;
            *)
    esac
done
echo "USER_NAME = $USER_NAME"
echo "PASSWORD = $PASSWORD"
echo "DOCKER_REPO = $DOCKER_REPO"
echo "WEB_REPO = $WEB_REPO"
echo "#############################################[initiallisation completed]##############################################"
cd $DOCKER_REPO
pwd
echo "#############################################[You are in CCD DOCKER REPO]#############################################"
echo $PASSWORD | sudo -S pwd
echo "#############################################[sudo login successful]##################################################"
az login
echo "#############################################[Azure CLI Login successful]#############################################"
./ccd enable default
./ccd compose down --force
echo "#############################################[./ccd compose down successful]##########################################"
docker volume prune --force
echo "#############################################[docker volum prune done]################################################"
docker system prune --force
echo "#############################################[docker system prune done]###############################################"
docker container ls -a
echo "#############################################[container ls -a done]###################################################"
./ccd login
echo "#############################################[login done]#############################################################"
./ccd enable default
echo "#############################################[ccd enable default done]################################################"
./ccd compose pull
echo "#############################################[compose pull done]######################################################"
./ccd init
echo "#############################################[ccd init]###############################################################"
./ccd compose up -d
echo "#############################################[ccd compose up done]####################################################"
docker ps -a
echo "#############################################[Docker ps -a done]######################################################"
cd $WEB_REPO
pwd
echo "#############################################[YOU ARE IN WEB REPO]####################################################"
echo $PASSWORD | sudo -S pwd
echo "#############################################[sudo login successful]##################################################"
sudo -S npm i
echo "#############################################[npm i done]#############################################################"
sudo -S npm add
echo "#############################################[npm add done]###########################################################"
sudo -S npm install -g codeceptjs webdriverio --verbose --no-sandbox --headless --port=4444
echo "#############################################[codeceptjs webdriverio installed]#######################################"
# sudo -S npm install -g selenium-standalone
# echo "#############################################[selenium-standalone installed]##########################################"
sudo -S selenium-standalone install
echo "#############################################[selenium-standalone installed]###########################################"
sudo -S selenium-standalone start
echo "#####################################[selenium-standalone start]######################################################"
