import { Cloud } from "Authentication/SDUCloudObject";
import { PayloadAction, Page } from "Types";
import { LoadableEvent, unwrapCall } from "LoadableContent";
import { resourceName } from "./AccountingObject";
import * as API from "../api";
import { buildQueryString } from "Utilities/URIUtilities";

export enum Tag {
    RECEIVE_CHART = "ACCOUNTING_RECEIVE_CHART",
    RECEIVE_EVENTS = "ACCOUNTING_RECEIVE_EVENTS",
    RECEIVE_USAGE = "ACCOUNTING_RECEIVE_USAGE"
}

export type Type = ReceiveChart | ReceiveEvents | ReceiveUsage;
type ReceiveChart = PayloadAction<typeof Tag.RECEIVE_CHART, { resource: string, event: LoadableEvent<API.ChartResponse> }>;
type ReceiveEvents = PayloadAction<typeof Tag.RECEIVE_EVENTS, { resource: string, event: LoadableEvent<Page<API.AccountingEvent>> }>;
type ReceiveUsage = PayloadAction<typeof Tag.RECEIVE_USAGE, { resource: string, event: LoadableEvent<API.Usage> }>;

export const fetchChart = async (resource: string, subResource: string): Promise<ReceiveChart> => ({
    type: Tag.RECEIVE_CHART,
    payload: {
        resource: resourceName(resource, subResource),
        event: await unwrapCall(
            Cloud.get(`/accounting/${resource}/${subResource}/chart`)
        )
    }
});

export const fetchEvents = async (
    resource: string, 
    subResource: string,
    itemsPerPage: number = 10,
    page: number = 0
): Promise<ReceiveEvents> => ({
    type: Tag.RECEIVE_EVENTS,
    payload: {
        resource: resourceName(resource, subResource),
        event: await unwrapCall(
            Cloud.get(
                buildQueryString(
                    `/accounting/${resource}/${subResource}/events`,
                    {
                        itemsPerPage,
                        page
                    }
                )
            )
        )
    }
});

export const fetchUsage = async (resource: string, subResource: string): Promise<ReceiveUsage> => ({
    type: Tag.RECEIVE_USAGE,
    payload: {
        resource: resourceName(resource, subResource),
        event: await unwrapCall(
            Cloud.get(`/accounting/${resource}/${subResource}/usage`)
        )
    }
});