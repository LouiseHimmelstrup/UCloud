import * as React from "react";
import {default as JobApi, Job} from "@/UCloud/JobsApi";
import {BaseResourceBrowseProps, ResourceBrowse} from "@/Resource/Browse";
import {ResourceRouter} from "@/Resource/Router";
import Create from "@/Applications/Jobs/Create";
import {useHistory} from "react-router";
import {BrowseType} from "@/Resource/BrowseType";

export const JobBrowse: React.FunctionComponent<BaseResourceBrowseProps<Job>> = props => {
    const history = useHistory();
    return <ResourceBrowse api={JobApi} {...props} browseType={props.browseType ?? BrowseType.MainContent}
        extraCallbacks={{
            startCreation() {history.push("/applications/overview")}
        }}
    />;
}

const JobRouter: React.FunctionComponent = () => {
    return <ResourceRouter api={JobApi} Browser={JobBrowse} Create={Create} />;
}

export default JobRouter;
