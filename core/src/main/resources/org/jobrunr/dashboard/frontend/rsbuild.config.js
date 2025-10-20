import {defineConfig, loadEnv} from '@rsbuild/core';
import {pluginReact} from '@rsbuild/plugin-react';

const {publicVars} = loadEnv({prefixes: ['REACT_APP_']});

export default defineConfig({
    plugins: [pluginReact()],
    html: {
        template: './public/index.html',
        templateParameters: {
            PUBLIC_URL: process.env.PUBLIC_URL,
            CSP_NONCE: process.env.CSP_NONCE,
        }
    },
    output: {
        distPath: {
            root: 'build',
        },
        assetPrefix: process.env.PUBLIC_URL,
        manifest: 'asset-manifest.json',
        cleanDistPath: process.env.NODE_ENV === 'production'
    },
    security: {
        nonce: process.env.CSP_NONCE,
    },
    source: {
        define: publicVars,
        tsconfigPath: './jsconfig.json',
        preEntry: ["./entry.js"]
    },
    server: {
        base: '/dashboard',
        proxy: {
            '/api': 'http://localhost:8000',
            '/sse': 'http://localhost:8000',
        },
        compress: false
    },
});