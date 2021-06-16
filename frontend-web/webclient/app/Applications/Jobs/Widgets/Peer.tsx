import * as React from "react";
import * as UCloud from "UCloud";
import {widgetId, WidgetProps, WidgetSetter, WidgetValidator} from "./index";
import {compute} from "UCloud";
import ApplicationParameterNS = compute.ApplicationParameterNS;
import Flex from "ui-components/Flex";
import {useState} from "react";
import Box from "ui-components/Box";
import styled from "styled-components";
import Input from "ui-components/Input";
import Label from "ui-components/Label";
import AppParameterValueNS = compute.AppParameterValueNS;
import {emptyPage} from "DefaultObjects";
import {useCloudAPI} from "Authentication/DataHook";

interface PeerProps extends WidgetProps {
    parameter: UCloud.compute.ApplicationParameterNS.Peer;
}

export const PeerParameter: React.FunctionComponent<PeerProps> = props => {
    const error = props.errors[props.parameter.name] != null;
    return <Flex mb={8}>
        <div>
            <Label>
                Hostname
                <Input
                    placeholder={"Example: spark-cluster"}
                    id={widgetId(props.parameter) + "name"}
                    value={props.parameter.title.length !== 0 ? props.parameter.name : undefined}
                    disabled={props.parameter.title.length !== 0}
                />
            </Label>
        </div>

        <Box flexGrow={1} ml={2}>
            <Label>
                Job
            </Label>
            <JobSelector
                parameter={props.parameter}
                suggestedApplication={props.parameter.suggestedApplication}
                error={error}
            />
        </Box>
    </Flex>;
};

export const PeerValidator: WidgetValidator = (param) => {
    if (param.type === "peer") {
        const nameElem = findElementName(param);
        const jobElem = findElementJob(param);
        if (nameElem === null || jobElem === null) return {valid: true};
        if (nameElem.value === "" && jobElem.value === "") return {valid: true};

        if (nameElem.value === "" || jobElem.value === "") {
            return {valid: false, message: "All fields must be filled out."};
        }

        return {valid: true, value: {type: "peer", jobId: jobElem.value, hostname: nameElem.value}};
    }

    return {valid: true};
};

export const PeerSetter: WidgetSetter = (param, value) => {
    if (param.type !== "peer") return;

    const name = findElementName(param);
    const job = findElementJob(param);
    if (name === null || job === null) throw "Missing element for: " + param;

    const peerValue = value as AppParameterValueNS.Peer;
    name.value = peerValue.hostname;
    job.value = peerValue.jobId;
};

function findElementName(param: ApplicationParameterNS.Peer): HTMLInputElement | null {
    return document.getElementById(widgetId(param) + "name") as HTMLInputElement | null;
}

function findElementJob(param: ApplicationParameterNS.Peer): HTMLInputElement | null {
    return document.getElementById(widgetId(param) + "job") as HTMLInputElement | null;
}

interface JobSelectorProps {
    parameter: ApplicationParameterNS.Peer;
    suggestedApplication?: string;
    error: boolean;
}

const JobSelector: React.FunctionComponent<JobSelectorProps> = props => {
    const [selectedPeer, setSelectedPeer] = useState<string>("");
    const [allowAutoConfigure, setAllowAutoConfigure] = useState<boolean>(true);

    const [suggestedApplicationApi] = useCloudAPI<Page<UCloud.compute.Job>>(
        props.suggestedApplication ?
            UCloud.compute.apps.findByName({appName: props.suggestedApplication, itemsPerPage: 50, page: 0}) :
            {noop: true},
        {...emptyPage, itemsPerPage: -1}
    );

    const [suggestedApplication] = suggestedApplicationApi.data.items;

    React.useEffect(() => {
        if (props.suggestedApplication === null && allowAutoConfigure) {
            setAllowAutoConfigure(false);
        }
    }, [props.suggestedApplication, allowAutoConfigure]);

    return null;
    /*
    return (
        <ControlledJobSelector
            hasSelectedJob={selectedPeer != null}
            suggestedApplication={suggestedApplication ? {
                name: suggestedApplication.specification.application.name,
                version: suggestedApplication.specification.application.version
            } : undefined}
            allowAutoConfigure={allowAutoConfigure}
            onSelect={job => {
                setSelectedPeer(job.id);
                setAllowAutoConfigure(false);
            }}
            trigger={
                <Input
                    id={widgetId(props.parameter) + "job"}
                    style={{height: "39px"}}
                    value={selectedPeer}
                    placeholder="No selected job"
                    readOnly
                />
            }
        />
    );
    */
};

export const PointerInput = styled(Input)`
    cursor: pointer;
`;
