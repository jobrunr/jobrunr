import {defineConfig, loadEnv} from '@rsbuild/core';
import {pluginReact} from '@rsbuild/plugin-react';
import {pluginSvgr} from "@rsbuild/plugin-svgr";

const {publicVars} = loadEnv({prefixes: ['REACT_APP_']});

export default defineConfig({
    plugins: [pluginReact(), pluginSvgr()],
    html: {
        template: './public/index.html',
        templateParameters: {
            PUBLIC_URL: process.env.PUBLIC_URL,
            CSP_NONCE: process.env.NODE_ENV === 'production' ? process.env.CSP_NONCE : "CSP_NONCE",
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
        nonce: process.env.NODE_ENV === 'production' ? process.env.CSP_NONCE : "CSP_NONCE",
    },
    source: {
        define: publicVars,
        tsconfigPath: './jsconfig.json',
    },
    server: {
        base: '/dashboard',
        proxy: {
            '/api': 'http://localhost:8000',
            '/sse': 'http://localhost:8000',
        },
        headers: {
            'Content-Security-Policy': "script-src 'nonce-CSP_NONCE' 'strict-dynamic';object-src 'none';base-uri 'none'",
        },
        compress: false
    },
});