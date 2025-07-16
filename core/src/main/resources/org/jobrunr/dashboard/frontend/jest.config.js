export default {
    testEnvironment: 'jest-environment-jsdom',
    setupFilesAfterEnv: ['<rootDir>/jest-setup.js'],
    reporters: [
        'default',
    ],
    testResultsProcessor: "jest-junit", // doesn't work on CI as reporter, see https://github.com/jest-community/jest-junit
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
