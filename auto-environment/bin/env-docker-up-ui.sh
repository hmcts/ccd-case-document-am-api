#!/bin/bash -e
# USER_NAME='sudo' PASSWORD='xxxxxxxx' DOCKER_REPO='/Users/GITHUB/CCD-Project/ccd-docker' WEB_REPO='/Users/GITHUB/Codeceptjs-POM'
echo "#############################################[Codeceptjs Project running UI test]##################################"
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
echo "#############################################[initiallisation completed]##########################################"
cd $WEB_REPO
pwd
echo "#############################################[YOU ARE IN WEB REPO]################################################"
echo $PASSWORD | sudo -S pwd
echo "#############################################[sudo login successful]##############################################"
./node_modules/.bin/codeceptjs run --grep "manageServices"
echo "#############################################[manageServices created successfully]################################"
./node_modules/.bin/codeceptjs run --grep "manageRoles"
echo "#############################################[manageRoles created successfully]###################################"
./node_modules/.bin/codeceptjs run --grep "assignableRoles"
echo "#############################################[assignableRoles completed successfully]#############################"
cd $DOCKER_REPO
pwd
echo "#############################################[YOU are in DOCKER REPO]#############################################"
./bin/idam-create-caseworker.sh ccd-import ccd.docker.default@hmcts.net Pa55word11 Default CCD_Docker
./bin/create-initial-roles-and-users.sh
echo "#############################################[idam-create-caseworker]#############################################"
./ccd enable backend frontend dm-store
./ccd compose up -d
./bin/document-management-store-create-blob-store-container.sh
echo "#############################################[Configured Document Management Store ]###############################"

echo "#############################################[END OF ENV Setup]###################################################"
