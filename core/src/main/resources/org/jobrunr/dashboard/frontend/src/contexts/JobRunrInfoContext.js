import {createContext} from 'react';

export const DEFAULT_JOBRUNR_INFO = {
    version: '0.0.0-SNAPSHOT',
    allowAnonymousDataUsage: false,
    clusterId: undefined,
    storageProviderType: undefined
}

export const JobRunrInfoContext = createContext(DEFAULT_JOBRUNR_INFO);