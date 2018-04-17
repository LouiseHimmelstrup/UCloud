import * as React from "react";
import * as swal from "sweetalert2";
import { RightsMap, RightsNameMap, SensitivityLevelMap } from "./DefaultObjects";
import { File, Acl } from "./types/types";
import Cloud from "../authentication/lib";

interface Type { type: string }
export const NotificationIcon = ({ type }: Type) => {
    if (type === "Complete") {
        return (<div className="initial32 bg-green-500">✓</div>)
    } else if (type === "In Progress") {
        return (<div className="initial32 bg-blue-500">...</div>)
    } else if (type === "Pending") {
        return (<div className="initial32 bg-blue-500" />)
    } else if (type === "Failed") {
        return (<div className="initial32 bg-red-500">&times;</div>)
    } else {
        return (<div>Unknown type</div>)
    }
};

export const toLowerCaseAndCapitalize = (str: string): string => !str ? "" : str.charAt(0).toUpperCase() + str.toLowerCase().slice(1);

export const WebSocketSupport = () =>
    !("WebSocket" in window) ?
        (<h3>
            <small>WebSockets are not supported in this browser. Notifications won't be updated automatically.
            </small>
        </h3>) : null;


export function sortByNumber<T>(list: T[], name: string, asc: boolean): T[] {
    list.sort((a: any, b: any) => (Number(a[name]) - (Number(b[name]))) * (asc ? -1 : 1));
    return list;
}

export function sortByString<T>(list: T[], name: string, asc: boolean): T[] {
    list.sort((a: any, b: any) => ((a[name] as string).localeCompare(b[name] as string)) * (asc ? 1 : -1));
    return list;
}

export const sortFilesByTypeAndName = (files: File[], asc: boolean) => {
    const order = asc ? 1 : -1;
    files.sort((a: File, b: File) => {
        if (a.type === "DIRECTORY" && b.type !== "DIRECTORY")
            return -1 * order;
        else if (b.type === "DIRECTORY" && a.type !== "DIRECTORY")
            return 1 * order;
        else {
            return getFilenameFromPath(a.path).localeCompare(getFilenameFromPath(b.path)) * order;
        }
    });
    return files;
};

export const sortFilesBySensitivity = (files: File[], asc: boolean) => {
    let order = asc ? 1 : -1;
    files.sort((a, b) => {
        return SensitivityLevelMap[a.sensitivityLevel] - SensitivityLevelMap[b.sensitivityLevel] * order;
    });
    return files;
};

export const favorite = (files: File[], path: string, cloud: Cloud) => {
    let file = files.find((file: File) => file.path === path);
    file.favorited = !file.favorited;
    if (file.favorited) {
        cloud.post(`/files/favorite?path=${file.path}`);
    } else {
        cloud.delete(`/files/favorite?path=${file.path}`);
    }
    return files;
};

export const getOwnerFromAcls = (acls: Acl[], cloud: Cloud) => {
    const userName: string = cloud.username;
    const result: Acl = acls.find((acl: Acl) => acl.entity.displayName === userName);
    if (!result) {
        return "None"
    } else if (acls.length > 1) {
        return `${acls.length} members`;
    } else {
        return "You";
    }
};

export const updateSharingOfFile = (filePath: string, user: string, currentRights: string, cloud: Cloud, callback: () => any) => {
    swal({
        title: "Please specify access level",
        text: `The file ${getFilenameFromPath(filePath)} is to be shared with ${user}.`,
        input: "select",
        showCancelButton: true,
        showCloseButton: true,
        inputOptions: {
            "READ": "Read Access",
            "READ_WRITE": "Read/Write Access",
            //"OWN": "Own the file"
        },
        inputValidator: (value: string) => {
            return currentRights === value && `${user} already has ${RightsNameMap[value]} access.`
        }
    }).then((type: any) => {
        if (type.dismiss) {
            return;
        }
        const body = {
            entity: user,
            onFile: filePath,
            rights: type.value,
            type: "grant",
        };
        cloud.put("/acl", body).then(() => {
            swal("Success!", `The file has been shared with ${user}`, "success").then(() => callback ? callback() : null);
        });
    });
};

export const shareFile = (filePath: string, cloud: Cloud, callback: Function) => {
    swal({
        title: "Share file",
        text: `Enter a username to share ${getFilenameFromPath(filePath)} with.`,
        input: "text",
        confirmButtonText: "Next",
        showCancelButton: true,
        showCloseButton: true,
        inputValidator: (value: string) => {
            return !value && 'Please enter a username'
        }
    }).then((input: any) => {
        if (input.dismiss) {
            return;
        }
        swal({
            title: "Please specify access level",
            text: `The file ${getFilenameFromPath(filePath)} is to be shared with ${input.value}.`,
            input: "select",
            showCancelButton: true,
            showCloseButton: true,
            inputOptions: {
                "READ": "Read Access",
                "READ_WRITE": "Read/Write Access",
                //"OWN": "Own the file"
            },
        }).then((type: any) => {
            if (type.dismiss) {
                return;
            }
            const body = {
                entity: input.value,
                onFile: filePath,
                rights: type.value,
                type: "grant",
            };
            cloud.put("/acl", body).then((response: any) => {
                swal("Success!", `The file has been shared with ${input.value}`, "success").then(() => callback ? callback() : null);
            });
        });
    }
    );
}

