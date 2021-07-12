import * as UCloud from ".";
import * as React from "react";
import {accounting, BulkRequest, BulkResponse, PageV2, PaginationRequestV2} from ".";
import ProductReference = accounting.ProductReference;
import Product = accounting.Product;
import {buildQueryString} from "Utilities/URIUtilities";
import {SidebarPages} from "ui-components/Sidebar";
import {InvokeCommand} from "Authentication/DataHook";
import {Operation} from "ui-components/Operation";
import {dialogStore} from "Dialog/DialogStore";
import {ResourcePermissionEditor} from "Resource/PermissionEditor";
import {doNothing} from "UtilityFunctions";
import {bulkRequestOf} from "DefaultObjects";
import {DateRangeFilter, FilterWidgetProps, PillProps, TextFilter} from "Resource/Filter";
import {IconName} from "ui-components/Icon";
import * as H from "history";
import {Dispatch} from "redux";
import {ResourceProperties} from "Resource/Properties";

export interface ProductSupport {
    product: ProductReference;
}

export interface ResolvedSupport<P extends UCloud.accounting.Product = UCloud.accounting.Product, S extends ProductSupport = ProductSupport> {
    product: P;
    support: S;
}

export type ResourceStatus = UCloud.provider.ResourceStatus;
export type ResourceUpdate = UCloud.provider.ResourceUpdate;
export type ResourceOwner = UCloud.provider.ResourceOwner;
export type ResourceSpecification = UCloud.provider.ResourceSpecification;

export type Permission = "READ" | "EDIT" | "ADMIN";

export type AclEntity =
    { type: "project_group", projectId: string, group: string } |
    { type: "user", username: string };

export interface ResourceAclEntry {
    entity: AclEntity;
    permissions: Permission[];
}

export interface ResourcePermissions {
    myself: Permission[];
    others?: ResourceAclEntry[];
}

export interface ResourceIncludeFlags {
    includeOthers?: boolean;
    includeSupport?: boolean;
    includeUpdates?: boolean;
}

export interface UpdatedAcl {
    id: string;
    added: ResourceAclEntry[];
    deleted: AclEntity[];
}

export interface Resource<Update extends ResourceUpdate = ResourceUpdate,
    Status extends ResourceStatus = ResourceStatus,
    Spec extends ResourceSpecification = ResourceSpecification> {
    id: string;
    createdAt: number;
    specification: Spec;
    status: Status;
    updates: Update[];
    owner: ResourceOwner;
    permissions: ResourcePermissions;
}

export interface FindById {
    id: string;
}

export interface SupportByProvider {
    productsByProvider: Record<string, ResolvedSupport[]>;
}

export interface ResourceBrowseCallbacks<Res extends Resource> {
    commandLoading: boolean;
    invokeCommand: InvokeCommand;
    reload: () => void;
    api: ResourceApi<Res, never>;
    isCreating: boolean;
    startCreation?: () => void;
    viewProperties?: (res: Res) => void;
    closeProperties?: () => void;
    onSelect?: (resource: Res) => void;
    embedded: boolean;
    dispatch: Dispatch;
    startRenaming?: (resource: Res, defaultValue: string) => void;
}

export interface SortFlags {
    sortBy?: string;
    sortDirection?: "ascending" | "descending";
}

export interface SortEntry {
    icon: IconName;
    title: string;
    column: string;
    helpText?: string;
}

