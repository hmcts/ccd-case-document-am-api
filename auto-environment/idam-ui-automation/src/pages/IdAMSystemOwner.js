const I = actor (); // old way of doing
/*
const { I, login } = inject();
*/

module.exports = {
// setting locators
    fields: {
        username: {xpath: '//*[@id="username"]'} ,
        password: {xpath: '//*[@id="password"]'} ,
        roleName: {xpath: '//*[@id="roleName"]'} ,
        roleDescription: {xpath: '//*[@id="roleDescription"]'} ,
    } ,
    signinButton: {xpath: '//*[@id="command"]/input'} ,

// introducing methods
    idamSystemOwnerlogin: function (url , username , password) {
        I.amOnPage ( url );
        I.fillField ( this.fields.username , username );
        I.fillField ( this.fields.password , password );
        I.click ( this.signinButton );
    } ,
    manageServices() {
        I.click ( 'Manage services' );
        I.click ( '//*[@id="continue"]' );
        I.fillField ( '//*[@id="label"]' , "ccd_gateway" );
        I.fillField ( '//*[@id="description"]' , "ccd_gateway" );
        I.fillField ( '//*[@id="oauth2ClientId"]' , "ccd_gateway" );
        I.fillField ( '//*[@id="oauth2ClientSecret"]' , "ccd_gateway_secret" );
        I.fillField ( '//*[@id="newRedirectUri"]' , "http://localhost:3451/oauth2redirect" );
        I.click ( '//*[@id="newRedirectUriButton"]' );
        I.checkOption ( 'selfRegistrationAllowed' );
        I.click ( '//*[@id="servicesCommand"]/input' );
        // pause ();
    } ,
    manageRoles(roleName , roleDescription) {
        I.click ( 'Manage roles' );
        I.click ( '//*[@id="radio-0"]' );
        I.click ( '//*[@id="continue"]' );
        I.click ( '//*[@id="roleForm"]/input[2]' );
        I.fillField ( this.fields.roleName , `${roleName}` );
        I.fillField ( this.fields.roleDescription , `${roleDescription}` );
        I.click ( '//*[@id="role-form"]/input[1]' );
    } ,
    assignableRoles(roleName) {
        I.click ( 'Manage roles' );
        I.click ( '//*[@id="radio-0"]' );
        I.click ( '//*[@id="continue"]' );
        I.click ( `${roleName}` );
        I.click ( '//*[@id="roleForm"]/input[2]' );
        // for (let i = 0; i < 9; i++) {
        //     I.checkOption ( `//*[@id="select-a-${i}"]` );
        // }
        console.log("i am going to start for loop");
        for (let i=0; i<16; i++) {
            console.log("i am in for loop");
            if (I.dontSeeCheckboxIsChecked ( `//*[@id="select-a-${i}"]` )) {
                console.log("i am in if statemetn");
                I.checkOption ( `//*[@id="select-a-${i}"]` );
            }
        }
        I.click ( '//*[@id="role-form"]/input[3]' );
        I.click('Home')
        I.click('Manage services')
        I.click('//*[@id="radio-0"]')
        I.click ( '//*[@id="continue"]' );
        I.click('ccd-import')
        I.click('Save Service')
     }
};