export const revokeSharing = (filePath: string, person: string, rightsLevel: string, cloud: Cloud) =>
    console.warn("Revoking of sharing must be rewritten");
    /*
    swal({
        title: "Revoke access",
        text: `Revoke ${rightsLevel} access for ${person}`,
    }).then((input: any) => {
        if (input.dismiss) {
            return;
        }
        const body = {
            onFile: filePath,
            entity: person,
            type: "revoke",
        };

        return cloud.delete("/acl", body);//.then(response => {

        //});
    });*/

export const renameFile = (filePath: string) =>
    swal({
        title: "Rename file",
        text: `The file ${getFilenameFromPath(filePath)} will be renamed`,
        confirmButtonText: "Rename",
        input: "text",
        showCancelButton: true,
        showCloseButton: true,
    }).then((result: any) => {
        if (result.dismiss) {
            return;
        }
    });

export const showFileDeletionPrompt = (filePath: string, cloud: Cloud, callback: () => void) =>
    swal({
        title: "Delete file",
        text: `Delete file ${getFilenameFromPath(filePath)}`,
        confirmButtonText: "Delete file",
        type: "warning",
        showCancelButton: true,
        showCloseButton: true,
    }).then((result: any) => {
        if (result.dismiss) {
            return;
        } else {
            cloud.delete("/files", { path: filePath}).then(() => callback ? callback() : null);
        }
    });

export const getParentPath = (path: string): string => {
    if (!path) {
        return "";
    }
    let splitPath = path.split("/");
    splitPath = splitPath.filter(path => path);
    let parentPath = "/";
    for (let i = 0; i < splitPath.length - 1; i++) {
        parentPath += splitPath[i] + "/";
    }
    return parentPath;
};

export const getFilenameFromPath = (path: string): string => 
    !path ? "" : path.split("/").pop();


export const downloadFile = (path: string, cloud: Cloud) =>
    cloud.createOneTimeTokenWithPermission("downloadFile,irods").then((token: string) => {
        let link = document.createElement("a");
        window.location.href = "/api/files/download?path=" + encodeURI(path) + "&token=" + encodeURI(token);
        link.setAttribute("download", "");
        link.click();
    });

export const fileSizeToString = (bytes: number): string => {
    if (!bytes) { return ""; }
    if (bytes < 1000) {
        return `${bytes} B`;
    } else if (bytes < 1000 ** 2) {
        return `${bytes / 1000} KB`;
    } else if (bytes < 1000 ** 3) {
        return `${bytes / 1000 ** 2} MB`;
    } else if (bytes < 1000 ** 4) {
        return `${bytes / 1000 ** 3} GB`;
    } else if (bytes < 1000 ** 5) {
        return `${bytes / 1000 ** 4} TB`;
    } else if (bytes < 1000 ** 6) {
        return `${bytes / 1000 ** 5} PB`;
    } else if (bytes < 1000 ** 7) {
        return `${bytes / 1000 ** 6} EB`;
    } else {
        return `${bytes} B`;
    }
};

export const getCurrentRights = (files: File[], cloud: Cloud) => {
    let lowestPrivilegeOptions = RightsMap["OWN"];
    files.forEach((it) => {
        it.acl.filter((acl: Acl) => acl.entity.displayName === cloud.username).forEach((acl: Acl) => {
            lowestPrivilegeOptions = Math.min(RightsMap[acl.right], lowestPrivilegeOptions);
        });
    });
    return {
        rightsName: Object.keys(RightsMap)[lowestPrivilegeOptions],
        rightsLevel: lowestPrivilegeOptions
    }
};

interface LastSorting { name: string, asc: boolean }
export const getSortingIcon = (lastSorting: LastSorting, name: string): string => {
    if (lastSorting.name === name) {
        return lastSorting.asc ? "ion-chevron-down" : "ion-chevron-up";
    }
    return "";
};

export const createRange = (count: number): number[] => {
    let range = [];
    for (let i = 0; i < count; i++) {
        range.push(i);
    }
    return range;
};

export const createRangeInclusive = (count: number): number[] => {
    let range = [];
    for (let i = 0; i <= count; i++) {
        range.push(i);
    }
    return range;
};

export const getTypeFromFile = (filename: string): string => {
    const extension = filename.split(".").pop();
    switch (extension) {
        case "kt":
        case "js":
        case "jsx":
        case "ts":
        case "tsx":
        case "java":
        case "py":
        case "tex":
        case "r":
        case "c":
        case "cc":
        case "c++":
        case "h++":
        case "cpp":
        case "h":
        case "hh":
        case "hpp":
        case "html":
        case "sql":
            return "ion-code";
        case "png":
        case "gif":
        case "tiff":
        case "eps":
        case "ppm":
            return "ion-image";
        case "txt":
        case "pdf":
        case "xml":
        case "json":
        case "csv":
        case "yml":
            return "ion-document";
        case "wav":
        case "mp3":
            return "ion-android-volume-up";
        default:
            console.warn(`Unhandled extension "${extension}"`)
            return "";
    }
}

export const inRange = (status: number, min: number, max: number): boolean => status >= min && status <= max;
export const inSuccessRange = (status: number): boolean => inRange(status, 200, 299);

export const shortUUID = (uuid: string): string => uuid.substring(0, 8).toUpperCase();
