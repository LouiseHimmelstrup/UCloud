import {snackbarStore} from "@/Snackbar/SnackbarStore";
import * as UF from "@/UtilityFunctions";
import {ProjectName} from "@/Project";

/**
 * Used for resolving paths, which contain either "." or "..", and returning the resolved path.
 * @param path The current input path, which can include relative paths
 */
export function resolvePath(path: string): string {
    const components = path.split("/");
    const result: string[] = [];
    components.forEach(it => {
        if (it === "") {
            return;
        } else if (it === ".") {
            return;
        } else if (it === "..") {
            result.pop();
        } else {
            result.push(it);
        }
    });
    return "/" + result.join("/");
}

/**
 * Splits a path into components based on the divider '/', and filters away empty strings.
 * @param path to be split.
 * @returns every filtered component as a string array.
 */
export function pathComponents(path: string): string[] {
    return resolvePath(path).split("/").filter(it => it !== "");
}

interface IsInvalidPathname {
    path: string;
    filePaths: string[];
}

/**
 * Checks if a pathname is legal/already in use
 * @param {string} path The path being tested
 * @param {string[]} filePaths the other file paths path is being compared against
 * @returns whether or not the path is invalid
 */
export const isInvalidPathName = ({path, filePaths}: IsInvalidPathname): boolean => {
    if (["..", "/"].some((it) => path.includes(it))) {
        snackbarStore.addFailure("Folder name cannot contain '..' or '/'", false);
        return true;
    }
    if (path === "" || path === ".") {
        snackbarStore.addFailure("Folder name cannot be empty or be \".\"", false);
        return true;
    }
    const existingName = filePaths.some(it => it === path);
    if (existingName) {
        snackbarStore.addFailure("File with that name already exists", false);
        return true;
    }
    return false;
};

export const getParentPath = (path: string): string => {
    if (path.length === 0) return path;
    let splitPath = path.split("/");
    splitPath = splitPath.filter(p => p);
    let parentPath = "/";
    for (let i = 0; i < splitPath.length - 1; i++) {
        parentPath += splitPath[i] + "/";
    }
    // TODO(Jonas): Should be equivalent, let's test it for a while and replace if it works. */
    // TODO: They are not equivalent for the empty string. // and /, respectively.
    const parentP = UF.addTrailingSlash(`/${path.split("/").filter(it => it).slice(0, -1).join("/")}`);
    if (window.location.hostname === "localhost" && parentP !== parentPath) {
        throw Error("ParentP and path not equal");
    }
    return parentPath;
};

const goUpDirectory = (
    count: number,
    path: string
): string => count ? goUpDirectory(count - 1, getParentPath(path)) : path;

export const fileName = (path: string): string => {
    const lastSlash = path.lastIndexOf("/");
    if (lastSlash !== -1 && path.length > lastSlash + 1) {
        return path.substring(lastSlash + 1);
    } else {
        return path;
    }
};

export function getFilenameFromPath(path: string, projects: ProjectName[]): string {
    const baseName: string = fileName(path);

    if (baseName === "..") return `.. (${getFilenameFromPath(goUpDirectory(2, path), projects)})`;
    if (baseName === ".") return `. (Current folder)`;
    return baseName;
}

function isInt(value: number): boolean {
    if (isNaN(value)) {
        return false;
    }
    return (value | 0) === value;
}

export const sizeToString = (bytes: number | null): string => {
    if (bytes === null) return "";
    if (bytes < 0) return "Invalid size";
    const {size, unit} = sizeToHumanReadableWithUnit(bytes);

    if (isInt(size)) {
        return `${size} ${unit}`;
    } else {
        return `${size.toFixed(2)} ${unit}`;
    }
};

export function sizeToHumanReadableWithUnit(bytes: number): {size: number; unit: string} {
    if (bytes < 1000) {
        return {size: bytes, unit: "B"};
    } else if (bytes < 1000 ** 2) {
        return {size: (bytes / 1000), unit: "KB"};
    } else if (bytes < 1000 ** 3) {
        return {size: (bytes / 1000 ** 2), unit: "MB"};
    } else if (bytes < 1000 ** 4) {
        return {size: (bytes / 1000 ** 3), unit: "GB"};
    } else if (bytes < 1000 ** 5) {
        return {size: (bytes / 1000 ** 4), unit: "TB"};
    } else if (bytes < 1000 ** 6) {
        return {size: (bytes / 1000 ** 5), unit: "PB"};
    } else {
        return {size: (bytes / 1000 ** 6), unit: "EB"};
    }
}

export function readableUnixMode(unixPermissions: number): string {
    let result = "";
    if ((unixPermissions & (1 << 8)) != 0) result += "r";
    else result += "-";
    if ((unixPermissions & (1 << 7)) != 0) result += "w";
    else result += "-";
    if ((unixPermissions & (1 << 6)) != 0) result += "x";
    else result += "-";

    if ((unixPermissions & (1 << 5)) != 0) result += "r";
    else result += "-";
    if ((unixPermissions & (1 << 4)) != 0) result += "w";
    else result += "-";
    if ((unixPermissions & (1 << 3)) != 0) result += "x";
    else result += "-";

    if ((unixPermissions & (1 << 2)) != 0) result += "r";
    else result += "-";
    if ((unixPermissions & (1 << 1)) != 0) result += "w";
    else result += "-";
    if ((unixPermissions & (1 << 0)) != 0) result += "x";
    else result += "-";

    return result;
}