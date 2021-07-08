import {ActivityFilter, ActivityForFrontend} from "Activity";
import {DashboardStateProps} from "Dashboard";
import {Notification} from "Notifications";
import * as ProjectRedux from "Project/Redux";
import {Reducer} from "redux";
import {ScrollResult} from "Scroll/Types";
import {SimpleSearchStateProps} from "Search";
import {SidebarOption} from "Types";
import {SidebarPages} from "ui-components/Sidebar";
import {Upload} from "Files/Upload";
import {defaultAvatar} from "UserSettings/Avataaar";
import {ProjectCache} from "Project/cache";
import {APICallStateWithParams} from "Authentication/DataHook";
import {
    ListGroupMembersRequestProps,
    ListOutgoingInvitesRequest,
    OutgoingInvite,
    ProjectMember,
    UserInProject
} from "Project";
import {GroupWithSummary} from "Project/GroupList";
import {Product} from "Accounting";
import * as UCloud from "UCloud";
import {BulkRequest, BulkResponse} from "UCloud";
import {useEffect} from "react";
import {useGlobal} from "Utilities/ReduxHooks";
import {doNothing} from "UtilityFunctions";

export enum KeyCode {
    ENTER = 13,
    ESC = 27
}

export function placeholderProduct(): { "id": "", "category": "", "provider": "ucloud_core" } {
    return { "id": "", "category": "", "provider": "ucloud_core" };
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
    {items: [], itemsPerPage: 25};

export enum SensitivityLevel {
    "INHERIT" = "Inherit",
    "PRIVATE" = "Private",
    "CONFIDENTIAL" = "Confidential",
    "SENSITIVE" = "Sensitive"
}

export type Sensitivity = keyof typeof SensitivityLevel;

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

export interface ComponentWithPage<T> extends ComponentWithLoadingState {
    page: Page<T>;
}

export interface ComponentWithScroll<Item, OffsetType> extends ComponentWithLoadingState {
    scroll?: ScrollResult<Item, OffsetType>;
}

export interface ResponsiveReduxObject {
    mediaType: string;
    orientation: string;
    lessThan: Record<string, boolean>;
    greaterThan: Record<string, boolean>;
    is: Record<string, boolean>;
}

export interface FileInfoReduxObject {
    error?: string;
    activity: Page<ActivityForFrontend>;
    loading: boolean;
}

export interface NotificationsReduxObject {
    redirectTo: string;
    items: Notification[];
    loading: boolean;
    error?: string;
}

export interface StatusReduxObject {
    title: string;
    page: SidebarPages;
    loading: boolean;
}

export interface SidebarReduxObject {
    pp: boolean;
    options: SidebarOption[];
    kcCount: number;
}

export interface HeaderSearchReduxObject {
    prioritizedSearch: HeaderSearchType;
    refresh?: () => void;
}

export type ActivityReduxObject = ComponentWithScroll<ActivityForFrontend, number> & ActivityFilter;

export type HeaderSearchType = "files" | "applications" | "projects";

export interface UploaderReduxObject {
    visible: boolean;
    path: string;
    allowMultiple: boolean;
    onFilesUploaded: () => void;
    error?: string;
    loading: boolean;
}

interface LegacyReducers {
    dashboard?: Reducer<DashboardStateProps>;
    uploader?: Reducer<UploaderReduxObject>;
    status?: Reducer<StatusReduxObject>;
    notifications?: Reducer<NotificationsReduxObject>;
    header?: Reducer<HeaderSearchReduxObject>;
    sidebar?: Reducer<SidebarReduxObject>;
    activity?: Reducer<ActivityReduxObject>;
}

/**
 * Global state created via useGlobal() similar to ReduxObject
 */
export interface HookStore {
    uploaderVisible?: boolean;
    uploads?: Upload[];
    uploadPath?: string;

    searchPlaceholder?: string;
    onSearch?: (query: string) => void;

    projectCache?: ProjectCache;
    projectManagementDetails?: APICallStateWithParams<UserInProject>;
    projectManagement?: APICallStateWithParams<Page<ProjectMember>>;
    projectManagementGroupMembers?: APICallStateWithParams<Page<string>, ListGroupMembersRequestProps>;
    projectManagementGroupSummary?: APICallStateWithParams<Page<GroupWithSummary>, PaginationRequest>;
    projectManagementQuery?: string;
    projectManagementOutgoingInvites?: APICallStateWithParams<Page<OutgoingInvite>, ListOutgoingInvitesRequest>;
    computeProducts?: APICallStateWithParams<Page<Product>>;
    storageProducts?: APICallStateWithParams<Page<Product>>;
    frameHidden?: boolean;
    cloudApiCache?: Record<string, { expiresAt: number, cached: any }>;
}

interface LegacyReduxObject {
    hookStore: HookStore;
    dashboard: DashboardStateProps;
    status: StatusReduxObject;
    notifications: NotificationsReduxObject;
    header: HeaderSearchReduxObject;
    sidebar: SidebarReduxObject;
    activity: ActivityReduxObject;
    simpleSearch: SimpleSearchStateProps;
    avatar: AvatarReduxObject;
    responsive?: ResponsiveReduxObject;
    project: ProjectRedux.State;
    loading?: boolean;
}

declare global {
    export type ReduxObject =
        LegacyReduxObject;
}

export const initActivity = (): ActivityReduxObject => ({
    loading: false
});

export const initNotifications = (): NotificationsReduxObject => ({
    items: [],
    loading: false,
    redirectTo: "",
    error: undefined
});

export const initHeader = (): HeaderSearchReduxObject => ({
    prioritizedSearch: "files"
});

export const initStatus = (): StatusReduxObject => ({
    title: "",
    page: SidebarPages.None,
    loading: false
});

export const initDashboard = (): DashboardStateProps => ({
    notifications: [],
});

export function initObject(): ReduxObject {
    return {
        hookStore: {},
        dashboard: initDashboard(),
        status: initStatus(),
        header: initHeader(),
        notifications: initNotifications(),
        sidebar: initSidebar(),
        activity: initActivity(),
        simpleSearch: initSimpleSearch(),
        avatar: initAvatar(),
        project: ProjectRedux.initialState,
        responsive: undefined,
    };
}

export type AvatarReduxObject = typeof defaultAvatar & { error?: string };
export const initAvatar = (): AvatarReduxObject => ({...defaultAvatar, error: undefined});

export const initSimpleSearch = (): SimpleSearchStateProps => ({
    errors: [],
    search: "",
});

export const initSidebar = (): SidebarReduxObject => ({
    pp: false,
    kcCount: 0,
    options: []
});

export function useSearch(onSearch: (query: string) => void) {
    const [, setOnSearch] = useGlobal("onSearch", doNothing);
    useEffect(() => {
        setOnSearch(() => onSearch);
        return () => {
            setOnSearch(() => doNothing);
        };
    }, [setOnSearch, onSearch]);
}

export function useSearchPlaceholder(searchPlaceholder: string) {
    const [, setSearchPlaceholder] = useGlobal("searchPlaceholder", "");
    useEffect(() => {
        setSearchPlaceholder(searchPlaceholder);
        return () => {
            setSearchPlaceholder("");
        };
    }, [setSearchPlaceholder, searchPlaceholder]);
}
