import * as React from "react";
import {api as FileCollectionsApi, FileCollection, FileCollectionSupport} from "@/UCloud/FileCollectionsApi";
import {BrowseType, ResourceBrowse} from "@/Resource/Browse";
import {ResourceRouter} from "@/Resource/Router";
import {useCallback} from "react";
import {ResolvedSupport, ResourceBrowseCallbacks} from "@/UCloud/ResourceApi";
import {bulkRequestOf} from "@/DefaultObjects";

export const FileCollectionBrowse: React.FunctionComponent<{
    onSelect?: (selection: FileCollection) => void;
    isSearch?: boolean;
    browseType: BrowseType;
}> = props => {
    const onRename = useCallback(async (text: string, res: FileCollection, cb: ResourceBrowseCallbacks<FileCollection>) => {
        await cb.invokeCommand(FileCollectionsApi.rename(bulkRequestOf({
            id: res.id,
            newTitle: text
        })));
    }, []);

    const productFilterForCreate = useCallback((product: ResolvedSupport) => {
        const support = product.support as FileCollectionSupport;
        if (support.collection.usersCanCreate !== true) {
            return false;
        }
        return true;
    }, []);

    return <ResourceBrowse
        api={FileCollectionsApi}
        onSelect={props.onSelect}
        onRename={onRename}
        browseType={props.browseType}
        onInlineCreation={((text, product, cb) => ({
                product: {id: product.name, category: product.category.name, provider: product.category.provider},
                title: text
            })
        )}
        productFilterForCreate={productFilterForCreate}
        navigateToChildren={FileCollectionsApi.navigateToChildren}
        isSearch={props.isSearch}
    />;
};

const Router: React.FunctionComponent = () => {
    return <ResourceRouter api={FileCollectionsApi} Browser={FileCollectionBrowse}/>;
};

export default Router;
