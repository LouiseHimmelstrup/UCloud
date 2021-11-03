import {
    DELETE_TAG, findSupport, PERMISSIONS_TAG,
    ProductSupport, Resource,
    ResourceApi, ResourceBrowseCallbacks,
    ResourceIncludeFlags,
    ResourceSpecification,
    ResourceStatus,
    ResourceUpdate
} from "./ResourceApi";
import {SidebarPages} from "@/ui-components/Sidebar";
import {Icon} from "@/ui-components";
import * as React from "react";
import * as H from "history";
import {buildQueryString} from "@/Utilities/URIUtilities";
import {ItemRenderer} from "@/ui-components/Browse";
import {ProductStorage, UCLOUD_PROVIDER} from "@/Accounting";
import {BulkRequest} from "@/UCloud/index";
import {apiUpdate} from "@/Authentication/DataHook";
import {Operation} from "@/ui-components/Operation";

export type FileCollection = Resource<FileCollectionUpdate, FileCollectionStatus, FileCollectionSpecification>;

export type FileCollectionUpdate = ResourceUpdate;
export type FileCollectionStatus = ResourceStatus;
export interface FileCollectionSpecification extends ResourceSpecification {
    title: string;
}
export type FileCollectionFlags = ResourceIncludeFlags;
export interface FileCollectionSupport extends ProductSupport {
    stats: {
        sizeInBytes?: boolean;
        sizeIncludingChildrenInBytes?: boolean;

        modifiedAt?: boolean;
        createdAt?: boolean;
        accessedAt?: boolean;

        unixPermissions?: boolean;
        unixOwner?: boolean;
        unixGroup?: boolean;
    },

    collection: {
        aclSupported?: boolean;
        aclModifiable?: boolean;

        usersCanCreate?: boolean;
        usersCanDelete?: boolean;
        usersCanRename?: boolean;

        searchSupported?: boolean;
    },

    files: {
        aclSupported?: boolean;
        aclModifiable?: boolean;

        trashSupported?: boolean;
        isReadOnly?: boolean;
    }
}

class FileCollectionsApi extends ResourceApi<FileCollection, ProductStorage, FileCollectionSpecification,
    FileCollectionUpdate, FileCollectionFlags, FileCollectionStatus, FileCollectionSupport> {
    routingNamespace = "drives";
    title = "Drive";
    page = SidebarPages.Files;
    productType = "STORAGE" as const;

    renderer: ItemRenderer<FileCollection> = {
        MainTitle({resource}) {return <>{resource?.specification?.title ?? ""}</>},
        Icon({resource, size}) {
            if (resource && resource.specification.product.id === "share" &&
                resource.specification.product.provider === UCLOUD_PROVIDER) {
                return <Icon name={"ftSharesFolder"} size={size} color={"FtFolderColor"} color2={"FtFolderColor2"} />
            }
            return <Icon name={"ftFileSystem"} size={size} />
        }
    };

    constructor() {
        super("files.collections");

        this.sortEntries.push({
            icon: "id",
            title: "Name",
            column: "title",
            helpText: "Name of the drive, for example: Research data"
        });
    }

    retrieveOperations(): Operation<FileCollection, ResourceBrowseCallbacks<FileCollection>>[] {
        const baseOperations = super.retrieveOperations();
        const permissions = baseOperations.find(it => it.tag === PERMISSIONS_TAG);
        if (permissions) {
            const enabled = permissions.enabled;
            permissions.enabled = (selected, cb, all) => {
                const isEnabled = enabled(selected, cb, all);
                if (isEnabled !== true) return isEnabled;
                const support = findSupport(cb.supportByProvider, selected[0])?.support as FileCollectionSupport;
                if (!support) return false;
                if (support.collection.aclModifiable !== true) {
                    return false;
                }
                return true;
            };
        }
        const deleteOperation = baseOperations.find(it => it.tag === DELETE_TAG);
        if (deleteOperation) {
            const enabled = deleteOperation.enabled;
            deleteOperation.enabled = (selected, cb, all) => {
                if (selected.find(it => it.specification.product.id === "share")) return false
                const isEnabled = enabled(selected, cb, all);
                if (isEnabled !== true) return isEnabled;
                const support = findSupport(cb.supportByProvider, selected[0])?.support as FileCollectionSupport;
                if (!support) return false;
                if (support.collection.usersCanDelete !== true) {
                    return "The provider does not allow you to delete this drive";
                }
                return true;
            };
        }
        return [
            {
                text: "Rename",
                icon: "rename",
                enabled: (selected, cb) => {
                    const support = selected.length > 0 &&
                        findSupport<FileCollectionSupport>(cb.supportByProvider, selected[0])?.support;

                    return selected.length === 1 && cb.startRenaming != null && !!support &&
                        support.collection.usersCanRename === true;
                },
                onClick: (selected, cb) => {
                    cb.startRenaming!(selected[0], selected[0].specification.title);
                }
            },
            ...baseOperations
        ]
    }

    navigateToChildren(history: H.History, resource: FileCollection) {
        history.push(buildQueryString("/files", {path: `/${resource.id}`}))
    }

    rename(request: BulkRequest<{id: string; newTitle: string;}>): APICallParameters {
        return apiUpdate(request, this.baseContext, "rename");
    }
}

const api = new FileCollectionsApi();
export {api};
