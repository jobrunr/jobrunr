import {defineConfig, loadEnv} from '@rsbuild/core';
import {pluginReact} from '@rsbuild/plugin-react';

const {publicVars} = loadEnv({prefixes: ['REACT_APP_']});

export default defineConfig({
    plugins: [pluginReact()],
    html: {
        template: './public/index.html',
        templateParameters: {
            PUBLIC_URL: process.env.PUBLIC_URL,
        }
    },
    output: {
        distPath: {
            root: 'build',
        },
        assetPrefix: process.env.PUBLIC_URL,
        manifest: 'asset-manifest.json',
    },
    source: {
        define: publicVars,
        tsconfigPath: './jsconfig.json'
    },
    server: {
        proxy: {
            '/api': 'http://localhost:8000',
            '/sse': 'http://localhost:8000',
        },
    },
});