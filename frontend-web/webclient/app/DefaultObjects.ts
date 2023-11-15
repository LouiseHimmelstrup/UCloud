import {DashboardStateProps} from "@/Dashboard";
import * as ProjectRedux from "@/Project/Redux";
import {Upload} from "@/Files/Upload";
import {defaultAvatar} from "@/UserSettings/Avataaar";
import {ProjectCache} from "@/Project/cache";
import {APICallStateWithParams} from "@/Authentication/DataHook";
import {Product} from "@/Accounting";
import * as UCloud from "@/UCloud";
import {BulkRequest, BulkResponse, PageV2} from "@/UCloud";
import {useEffect} from "react";
import {useGlobal} from "@/Utilities/ReduxHooks";
import {UCLOUD_CORE} from "@/UCloud/ResourceApi";
import {buildQueryString} from "@/Utilities/URIUtilities";
import {NavigateFunction} from "react-router";
import {initTerminalState, TerminalState} from "@/Terminal/State";
import {PopInArgs} from "./ui-components/PopIn";
import {SidebarStateProps} from "./Applications/Redux/Reducer";
import {getUserThemePreference} from "./UtilityFunctions";

export enum KeyCode {
    ENTER = 13,
    ESC = 27
}

export function placeholderProduct(): {"id": "", "category": "", "provider": string} {
    return {"id": "", "category": "", "provider": UCLOUD_CORE};
}

export function bulkRequestOf<T>(...items: T[]): BulkRequest<T> {
    return {"type": "bulk", items};
}

export function bulkResponseOf<T>(...items: T[]): BulkResponse<T> {
    return {responses: items};
}

export const emptyPage: Readonly<Page<any>> =
    {items: [], itemsInTotal: 0, itemsPerPage: 25, pageNumber: 0};

export const emptyPageV2: Readonly<UCloud.PageV2<any>> =
    {items: [], itemsPerPage: 100};

export function pageV2Of<T>(...items: T[]): PageV2<T> {
    return {items, itemsPerPage: items.length, next: undefined};
}

export enum SensitivityLevel {
    "INHERIT" = "Inherit",
    "PRIVATE" = "Private",
    "CONFIDENTIAL" = "Confidential",
    "SENSITIVE" = "Sensitive"
}

export enum SensitivityLevelMap {
    INHERIT = "INHERIT",
    PRIVATE = "PRIVATE",
    CONFIDENTIAL = "CONFIDENTIAL",
    SENSITIVE = "SENSITIVE"
}

export interface ComponentWithLoadingState {
    loading: boolean;
    error?: string;
}

export interface StatusReduxObject {
    title: string;
    loading: boolean;
}

export interface HeaderSearchReduxObject {
    prioritizedSearch: HeaderSearchType;
    refresh?: () => void;
}

export type HeaderSearchType = "files" | "applications" | "projects";

/**
 * Global state created via useGlobal() similar to ReduxObject
 */
export interface HookStore {
    uploaderVisible?: boolean;
    uploads?: Upload[];
    uploadPath?: string;

    searchPlaceholder?: string;
    onSearch?: (query: string, navigate: NavigateFunction) => void;

    projectCache?: ProjectCache;
    computeProducts?: APICallStateWithParams<Page<Product>>;
    storageProducts?: APICallStateWithParams<Page<Product>>;
    frameHidden?: boolean;
    cloudApiCache?: Record<string, {expiresAt: number, cached: any}>;

    mainContainerHeaderSize?: number;
}

interface LegacyReduxObject {
    hookStore: HookStore;
    dashboard: DashboardStateProps;
    status: StatusReduxObject;
    header: HeaderSearchReduxObject;
    avatar: AvatarReduxObject;
    project: ProjectRedux.State;
    terminal: TerminalState;
    popinChild: PopInArgs | null;
    loading?: boolean;
    sidebar: SidebarStateProps;
}

declare global {
    export type ReduxObject =
        LegacyReduxObject;
}

export function initHeader(): HeaderSearchReduxObject {
    return ({
        prioritizedSearch: "files"
    });
}

export function initStatus(): StatusReduxObject {
    return ({
        title: "",
        loading: false
    });
}

export const initDashboard = (): DashboardStateProps => ({
    loading: false,
});

export function initObject(): ReduxObject {
    return {
        hookStore: {},
        dashboard: initDashboard(),
        status: initStatus(),
        header: initHeader(),
        avatar: initAvatar(),
        project: ProjectRedux.initialState,
        terminal: initTerminalState(),
        popinChild: null,
        sidebar: {favorites: [], theme: getThemeOrDefaultValue()}
    };
}

function getThemeOrDefaultValue(): "light" | "dark" {
    return (window.localStorage.getItem("theme") ?? getUserThemePreference()) as "light" | "dark"
}

export type AvatarReduxObject = typeof defaultAvatar & {error?: string};
export function initAvatar(): AvatarReduxObject {
    return {...defaultAvatar, error: undefined};
}

export const defaultSearchPlaceholder = "Search files and applications..."

export function defaultSearch(query: string, navigate: NavigateFunction) {
    navigate(buildQueryString("/files/search", {q: query}));
}

export function useSearch(onSearch: (query: string, navigate: NavigateFunction) => void): void {
    const [, setOnSearch] = useGlobal("onSearch", defaultSearch);
    useEffect(() => {
        setOnSearch(() => onSearch);
        return () => {
            setOnSearch(() => defaultSearch);
        };
    }, [setOnSearch, onSearch]);
}

export function useSearchPlaceholder(searchPlaceholder: string): void {
    const [, setSearchPlaceholder] = useGlobal("searchPlaceholder", defaultSearchPlaceholder);
    useEffect(() => {
        setSearchPlaceholder(searchPlaceholder);
        return () => {
            setSearchPlaceholder(defaultSearchPlaceholder);
        };
    }, [setSearchPlaceholder, searchPlaceholder]);
}
