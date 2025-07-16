export default {
    testEnvironment: 'jest-environment-jsdom',
    setupFilesAfterEnv: ['<rootDir>/jest-setup.js'],
    reporters: [
        'default',
        ['jest-junit', {
            outputDirectory: '/tmp/reports/core/frontend'
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
