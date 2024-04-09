import {createContext} from "react";

export const DEFAULT_PROBLEMS_CONTEXT = {
    reload: () => void 0,
    isLoading: false,
    problems: []
}

export const ProblemsContext = createContext(DEFAULT_PROBLEMS_CONTEXT);