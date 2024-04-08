const {defineConfig} = require('cypress')

module.exports = defineConfig({
    e2e: {
        supportFile: false,
        specPattern: 'cypress/integration/*.js',
        testIsolation: false
    },
    video: false
})