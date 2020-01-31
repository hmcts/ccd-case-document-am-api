/// <reference path="./steps.d.ts" />
// Feature(`CMC Citizen frontend on ${currentEnv.Environment}`);

let envs = [
    {
        Environment: 'LOCAL0',
        Url: 'http://localhost:8082/login',
        User: 'idamOwner@hmcts.net',
        Password: 'Ref0rmIsFun',

    }
];

let newRole = [

    { roleName: '	ccd-import	', roleDescription: '	 ccd-import	' },
    { roleName: '	caseworker	', roleDescription: '	 caseworker	' },
    { roleName: '	caseworker-autotest1	', roleDescription: '	 caseworker-autotest1	' },
    { roleName: '	caseworker-autotest1-solicitor	', roleDescription: '	 caseworker-autotest1-solicitor	' },
    { roleName: '	caseworker-autotest1-panelmember	', roleDescription: '	 caseworker-autotest1-panelmember	' },
    { roleName: '	caseworker-autotest1-localAuthority	', roleDescription: '	 caseworker-autotest1-localAuthority	' },
    { roleName: '   citizen ', roleDescription: '    citizen    ' },
    { roleName: '	caseworker-autotest1-citizen	', roleDescription: '	 caseworker-autotest1-citizen	' },
    { roleName: '	caseworker-autotest2	', roleDescription: '	 caseworker-autotest2	' },
    { roleName: 'caseworker-befta_jurisdiction_1', roleDescription: 'caseworker-befta_jurisdiction_1' },
    { roleName: 'caseworker-befta_jurisdiction_2', roleDescription: 'caseworker-befta_jurisdiction_2' },
    { roleName: 'caseworker-befta_jurisdiction_2-solicitor_1', roleDescription: 'caseworker-befta_jurisdiction_2-solicitor_1' },
    { roleName: 'caseworker-befta_jurisdiction_2-solicitor_2', roleDescription: 'caseworker-befta_jurisdiction_2-solicitor_2' },
    { roleName: 'caseworker-befta_jurisdiction_2-solicitor_3', roleDescription: 'caseworker-befta_jurisdiction_2-solicitor_3' },
    { roleName: 'caseworker-befta_jurisdiction_3', roleDescription: 'caseworker-befta_jurisdiction_3' },
    { roleName: 'caseworker-befta_jurisdiction_3-solicitor', roleDescription: '	caseworker-befta_jurisdiction_3-solicitor' },
    { roleName: 'caseworker-befta_jurisdiction_3-restricted', roleDescription: 'caseworker-befta_jurisdiction_3-restricted' },
];
let referenceNo = newRole;

let currentEnv = envs[0];

Feature(`IdAM System Owner on Env :: ${currentEnv.Environment}`);

Before((I, IdAMSystemOwner) => {
    // I.amOnPage(`${currentEnv.Url}`);
    IdAMSystemOwner.idamSystemOwnerlogin(`${currentEnv.Url}`, `${currentEnv.User}`, `${currentEnv.Password}`);
});


Scenario('manageServices', async (I, IdAMSystemOwner) => {
    // IdAMSystemOwner.sendform ( `${currentEnv.User}` , `${currentEnv.Password}` );
    IdAMSystemOwner.manageServices();
});


referenceNo.forEach(function (eachRef) {
    Scenario('manageRoles', async (I, IdAMSystemOwner) => {
        // IdAMSystemOwner.sendform ( `${currentEnv.User}` , `${currentEnv.Password}` );
        IdAMSystemOwner.manageRoles(`${eachRef.roleName.trim()}`, `${eachRef.roleDescription.trim()}`);
    });
});
referenceNo.forEach(function (eachRef) {
    Scenario('assignableRoles', async (I, IdAMSystemOwner) => {
        // IdAMSystemOwner.sendform ( `${currentEnv.User}` , `${currentEnv.Password}` );
        IdAMSystemOwner.assignableRoles(`${eachRef.roleName.trim()}`);
    });
});
