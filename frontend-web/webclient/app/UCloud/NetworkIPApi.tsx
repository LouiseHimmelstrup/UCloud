import * as React from "react";
import {
    ProductSupport,
    Resource,
    ResourceApi, ResourceIncludeFlags,
    ResourceSpecification,
    ResourceStatus,
    ResourceUpdate
} from "UCloud/ResourceApi";
import {BulkRequest, compute} from "UCloud/index";
import {SidebarPages} from "ui-components/Sidebar";
import {Icon} from "ui-components";
import {EnumFilter} from "Resource/Filter";
import {JobBinding} from "UCloud/JobsApi";
import PortRangeAndProto = compute.PortRangeAndProto;
import {ResourceProperties} from "Resource/Properties";
import {FirewallEditor} from "Applications/NetworkIP/FirewallEditor";
import {ItemRenderer} from "ui-components/Browse";
import {ProductNetworkIP} from "Accounting";

export interface NetworkIPSpecification extends ResourceSpecification {
    firewall?: Firewall;
}

export interface Firewall {
    openPorts: PortRangeAndProto[];
}

export type NetworkIPState = "READY" | "PREPARING" | "UNAVAILABLE";

export interface NetworkIPStatus extends ResourceStatus {
    boundTo: string[];
    state: NetworkIPState;
    ipAddress?: string;
}

export interface NetworkIPSupport extends ProductSupport {
    binding?: JobBinding;
}

export interface NetworkIPUpdate extends ResourceUpdate {
    state?: NetworkIPState;
    didBind: boolean;
    newBinding?: string;
}

export interface NetworkIPFlags extends ResourceIncludeFlags {
    filterState?: string;
}

export type NetworkIP = Resource<NetworkIPUpdate, NetworkIPStatus, NetworkIPSpecification>;

export interface FirewallAndId {
    id: string;
    firewall: Firewall;
}

class NetworkIPApi extends ResourceApi<NetworkIP, ProductNetworkIP, NetworkIPSpecification, NetworkIPUpdate,
    NetworkIPFlags, NetworkIPStatus, NetworkIPSupport> {
    routingNamespace = "public-ips";
    title = "Public IP";
    page = SidebarPages.Runs;
    productType = "NETWORK_IP" as const;

    renderer: ItemRenderer<NetworkIP> = {
        MainTitle({resource}) {
            return !resource ? <>Public IP</> :
                <>{resource.status.ipAddress ?? resource.id.toString()}</>
        },
        Icon({size}) {return <Icon name={"networkWiredSolid"} size={size} />}
    };

    Properties = (props) => {
        return <ResourceProperties
            api={this}
            {...props}
            ContentChildren={(p) => (
                <FirewallEditor inspecting={p.resource as NetworkIP} reload={p.reload} />
            )}
        />;
    };

    constructor() {
        super("networkips");

        this.registerFilter(EnumFilter(
            "radioEmpty",
            "filterState",
            "Status",
            [
                {
                    title: "Preparing",
                    value: "PREPARING",
                    icon: "hashtag"
                },
                {
                    title: "Ready",
                    value: "READY",
                    icon: "hashtag"
                },
                {
                    title: "Unavailable",
                    value: "UNAVAILABLE",
                    icon: "hashtag"
                }
            ]
        ));
    }

    updateFirewall(request: BulkRequest<FirewallAndId>): APICallParameters<BulkRequest<FirewallAndId>> {
        return {
            context: "",
            method: "POST",
            path: this.baseContext + "/firewall",
            parameters: request,
            payload: request
        };
    }
}

export default new NetworkIPApi();