export abstract class ResourceApi<Res extends Resource,
    Prod extends Product,
    Spec extends ResourceSpecification = ResourceSpecification,
    Update extends ResourceUpdate = ResourceUpdate,
    Flags extends ResourceIncludeFlags = ResourceIncludeFlags,
    Status extends ResourceStatus = ResourceStatus,
    Support extends ProductSupport = ProductSupport> {
    protected namespace: string;
    protected baseContext: string;

    public abstract routingNamespace;
    public abstract title: string;
    public abstract page: SidebarPages;

    public filterWidgets: React.FunctionComponent<FilterWidgetProps>[] = [];
    public filterPills: React.FunctionComponent<PillProps>[] = [];
    public sortEntries: SortEntry[] = [
        {
            icon: "calendar",
            title: "Date created",
            column: "createdAt",
            helpText: "Date and time of initial creation"
        },
        {
            icon: "user",
            title: "Created by",
            column: "createdBy",
            helpText: "The user who initially created the resource"
        }
    ];

    public registerFilter([w, p]: [React.FunctionComponent<FilterWidgetProps>, React.FunctionComponent<PillProps>]) {
        this.filterWidgets.push(w);
        this.filterPills.push(p);
    }

    public idIsUriEncoded: boolean = false;

    public InlineTitleRenderer?: React.FunctionComponent<{ resource: Res }>;
    public IconRenderer?: React.FunctionComponent<{ resource: Res | null; size: string; }>
    public StatsRenderer?: React.FunctionComponent<{ resource: Res }>;
    public TitleRenderer?: React.FunctionComponent<{ resource: Res }>;
    public ImportantStatsRenderer?: React.FunctionComponent<{ resource: Res }>;
    public Properties: React.FunctionComponent<{
        resource?: Res;
        reload?: () => void;
        closeProperties?: () => void;
        api: ResourceApi<Res, Prod, Spec, Update, Flags, Status, Support>;
        embedded?: boolean;
    }> = (props) => <ResourceProperties {...props}/>

    protected constructor(namespace: string) {
        this.namespace = namespace;
        this.baseContext = "/api/" + namespace.replace(".", "/") + "/";

        this.registerFilter(TextFilter("user", "filterCreatedBy", "Created by"));
        this.registerFilter(DateRangeFilter("calendar", "Date created", "filterCreatedBefore", "filterCreatedAfter"));
        // TODO We need to add a pill for provider and product
    }

    public retrieveOperations(): Operation<Res, ResourceBrowseCallbacks<Res>>[] {
        return [
            {
                text: "Back to " + this.titlePlural.toLowerCase(),
                primary: true,
                icon: "backward",
                enabled: (selected, cb) => cb.closeProperties != null,
                onClick: (selected, cb) => {
                    cb.closeProperties!();
                }
            },
            {
                text: "Use",
                primary: true,
                enabled: (selected, cb) => selected.length === 1 && cb.onSelect !== undefined,
                canAppearInLocation: loc => loc === "IN_ROW",
                onClick: (selected, cb) => {
                    cb.onSelect!(selected[0]);
                }
            },
            {
                text: "Create " + this.title.toLowerCase(),
                icon: "upload",
                color: "blue",
                primary: true,
                canAppearInLocation: loc => loc !== "IN_ROW",
                enabled: (selected, cb) => {
                    if (selected.length !== 0 || cb.startCreation == null) return false;
                    if (cb.isCreating) return "You are already creating a " + this.title.toLowerCase();
                    return true;
                },
                onClick: (selected, cb) => cb.startCreation!(),
                tag: CREATE_TAG
            },
            {
                text: "Permissions",
                icon: "share",
                enabled: (selected, cb) => selected.length === 1 && selected[0].owner.project != null
                    && cb.viewProperties != null,
                onClick: (selected, cb) => {
                    if (!cb.embedded) {
                        dialogStore.addDialog(
                            <ResourcePermissionEditor reload={cb.reload} entity={selected[0]} api={cb.api}/>,
                            doNothing,
                            true
                        );
                    } else {
                        cb.viewProperties!(selected[0]);
                    }
                },
                tag: PERMISSIONS_TAG
            },
            {
                text: "Delete",
                icon: "trash",
                color: "red",
                confirm: true,
                enabled: (selected) => selected.length >= 1,
                onClick: async (selected, cb) => {
                    await cb.invokeCommand(cb.api.remove(bulkRequestOf(...selected.map(it => ({id: it.id})))));
                    cb.reload();
                    cb.closeProperties?.();
                },
                tag: DELETE_TAG
            },
            {
                text: "Properties",
                icon: "properties",
                enabled: (selected, cb) => selected.length === 1 && cb.viewProperties != null,
                onClick: (selected, cb) => {
                    cb.viewProperties!(selected[0]);
                },
                tag: PROPERTIES_TAG
            }
        ];
    }

    public get titlePlural(): string {
        if (this.title.endsWith("s")) return this.title + "es";
        return this.title + "s";
    }

    browse(req: PaginationRequestV2 & Flags & SortFlags): APICallParameters<PaginationRequestV2 & Flags, PageV2<Res>> {
        return {
            context: "",
            method: "GET",
            path: buildQueryString(this.baseContext + "browse", req),
            parameters: req
        };
    }

    retrieve(req: FindById & Flags): APICallParameters<FindById & Flags, Res> {
        return {
            context: "",
            method: "GET",
            path: buildQueryString(this.baseContext + "retrieve", req),
            parameters: req
        };
    }

    create(req: BulkRequest<Spec>): APICallParameters<BulkRequest<Spec>, BulkResponse<{} | null>> {
        return {
            context: "",
            method: "POST",
            path: this.baseContext,
            payload: req,
            parameters: req
        };
    }

    remove(req: BulkRequest<FindById>): APICallParameters<BulkRequest<FindById>, BulkResponse<{} | null>> {
        return {
            context: "",
            method: "DELETE",
            path: this.baseContext,
            payload: req,
            parameters: req
        };
    }

    retrieveProducts(): APICallParameters<{}, SupportByProvider> {
        return {
            context: "",
            method: "GET",
            path: this.baseContext + "retrieveProducts",
        };
    }

    updateAcl(req: BulkRequest<UpdatedAcl>): APICallParameters<BulkRequest<UpdatedAcl>, BulkResponse<{} | null>> {
        return {
            context: "",
            method: "POST",
            path: this.baseContext + "updateAcl",
            payload: req,
            parameters: req
        };
    }

    search(
        req: {query: string; flags: Flags; } & PaginationRequestV2 & SortFlags
    ): APICallParameters<{query: string; flags: Flags; } & PaginationRequestV2, PageV2<Res>> {
        return {
            context: "",
            method: "POST",
            path: this.baseContext + "search",
            payload: req,
            parameters: req
        };
    }
}

export const PERMISSIONS_TAG = "permissions";
export const DELETE_TAG = "delete";
export const PROPERTIES_TAG = "properties";
export const CREATE_TAG = "create";