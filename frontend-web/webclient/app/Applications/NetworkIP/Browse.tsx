import * as React from "react";
import {default as NetworkIPApi, NetworkIP} from "@/UCloud/NetworkIPApi";
import {ResourceBrowse} from "@/Resource/Browse";
import {ResourceTab, ResourceTabOptions} from "@/Resource/ResourceTabs";
import {BrowseType} from "@/Resource/BrowseType";

export const NetworkIPBrowse: React.FunctionComponent<{
    provider?: string;
    onSelect?: (selection: NetworkIP) => void;
    isSearch?: boolean;
    browseType?: BrowseType;
}> = props => {
    const browseType = props.browseType ?? BrowseType.MainContent;
    return <ResourceBrowse
        api={NetworkIPApi}
        onSelect={props.onSelect}
        onInlineCreation={(text, product, cb) => ({
            product: {id: product.name, category: product.category.name, provider: product.category.provider},
        })}
        header={
            browseType === BrowseType.MainContent ? (
                <ResourceTab active={ResourceTabOptions.PUBLIC_IP} />) : undefined
        }
        headerSize={48}
        inlineCreationMode={"NONE"}
        browseType={browseType}
        isSearch={props.isSearch}
    />;
};
