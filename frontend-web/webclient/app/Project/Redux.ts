import {File} from "Files";
import {Action, Dispatch} from "redux";
import {Dictionary, Page} from "Types";

export interface State {
    project?: string;
    files?: Dictionary<ProjectFilesCacheEntry>;
}

export interface ProjectFilesCacheEntry {
    project: string;
    path: string;
    files: Page<File>;
}

export const initialState = {project: getStoredProject() ?? undefined};

interface SetProjectAction extends Action<"SET_PROJECT"> {
    project?: string;
}

export function dispatchSetProjectAction(dispatch: Dispatch, project?: string) {
    dispatch<ProjectAction>({project, type: "SET_PROJECT"});
}

type ProjectAction = SetProjectAction;

export const reducer = (state: State = initialState, action: ProjectAction) => {
    switch (action.type) {
        case "SET_PROJECT": {
            console.log("Set project");
            setStoredProject(action.project ?? null);
            return {...state, project: action.project, shouldVerify: false};
        }

        default:
            return state;
    }
};

export function getStoredProject(): string | null {
    return window.localStorage.getItem("project") ?? null;
}

function setStoredProject(value: string | null) {
    if (value === null) {
        window.localStorage.removeItem("project");
    } else {
        window.localStorage.setItem("project", value);
    }
}

