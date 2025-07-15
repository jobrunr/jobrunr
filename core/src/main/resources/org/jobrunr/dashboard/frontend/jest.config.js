export default {
    testEnvironment: 'jest-environment-jsdom',
    setupFilesAfterEnv: ['<rootDir>/jest-setup.js'],
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
