import * as React from "react";
import {LinkInfo, SidebarLinkColumn} from "@/ui-components/SidebarLink";
import {useProjectId, useProjectIdFromParams} from "./Api";
import AppRoutes from "@/Routes";

export function ProjectLinks(): JSX.Element {
    const activeProjectId = useProjectId();
    const paramsProjectId = useProjectIdFromParams();

    const links: LinkInfo[] = React.useMemo(() => {
        const active = paramsProjectId.length > 0 ? paramsProjectId : activeProjectId ?? "My Workspace";
        const isPersonalWorkspace = !activeProjectId;

        const result: LinkInfo[] = [];
        result.push({
            to: AppRoutes.project.members(active),
            text: "Members",
            icon: "projects",
            disabled: isPersonalWorkspace,
        })
        result.push({
            to: AppRoutes.project.usage(active),
            text: "Resource Usage",
            icon: "projects"
        });
        result.push({
            to: AppRoutes.project.allocations(active),
            text: "Resource Allocations",
            icon: "projects",
        });
        result.push({
            to: AppRoutes.project.grants(active),
            text: "Grant Applications",
            icon: "projects",
        });
        result.push({
            to: AppRoutes.project.settings(active),
            text: "Settings",
            icon: "projects",
            disabled: isPersonalWorkspace
        });
        result.push({
            to: AppRoutes.project.subprojects(active),
            text: "Subprojects",
            icon: "projects",
            disabled: isPersonalWorkspace
        });
        return result;
    }, [activeProjectId, paramsProjectId]);
    return <SidebarLinkColumn links={links} />
}