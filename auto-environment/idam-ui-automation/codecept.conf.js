exports.config = {
  tests: './src/test/*_test.js',
  output: './output',
  helpers: {
    WebDriver: {
      url: 'http://localhost:8082/login',
      browser: 'chrome'
    }
  },
  include: {
    I: './steps_file.js',
    IdAMSystemOwner: './src/pages/IdAMSystemOwner.js'
  },
  bootstrap: null,
  mocha: {},
  name: 'idam-ui-automation',
  plugins: {
    retryFailedStep: {
      enabled: true
    },
    allure: {
      enabled: true
    },
    screenshotOnFail: {
      enabled: true
    }
  }
}
