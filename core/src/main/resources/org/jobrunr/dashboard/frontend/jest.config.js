export default {
    testEnvironment: 'jest-environment-jsdom',
    setupFilesAfterEnv: ['<rootDir>/jest-setup.js'],
    reporters: [
        'default',
        ['jest-html-reporter', {
            outputPath: '/tmp/reports/jobrunr/jest-report.html',
            pageTitle: 'JobRunr Core Dashboard Jest Reports'
        }]
    ],
    transform: {
        '^.+\\.(t|j)sx?$': [
            '@swc/jest',
            {
                jsc: {
                    parser: {
                        jsx: true,
                        syntax: 'ecmascript',
                    },
                    transform: {
                        react: {
                            runtime: 'automatic',
                        },
                    },
                },
                isModule: 'unknown',
            },
        ],
    }
};
